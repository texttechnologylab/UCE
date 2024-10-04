-- Some standard indexes on title and such
CREATE INDEX IF NOT EXISTS idx_metadatatitleinfo_title ON metadatatitleinfo (title);
CREATE INDEX IF NOT EXISTS idx_metadatatitleinfo_published ON metadatatitleinfo (published);

-- Enable the pg_trgm extension, if not already enabled
-- For the following indexes, see also: https://www.postgresql.org/docs/9.1/textsearch-indexes.html
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Create trigram indexes on the 'coveredtext' columns for the relevant tables
CREATE INDEX IF NOT EXISTS idx_namedentity_coveredtext_trgm ON namedentity USING gin (coveredtext gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_time_coveredtext_trgm ON time USING gin (coveredtext gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_taxon_coveredtext_trgm ON taxon USING gin (coveredtext gin_trgm_ops);
-- For the metadata lemma search, we use coveredtext and value of the column, which is why we add them both in the index as well.
CREATE INDEX IF NOT EXISTS idx_lemma_coveredtext_trgm ON lemma USING gin ((value || ' ' || coveredtext) gin_trgm_ops); 
