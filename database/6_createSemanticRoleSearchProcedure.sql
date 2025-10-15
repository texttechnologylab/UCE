CREATE OR REPLACE FUNCTION uce_semantic_role_search(

	IN corpusid_val bigint,
	
	IN arg0 text[], 
	IN arg1 text[], 
	IN arg2 text[], 
	IN argm text[], 
	IN verb text,
	
    IN take_count integer,
    IN offset_count integer,
	
    IN count_all boolean DEFAULT false,
	
    IN order_direction text DEFAULT 'DESC',
    IN order_by_column text DEFAULT 'title',
	
    OUT total_count_out integer,
    OUT document_ids integer[],
    OUT named_entities_found text[][][][],
    OUT time_found text[][][][],
    OUT taxons_found text[][][][]
)
RETURNS record AS $$
DECLARE
    -- Declare variables to hold total count, document IDs, named entities, time, and taxons
    total_count_temp integer;
    document_ids_temp integer[];
    named_entities_temp text[][][][];
    time_temp text[][][][];
    taxons_temp text[][][][];
BEGIN
	CREATE TEMPORARY TABLE documents_query AS (
		SELECT DISTINCT 
			d.id
		FROM document d
		JOIN srlink sr ON d.id = sr.document_id
		WHERE d.corpusid = corpusid_val 
			AND (LOWER(sr.figurecoveredtext) = verb OR verb = '')
			-- Pre-filter based on ARG0, ARG1, ARG2, or ARGM conditions in WHERE clause
			AND (
				((sr.relationtype = 'ARG0' OR sr.relationtype = 'I-ARG0' OR sr.relationtype = 'B-C-ARG0') AND LOWER(sr.groundcoveredtext) = ANY(arg0)) OR
				((sr.relationtype = 'ARG1' OR sr.relationtype = 'I-ARG1' OR sr.relationtype = 'B-C-ARG1') AND LOWER(sr.groundcoveredtext) = ANY(arg1)) OR
				((sr.relationtype = 'ARG2' OR sr.relationtype = 'I-ARG2' OR sr.relationtype = 'B-C-ARG2') AND LOWER(sr.groundcoveredtext) = ANY(arg2)) OR
				((sr.relationtype = 'ARGM' OR sr.relationtype = 'ARGM-LOC' OR sr.relationtype = 'B-C-ARGM-LOC') AND LOWER(sr.groundcoveredtext) = ANY(argm))
			)
		GROUP BY d.id, sr.figurebegin
		HAVING 
			COUNT(DISTINCT 
				CASE 
					WHEN ((sr.relationtype = 'ARG0' OR sr.relationtype = 'I-ARG0' OR sr.relationtype = 'B-C-ARG0') AND LOWER(sr.groundcoveredtext) = ANY(arg0)) THEN sr.relationtype
					WHEN ((sr.relationtype = 'ARG1' OR sr.relationtype = 'I-ARG1' OR sr.relationtype = 'B-C-ARG1') AND LOWER(sr.groundcoveredtext) = ANY(arg1)) THEN sr.relationtype
					WHEN ((sr.relationtype = 'ARG2' OR sr.relationtype = 'I-ARG2' OR sr.relationtype = 'B-C-ARG2') AND LOWER(sr.groundcoveredtext) = ANY(arg2)) THEN sr.relationtype
					WHEN ((sr.relationtype = 'ARGM' OR sr.relationtype = 'ARGM-LOC' OR sr.relationtype = 'I-ARGM-LOC' OR sr.relationtype = 'B-C-ARGM-LOC') AND LOWER(sr.groundcoveredtext) = ANY(argm)) THEN sr.relationtype
					ELSE NULL
				END
			) = 
			(
				CASE WHEN cardinality(arg0) > 0 THEN 1 ELSE 0 END +
				CASE WHEN cardinality(arg1) > 0 THEN 1 ELSE 0 END +
				CASE WHEN cardinality(arg2) > 0 THEN 1 ELSE 0 END +
				CASE WHEN cardinality(argm) > 0 THEN 1 ELSE 0 END
			)
	);

    -- Temporary table to count all found documents
    CREATE TEMPORARY TABLE counted_documents AS (
        SELECT COUNT(*) AS total_count FROM documents_query
    );
    
    -- Retrieve total count, document IDs, named entities, time, and taxons (conditionally)
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
					WHEN order_direction = 'ASC' THEN 
					  CASE 
						WHEN me.published ~ '^\d{2}\.\d{2}\.\d{4}$' THEN TO_DATE(me.published, 'DD.MM.YYYY')::text -- Full date format
						WHEN me.published ~ '^\d{4}$' THEN TO_DATE(me.published || '-01-01', 'YYYY-MM-DD')::text -- Only a year, assume January 1st
						ELSE NULL
					  END
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
					WHEN order_direction = 'DESC' THEN 
					  CASE 
						WHEN me.published ~ '^\d{2}\.\d{2}\.\d{4}$' THEN TO_DATE(me.published, 'DD.MM.YYYY')::text -- Full date format
						WHEN me.published ~ '^\d{4}$' THEN TO_DATE(me.published || '-01-01', 'YYYY-MM-DD')::text -- Only a year, assume January 1st
						ELSE NULL
					  END
					ELSE NULL 
				  END
				-- Add more cases for other valid columns
				ELSE NULL
			  END DESC

			LIMIT take_count OFFSET offset_count
        ) AS dq
      ) AS document_ids_temp,
	  
	  -- Count the occurences of all the found entities, taxons etc. --
 	 CASE WHEN count_all THEN
		  ARRAY(
			SELECT ARRAY[ne.id::text, ne.coveredtext, COUNT(ne.id)::text, ne.typee, ne.document_id::text] AS named_entity
			FROM documents_query dq
			JOIN namedentity ne ON dq.id = ne.document_id
			GROUP BY ne.id, ne.coveredtext, ne.typee, ne.document_id
		  )
		  ELSE ARRAY[]::text[][]
	  END AS named_entities_temp,
	  
	  CASE WHEN count_all THEN
		  ARRAY(
			SELECT ARRAY[t.id::text, t.coveredtext, COUNT(t.id)::text, t.valuee, t.document_id::text] AS time
			FROM documents_query dq
			JOIN time t ON dq.id = t.document_id
			GROUP BY t.id, t.coveredtext, t.valuee, t.document_id
		  ) 
		  ELSE ARRAY[]::text[][]
	  END AS time_temp,
	  
	  CASE WHEN count_all THEN
	  ARRAY(
			SELECT ARRAY[ta.id::text, ta.coveredtext, COUNT(ta.id)::text, ta.valuee, ta.document_id::text] AS taxon
			FROM documents_query dq
			JOIN gazetteertaxon ta ON dq.id = ta.document_id
			GROUP BY ta.id, ta.coveredtext, ta.valuee, ta.document_id

			UNION ALL

			SELECT ARRAY[ta.id::text, ta.coveredtext, COUNT(ta.id)::text, ta.valuee, ta.document_id::text] AS taxon
			FROM documents_query dq
			JOIN gnfindertaxon ta ON dq.id = ta.document_id
			GROUP BY ta.id, ta.coveredtext, ta.valuee, ta.document_id
	  )

	  ELSE ARRAY[]::text[][]
	  END AS taxons_temp
	  
    INTO total_count_temp, document_ids_temp, named_entities_temp, time_temp, taxons_temp
    FROM (SELECT 1) AS dummy;
    
    -- Set out parameters
    total_count_out := total_count_temp;
    document_ids := document_ids_temp;
    named_entities_found := named_entities_temp;
    time_found := time_temp;
    taxons_found := taxons_temp;
	
	-- Drop the temporary table to avoid cluttering the database
    DROP TABLE IF EXISTS documents_query;
    DROP TABLE IF EXISTS counted_documents;
END;
$$ LANGUAGE plpgsql;
