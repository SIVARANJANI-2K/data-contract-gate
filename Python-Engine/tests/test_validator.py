import json
from pathlib import Path
from validation.validator import validate_payload
CONTRACT=json.loads((Path(__file__).resolve().parents[1]/"schemas"/"orders_v1.json").read_text())
def test_valid_payload_passes():
    rows=[{"order_id":101,"customer_name":"Ravi","amount":"500.50","order_date":"2026-11-01"}]
    assert validate_payload(rows,CONTRACT)["valid"]
def test_invalid_payload_fails():
    rows=[{"order_id":"x","customer_name":"","amount":"INVALID","order_date":"bad"}]
    result=validate_payload(rows,CONTRACT)
    assert not result["valid"] and result["error_count"] >= 3
