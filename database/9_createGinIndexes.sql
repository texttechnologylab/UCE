-- Some standard indexes on title and such
CREATE INDEX IF NOT EXISTS idx_metadatatitleinfo_title ON metadatatitleinfo (title);
CREATE INDEX IF NOT EXISTS idx_metadatatitleinfo_published ON metadatatitleinfo (published);
CREATE INDEX IF NOT EXISTS idx_metadatatitleinfo_author ON metadatatitleinfo (author);

-- Filters on the UCEMEtadata filters since we always join those
CREATE INDEX IF NOT EXISTS idx_ucemetadata_doc_filters ON ucemetadata(document_id, key, value, valueType) WHERE valueType != 2;
CREATE INDEX IF NOT EXISTS idx_ucemetadata_value ON ucemetadata (valueType);
CREATE INDEX IF NOT EXISTS idx_ucemetadata_document_id ON ucemetadata (document_id);
CREATE INDEX IF NOT EXISTS idx_ucemetadata_value_gin ON ucemetadata USING gin (value, key gin_trgm_ops) WHERE valueType != 2;

-- and also some trigram index:
CREATE INDEX IF NOT EXISTS idx_metadatatitleinfo_title_trgm ON metadatatitleinfo USING gin (title gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_metadatatitleinfo_published_trgm ON metadatatitleinfo USING gin (published gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_metadatatitleinfo_author_trgm ON metadatatitleinfo USING gin (author gin_trgm_ops);

-- For the following indexes, see also: https://www.postgresql.org/docs/9.1/textsearch-indexes.html
-- Create trigram indexes on the 'coveredtext' columns for the relevant tables
CREATE INDEX IF NOT EXISTS idx_namedentity_coveredtext_trgm ON namedentity USING gin (coveredtext gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_time_coveredtext_trgm ON time USING gin (coveredtext gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_biofidtaxon_coveredtext_trgm ON biofidtaxon USING gin (coveredtext gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_biofidtaxon_primaryname_trgm ON biofidtaxon USING gin (primaryname gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_sentence_coveredtext_trgm ON sentence USING gin (coveredtext gin_trgm_ops);

-- For the metadata lemma search, we use coveredtext and value of the column, which is why we add them both in the index as well.
CREATE INDEX IF NOT EXISTS idx_lemma_coveredtext_trgm ON lemma USING gin ((value || ' ' || coveredtext) gin_trgm_ops); 

-- For the fulltext search of the documents
--CREATE INDEX IF NOT EXISTS idx_document_fulltext_trgm ON document USING gin (fulltext gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_page_fulltext_trgm ON page USING gin (coveredtext gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_textsearch_gin ON page USING gin(to_tsvector('simple', lower(coveredtext)));
CREATE INDEX IF NOT EXISTS idx_textsearch_gin_raw ON page USING gin (textsearch);

-- Some join indexes for documents/pages
CREATE INDEX IF NOT EXISTS idx_document_id ON document (id);
CREATE INDEX IF NOT EXISTS idx_page_document_id ON page (document_id);
CREATE INDEX IF NOT EXISTS idx_rank ON document (documenttitle DESC);
CREATE INDEX IF NOT EXISTS idx_document_corpusid ON document (corpusid) INCLUDE (id);

-- Since we look for annotations a lot:
CREATE INDEX IF NOT EXISTS idx_namedentity_document_id ON namedentity (document_id);
CREATE INDEX IF NOT EXISTS idx_time_document_id ON time (document_id);
CREATE INDEX IF NOT EXISTS idx_taxon_document_id ON biofidtaxon (document_id);
CREATE INDEX IF NOT EXISTS idx_lemma_document_id ON lemma (document_id);

-- For the semantic role labels
CREATE INDEX IF NOT EXISTS idx_srl_relationtype_trgm ON srlink USING gin (relationtype gin_trgm_ops); 

-- Create indexes for SRL since we have so many
CREATE INDEX IF NOT EXISTS idx_document_corpusid ON document(corpusid);
CREATE INDEX IF NOT EXISTS idx_srlink_document_id ON srlink(document_id);
CREATE INDEX IF NOT EXISTS idx_srlink_figurecoveredtext ON srlink(LOWER(figurecoveredtext));
CREATE INDEX IF NOT EXISTS idx_srlink_groundcoveredtext ON srlink(LOWER(groundcoveredtext));

-- Create indexes on our lexicon.
CREATE INDEX IF NOT EXISTS idx_lexicon_coveredtext_lower_trgm ON lexicon USING gin (lower(coveredtext) gin_trgm_ops);

-- Create indexes for the Logical Links
CREATE INDEX IF NOT EXISTS idx_links_fromid_partial ON annotationlink(fromid) WHERE fromid IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_links_toid_partial ON annotationlink(toid) WHERE toid IS NOT NULL;

-- Create indexes for the Geoname Locations and Postgis in general
CREATE INDEX IF NOT EXISTS idx_location_geography ON geoname USING GIST (location);

