
import json
import sys
from pathlib import Path

from compatibility.engine import compare_contracts


def load_contract(path):
    with open(path, "r", encoding="utf-8") as file:
        return json.load(file)


def main():
    if len(sys.argv) != 3:
        print("Usage: python -m compatibility.ci_gate OLD NEW")
        sys.exit(2)

    old_path = Path(sys.argv[1])
    new_path = Path(sys.argv[2])

    try:
        old_contract = load_contract(old_path)
        new_contract = load_contract(new_path)

        result = compare_contracts(old_contract, new_contract)

        print("\n===== CAP-34 COMPATIBILITY REPORT =====")
        print("Old version:", result.get("old_version"))
        print("New version:", result.get("new_version"))
        print("Summary:", result.get("summary"))

        for change in result.get("breaking_changes", []):
            print("\nBREAKING CHANGE")
            print("Field:", change["field"])
            print("Change:", change["change_type"])
            print("Old:", change["old_value"])
            print("New:", change["new_value"])
            print("Reason:", change["reason"])

        for change in result.get("compatible_changes", []):
            print("\nCOMPATIBLE CHANGE")
            print("Field:", change["field"])
            print("Change:", change["change_type"])

        if not result["compatible"]:
            print("\nCI FAILED: Breaking contract changes detected.")
            sys.exit(1)

        print("\nCI PASSED: Contract is backward-compatible.")

    except Exception as error:
        print("Compatibility gate error:", error)
        sys.exit(2)


if __name__ == "__main__":
    main()
