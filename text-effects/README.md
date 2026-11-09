# Text Effects

First collection for the graphicdesign project: visual text-effect references and a future training dataset.

## Status

Initialized 2026-11-09. No images collected and no model trained.

The requested Magnific search is recorded in `sources.json`. Its standard content terms prohibit machine-learning/AI use. Do not treat free downloads or screenshots as training permission. Obtain a separate licence covering the intended use before collecting this source for training.

## Collection structure

- `sources.json`: source review and acquisition status.
- `manifest.jsonl`: one asset record per line; empty until assets are admitted.
- `images/`: admitted source images (create when collecting).
- `licenses/`: evidence of permission for each admitted asset.
- `annotations/`: descriptions of lettering, material, lighting, composition and effect construction.

## Asset admission

Record id, source URL, creator, title, licence name and URL, permission evidence path, attribution, retrieval date, SHA-256, image path, dimensions, and independent statuses for training and redistribution. Unknown permission stays pending. Only approved training assets enter a training export; public GitHub image commits also require redistribution permission.

## Annotation categories

Chrome/metallic; gold/foil; neon/glow; embossed/debossed; extruded 3D; glass; liquid; retro; distressed; paper-cut; graffiti; cinematic.

For each asset describe the exact visible text, font characteristics, extrusion, bevel, material, palette, lighting, background, texture, layout and a reusable construction recipe. Keep observations distinct from guesses.

## Next stage

Collect original artwork, public-domain material with verified provenance, or assets with documented permission covering the intended use. Define the target model and training format before generating dataset exports. This repository is a collection scaffold, not a trained model or ChatGPT integration.
