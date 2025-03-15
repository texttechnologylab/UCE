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
    ts_function TEXT;
    ts_condition TEXT;
    snippet_query TEXT;
	ranked_pages_cte TEXT;
BEGIN
    -- Ensure PostgreSQL uses indexes and has enough memory
    --SET enable_seqscan = OFF;
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

    -- Determine the appropriate search function
    IF useTsVector THEN
        ts_function := 'to_tsquery(''simple'', $4)';
    ELSE
        ts_function := 'websearch_to_tsquery(''simple'', $4)';
    END IF;
	
    -- Check source table
    IF source_table != 'page' THEN
        additional_join_1 := FORMAT('INNER JOIN %I.%I t ON um.document_id = t.document_id', schema_name, source_table);
        additional_join_2 := FORMAT('INNER JOIN %I.%I t ON p.id = t.id', schema_name, source_table);
    END IF;

    -- Define snippet selection logic
	IF input2 IS NULL OR input2 = '' THEN
		snippet_query := 'SELECT jsonb_agg(jsonb_build_object(
							''snippet'', LEFT(p.coveredtext, 400),
							''pageId'', p.id
						  ) ORDER BY p.id ASC)
						  FROM (SELECT p.id, p.coveredtext 
								FROM page p 
								WHERE p.document_id = lp.doc_id
								ORDER BY p.id ASC
								LIMIT 1) p';
								
		-- If we dont search for any string, we dont need to fulltext search all documents
		ranked_pages_cte := 'WITH limited_docs AS NOT MATERIALIZED (
				SELECT id, documenttitle from document WHERE corpusid = $2 LIMIT 999
			)
			SELECT 
                p.document_id AS doc_id, 
                0 AS rank,
				-- %s
                d.documenttitle
            FROM limited_docs d
            %s
            JOIN page p ON d.id = p.document_id
			-- %s
            AND ($5 IS NULL OR p.document_id IN (SELECT document_id FROM filter_matches))';
	ELSE
		snippet_query := 'SELECT jsonb_agg(jsonb_build_object(
							''snippet'', ts_headline(
								''simple'',
								p.coveredtext, 
								' || ts_function || ',
								''StartSel=<b>, StopSel=</b>, MaxWords=60, MinWords=35, MaxFragments=3, FragmentDelimiter=" [...] "''
							),
							''pageId'', p.id
						  ) ORDER BY rank_score DESC)
						  FROM (SELECT p.id, p.coveredtext, ts_rank_cd(p.textsearch, ' || ts_function || ') AS rank_score
								FROM page p
								WHERE p.document_id = lp.doc_id 
								AND p.textsearch @@ ' || ts_function || '
								ORDER BY rank_score DESC
								LIMIT 5) p';

		ranked_pages_cte := 'SELECT
                p.document_id AS doc_id, 
                ts_rank_cd(p.textsearch, %s) AS rank,
                d.documenttitle
            FROM page p
            %s
            JOIN document d ON d.id = p.document_id
            WHERE (
                ($4 IS NOT NULL AND $4 <> '''' AND p.textsearch @@ %s)
                OR ($4 IS NULL OR $4 = '''')
            )
            AND ($5 IS NULL OR p.document_id IN (SELECT document_id FROM filter_matches))
            AND d.corpusid = $2
			LIMIT 5000'; -- Put a Limit here, it will increase perfomance and we dont need to show millions of hits. But I know, it's a hack...
	END IF;

    -- Construct the full query dynamically
    query := FORMAT('
		WITH expanded_filters AS NOT MATERIALIZED (
			SELECT 
				(filter->>''key'')::text AS key,
				(filter->>''value'')::text AS value,
				(filter->>''valueType'')::text AS value_type
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
				(ef.value_type IS NULL OR um.valueType::text = ef.value_type) 
				AND (ef.key IS NULL OR um.key = ef.key) 
				AND (ef.value IS NULL OR um.value = ef.value)
			JOIN document d ON um.document_id = d.id AND d.corpusid = $2
			%s
			GROUP BY um.document_id
			HAVING COUNT(ef.key) = (SELECT COUNT(*) FROM expanded_filters)
		),
        ranked_pages AS NOT MATERIALIZED ( ' || ranked_pages_cte || '),
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
                JSONB_AGG((' || snippet_query || ')) AS snippets
            FROM limited_pages lp
            JOIN document d ON d.id = lp.doc_id
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
            SELECT ARRAY[ta.id::text, ta.coveredtext, COUNT(ta.id)::text, ta.valuee, ta.document_id::text] 
            FROM ranked_documents rd
            JOIN taxon ta ON rd.id = ta.document_id
            GROUP BY ta.id, ta.coveredtext, ta.valuee, ta.document_id
        )
        SELECT 
            CASE WHEN $8 THEN (SELECT total_count FROM counted_documents) ELSE NULL END,
            ARRAY(SELECT id FROM ranked_documents),
            ARRAY(SELECT rank FROM ranked_documents),
            CASE WHEN $8 THEN ARRAY(SELECT * FROM extracted_entities) ELSE ARRAY[]::text[][] END,
            CASE WHEN $8 THEN ARRAY(SELECT * FROM extracted_times) ELSE ARRAY[]::text[][] END,
            CASE WHEN $8 THEN ARRAY(SELECT * FROM extracted_taxons) ELSE ARRAY[]::text[][] END,
            ARRAY(SELECT snippets FROM ranked_documents)
    ', additional_join_1, ts_function, additional_join_2, ts_function, order_by_clause);

    EXECUTE query
    USING uce_metadata_filters, corpus_id, useTsVector, input2, uce_metadata_filters, take_count, offset_count, count_all
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
