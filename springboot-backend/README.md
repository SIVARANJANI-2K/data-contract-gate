# CAP-34 Spring Boot Backend

A starter backend for the CAP-34 Data Contract Drift & Producer-Consumer Compatibility Gate.

## Requirements
- JDK 17 or newer
- Maven 3.9+
- MySQL Server running
- Existing database `data_contract_db` with the seven CAP-34 tables

## 1. Configure database credentials
Edit `src/main/resources/application.properties` and replace `CHANGE_ME` with your MySQL root password.

Alternatively, set environment variables before running:
```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_mysql_password"
$env:DB_URL="jdbc:mysql://localhost:3306/data_contract_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata"
```

## 2. Check schema compatibility
This project uses `spring.jpa.hibernate.ddl-auto=validate`. It will NOT create or alter your tables.
The entity mappings expect these table/column names:
- datasets: dataset_id, dataset_name, producer_name, description, created_at
- contract_versions: contract_id, dataset_id, version, schema_definition, status, created_at
- contract_fields: field_id, contract_id, field_name, data_type, is_nullable, is_required
- consumers: consumer_id, consumer_name, consumer_type, owner, status, created_at
- consumer_dependencies: dependency_id, consumer_id, dataset_id, field_name, usage_type
- validation_runs: validation_id, dataset_id, contract_id, base_contract_id, validation_type, source_file, result, error_summary, created_at, completed_at
- drift_details: drift_id, validation_id, field_name, change_type, old_value, new_value, severity, reason

If your actual table definitions differ, update the entity mappings to match them. Do not switch to `ddl-auto=update` on an existing database.

## 3. Run
From this folder:
```powershell
mvn spring-boot:run
```
Backend URL: http://localhost:8080

## 4. API examples

### Register dataset
POST `/api/datasets`
```json
{"datasetName":"orders","producerName":"OrderService","description":"Order transaction data"}
```

### Register contract (fields are automatically inserted into contract_fields)
POST `/api/contracts`
```json
{
  "datasetId":1,
  "version":"1.0.0",
  "fields":[
    {"name":"order_id","type":"integer","required":true,"nullable":false},
    {"name":"customer_name","type":"string","required":true,"nullable":false},
    {"name":"amount","type":"decimal","required":true,"nullable":false}
  ]
}
```
The contract is created as DRAFT. It is not automatically approved or compatibility-checked.

### List contracts for dataset
GET `/api/contracts/dataset/1`

### List extracted contract fields
GET `/api/contracts/1/fields`

### Change contract status
PATCH `/api/contracts/1/status?value=APPROVED`

### Register consumer
POST `/api/consumers`
```json
{"consumerName":"PowerBI_Sales_Dashboard","consumerType":"POWER_BI","owner":"Analytics Team"}
```

### Register dependency
POST `/api/dependencies`
```json
{"consumerId":1,"datasetId":1,"fieldName":"amount","usageType":"SUM"}
```

### Find consumers depending on a field
GET `/api/dependencies/impact?datasetId=1&fieldName=amount`

### Record validation run
POST `/api/validations`
```json
{"datasetId":1,"contractId":1,"baseContractId":null,"validationType":"PAYLOAD_VALIDATION","sourceFile":"uploads/incoming/orders.csv","result":"PENDING","errorSummary":null}
```

### Record drift detail
POST `/api/drifts`
```json
{"validationId":1,"fieldName":"amount","changeType":"TYPE_CHANGED","oldValue":"decimal","newValue":"string","severity":"CRITICAL","reason":"Consumer aggregation expects numeric data"}
```

### Upload CSV or JSON (stores locally only at this stage)
POST `/api/files/upload` as multipart form-data:
- `file`: select CSV or JSON
- `datasetId`: dataset ID

## Important implementation boundaries
- Python compatibility and payload validation engines are not called yet.
- File upload stores files under `./uploads/incoming`; it does not validate, quarantine or load into Snowflake yet.
- Consumer impact endpoint currently finds registered consumers depending on a specific dataset field. A later integration step will store impact results against each validation.
- Add authentication/authorization before exposing this API outside localhost.
- Do not expose the MySQL root account to a deployed application; create a least-privilege database user for deployment.
