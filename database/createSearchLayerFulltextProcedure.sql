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
BEGIN
    -- Ensure PostgreSQL uses indexes and has enough memory
    SET enable_seqscan = OFF;
    SET work_mem = '128MB';

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
    
	-- Check what source we take, the normal page or a layered search table.
	IF source_table != 'page' THEN
        additional_join_1 := FORMAT('INNER JOIN %I.%I t ON um.document_id = t.document_id', source_table, schema_name);
        additional_join_2 := FORMAT('INNER JOIN %I.%I t ON p.id = t.id', source_table, schema_name);
    END IF;
	
    -- Construct the full query dynamically
    query := FORMAT('
        WITH expanded_filters AS (
            SELECT 
                (filter->>''key'')::text AS key,
                (filter->>''value'')::text AS value,
                (filter->>''valueType'')::text AS value_type
            FROM jsonb_array_elements($1) AS filter
        ),
		filter_matches AS (
			SELECT um.document_id
			FROM expanded_filters ef
			JOIN ucemetadata um ON 
				(ef.value_type IS NULL OR um.valueType::text = ef.value_type) 
				AND (ef.key IS NULL OR um.key = ef.key) 
				AND (ef.value IS NULL OR um.value = ef.value)
				AND um.valueType != 2
			JOIN document d ON um.document_id = d.id AND d.corpusid = $2
			%s -- additional_join_1
			GROUP BY um.document_id
			HAVING COUNT(DISTINCT ef.key) = (SELECT COUNT(*) FROM expanded_filters)
		),
        page_ranked AS (
            SELECT 
                p.document_id AS doc_id, 
                CASE 
                    WHEN $3 AND $4 IS NOT NULL AND $4 <> '''' 
                    THEN MAX(ts_rank_cd(p.textsearch, 
                        CASE 
                            WHEN $3 THEN to_tsquery(''simple'', $4) 
                            ELSE websearch_to_tsquery(''simple'', $4) 
                        END
                    )) 
                    ELSE NULL 
                END AS rank,
                d.documenttitle
            FROM page p
			%s	
            JOIN document d ON d.id = p.document_id
            WHERE (
                ($3 AND $4 IS NOT NULL AND $4 <> '''' AND p.textsearch @@ to_tsquery(''simple'', $4))
                OR (NOT $3 AND $4 IS NOT NULL AND $4 <> '''' AND p.textsearch @@ websearch_to_tsquery(''simple'', $4))
                OR ($4 IS NULL OR $4 = '''')
            )
            AND ($5 IS NULL OR p.document_id IN (SELECT document_id FROM filter_matches))
            AND d.corpusid = $2
            GROUP BY p.document_id, d.documenttitle
        ),
        limited_pages AS (
            SELECT 
                pr.doc_id,
                pr.rank,
                pr.documenttitle
            FROM page_ranked pr
            %s
            LIMIT $6 OFFSET $7
        ),
        counted_documents AS (
            SELECT COUNT(*) AS total_count FROM page_ranked
        ),
		ranked_documents AS (
            SELECT 
                d.id,
                lp.rank,
                CASE 
                    WHEN $4 IS NULL OR $4 = '''' THEN 
                        ARRAY_AGG(DISTINCT LEFT(p.coveredtext, 400))
                    WHEN $3 THEN 
                        ARRAY_AGG(DISTINCT (
                            SELECT ts_headline(
                                ''simple'',
                                p.coveredtext, 
                                to_tsquery(''simple'', $4),
                                ''StartSel=<b>, StopSel=</b>, MaxWords=150, MinWords=105, MaxFragments=2, FragmentDelimiter=" ... "''
                            )
                            FROM page p
                            WHERE p.document_id = lp.doc_id 
                            AND p.textsearch @@ to_tsquery(''simple'', $4)
                            ORDER BY ts_rank_cd(p.textsearch, to_tsquery(''simple'', $4)) DESC
                            LIMIT 1
                        ))
                    ELSE 
                        ARRAY_AGG(DISTINCT (
                            SELECT ts_headline(
                                ''simple'',
                                p.coveredtext, 
                                websearch_to_tsquery(''simple'', $4),
                                ''StartSel=<b>, StopSel=</b>, MaxWords=150, MinWords=105, MaxFragments=2, FragmentDelimiter=" ... "''
                            )
                            FROM page p
                            WHERE p.document_id = lp.doc_id 
                            AND p.textsearch @@ websearch_to_tsquery(''simple'', $4)
                            ORDER BY ts_rank_cd(p.textsearch, websearch_to_tsquery(''simple'', $4)) DESC
                            LIMIT 1
                        ))
                END AS snippets
            FROM limited_pages lp
            JOIN document d ON d.id = lp.doc_id
            JOIN page p ON p.document_id = lp.doc_id
            GROUP BY d.id, lp.rank
            ORDER BY lp.rank DESC
        ),
        extracted_entities AS (
            SELECT ARRAY[
                ne.id::text, 
                ne.coveredtext, 
                COUNT(ne.id)::text, 
                ne.typee, 
                ne.document_id::text
            ] AS named_entity
            FROM ranked_documents rd
            JOIN namedentity ne ON rd.id = ne.document_id
            GROUP BY ne.id, ne.coveredtext, ne.typee, ne.document_id
        ),
        extracted_times AS (
            SELECT ARRAY[t.id::text, t.coveredtext, COUNT(t.id)::text, t.valuee, t.document_id::text] 
            FROM ranked_documents rd
            JOIN time t ON rd.id = t.document_id
            GROUP BY t.id, t.coveredtext, t.valuee, t.document_id
        ),
        extracted_taxons AS (
            SELECT ARRAY[ta.id::text, ta.coveredtext, COUNT(ta.id)::text, ta.valuee, ta.document_id::text] 
            FROM ranked_documents rd
            JOIN taxon ta ON rd.id = ta.document_id
            GROUP BY ta.id, ta.coveredtext, ta.valuee, ta.document_id
        )
        SELECT 
            CASE WHEN $8 THEN (SELECT total_count FROM counted_documents) ELSE NULL END,
            ARRAY(SELECT id FROM ranked_documents),
            ARRAY(SELECT rank FROM ranked_documents),
            CASE WHEN $8 THEN ARRAY(SELECT named_entity FROM extracted_entities) ELSE ARRAY[]::text[][] END,
            CASE WHEN $8 THEN ARRAY(SELECT * FROM extracted_times) ELSE ARRAY[]::text[][] END,
            CASE WHEN $8 THEN ARRAY(SELECT * FROM extracted_taxons) ELSE ARRAY[]::text[][] END,
            ARRAY(SELECT unnest(snippets) FROM ranked_documents)
        ', additional_join_1, additional_join_2, order_by_clause);

    -- Execute the query with safe parameter passing
    EXECUTE query
    USING uce_metadata_filters, corpus_id, useTsVector, input2, uce_metadata_filters, take_count, offset_count, count_all
    INTO total_count_temp, document_ids_temp, document_ranks_temp, named_entities_temp, time_temp, taxons_temp, snippets_temp;

    -- Set output variables
    total_count_out := total_count_temp;
    document_ids := document_ids_temp;
    document_ranks := document_ranks_temp;
    named_entities_found := named_entities_temp;
    time_found := time_temp;
    taxons_found := taxons_temp;
    snippets_found := snippets_temp;
END;
$$ LANGUAGE plpgsql;
