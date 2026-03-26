-- DISABLED: Simplified enhanced search function with robust tsquery handling
-- Focuses on fixing the immediate issues without complex two-phase logic

-- Function: uce_search_layer_fulltext_safe
-- Safe version that uses our safe tsquery functions
-- DISABLED: Use baseline uce_search_layer_fulltext instead
/*
CREATE OR REPLACE FUNCTION uce_search_layer_fulltext_safe(
    corpus_id bigint,
    input1 text[],
    input2 text,
    take_count integer,
    offset_count integer,
    count_all boolean DEFAULT false,
    order_direction text DEFAULT 'DESC',
    order_by_column text DEFAULT 'rank',
    uce_metadata_filters jsonb DEFAULT NULL,
    useTsVector boolean DEFAULT true,
    source_table text DEFAULT 'page',
    schema_name text DEFAULT 'public',
    p_user_name text DEFAULT NULL,
    p_min_level integer DEFAULT 1,
    expanded_terms text[] DEFAULT NULL
) RETURNS TABLE(
    total_count_out integer,
    document_ids integer[],
    document_ranks double precision[],
    named_entities_found text[],
    time_found text[],
    taxons_found text[],
    snippets_found text[]
) AS $$
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
    ts_function TEXT;
    ts_condition TEXT;
    snippet_query TEXT;
    ranked_pages_cte TEXT;
    
    -- Safe tsquery variables
    safe_tsquery tsquery;
    tsquery_text text;
BEGIN
    -- Ensure PostgreSQL uses indexes and has enough memory
    SET work_mem = '256MB';
    
    -- If input2 is NULL or empty, disable TsVector search
    IF input2 IS NULL OR input2 = '' THEN
        useTsVector := false;
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
    
    -- SAFE TSQUERY CONSTRUCTION: Use our safe functions
    IF expanded_terms IS NOT NULL AND array_length(expanded_terms, 1) > 0 THEN
        -- Use expanded terms with safe construction
        IF useTsVector THEN
            -- Pro mode with expanded terms
            safe_tsquery := safe_to_tsquery('simple', expanded_terms, 100);
        ELSE
            -- Non-pro mode with expanded terms
            tsquery_text := array_to_string(expanded_terms, ' OR ');
            safe_tsquery := safe_websearch_to_tsquery('simple', tsquery_text);
        END IF;
    ELSE
        -- Use original query
        IF useTsVector THEN
            -- Pro mode
            BEGIN
                safe_tsquery := to_tsquery('simple', input2);
            EXCEPTION
                WHEN OTHERS THEN
                    -- Fallback to websearch for malformed pro mode queries
                    RAISE WARNING 'Pro mode query failed, falling back to websearch: %', SQLERRM;
                    safe_tsquery := safe_websearch_to_tsquery('simple', input2);
            END;
        ELSE
            -- Non-pro mode
            safe_tsquery := safe_websearch_to_tsquery('simple', input2);
        END IF;
    END IF;
    
    -- Convert tsquery to text for dynamic SQL
    IF safe_tsquery IS NULL THEN
        ts_function := 'NULL';
        ts_condition := 'FALSE';
    ELSE
        ts_function := quote_literal(safe_tsquery::text);
        ts_condition := FORMAT('p.textsearch @@ %s', ts_function);
    END IF;
    
    -- Check source table
    IF source_table != 'page' THEN
        additional_join_1 := FORMAT('INNER JOIN %I.%I t ON um.document_id = t.document_id', schema_name, source_table);
        additional_join_2 := FORMAT('INNER JOIN %I.%I t ON p.id = t.id', schema_name, source_table);
    END IF;
    
    -- Define snippet selection logic
    IF input2 IS NULL OR input2 = '' OR safe_tsquery IS NULL THEN
        snippet_query := 'SELECT jsonb_agg(jsonb_build_object(
                                ''snippet'', LEFT(p.coveredtext, 400),
                                ''pageId'', p.id
                              ) ORDER BY p.id ASC)
                          FROM (SELECT p.id, p.coveredtext
                                FROM page p
                                WHERE p.document_id = lp.doc_id
                                ORDER BY p.id ASC
                                LIMIT 1) p';
        
        -- If we don't search for any string, we don't need to fulltext search all documents
        ranked_pages_cte := FORMAT('WITH limited_docs AS NOT MATERIALIZED (
                                SELECT id, documenttitle FROM permitted_documents($13, $14) pd 
                                WHERE pd.corpusid = $2 
                                LIMIT 999
                            )
                            SELECT
                                p.document_id AS doc_id,
                                0 AS rank,
                                d.documenttitle
                            FROM limited_docs d
                            JOIN page p ON d.id = p.document_id
                            %s
                            AND ($5 IS NULL OR p.document_id IN (SELECT document_id FROM filter_matches))',
                            additional_join_2);
    ELSE
        snippet_query := FORMAT('SELECT jsonb_agg(jsonb_build_object(
                                ''snippet'', ts_headline(
                                    ''simple'',
                                    p.coveredtext, 
                                    %s,
                                    ''StartSel=<b>, StopSel=</b>, MaxWords=60, MinWords=35, MaxFragments=3, FragmentDelimiter=" [...] "''
                                ),
                                ''pageId'', p.id
                              ) ORDER BY rank_score DESC)
                              FROM (SELECT p.id, p.coveredtext, ts_rank_cd(p.textsearch, %s) AS rank_score
                                    FROM page p
                                    WHERE p.document_id = lp.doc_id 
                                    AND %s
                                    ORDER BY rank_score DESC
                                    LIMIT 5) p', 
                              ts_function, ts_function, ts_condition);
        
        ranked_pages_cte := FORMAT('SELECT
                                p.document_id AS doc_id,
                                ts_rank_cd(p.textsearch, %s) AS rank,
                                d.documenttitle
                            FROM page p
                            %s
                            JOIN permitted_documents($13, $14) d ON d.id = p.document_id
                            WHERE %s
                            AND ($5 IS NULL OR p.document_id IN (SELECT document_id FROM filter_matches))
                            AND d.corpusid = $2
                            LIMIT 20000', 
                          ts_function, additional_join_2, ts_condition);
    END IF;
    
    -- Construct the full query dynamically
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
                JOIN permitted_documents($13, $14) d ON um.document_id = d.id AND d.corpusid = $2
                %s
                GROUP BY um.document_id
                HAVING COUNT(ef.key) = (SELECT COUNT(*) FROM expanded_filters)
            ),
            ranked_pages AS NOT MATERIALIZED ( %s ),
            page_ranked AS NOT MATERIALIZED (
                SELECT 
                    doc_id,
                    AVG(rank) AS rank, -- Get highest rank per document
                    documenttitle
                FROM ranked_pages
                GROUP BY doc_id, documenttitle
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
                    JSONB_AGG((%s)) AS snippets
                FROM limited_pages lp
                JOIN permitted_documents($13, $14) d ON d.id = lp.doc_id
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
        ',
        additional_join_1,
        ranked_pages_cte,
        order_by_clause,
        snippet_query
    );
    
    -- Execute the query with proper parameter binding
    -- Note: The original function uses $13 for p_user_name and $14 for p_min_level
    RETURN QUERY EXECUTE query
    USING uce_metadata_filters, corpus_id, input1, input2, 
          uce_metadata_filters, take_count, offset_count, count_all,
          order_direction, order_by_column, uce_metadata_filters,
          useTsVector, source_table, schema_name, p_user_name, p_min_level;
    
EXCEPTION
    WHEN OTHERS THEN
        -- Enhanced error handling with fallback
        RAISE WARNING 'Safe search failed: %, falling back to original function', SQLERRM;
        
        -- Fallback to original function without expanded terms
        RETURN QUERY
        SELECT * FROM uce_search_layer_fulltext(
            corpus_id, input1, input2, take_count, offset_count,
            count_all, order_direction, order_by_column,
            uce_metadata_filters, useTsVector, source_table,
            schema_name, p_user_name, p_min_level,
            NULL::text[]  -- No expanded terms
        );
END;
$$ LANGUAGE plpgsql;

-- Test the safe function
COMMENT ON FUNCTION uce_search_layer_fulltext_safe IS 
    'Safe search function with robust tsquery construction and error handling';