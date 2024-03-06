CREATE OR REPLACE FUNCTION biofid_search_layer_named_entities(
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
        
        UNION
        
        SELECT DISTINCT d.id
        FROM document d 
        JOIN namedentity ne ON d.id = ne.document_id 
        WHERE ne.coveredtext = ANY(input1)
        
        UNION
        
        SELECT DISTINCT d.id
        FROM document d 
        JOIN time t ON d.id = t.document_id
        WHERE t.coveredtext = ANY(input1)
        
        UNION
        
        SELECT DISTINCT d.id
        FROM document d
        JOIN taxon ta ON d.id = ta.document_id
        WHERE ta.coveredtext = ANY(input1)
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
