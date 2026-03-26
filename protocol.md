
### 2026-03-25 21:10:43 +0100 - Taxon enrichment matching tightened
- Root cause identified: taxon candidate lookup allowed prefix matching (), causing  to resolve to .
- Change:  now ranks/filters by
  - exact match, or
  - whole-word boundary regex match
  before optional contains/fuzzy fallbacks.
- Validation: compile success; DB inspection confirms  has no exact/word-boundary taxon in current corpus.

### 2026-03-25 - Taxon enrichment matching tightened
- Root cause identified: taxon candidate lookup allowed prefix matching (`LOWER(primaryname) LIKE :prefix`), causing `Raupe` to resolve to `Raupen-Kernkeule`.
- Change: `getIdentifiableTaxonsByValue` now ranks/filters by exact match or whole-word boundary regex match before optional contains/fuzzy fallbacks.
- Validation: compile success; DB inspection confirms `Raupe` has no exact/word-boundary taxon in current corpus.
