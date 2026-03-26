CREATE OR REPLACE FUNCTION uce_search_layer_fulltext(
    IN corpus_id bigint,
    IN input1 text[], 
    IN input2 text,
    IN take_count integer,
    IN offset_count integer,
    IN count_all boolean DEFAULT false,
    IN order_direction text DEFAULT 'DESC',
    IN order_by_column text DEFAULT 'rank',
    IN uce_metadata_filters jsonb DEFAULT NULL,
    IN useTsVector boolean DEFAULT true,
    IN source_table text DEFAULT 'page',
    IN schema_name text DEFAULT 'public',
    IN p_user_name text DEFAULT NULL,
    IN p_min_level integer DEFAULT 1,
    IN expanded_terms text[] DEFAULT NULL,
    OUT total_count_out integer,
    OUT document_ids integer[],
    OUT named_entities_found text[][],
    OUT time_found text[][],
    OUT taxons_found text[][],
    OUT snippets_found text[],
    OUT document_ranks float[]
)
RETURNS record AS $$
DECLARE
    query TEXT;
    total_count_temp integer;
    document_ids_temp integer[];
    document_ranks_temp float[];
    named_entities_temp text[][];
    time_temp text[][];
    taxons_temp text[][];
    snippets_temp text[];
    additional_join_1 TEXT := '';
    additional_join_2 TEXT := '';
    order_by_clause TEXT := '';
    ts_condition TEXT;
BEGIN
    -- Ensure PostgreSQL uses indexes and has enough memory
    --SET enable_seqscan = OFF;
    SET work_mem = '128MB';
    
    -- Set statement timeout to prevent hanging queries (30 seconds)
    SET statement_timeout = '30s';
    SET lock_timeout = '10s';

    -- If input2 is NULL or empty, disable TsVector search
    IF input2 IS NULL OR input2 = '' THEN
        useTsVector := false;
    END IF;
    
    -- Validate input2 for Pro Mode (to_tsquery) to prevent syntax errors.
    -- Keep strict semantics for pro mode: invalid tsquery should fail.
    IF useTsVector AND input2 IS NOT NULL AND input2 != '' THEN
        PERFORM to_tsquery('simple', input2);
    END IF;

    -- Pro mode must preserve strict boolean semantics from input2.
    -- Expanded term broadening is non-pro behavior only.
    IF useTsVector THEN
        expanded_terms := NULL;
    END IF;

    -- Validate the order direction
    IF order_direction NOT IN ('ASC', 'DESC') THEN
        RAISE EXCEPTION 'Invalid order_direction: %', order_direction;
    END IF;

    -- Construct ORDER BY clause dynamically
    IF order_by_column = 'rank' THEN
        order_by_clause := FORMAT('ORDER BY pr.rank %s', order_direction);
    ELSIF order_by_column = 'documenttitle' THEN
        order_by_clause := FORMAT('ORDER BY pr.documenttitle %s', order_direction);
    ELSE
        order_by_clause := 'ORDER BY pr.rank DESC';  -- Default ordering
    END IF;

    -- Final tsquery keeps original query semantics from input2.
    IF useTsVector THEN
        ts_condition := 'to_tsquery(''simple'', $4)';
    ELSE
        ts_condition := 'websearch_to_tsquery(''simple'', $4)';
    END IF;
	
    -- Check source table
    IF source_table != 'page' THEN
        additional_join_1 := FORMAT('INNER JOIN %I.%I t ON um.document_id = t.document_id', schema_name, source_table);
        additional_join_2 := FORMAT('INNER JOIN %I.%I t ON p.id = t.id', schema_name, source_table);
    END IF;

    IF input2 IS NULL OR input2 = '' THEN
        query := FORMAT('
            WITH expanded_filters AS NOT MATERIALIZED (
                SELECT 
                    (filter->>''key'')::text AS key,
                    (filter->>''value'')::text AS value,
                    (filter->>''min'')::decimal AS min,
                    (filter->>''max'')::decimal AS max,
                    (filter->>''valueType'')::int AS value_type
                FROM jsonb_array_elements($1) AS filter
            ),
            filtered_ucemetadata AS (
                SELECT document_id, key, value, valueType 
                FROM ucemetadata 
                WHERE valueType != 2
            ),
            filter_matches AS NOT MATERIALIZED (
                SELECT um.document_id
                FROM expanded_filters ef
                JOIN filtered_ucemetadata um ON 
                    (ef.value_type IS NULL OR um.valueType = ef.value_type) 
                    AND (ef.key IS NULL OR um.key = ef.key)
                    AND (
                        ef.value IS NULL OR um.value = ef.value
                        OR (
                            -- range search for NUMBER meta fields
                            ef.value_type = 1
                            AND (
                                (ef.min IS NULL OR um.value::decimal >= ef.min)
                                AND
                                (ef.max IS NULL OR um.value::decimal <= ef.max)
                            )
                        )
                    )
                JOIN permitted_documents($9, $10) d ON um.document_id = d.id AND d.corpusid = $2
                %s
                GROUP BY um.document_id
                HAVING COUNT(ef.key) = (SELECT COUNT(*) FROM expanded_filters)
            ),
            limited_docs AS NOT MATERIALIZED (
                SELECT id, documenttitle FROM permitted_documents($9, $10) pd WHERE pd.corpusid = $2 LIMIT 999
            ),
            page_ranked AS NOT MATERIALIZED (
                SELECT 
                    p.document_id AS doc_id, 
                    0 AS rank,
                    d.documenttitle
                FROM limited_docs d
                JOIN page p ON d.id = p.document_id
                %s
                AND ($5 IS NULL OR p.document_id IN (SELECT document_id FROM filter_matches))
                GROUP BY p.document_id, d.documenttitle
            ),
            limited_pages AS NOT MATERIALIZED (
                SELECT 
                    pr.doc_id,
                    pr.rank,
                    pr.documenttitle
                FROM page_ranked pr
                %s
                LIMIT $6 OFFSET $7
            ),
            counted_documents AS NOT MATERIALIZED (
                SELECT COUNT(*) AS total_count FROM page_ranked
            ),
            ranked_documents AS NOT MATERIALIZED (
                SELECT 
                    d.id,
                    lp.rank,
                    JSONB_AGG((
                        SELECT jsonb_agg(jsonb_build_object(
                            ''snippet'', LEFT(p.coveredtext, 400),
                            ''pageId'', p.id
                        ) ORDER BY p.id ASC)
                        FROM (SELECT p.id, p.coveredtext 
                            FROM page p 
                            WHERE p.document_id = lp.doc_id
                            ORDER BY p.id ASC
                            LIMIT 1) p
                    )) AS snippets
                FROM limited_pages lp
                JOIN permitted_documents($9, $10) d ON d.id = lp.doc_id
                GROUP BY d.id, lp.rank
                ORDER BY lp.rank DESC
            ),
            extracted_entities AS NOT MATERIALIZED (
                SELECT ARRAY[ne.id::text, ne.coveredtext, COUNT(ne.id)::text, ne.typee, ne.document_id::text] 
                FROM ranked_documents rd
                JOIN namedentity ne ON rd.id = ne.document_id
                GROUP BY ne.id, ne.coveredtext, ne.typee, ne.document_id
            ),
            extracted_times AS NOT MATERIALIZED (
                SELECT ARRAY[t.id::text, t.coveredtext, COUNT(t.id)::text, t.valuee, t.document_id::text] 
                FROM ranked_documents rd
                JOIN time t ON rd.id = t.document_id
                GROUP BY t.id, t.coveredtext, t.valuee, t.document_id
            ),
            extracted_taxons AS NOT MATERIALIZED (
                SELECT ARRAY[ta.id::text, ta.coveredtext, COUNT(ta.id)::text, ta.primaryname, ta.document_id::text] 
                FROM ranked_documents rd
                JOIN biofidtaxon ta ON rd.id = ta.document_id
                GROUP BY ta.id, ta.coveredtext, ta.primaryname, ta.document_id
            )
            SELECT 
                CASE WHEN $8 THEN (SELECT total_count FROM counted_documents) ELSE NULL END,
                ARRAY(SELECT id FROM ranked_documents),
                ARRAY(SELECT rank FROM ranked_documents),
                CASE WHEN $8 THEN ARRAY(SELECT * FROM extracted_entities) ELSE ARRAY[]::text[][] END,
                CASE WHEN $8 THEN ARRAY(SELECT * FROM extracted_times) ELSE ARRAY[]::text[][] END,
                CASE WHEN $8 THEN ARRAY(SELECT * FROM extracted_taxons) ELSE ARRAY[]::text[][] END,
                ARRAY(SELECT snippets FROM ranked_documents)
        ', additional_join_1, additional_join_2, order_by_clause);
    ELSE
        query := FORMAT('
            WITH expanded_filters AS NOT MATERIALIZED (
                SELECT 
                    (filter->>''key'')::text AS key,
                    (filter->>''value'')::text AS value,
                    (filter->>''min'')::decimal AS min,
                    (filter->>''max'')::decimal AS max,
                    (filter->>''valueType'')::int AS value_type
                FROM jsonb_array_elements($1) AS filter
            ),
            filtered_ucemetadata AS (
                SELECT document_id, key, value, valueType 
                FROM ucemetadata 
                WHERE valueType != 2
            ),
            filter_matches AS NOT MATERIALIZED (
                SELECT um.document_id
                FROM expanded_filters ef
                JOIN filtered_ucemetadata um ON 
                    (ef.value_type IS NULL OR um.valueType = ef.value_type) 
                    AND (ef.key IS NULL OR um.key = ef.key)
                    AND (
                        ef.value IS NULL OR um.value = ef.value
                        OR (
                            -- range search for NUMBER meta fields
                            ef.value_type = 1
                            AND (
                                (ef.min IS NULL OR um.value::decimal >= ef.min)
                                AND
                                (ef.max IS NULL OR um.value::decimal <= ef.max)
                            )
                        )
                    )
                JOIN permitted_documents($9, $10) d ON um.document_id = d.id AND d.corpusid = $2
                %s
                GROUP BY um.document_id
                HAVING COUNT(ef.key) = (SELECT COUNT(*) FROM expanded_filters)
            ),
            expanded_term_chunks AS NOT MATERIALIZED (
                SELECT
                    ((ROW_NUMBER() OVER ()) - 1) / 200 AS chunk_id,
                    btrim(term) AS term
                FROM unnest($11) AS term
                WHERE term IS NOT NULL AND btrim(term) <> ''''
            ),
            expanded_term_queries AS NOT MATERIALIZED (
                SELECT
                    chunk_id,
                    websearch_to_tsquery(''simple'', string_agg(term, '' OR '')) AS chunk_query
                FROM expanded_term_chunks
                GROUP BY chunk_id
            ),
            expanded_prefilter_docs AS NOT MATERIALIZED (
                SELECT DISTINCT p.document_id
                FROM page p
                JOIN expanded_term_queries etq ON p.textsearch @@ etq.chunk_query
            ),
            candidate_pages AS MATERIALIZED (
                SELECT
                    p.document_id AS doc_id,
                    p.id AS page_id,
                    CASE
                        WHEN p.textsearch @@ ' || ts_condition || ' THEN ts_rank_cd(p.textsearch, ' || ts_condition || ')
                        ELSE 0
                    END AS rank_score,
                    d.documenttitle
                FROM page p
                %s
                JOIN permitted_documents($9, $10) d ON d.id = p.document_id
                WHERE (
                    p.textsearch @@ ' || ts_condition || '
                    OR (
                        $11 IS NOT NULL
                        AND cardinality($11) > 0
                        AND p.document_id IN (SELECT document_id FROM expanded_prefilter_docs)
                    )
                )
                AND ($5 IS NULL OR p.document_id IN (SELECT document_id FROM filter_matches))
                AND d.corpusid = $2
            ),
            document_ranks AS NOT MATERIALIZED (
                SELECT 
                    doc_id,
                    AVG(rank_score) AS rank,
                    documenttitle
                FROM candidate_pages
                GROUP BY doc_id, documenttitle
            ),
            limited_docs AS NOT MATERIALIZED (
                SELECT 
                    pr.doc_id,
                    pr.rank,
                    pr.documenttitle
                FROM document_ranks pr
                %s
                LIMIT $6 OFFSET $7
            ),
            counted_documents AS NOT MATERIALIZED (
                SELECT COUNT(*) AS total_count FROM document_ranks
            ),
            ranked_documents AS NOT MATERIALIZED (
                SELECT 
                    d.id,
                    ld.rank,
                    JSONB_AGG((
                        SELECT jsonb_agg(jsonb_build_object(
                            ''snippet'', ts_headline(
                                ''simple'',
                                p.coveredtext, 
                                ' || ts_condition || ',
                                ''StartSel=<b>, StopSel=</b>, MaxWords=60, MinWords=35, MaxFragments=3, FragmentDelimiter=" [...] "''
                            ),
                            ''pageId'', p.id
                        ) ORDER BY p.rank_score DESC)
                        FROM (
                            SELECT cp.page_id AS id, p.coveredtext, cp.rank_score
                            FROM candidate_pages cp
                            JOIN page p ON p.id = cp.page_id
                            WHERE cp.doc_id = ld.doc_id
                            ORDER BY cp.rank_score DESC
                            LIMIT 5
                        ) p
                    )) AS snippets
                FROM limited_docs ld
                JOIN permitted_documents($9, $10) d ON d.id = ld.doc_id
                GROUP BY d.id, ld.rank
                ORDER BY ld.rank DESC
            ),
            extracted_entities AS NOT MATERIALIZED (
                SELECT ARRAY[ne.id::text, ne.coveredtext, COUNT(ne.id)::text, ne.typee, ne.document_id::text] 
                FROM ranked_documents rd
                JOIN namedentity ne ON rd.id = ne.document_id
                GROUP BY ne.id, ne.coveredtext, ne.typee, ne.document_id
            ),
            extracted_times AS NOT MATERIALIZED (
                SELECT ARRAY[t.id::text, t.coveredtext, COUNT(t.id)::text, t.valuee, t.document_id::text] 
                FROM ranked_documents rd
                JOIN time t ON rd.id = t.document_id
                GROUP BY t.id, t.coveredtext, t.valuee, t.document_id
            ),
            extracted_taxons AS NOT MATERIALIZED (
                SELECT ARRAY[ta.id::text, ta.coveredtext, COUNT(ta.id)::text, ta.primaryname, ta.document_id::text] 
                FROM ranked_documents rd
                JOIN biofidtaxon ta ON rd.id = ta.document_id
                GROUP BY ta.id, ta.coveredtext, ta.primaryname, ta.document_id
            )
            SELECT 
                CASE WHEN $8 THEN (SELECT total_count FROM counted_documents) ELSE NULL END,
                ARRAY(SELECT id FROM ranked_documents),
                ARRAY(SELECT rank FROM ranked_documents),
                CASE WHEN $8 THEN ARRAY(SELECT * FROM extracted_entities) ELSE ARRAY[]::text[][] END,
                CASE WHEN $8 THEN ARRAY(SELECT * FROM extracted_times) ELSE ARRAY[]::text[][] END,
                CASE WHEN $8 THEN ARRAY(SELECT * FROM extracted_taxons) ELSE ARRAY[]::text[][] END,
                ARRAY(SELECT snippets FROM ranked_documents)
        ', additional_join_1, additional_join_2, order_by_clause);
    END IF;

    EXECUTE query
    USING 
        uce_metadata_filters,  -- $1  : uce_metadata_filters
        corpus_id,             -- $2  : corpus_id
        input1,                -- $3  : input1 / search tokens array
        input2,                -- $4  : input2 / search string
        uce_metadata_filters,  -- $5  : uce_metadata_filters (for filter checks)
        take_count,            -- $6  : limit
        offset_count,          -- $7  : offset
        count_all,             -- $8  : return counts?
        p_user_name,           -- $9  : principal for permitted_documents
        p_min_level,           -- $10 : min permission level
        expanded_terms         -- $11 : expanded terms array
    INTO total_count_temp, document_ids_temp, document_ranks_temp, named_entities_temp, time_temp, taxons_temp, snippets_temp;

    total_count_out := total_count_temp;
    document_ids := document_ids_temp;
    document_ranks := document_ranks_temp;
    named_entities_found := named_entities_temp;
    time_found := time_temp;
    taxons_found := taxons_temp;
    snippets_found := snippets_temp;
END;
$$ LANGUAGE plpgsql;
