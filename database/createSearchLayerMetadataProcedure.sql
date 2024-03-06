CREATE OR REPLACE FUNCTION biofid_search_layer_metadata(
    IN input1 text[], 
    IN input2 text,
    IN take_count integer,
    IN offset_count integer,
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
    ),
    -- Select document IDs with TAKE and OFFSET
    paginated_documents AS (
        SELECT * 
        FROM (
            SELECT * 
            FROM documents_query
            LIMIT take_count OFFSET offset_count
        ) AS subquery
    )
    
	-- Retrieve total count and document IDs
	SELECT total_count, ARRAY(
		SELECT * 
		FROM (
			SELECT * 
			FROM documents_query
			-- ORDER BY document_id
			LIMIT take_count OFFSET offset_count
		) AS subquery
	) INTO total_count_temp, document_ids_temp
	FROM counted_documents;
    
    -- Set out parameters
    total_count_out := total_count_temp;
    document_ids := document_ids_temp;
END;
$$ LANGUAGE plpgsql;
