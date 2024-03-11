CREATE OR REPLACE FUNCTION biofid_search_layer_metadata(
    IN input1 text[], 
    IN input2 text,
    IN take_count integer,
    IN offset_count integer,
    IN count_all boolean DEFAULT false,
	IN order_direction text DEFAULT 'DESC',
	IN order_by_column text DEFAULT 'title',
    OUT total_count_out integer,
    OUT document_ids integer[]
)
RETURNS record AS $$
DECLARE
    -- Declare variables to hold total count and document IDs
    total_count_temp integer;
    document_ids_temp integer[];
BEGIN
    -- Common table expression to define the set of documents
    WITH documents_query AS (
        SELECT DISTINCT d.id
        FROM document d
        WHERE d.documenttitle = ANY(input1) OR d.language = ANY(input1)
        
        UNION
        
        SELECT DISTINCT d.id
        FROM document d
        JOIN metadatatitleinfo me ON d.id = me.id
        WHERE me.title ~* input2 OR me.published ~* input2
    ),
    -- Count all found documents
    counted_documents AS (
        SELECT COUNT(*) AS total_count FROM documents_query
    )
    
    -- Retrieve total count and document IDs (conditionally)
    SELECT 
      CASE WHEN count_all THEN (SELECT total_count FROM counted_documents) ELSE NULL END AS total_count,
      ARRAY(
        SELECT dq.id
        FROM (
            SELECT dq.id
            FROM documents_query dq
            JOIN metadatatitleinfo me ON dq.id = me.id
			-- This ordering is a bit scuffed, but it finally works. A lot of copy pasting when adding new cases, but that should happend often. --
			ORDER BY 
			  CASE 
				WHEN order_by_column = 'title' THEN 
				  CASE WHEN order_direction = 'ASC' THEN me.title ELSE NULL END
				WHEN order_by_column = 'published' THEN 
				  CASE WHEN order_direction = 'ASC' THEN me.published ELSE NULL END
				-- Add more cases for other valid columns
				ELSE NULL
			  END ASC,
			  CASE 
				WHEN order_by_column = 'title' THEN 
				  CASE WHEN order_direction = 'DESC' THEN me.title ELSE NULL END
				WHEN order_by_column = 'published' THEN 
				  CASE WHEN order_direction = 'DESC' THEN me.published ELSE NULL END
				-- Add more cases for other valid columns
				ELSE NULL
			  END DESC

			LIMIT take_count OFFSET offset_count
        ) AS dq
      ) AS document_ids_temp
    INTO total_count_temp, document_ids_temp
    FROM (SELECT 1) AS dummy;
    
    -- Set out parameters
    total_count_out := total_count_temp;
    document_ids := document_ids_temp;
END;
$$ LANGUAGE plpgsql;
