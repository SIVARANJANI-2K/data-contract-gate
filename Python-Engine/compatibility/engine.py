from typing import Any


def _fields(contract: dict[str, Any]) -> dict[str, dict[str, Any]]:
    fields = contract.get("fields")

    # Support dictionary format used by MySQL schema_definition
    if isinstance(fields, dict):
        result = {}

        for name, details in fields.items():
            if not isinstance(details, dict):
                raise ValueError(f"Invalid definition for field: {name}")

            result[name] = {
                **details,
                "name": name
            }

        return result

    # Also support the list format
    if isinstance(fields, list):
        result = {}

        for field in fields:
            if not isinstance(field, dict) or not field.get("name"):
                raise ValueError("Each field needs a non-empty name")

            name = field["name"]

            if name in result:
                raise ValueError(f"Duplicate field: {name}")

            result[name] = field

        return result

    raise ValueError(
        "Contract must contain a fields object or fields array"
    )


def compare_contracts(old_contract: dict[str, Any], new_contract: dict[str, Any],
                      allow_unknown_fields: bool = True) -> dict[str, Any]:
    """Backward-compatibility check: old consumers continue against new producer schema."""
    old, new = _fields(old_contract), _fields(new_contract)
    breaking, compatible = [], []
    for name, old_field in old.items():
        new_field = new.get(name)
        if new_field is None:
            breaking.append({"field": name, "change_type": "FIELD_REMOVED",
                "old_value": old_field.get("type"), "new_value": None,
                "reason": "Existing consumers may depend on this field."})
            continue
        old_type = str(old_field.get("type", "")).lower()
        new_type = str(new_field.get("type", "")).lower()
        if old_type != new_type:
            breaking.append({"field": name, "change_type": "TYPE_CHANGED",
                "old_value": old_type, "new_value": new_type,
                "reason": "Changing a field type can break consumers."})
        if not bool(old_field.get("nullable", False)) and bool(new_field.get("nullable", False)):
            breaking.append({"field": name, "change_type": "NULLABILITY_RELAXED",
                "old_value": False, "new_value": True,
                "reason": "Consumers may now receive null for a previously non-null field."})
        elif bool(old_field.get("nullable", False)) and not bool(new_field.get("nullable", False)):
            compatible.append({"field": name, "change_type": "NULLABILITY_RESTRICTED",
                "old_value": True, "new_value": False,
                "reason": "Schema is stricter; validate actual payloads."})
    for name, field in new.items():
        if name in old:
            continue
        if bool(field.get("required", True)):
            breaking.append({"field": name, "change_type": "REQUIRED_FIELD_ADDED",
                "old_value": None, "new_value": field.get("type"),
                "reason": "Existing producers may not provide this required field."})
        elif allow_unknown_fields:
            compatible.append({"field": name, "change_type": "OPTIONAL_FIELD_ADDED",
                "old_value": None, "new_value": field.get("type"),
                "reason": "Assumes old consumers tolerate unknown fields."})
        else:
            breaking.append({"field": name, "change_type": "OPTIONAL_FIELD_ADDED",
                "old_value": None, "new_value": field.get("type"),
                "reason": "Consumer policy rejects unknown fields."})
    return {"compatible": not breaking, "old_version": old_contract.get("version"),
            "new_version": new_contract.get("version"), "breaking_changes": breaking,
            "compatible_changes": compatible,
            "summary": "Contract is backward-compatible." if not breaking
                       else f"Contract contains {len(breaking)} breaking change(s)."}
