
import os
import json
import mysql.connector
import snowflake.connector


# ==========================================================
# 1. CONFIGURATION: MYSQL SOURCE AND SNOWFLAKE DESTINATION
# ==========================================================

MYSQL_CONFIG = {
    "host": os.getenv("MYSQL_HOST", "localhost"),
    "port": int(os.getenv("MYSQL_PORT", "3306")),
    "user": os.getenv("MYSQL_USER"),
    "password": os.getenv("MYSQL_PASSWORD"),
    "database": os.getenv("MYSQL_DATABASE", "data_contract_db")
}

SNOWFLAKE_CONFIG = {
    "account": os.getenv("SNOWFLAKE_ACCOUNT"),
    "user": os.getenv("SNOWFLAKE_USER"),
    "password": os.getenv("SNOWFLAKE_PASSWORD"),
    "authenticator": "snowflake",
    "warehouse": os.getenv("SNOWFLAKE_WAREHOUSE", "SIVA_WAREHOUSE"),
    "database": "CAP34_DB",
    "schema": "GOVERNANCE",
    "enable_server_session_keep_alive_auto_detection": False
}


# ==========================================================
# 2. ETL TABLE CONFIGURATION
# ==========================================================

TABLES = [
    {
        "source": "datasets",
        "target": "DATASET_METADATA",
        "key": "DATASET_ID",
        "columns": [
            "DATASET_ID",
            "DATASET_NAME",
            "PRODUCER_NAME",
            "DESCRIPTION",
            "CREATED_AT"
        ],
        "select": """
            SELECT dataset_id, dataset_name, producer_name,
                   description, created_at
            FROM datasets
        """,
        "json_columns": []
    },

    {
        "source": "contract_versions",
        "target": "CONTRACT_VERSION_HISTORY",
        "key": "CONTRACT_ID",
        "columns": [
            "CONTRACT_ID",
            "DATASET_ID",
            "VERSION",
            "SCHEMA_DEFINITION",
            "STATUS",
            "CREATED_AT"
        ],
        "select": """
            SELECT contract_id, dataset_id, version,
                   schema_definition, status, created_at
            FROM contract_versions
        """,
        "json_columns": ["SCHEMA_DEFINITION"]
    },

    {
        "source": "contract_fields",
        "target": "CONTRACT_FIELD_HISTORY",
        "key": "FIELD_ID",
        "columns": [
            "FIELD_ID",
            "CONTRACT_ID",
            "FIELD_NAME",
            "DATA_TYPE",
            "IS_NULLABLE",
            "IS_REQUIRED"
        ],
        "select": """
            SELECT field_id, contract_id, field_name,
                   data_type, is_nullable, is_required
            FROM contract_fields
        """,
        "json_columns": []
    },

    {
        "source": "consumers",
        "target": "CONSUMER_METADATA",
        "key": "CONSUMER_ID",
        "columns": [
            "CONSUMER_ID",
            "CONSUMER_NAME",
            "CONSUMER_TYPE",
            "OWNER",
            "STATUS",
            "CREATED_AT"
        ],
        "select": """
            SELECT consumer_id, consumer_name, consumer_type,
                   owner, status, created_at
            FROM consumers
        """,
        "json_columns": []
    },

    {
        "source": "consumer_dependencies",
        "target": "CONSUMER_DEPENDENCIES",
        "key": "DEPENDENCY_ID",
        "columns": [
            "DEPENDENCY_ID",
            "CONSUMER_ID",
            "DATASET_ID",
            "FIELD_NAME",
            "USAGE_TYPE"
        ],
        "select": """
            SELECT dependency_id, consumer_id, dataset_id,
                   field_name, usage_type
            FROM consumer_dependencies
        """,
        "json_columns": []
    },

    {
        "source": "validation_runs",
        "target": "VALIDATION_HISTORY",
        "key": "VALIDATION_ID",
        "columns": [
            "VALIDATION_ID",
            "DATASET_ID",
            "CONTRACT_ID",
            "BASE_CONTRACT_ID",
            "VALIDATION_TYPE",
            "SOURCE_FILE",
            "RESULT",
            "ERROR_SUMMARY",
            "CREATED_AT",
            "COMPLETED_AT"
        ],
        "select": """
            SELECT validation_id, dataset_id, contract_id,
                   base_contract_id, validation_type, source_file,
                   result, error_summary, created_at, completed_at
            FROM validation_runs
        """,
        "json_columns": []
    },

    {
        "source": "drift_details",
        "target": "DRIFT_REPORT",
        "key": "DRIFT_ID",
        "columns": [
            "DRIFT_ID",
            "VALIDATION_ID",
            "FIELD_NAME",
            "CHANGE_TYPE",
            "OLD_VALUE",
            "NEW_VALUE",
            "SEVERITY",
            "REASON"
        ],
        "select": """
            SELECT drift_id, validation_id, field_name,
                   change_type, old_value, new_value,
                   severity, reason
            FROM drift_details
        """,
        "json_columns": []
    }
]


# ==========================================================
# 3. HELPER: CONVERT MYSQL JSON VALUES
# ==========================================================

def prepare_value(value, column):

    if column in ("SCHEMA_DEFINITION",):
        if isinstance(value, (dict, list)):
            return json.dumps(value)

        if value is not None:
            # Check that the JSON string is valid
            json.loads(value)

    return value


# ==========================================================
# 4. EXTRACT DATA FROM MYSQL
# ==========================================================

def extract_records(mysql_cursor, table):

    mysql_cursor.execute(table["select"])

    records = mysql_cursor.fetchall()

    prepared_records = []

    for record in records:

        prepared_record = tuple(
            prepare_value(value, column)
            for column, value in zip(table["columns"], record)
        )

        prepared_records.append(prepared_record)

    return prepared_records


# ==========================================================
# 5. LOAD DATA INTO SNOWFLAKE STAGING TABLE
# ==========================================================


def load_staging(sf_cursor, table, records):

    target = table["target"]
    columns = table["columns"]
    json_columns = table["json_columns"]

    staging = f"STG_{target}"

    # Create staging table.
    # JSON columns are temporarily stored as VARCHAR.
    if json_columns:

        select_columns = []

        for col in columns:
            if col in json_columns:
                select_columns.append(
                    f"{col}::VARCHAR AS {col}"
                )
            else:
                select_columns.append(col)

        select_clause = ", ".join(select_columns)

        sf_cursor.execute(f"""
            CREATE OR REPLACE TEMPORARY TABLE {staging} AS
            SELECT {select_clause}
            FROM {target}
            WHERE 1 = 0
        """)

    else:
        sf_cursor.execute(f"""
            CREATE OR REPLACE TEMPORARY TABLE {staging}
            LIKE {target}
        """)

    if not records:
        print("No records found. Staging table is empty.")
        return staging

    column_list = ", ".join(columns)
    placeholders = ", ".join(["%s"] * len(columns))

    insert_sql = f"""
        INSERT INTO {staging} ({column_list})
        VALUES ({placeholders})
    """

    sf_cursor.executemany(insert_sql, records)

    return staging



# ==========================================================
# 6. MERGE STAGING DATA INTO FINAL SNOWFLAKE TABLE
# ==========================================================


def merge_records(sf_cursor, table, staging):

    target = table["target"]
    columns = table["columns"]
    key = table["key"]
    json_columns = table["json_columns"]

    update_columns = [
        col for col in columns if col != key
    ]

    update_clause = ", ".join(
        f"target.{col} = source.{col}"
        for col in update_columns
    )

    column_list = ", ".join(columns)

    source_values = ", ".join(
        f"source.{col}" for col in columns
    )

    # Convert JSON text into Snowflake VARIANT
    # while selecting from the staging table.
    source_expressions = []

    for col in columns:
        if col in json_columns:
            source_expressions.append(
                f"PARSE_JSON({col}) AS {col}"
            )
        else:
            source_expressions.append(col)

    source_select = ", ".join(source_expressions)

    merge_sql = f"""
        MERGE INTO {target} target

        USING (
            SELECT {source_select}
            FROM {staging}
        ) source

        ON target.{key} = source.{key}

        WHEN MATCHED THEN UPDATE SET
            {update_clause}

        WHEN NOT MATCHED THEN INSERT (
            {column_list}
        )
        VALUES (
            {source_values}
        )
    """

    sf_cursor.execute(merge_sql)


 


# ==========================================================
# 7. VERIFY SNOWFLAKE RECORD COUNTS
# ==========================================================

def verify_records(sf_cursor, target):

    sf_cursor.execute(f"""
        SELECT COUNT(*)
        FROM {target}
    """)

    return sf_cursor.fetchone()[0]


# ==========================================================
# 8. MAIN ETL PIPELINE
# ==========================================================

def run_etl_pipeline():

    mysql_conn = None
    snowflake_conn = None

    total_tables = len(TABLES)
    successful_tables = 0

    print("\n========================================")
    print("       CAP-34 GOVERNANCE ETL")
    print("========================================\n")

    try:

        # Connect to MySQL
        print("Connecting to MySQL...")

        mysql_conn = mysql.connector.connect(**MYSQL_CONFIG)

        mysql_cursor = mysql_conn.cursor()

        print("MySQL connected successfully!\n")

        # Connect to Snowflake
        print("Connecting to Snowflake...")

        snowflake_conn = snowflake.connector.connect(
            **SNOWFLAKE_CONFIG
        )

        sf_cursor = snowflake_conn.cursor()

        print("Snowflake connected successfully!\n")

        # Execute all seven table transfers
        for index, table in enumerate(TABLES, start=1):

            source = table["source"]
            target = table["target"]

            print("----------------------------------------")
            print(f"ETL {index}/{total_tables}: {source}")
            print("----------------------------------------")

            # Extract
            records = extract_records(mysql_cursor, table)

            print(f"Extracted records: {len(records)}")

            # Load staging
            staging = load_staging(
                sf_cursor,
                table,
                records
            )

            print("Staging load completed.")

            # Merge into destination
            merge_records(
                sf_cursor,
                table,
                staging
            )

            print("MERGE completed.")

            # Verify destination
            count = verify_records(
                sf_cursor,
                target
            )

            print(f"Snowflake target: {target}")
            print(f"Total target records: {count}")

            successful_tables += 1

            print(f"ETL status: SUCCESS\n")

        # Commit after all table transfers
        snowflake_conn.commit()

        print("========================================")
        print("        ETL PIPELINE COMPLETED")
        print("========================================")
        print(f"Successful tables: {successful_tables}")
        print(f"Total tables: {total_tables}")

    except Exception as error:

        print("\n========================================")
        print("             ETL FAILED")
        print("========================================")
        print(f"Error: {error}")

        if snowflake_conn:
            snowflake_conn.rollback()

        raise

    finally:

        if mysql_conn and mysql_conn.is_connected():
            mysql_conn.close()

        if snowflake_conn:
            snowflake_conn.close()

        print("\nDatabase connections closed.")


# ==========================================================
# 9. EXECUTION ENTRY POINT
# ==========================================================

if __name__ == "__main__":
    run_etl_pipeline()
