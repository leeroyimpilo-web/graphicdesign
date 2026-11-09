# Rights, sourcing and training policy

## Two strictly separated libraries

**Historical archive:** `text-effects/` currently contains 130 third-party Magnific result-page thumbnails, source URLs and metadata collected on 2026-11-09. They are not included in a training manifest. Do not present them as freely licensed content. The user may manually review source pages; do not automate bulk extraction or use the stock assets/metadata for model training, AI-powered reference retrieval, embedding, or prompting without suitable permission.

**Approved collection:** `training-approved/` can contain original work commissioned or created for this repository and third-party works only where provenance and scope of AI usage are verified.

## Magnific terms

Terms of use: https://www.magnific.com/legal/terms-of-use/

Section 8.1(7) restricts using Magnific Content, including associated titles and metadata, for machine-learning / AI purposes. Section 2 additionally restricts unauthorized automated access, copying and downloading. A thumbnail rather than the high-resolution original does not override these terms. Separate, express authorization is needed to change the training status.

## Required evidence for third-party assets

Record a stable source URL, creator/rightsholder, exact license or written permission, license snapshot/evidence location, permitted activities (especially AI training, embeddings/retrieval, and output commercialization), restrictions and date checked. If any essential fact is uncertain, mark it `not_approved` and exclude it.

For original assets record who created them, when, the editable source, and any incorporated third-party materials. Use fonts and imagery only under their individual licenses.

## Implementation rules

- Never treat "free download," "royalty free," or a private GitHub repository as equivalent to training permission.
- Never import scraped assets from the reference archive into `training-approved/` without documented new permission.
- Treat the manifest validator as a technical consistency check, not legal verification.
- Do not publish the historical archive without rights review.
- Maintain an audit trail for removals or licensing changes.
