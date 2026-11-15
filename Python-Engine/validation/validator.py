
from datetime import date, datetime
from decimal import Decimal, InvalidOperation

ALIASES = {
    "int": "integer",
    "long": "integer",
    "float": "decimal",
    "double": "decimal",
    "number": "decimal",
    "bool": "boolean",
    "varchar": "string",
    "text": "string"
}


def _type_name(value):
    raw = str(value or "").lower()
    return ALIASES.get(raw, raw)


def _empty(value):
    return value is None or (
        isinstance(value, str) and value.strip() == ""
    )


def _valid_type(value, expected):
    kind = _type_name(expected)

    if kind == "string":
        return isinstance(value, str)

    if kind == "integer":
        if isinstance(value, bool):
            return False
        try:
            text = str(value).strip()
            return bool(text) and str(int(text)) == text
        except (ValueError, TypeError):
            return False

    if kind == "decimal":
        if isinstance(value, bool):
            return False
        try:
            Decimal(str(value).strip())
            return True
        except (InvalidOperation, ValueError, TypeError):
            return False

    if kind == "boolean":
        return (
            isinstance(value, bool)
            or str(value).strip().lower()
            in {"true", "false", "0", "1", "yes", "no"}
        )

    if kind == "date":
        try:
            date.fromisoformat(str(value).strip())
            return True
        except (ValueError, TypeError):
            return False

    if kind == "datetime":
        try:
            datetime.fromisoformat(
                str(value).strip().replace("Z", "+00:00")
            )
            return True
        except (ValueError, TypeError):
            return False

    return False


def validate_payload(
    records,
    contract,
    reject_unexpected_fields=True
):
    fields = contract.get("fields")

    # Contract fields must be an object/dictionary
    if not isinstance(fields, dict):
        raise ValueError(
            "Contract must contain a fields object"
        )

    if not records:
        raise ValueError(
            "Payload must contain at least one record"
        )

    # Validate field definitions
    for name, definition in fields.items():
        if not name or not isinstance(definition, dict):
            raise ValueError(
                f"Invalid definition for field: {name}"
            )

        if not definition.get("type"):
            raise ValueError(
                f"Missing data type for field: {name}"
            )

    errors = []
    checked = 0

    for row_no, record in enumerate(records, start=1):

        if not isinstance(record, dict):
            errors.append({
                "row": row_no,
                "field": None,
                "code": "INVALID_RECORD",
                "message": "Each record must be an object."
            })
            continue

        checked += 1

        # Validate each contract field
        for name, definition in fields.items():

            required = bool(
                definition.get("required", True)
            )

            nullable = bool(
                definition.get("nullable", False)
            )

            if name not in record:
                if required:
                    errors.append({
                        "row": row_no,
                        "field": name,
                        "code": "MISSING_REQUIRED_FIELD",
                        "message": (
                            f"Required field '{name}' is missing."
                        )
                    })
                continue

            value = record[name]

            if _empty(value):
                if not nullable:
                    errors.append({
                        "row": row_no,
                        "field": name,
                        "code": "NULL_NOT_ALLOWED",
                        "message": (
                            f"Field '{name}' cannot be null or empty."
                        )
                    })
                continue

            if not _valid_type(
                value,
                definition.get("type")
            ):
                errors.append({
                    "row": row_no,
                    "field": name,
                    "code": "INVALID_DATA_TYPE",
                    "message": (
                        f"Field '{name}' expected "
                        f"{_type_name(definition.get('type'))}; "
                        f"received {value!r}."
                    )
                })

        # Detect unexpected CSV columns or JSON fields
        if reject_unexpected_fields:
            for name in set(record) - set(fields):
                errors.append({
                    "row": row_no,
                    "field": str(name),
                    "code": "UNEXPECTED_FIELD",
                    "message": (
                        f"Field '{name}' is not defined "
                        "in the contract."
                    )
                })

    return {
        "valid": not errors,
        "contract_version": contract.get("version"),
        "total_records": len(records),
        "checked_records": checked,
        "error_count": len(errors),
        "errors": errors,
        "summary": (
            "Payload passed contract validation."
            if not errors
            else f"Payload failed with {len(errors)} validation error(s)."
        )
    }
