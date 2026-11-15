
import os
import mysql.connector
import snowflake.connector

# MySQL connection
mysql_conn = mysql.connector.connect(
    host=os.getenv("MYSQL_HOST"),
    port=int(os.getenv("MYSQL_PORT", "3306")),
    user=os.getenv("MYSQL_USER"),
    password=os.getenv("MYSQL_PASSWORD"),
    database=os.getenv("MYSQL_DATABASE")
)

print("MySQL connection successful!")


snowflake_conn = snowflake.connector.connect(
    account=os.getenv("SNOWFLAKE_ACCOUNT"),
    user=os.getenv("SNOWFLAKE_USER"),
    password=os.getenv("SNOWFLAKE_PASSWORD"),
    authenticator="snowflake",
    warehouse=os.getenv("SNOWFLAKE_WAREHOUSE"),
    database="CAP34_DB",
    schema="GOVERNANCE",
    enable_server_session_keep_alive_auto_detection=False
)

print("Snowflake connection successful!")


print("Snowflake connection successful!")

# Verify target database and schema
cursor = snowflake_conn.cursor()

cursor.execute("""
    SELECT CURRENT_DATABASE(), CURRENT_SCHEMA(), CURRENT_WAREHOUSE()
""")

print("Snowflake context:", cursor.fetchone())

cursor.close()
mysql_conn.close()
snowflake_conn.close()

print("Both connections closed successfully.")
