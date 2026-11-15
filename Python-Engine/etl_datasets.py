
import os
import mysql.connector
import snowflake.connector

def transfer_datasets():

    mysql_conn = None
    snowflake_conn = None

    try:
        # 1. Connect to MySQL
        mysql_conn = mysql.connector.connect(
            host=os.getenv("MYSQL_HOST", "localhost"),
            port=int(os.getenv("MYSQL_PORT", "3306")),
            user=os.getenv("MYSQL_USER"),
            password=os.getenv("MYSQL_PASSWORD"),
            database=os.getenv("MYSQL_DATABASE")
        )

        print("Connected to MySQL")

        mysql_cursor = mysql_conn.cursor()

        # 2. Fetch dataset records
        mysql_cursor.execute("""
            SELECT
                dataset_id,
                dataset_name,
                producer_name,
                description,
                created_at
            FROM datasets
        """)

        records = mysql_cursor.fetchall()

        print(f"Fetched {len(records)} dataset records")

        # 3. Connect to Snowflake
        snowflake_conn = snowflake.connector.connect(
            account=os.getenv("SNOWFLAKE_ACCOUNT"),
            user=os.getenv("SNOWFLAKE_USER"),
            password=os.getenv("SNOWFLAKE_PASSWORD"),
            authenticator="snowflake",
            warehouse=os.getenv("SNOWFLAKE_WAREHOUSE"),
            database="CAP34_DB",
            schema="GOVERNANCE"
        )

        print("Connected to Snowflake")

        sf_cursor = snowflake_conn.cursor()

        # 4. Create temporary staging table
        sf_cursor.execute("""
            CREATE OR REPLACE TEMPORARY TABLE STG_DATASET_METADATA
            LIKE DATASET_METADATA
        """)

        # 5. Insert MySQL records into staging table
        if records:
            sf_cursor.executemany("""
                INSERT INTO STG_DATASET_METADATA (
                    DATASET_ID,
                    DATASET_NAME,
                    PRODUCER_NAME,
                    DESCRIPTION,
                    CREATED_AT
                )
                VALUES (%s, %s, %s, %s, %s)
            """, records)

        # 6. Merge staging records into target table
        sf_cursor.execute("""
            MERGE INTO DATASET_METADATA target
            USING STG_DATASET_METADATA source
            ON target.DATASET_ID = source.DATASET_ID

            WHEN MATCHED THEN UPDATE SET
                target.DATASET_NAME = source.DATASET_NAME,
                target.PRODUCER_NAME = source.PRODUCER_NAME,
                target.DESCRIPTION = source.DESCRIPTION,
                target.CREATED_AT = source.CREATED_AT

            WHEN NOT MATCHED THEN INSERT (
                DATASET_ID,
                DATASET_NAME,
                PRODUCER_NAME,
                DESCRIPTION,
                CREATED_AT
            )
            VALUES (
                source.DATASET_ID,
                source.DATASET_NAME,
                source.PRODUCER_NAME,
                source.DESCRIPTION,
                source.CREATED_AT
            )
        """)

        snowflake_conn.commit()

        print("Dataset ETL completed successfully!")

        sf_cursor.execute("""
            SELECT COUNT(*)
            FROM DATASET_METADATA
        """)

        print("Total records in Snowflake:", sf_cursor.fetchone()[0])

        sf_cursor.close()
        mysql_cursor.close()

    except Exception as e:
        print("ETL failed:", str(e))
        raise

    finally:
        if mysql_conn and mysql_conn.is_connected():
            mysql_conn.close()

        if snowflake_conn:
            snowflake_conn.close()


if __name__ == "__main__":
    transfer_datasets()
