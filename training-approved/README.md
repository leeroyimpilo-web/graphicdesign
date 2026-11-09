# Training-approved assets

This separate area is for **original work or assets with explicit, checked AI-use permissions**. The historic Magnific stock thumbnail archive is excluded.

## Structure

- `text-effects/` — original, editable examples and style recipes.
- `manifest.jsonl` — one JSON object per approved asset.
- `../scripts/validate_training_manifest.py` — offline integrity/provenance checks.

## Required manifest keys

- `id`: stable unique identifier.
- `asset_path`: repository-root-relative path inside `training-approved/`.
- `title`: descriptive title.
- `creator`: creator or identified rightsholder.
- `source_type`: `original` or `explicit_permission`.
- `license`: exact license / permission identifier.
- `rights_evidence`: human-readable audit note or path / URL to written authorization.
- `ai_training_permitted`: boolean, true only when confirmed.
- `ai_retrieval_permitted`: boolean, true only when confirmed.
- `tags`: list of relevant design descriptors.

Optional keys: `sha256` (asset hash; validated if present), `created_on`, `source_url`, `notes`. Metadata declarations should never be taken as independent legal proof.

Run from the repository root:

```bash
python scripts/validate_training_manifest.py
```

## What this does and does not do

This repository can support a ChatGPT design skill with rights-cleared examples. It cannot silently change ChatGPT's model weights; real fine-tuning requires a separately supported process. Adding an example to `manifest.jsonl` does not itself train any model.
