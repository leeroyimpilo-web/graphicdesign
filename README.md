# GraphicDesign — Original Design Skill & Reference Library

A private GitHub home for professional graphic-design study, original design recipes, and rights-checked assets usable in a ChatGPT-assisted design workflow.

## Current state (2026-11-09)

| Area | Status |
| --- | --- |
| `text-effects/` | 130 Magnific search-result thumbnails archived previously. **Reference-only; AI use not approved.** |
| `training-approved/` | New, distinct repository area for original or explicitly permitted assets. |
| `SKILL.md` | First version of a reusable graphic-design workflow. |
| `scripts/validate_training_manifest.py` | Offline metadata, provenance and integrity checks. |

The stock collection is incomplete: approximately 4,800 search results were displayed, but only 130 usable thumbnails were verified and saved. The original files were not downloaded.

## Explore

- [Text Effects: archived gallery](text-effects/GALLERY.md) — manually browse linked source pages. **Not for AI prompts, retrieval, indexing for AI, or model training without separate permission.**
- [Graphic design skill](SKILL.md) — original design process, art direction, and quality checks.
- [Training-approved collection](training-approved/README.md) — exclusively original or specifically AI-permitted materials.
- [Rights and source rules](docs/RIGHTS_AND_SOURCES.md) — non-negotiable provenance restrictions.

## Add an approved asset

1. Verify that you created it or received written permission covering AI training/retrieval.
2. Save the file inside `training-approved/assets/` (or the applicable named category).
3. Add a JSONL entry to `training-approved/manifest.jsonl` following [the schema](training-approved/README.md).
4. Run `python scripts/validate_training_manifest.py`.
5. Only then consider it for any model-training workflow. A valid local manifest is **not** proof that a third party's license grants those rights; review the original license.

## Scope

This repository helps ChatGPT follow a consistent creative process and locate approved design references. Adding files to GitHub does **not** train or fine-tune ChatGPT's underlying model. Actual weight training would require a separate supported training workflow.

Future sections: typography, logo design, posters, branding, brochures, social graphics, layouts, packaging, and print production.
