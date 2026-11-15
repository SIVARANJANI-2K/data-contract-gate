
from compatibility.engine import compare_contracts


def make_contract(version, fields):
    return {
        "version": version,
        "fields": fields
    }


def test_unchanged_contract_is_compatible():
    old = make_contract("1.0.0", [
        {"name": "order_id", "type": "integer",
         "required": True, "nullable": False}
    ])

    new = make_contract("1.1.0", [
        {"name": "order_id", "type": "integer",
         "required": True, "nullable": False}
    ])

    result = compare_contracts(old, new)

    assert result["compatible"] is True
    assert result["breaking_changes"] == []


def test_removed_field_is_breaking():
    old = make_contract("1.0.0", [
        {"name": "order_id", "type": "integer"},
        {"name": "amount", "type": "decimal"}
    ])

    new = make_contract("1.1.0", [
        {"name": "order_id", "type": "integer"}
    ])

    result = compare_contracts(old, new)

    assert result["compatible"] is False
    assert any(
        change["change_type"] == "FIELD_REMOVED"
        for change in result["breaking_changes"]
    )


def test_type_change_is_breaking():
    old = make_contract("1.0.0", [
        {"name": "amount", "type": "decimal"}
    ])

    new = make_contract("1.1.0", [
        {"name": "amount", "type": "string"}
    ])

    result = compare_contracts(old, new)

    assert result["compatible"] is False
    assert any(
        change["change_type"] == "TYPE_CHANGED"
        for change in result["breaking_changes"]
    )


def test_required_field_addition_is_breaking():
    old = make_contract("1.0.0", [
        {"name": "order_id", "type": "integer"}
    ])

    new = make_contract("1.1.0", [
        {"name": "order_id", "type": "integer"},
        {"name": "amount", "type": "decimal",
         "required": True}
    ])

    result = compare_contracts(old, new)

    assert result["compatible"] is False
    assert any(
        change["change_type"] == "REQUIRED_FIELD_ADDED"
        for change in result["breaking_changes"]
    )


def test_optional_field_addition_is_compatible():
    old = make_contract("1.0.0", [
        {"name": "order_id", "type": "integer"}
    ])

    new = make_contract("1.1.0", [
        {"name": "order_id", "type": "integer"},
        {"name": "order_status", "type": "string",
         "required": False, "nullable": True}
    ])

    result = compare_contracts(old, new)

    assert result["compatible"] is True
    assert any(
        change["change_type"] == "OPTIONAL_FIELD_ADDED"
        for change in result["compatible_changes"]
    )
