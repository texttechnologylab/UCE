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
    total_count_temp integer;
    document_ids_temp integer[];
    document_ranks_temp float[];
    named_entities_temp text[][];
    time_temp text[][];
    taxons_temp text[][];
    snippets_temp text[];
BEGIN
    -- Ensure PostgreSQL uses indexes
    SET enable_seqscan = OFF;
	SET work_mem = '2048MB';

    -- If input2 is NULL or empty, set useTsVector to false
    IF input2 IS NULL OR input2 = '' THEN
        useTsVector := false;
    END IF;

    -- Validate the order direction
    IF order_direction NOT IN ('ASC', 'DESC') THEN
        RAISE EXCEPTION 'Invalid order_direction: %', order_direction;
    END IF;
	
    -- Handle potential metadata filters dynamically via JSON
    WITH expanded_filters AS (
        SELECT 
            (filter->>'key')::text AS key,
            (filter->>'value')::text AS value,
            (filter->>'valueType')::text AS value_type
        FROM jsonb_array_elements(uce_metadata_filters) AS filter
    ),
    filter_matches AS (
        SELECT um.document_id
        FROM ucemetadata um
        JOIN expanded_filters ef ON (
            (ef.value_type IS NULL OR um.valueType::text = ef.value_type) AND
            (ef.key IS NULL OR um.key = ef.key) AND
            (ef.value IS NULL OR um.value = ef.value)
        )
        WHERE um.valueType != 2 -- Json is not filterable
        GROUP BY um.document_id
        HAVING COUNT(*) = (SELECT COUNT(*) FROM expanded_filters)
    ),
    
    -- This gets all documents that match the filters and search query.
	page_ranked AS (
		SELECT 
			p.document_id AS doc_id, 
			CASE 
				WHEN useTsVector AND input2 IS NOT NULL AND input2 <> '' 
				THEN MAX(ts_rank_cd(p.textsearch, 
					CASE 
						WHEN useTsVector THEN to_tsquery('simple', input2) 
						ELSE plainto_tsquery('simple', input2) 
					END
				)) 
				ELSE NULL 
			END AS rank,
			d.documenttitle
		FROM page p
		JOIN document d ON d.id = p.document_id
		WHERE 
			(
				-- Full-text search using `to_tsquery()` if enabled
				(useTsVector AND input2 IS NOT NULL AND input2 <> '' 
					AND p.textsearch @@ to_tsquery('simple', input2))

				-- Use `plainto_tsquery()` if full-text search is disabled
				OR (NOT useTsVector AND input2 IS NOT NULL AND input2 <> '' 
					AND p.textsearch @@ plainto_tsquery('simple', input2))

				-- Skip search if input2 is empty
				OR (input2 IS NULL OR input2 = '')
			)
			AND (uce_metadata_filters IS NULL OR p.document_id IN (SELECT document_id FROM filter_matches))
			AND d.corpusid = corpus_id
		GROUP BY p.document_id, d.documenttitle
	),

    -- This limits and sorts those found documents.
    limited_pages AS (
        SELECT 
            pr.doc_id,
            pr.rank,
            pr.documenttitle
        FROM page_ranked pr
        ORDER BY 
            CASE 
                WHEN order_by_column = 'rank' AND order_direction = 'ASC' THEN pr.rank
                WHEN order_by_column = 'documenttitle' AND order_direction = 'ASC' THEN ROW_NUMBER() OVER (ORDER BY pr.documenttitle ASC)
            END ASC,
            CASE 
                WHEN order_by_column = 'rank' AND order_direction = 'DESC' THEN pr.rank
                WHEN order_by_column = 'documenttitle' AND order_direction = 'DESC' THEN ROW_NUMBER() OVER (ORDER BY pr.documenttitle DESC)
            END DESC
        LIMIT take_count OFFSET offset_count
    ),
    
    -- Snippet extraction: Uses full-text highlighting or raw substring extraction
	ranked_documents AS (
		SELECT 
			d.id,
			lp.rank,
			CASE 
				-- If input2 is empty, just take the first 400 characters of the text
				WHEN input2 IS NULL OR input2 = '' THEN 
					ARRAY_AGG(DISTINCT LEFT(p.coveredtext, 400))

				-- Use full-text search highlighting when tsVector is enabled
				WHEN useTsVector THEN 
					ARRAY_AGG(DISTINCT ts_headline(
						'simple',
						p.coveredtext, 
						to_tsquery('simple', input2),
						'StartSel=<b>, StopSel=</b>, MaxWords=150, MinWords=105, MaxFragments=2, FragmentDelimiter=" ... "' 
					))

				-- Otherwise, perform normal ILIKE-based substring extraction
				ELSE 
					ARRAY_AGG(DISTINCT 
						SUBSTRING(p.coveredtext FROM POSITION(input2 IN p.coveredtext) FOR 400)
					)
			END AS snippets
		FROM limited_pages lp
		JOIN document d ON d.id = lp.doc_id
		JOIN page p ON p.document_id = lp.doc_id
		GROUP BY d.id, lp.rank
		ORDER BY lp.rank DESC
	),

    counted_documents AS (
        SELECT COUNT(*) AS total_count FROM page_ranked
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
    )
    
    SELECT 
        CASE WHEN count_all THEN (SELECT total_count FROM counted_documents) ELSE NULL END,
        ARRAY(SELECT id FROM ranked_documents),
        ARRAY(SELECT rank FROM ranked_documents),
        CASE WHEN count_all THEN ARRAY(SELECT named_entity FROM extracted_entities) ELSE ARRAY[]::text[][] END,
        CASE WHEN count_all THEN ARRAY(
            SELECT ARRAY[t.id::text, t.coveredtext, COUNT(t.id)::text, t.valuee, t.document_id::text] 
            FROM ranked_documents rd
            JOIN time t ON rd.id = t.document_id
            GROUP BY t.id, t.coveredtext, t.valuee, t.document_id
        ) ELSE ARRAY[]::text[][] END,
        CASE WHEN count_all THEN ARRAY(
            SELECT ARRAY[ta.id::text, ta.coveredtext, COUNT(ta.id)::text, ta.valuee, ta.document_id::text] 
            FROM ranked_documents rd
            JOIN taxon ta ON rd.id = ta.document_id
            GROUP BY ta.id, ta.coveredtext, ta.valuee, ta.document_id
        ) ELSE ARRAY[]::text[][] END,
        ARRAY(SELECT unnest(snippets) FROM ranked_documents)
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
