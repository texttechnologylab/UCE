CREATE OR REPLACE FUNCTION uce_search_layer_fulltext(
	IN corpus_id bigint,
	
    IN input1 text[], 
    IN input2 text,
    IN take_count integer,
    IN offset_count integer,
	
    IN count_all boolean DEFAULT false,
	
    IN order_direction text DEFAULT 'DESC',
    IN order_by_column text DEFAULT 'title',
	
    OUT total_count_out integer,
    OUT document_ids integer[],
    OUT named_entities_found text[][],
    OUT time_found text[][],
    OUT taxons_found text[][],
    OUT snippets_found text[] -- Add an output for snippets
)
RETURNS record AS $$
DECLARE
    -- Declare variables to hold total count, document IDs, named entities, time, taxons, and snippets
    total_count_temp integer;
    document_ids_temp integer[];
    named_entities_temp text[][];
    time_temp text[][];
    taxons_temp text[][];
    snippets_temp text[];
BEGIN
    -- Common table expression to define the set of documents and matching snippets
	WITH documents_query AS (
		SELECT DISTINCT d.id,
						unnest(regexp_matches(d.fulltext, input2, 'gi')) AS match,
						substring(d.fulltext from position(unnest(regexp_matches(d.fulltext, input2, 'gi')) in d.fulltext) - 250 for 500) AS snippet
		FROM document d
		WHERE d.corpusid = corpus_id 
		  AND (d.documenttitle = ANY(input1) OR d.language = ANY(input1) OR d.fulltext ~* input2)

		UNION ALL

		SELECT DISTINCT d.id,
						unnest(regexp_matches(me.title || ' ' || coalesce(me.published, ''), input2, 'gi')) AS match,
						substring(me.title || ' ' || coalesce(me.published, '') from position(unnest(regexp_matches(me.title || ' ' || coalesce(me.published, ''), input2, 'gi')) in me.title || ' ' || coalesce(me.published, '')) - 50 for 100) AS snippet
		FROM document d
		JOIN metadatatitleinfo me ON d.id = me.id
		WHERE d.corpusid = corpus_id 
		  AND (me.title ~* input2 OR me.published ~* input2)
	),
    
    -- Count all found documents
    counted_documents AS (
        SELECT COUNT(*) AS total_count FROM documents_query
    )
    
    -- Retrieve total count, document IDs, named entities, time, taxons, and snippets (conditionally)
    SELECT 
      CASE WHEN count_all THEN (SELECT total_count FROM counted_documents) ELSE NULL END AS total_count,
      ARRAY(
        SELECT dq.id
        FROM (
            SELECT dq.id
            FROM documents_query dq
            JOIN metadatatitleinfo me ON dq.id = me.id
			
			-- This ordering is a bit scuffed, but it finally works. A lot of copy pasting when adding new cases, but that shouldn't happen often. --
			ORDER BY 
			  CASE 
				WHEN order_by_column = 'title' THEN 
				  CASE WHEN order_direction = 'ASC' THEN me.title::text ELSE NULL END
				WHEN order_by_column = 'published' THEN 
				  CASE 
					WHEN order_direction = 'ASC' THEN TO_DATE(me.published, 'DD.MM.YYYY')::text -- Adjust the format as necessary
					ELSE NULL 
				  END
				-- Add more cases for other valid columns
				ELSE NULL
			  END ASC,
			  CASE 
				WHEN order_by_column = 'title' THEN 
				  CASE WHEN order_direction = 'DESC' THEN me.title::text ELSE NULL END
				WHEN order_by_column = 'published' THEN 
				  CASE 
					WHEN order_direction = 'DESC' THEN TO_DATE(me.published, 'DD.MM.YYYY')::text -- Adjust the format as necessary
					ELSE NULL 
				  END
				-- Add more cases for other valid columns
				ELSE NULL
			  END DESC

			LIMIT take_count OFFSET offset_count
        ) AS dq
      ) AS document_ids_temp,
	  
	  -- Count the occurrences of all the found entities, taxons etc. --
	  
	  CASE WHEN count_all THEN
		  ARRAY(
			SELECT ARRAY[ne.coveredtext, COUNT(ne.id)::text, ne.typee, ne.document_id::text] AS named_entity
			FROM documents_query dq
			JOIN namedentity ne ON dq.id = ne.document_id
			GROUP BY ne.coveredtext, ne.typee, ne.document_id
		  )
		  ELSE ARRAY[]::text[][]
	  END AS named_entities_temp,
	  
	  CASE WHEN count_all THEN
		  ARRAY(
			SELECT ARRAY[t.coveredtext, COUNT(t.id)::text, t.valuee, t.document_id::text] AS time
			FROM documents_query dq
			JOIN time t ON dq.id = t.document_id
			GROUP BY t.coveredtext, t.valuee, t.document_id
		  ) 
		  ELSE ARRAY[]::text[][]
	  END AS time_temp,
	  
	  CASE WHEN count_all THEN
      ARRAY(
        SELECT ARRAY[ta.coveredtext, COUNT(ta.id)::text, ta.valuee, ta.document_id::text] AS taxon
        FROM documents_query dq
        JOIN taxon ta ON dq.id = ta.document_id
        GROUP BY ta.coveredtext, ta.valuee, ta.document_id
      )
	  ELSE ARRAY[]::text[][]
	  END AS taxons_temp,
	  
	  -- Fetch the snippets for the matched input2
	  ARRAY(
        SELECT dq.snippet
        FROM documents_query dq
      ) AS snippets_temp -- New snippet logic
    
    INTO total_count_temp, document_ids_temp, named_entities_temp, time_temp, taxons_temp, snippets_temp
    FROM (SELECT 1) AS dummy;
    
    -- Set out parameters
    total_count_out := total_count_temp;
    document_ids := document_ids_temp;
    named_entities_found := named_entities_temp;
    time_found := time_temp;
    taxons_found := taxons_temp;
    snippets_found := snippets_temp; -- Set the snippets output
END;
$$ LANGUAGE plpgsql;
