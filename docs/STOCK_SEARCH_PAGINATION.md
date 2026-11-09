# Magnific Text Effects Search — Pagination Investigation

**Checked:** 2026-11-09  
**Status:** Pagination method verified; exhaustive stock download **not authorized** for this AI reference/training project.

## Source search

User-provided search:

`https://www.magnific.com/search?format=search&last_filter=query&last_value=text+effects+--variety&query=text+effects+--variety&selection=1&sort=relevance#uuid=1314993d-78b7-4164-8a08-e3da495bacb9`

The `#uuid=...` fragment is a client-side URL fragment; it is **not a pagination parameter**. Do not interpret it as a cursor. Direct search URL fetching was unavailable through the research browser, so website-internal page or cursor behavior has **not** been established.

## Verified paginated integration

The installed Magnific integration exposes `stock_search`:

```text
query: "text effects --variety"
content_type: "psd,vector,illustration"
order: "relevance"
page: 1, 2, 3, ...
```

Observed, on 2026-11-09:

| Test | Result |
| --- | --- |
| Page 1 | 50 items, reported total 145,595, lastPage 50 |
| Page 2 | 50 items, reported total 145,592, lastPage 50 |
| Page 3 | 50 items, reported total 145,595, lastPage 50 |
| Plain "text effects", page 1 | 50 items, reported total 145,596, lastPage 50 |

The results **may differ from the exact web UI selection**. The reported total is not the number of accessible items. The integration caps search results at 50 pages × 50 items = **2,500 items per filter/query combination**. The slight changes in the total reflect a live index. A three-page smoke test confirms functional pagination, **not** completeness.

## How an authorized exhaustive process would work

If Magnific grants express written authorization covering automated collection and the intended AI use:

1. Use official stock API/integration (not a site scraper or hidden/internal AJAX endpoint).
2. Enumerate pages 1 through the returned `lastPage` for one fixed query/filter combination.
3. Store a checkpoint recording the filter, page number, retrieval time, item IDs, and errors for reproducibility.
4. Deduplicate globally by stable stock asset ID; also hash actual files when authorized.
5. Use disjoint, collectively exhaustive filter partitions where supported. Test whether filtering actually partitions the full set; avoid assuming that keyword synonyms or colors are exhaustive.
6. Treat any partition with `lastPage=50` and `total>2500` as **truncated**. Split further or request a provider export. Do not declare 100% coverage while a truncated partition remains.
7. Respect documented rate limits, rights, and approved usage. Verify that assets and metadata can legally be stored and used for the intended AI workflow.
8. Keep the stock archive distinct from `training-approved/`; only expressly AI-permitted material may enter that dataset.

**Current block:** Terms of Use Section 2 bars unauthorized automated copying and non-ordinary access; Section 8.1(7) prohibits using stock content **and metadata** for AI / machine-learning purposes. See https://www.magnific.com/legal/terms-of-use/

No additional third-party thumbnails, content, or metadata were written to the repository during this investigation.

## Existing archive

`text-effects/` retains 130 thumbnails previously verified. Those assets remain excluded from AI training/retrieval; see `text-effects/sources.json` and `docs/RIGHTS_AND_SOURCES.md`.

## Alternative

To build a searchable visual-design skill now, curate original design studies in `training-approved/` and sources with explicitly verified AI training/retrieval permissions. The pagination approach above can be applied to a properly licensed source or official export.
