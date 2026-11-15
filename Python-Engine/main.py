import json
from pathlib import Path
from typing import Any
from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from pydantic import BaseModel, Field
from compatibility.engine import compare_contracts
from validation.validator import validate_payload
from fastapi import Request


ROOT = Path(__file__).resolve().parent
app = FastAPI(title="CAP-34 Python Engine", version="1.0.0")

@app.post("/debug")
async def debug_request(request: Request):
    raw_body = await request.body()
    print(f"Debug request received: {request.method} {request.url}")
    print(raw_body)

    return {
        "content_type": request.headers.get("content-type"),
        "body_length": len(raw_body),
        "body": raw_body.decode("utf-8", errors="replace")
    }

class CompatibilityRequest(BaseModel):
    old_contract: dict[str, Any]
    new_contract: dict[str, Any]

class PayloadRequest(BaseModel):
    contract: dict[str, Any]
    payload: list[dict[str, Any]] = Field(min_length=1)

@app.get("/health")
def health():
    return {"status": "UP", "service": "cap34-python-engine"}

@app.post("/api/compatibility/check")
def check_compatibility(request: CompatibilityRequest):
    return compare_contracts(request.old_contract, request.new_contract)

@app.post("/api/validation/json")
def validate_json(request: PayloadRequest):
    return validate_payload(request.payload, request.contract)

@app.post("/api/validation/file")
async def validate_file(file: UploadFile = File(...), contract: str = Form(...)):
    try:
        schema = json.loads(contract)
    except json.JSONDecodeError as exc:
        raise HTTPException(400, f"Invalid contract JSON: {exc.msg}")
    suffix = Path(file.filename or "").suffix.lower()
    if suffix not in {".csv", ".json"}:
        raise HTTPException(400, "Only CSV and JSON files are supported")
    raw = await file.read()
    try:
        if suffix == ".csv":
            import csv, io
            rows = list(csv.DictReader(io.StringIO(raw.decode("utf-8-sig"))))
        else:
            parsed = json.loads(raw.decode("utf-8-sig"))
            rows = parsed if isinstance(parsed, list) else parsed.get("records", [])
    except (UnicodeDecodeError, json.JSONDecodeError, AttributeError) as exc:
        raise HTTPException(400, f"Could not read uploaded file: {exc}")
    if not isinstance(rows, list) or not rows:
        raise HTTPException(400, "File must contain at least one data record")
    result = validate_payload(rows, schema)
    result["source_file"] = file.filename
    return result

@app.get("/api/schemas/{schema_name}")
def get_schema(schema_name: str):
    path = ROOT / "schemas" / schema_name
    if path.suffix != ".json" or not path.is_file():
        raise HTTPException(404, "Schema not found")
    return json.loads(path.read_text(encoding="utf-8"))
