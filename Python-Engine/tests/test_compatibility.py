import json
from pathlib import Path
from compatibility.engine import compare_contracts
ROOT = Path(__file__).resolve().parents[1]
def load(name): return json.loads((ROOT/"schemas"/name).read_text(encoding="utf-8"))
def test_optional_field_addition_passes():
    assert compare_contracts(load("orders_v1.json"),load("orders_v1_1_optional.json"))["compatible"]
def test_breaking_changes_fail():
    result=compare_contracts(load("orders_v1.json"),load("orders_v1_1_breaking.json"))
    assert not result["compatible"]
    kinds={x["change_type"] for x in result["breaking_changes"]}
    assert "TYPE_CHANGED" in kinds and "REQUIRED_FIELD_ADDED" in kinds
def test_removed_field_fails():
    old={"fields":[{"name":"id","type":"integer","required":True,"nullable":False}]}
    new={"fields":[]}
    assert not compare_contracts(old,new)["compatible"]
