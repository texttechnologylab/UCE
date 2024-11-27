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

-- For the fulltext search of the documents
CREATE INDEX IF NOT EXISTS idx_document_fulltext_trgm ON document USING gin (fulltext gin_trgm_ops); 

-- For the semantic role labels
CREATE INDEX IF NOT EXISTS idx_srl_relationtype_trgm ON srlink USING gin (relationtype gin_trgm_ops); 

-- Create indexes for SRL since we have so many
CREATE INDEX IF NOT EXISTS idx_document_corpusid ON document(corpusid);
CREATE INDEX IF NOT EXISTS idx_srlink_document_id ON srlink(document_id);
CREATE INDEX IF NOT EXISTS idx_srlink_figurecoveredtext ON srlink(LOWER(figurecoveredtext));
CREATE INDEX IF NOT EXISTS idx_srlink_groundcoveredtext ON srlink(LOWER(groundcoveredtext));

-- Create a Generated Column for the "taxon" value column that splits the values x|y|z into its own array
DO $$
BEGIN
    -- Check if the value_array column exists in the taxon table
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'taxon' AND column_name = 'value_array'
    ) THEN
        -- Add the generated column, and remove occurrences of " before splitting
        ALTER TABLE taxon
        ADD COLUMN value_array TEXT[] GENERATED ALWAYS AS (string_to_array(REPLACE(valuee, '"', ''), '|')) STORED;
    END IF;
END
$$;

-- Add the index on the value_array column
CREATE INDEX IF NOT EXISTS idx_taxon_value_array ON taxon USING gin (value_array);

