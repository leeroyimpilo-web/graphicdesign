#!/usr/bin/env python3
"""Offline consistency check for user-owned or expressly AI-permitted assets."""
import hashlib
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "training-approved" / "manifest.jsonl"
REQUIRED = {
    "id", "asset_path", "title", "creator", "source_type",
    "license", "rights_evidence", "ai_training_permitted",
    "ai_retrieval_permitted", "tags",
}
SOURCE_TYPES = {"original", "explicit_permission"}

def validate():
    if not MANIFEST.exists():
        print("ERROR: manifest not found:", MANIFEST)
        return 1
    failures = []
    seen = set()
    approved = 0
    for number, raw in enumerate(MANIFEST.read_text(encoding="utf-8").splitlines(), start=1):
        if not raw.strip():
            continue
        where = f"line {number}"
        try:
            obj = json.loads(raw)
            if not isinstance(obj, dict):
                raise ValueError("entry must be an object")
            missing = REQUIRED.difference(obj)
            if missing:
                raise ValueError("missing keys: " + ", ".join(sorted(missing)))
            ident = obj["id"]
            if not isinstance(ident, str) or not ident.strip() or ident in seen:
                raise ValueError("invalid or duplicate id")
            seen.add(ident)
            if obj["source_type"] not in SOURCE_TYPES:
                raise ValueError("invalid source_type")
            if obj["ai_training_permitted"] is not True or obj["ai_retrieval_permitted"] is not True:
                raise ValueError("unapproved AI usage; remove from approved manifest")
            for key in ("title", "creator", "license", "rights_evidence"):
                if not isinstance(obj[key], str) or not obj[key].strip():
                    raise ValueError(f"{key} must be a non-empty string")
            if not isinstance(obj["tags"], list) or not obj["tags"] or not all(
                isinstance(tag, str) and tag.strip() for tag in obj["tags"]
            ):
                raise ValueError("tags must be a nonempty list of strings")
            rel = Path(obj["asset_path"])
            if rel.is_absolute() or ".." in rel.parts or not rel.parts or rel.parts[0] != "training-approved":
                raise ValueError("asset_path must stay within training-approved/")
            file = (ROOT / rel).resolve()
            if not file.is_relative_to((ROOT / "training-approved").resolve()) or not file.is_file():
                raise ValueError("asset file missing or outside approved directory")
            if "sha256" in obj and obj["sha256"]:
                digest = hashlib.sha256(file.read_bytes()).hexdigest()
                if digest != obj["sha256"]:
                    raise ValueError("sha256 mismatch")
            approved += 1
        except (json.JSONDecodeError, ValueError, TypeError) as exc:
            failures.append(f"{where}: {exc}")
    if failures:
        print("FAIL", len(failures), "issue(s):")
        for issue in failures:
            print(" -", issue)
        return 1
    print(f"PASS: {approved} approval-declared asset(s) validated.")
    print("Note: rights declarations must still be independently verified.")
    return 0

if __name__ == "__main__":
    sys.exit(validate())
