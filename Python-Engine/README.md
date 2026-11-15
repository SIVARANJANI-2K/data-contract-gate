# CAP-34 Python Engine

Python REST service for contract compatibility and CSV/JSON payload validation.

## Setup on Windows (PowerShell)
```powershell
py -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install --upgrade pip
pip install -r requirements.txt
```
If PowerShell blocks activation, use Command Prompt:
```cmd
py -m venv .venv
.venv\Scripts\activate.bat
pip install -r requirements.txt
```

## Start service
```powershell
uvicorn main:app --reload --host 127.0.0.1 --port 8001
```
Swagger UI: http://127.0.0.1:8001/docs
Health: http://127.0.0.1:8001/health

## Endpoints
- `POST /api/compatibility/check` — JSON body with `old_contract` and `new_contract`.
- `POST /api/validation/json` — JSON body with `contract` and `payload` array.
- `POST /api/validation/file` — multipart form: `file` (CSV/JSON) and `contract` (contract JSON string).
- `GET /api/schemas/orders_v1.json` — retrieve sample contract.

## Compatibility example
```json
{
  "old_contract": {
    "version":"1.0.0",
    "fields":[
      {"name":"order_id","type":"integer","required":true,"nullable":false},
      {"name":"amount","type":"decimal","required":true,"nullable":false}
    ]
  },
  "new_contract": {
    "version":"1.1.0",
    "fields":[
      {"name":"order_id","type":"integer","required":true,"nullable":false},
      {"name":"amount","type":"decimal","required":true,"nullable":false},
      {"name":"city","type":"string","required":false,"nullable":true}
    ]
  }
}
```
Expected: `compatible: true`.

## Run tests
```powershell
pytest -q
```

## Policy implemented
- Removed field: breaking.
- Type change: breaking.
- Previously non-nullable field becoming nullable: breaking.
- Newly added required field: breaking.
- Added optional field: compatible if old consumers tolerate unknown fields.
- Nullable becoming non-nullable: schema-compatible, but actual payloads still need validation.

Spring Boot should call this service on port 8001 and remain responsible for MySQL metadata, workflow orchestration, validation history and Snowflake loading. This Python service does not directly connect to either database.
