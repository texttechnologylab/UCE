-- Gets take_count of each annotation type, counts them, sorts them and returns them as a table.

CREATE OR REPLACE FUNCTION get_corpus_annotations(
    corpusid_val BIGINT, 
    take_count INTEGER, 
    offset_count INTEGER
)

RETURNS TABLE (
    annotation_text TEXT,
    annotation_count BIGINT,
    annotation_type TEXT
) AS $$

BEGIN
    RETURN QUERY
	
    WITH time_counts AS (
        SELECT 
            t.coveredtext, 
            COUNT(*) AS amount,
            'time' AS type
        FROM time t
        WHERE t.document_id IN (SELECT d.id FROM document d WHERE d.corpusid = corpusid_val)
        GROUP BY t.coveredtext
    ),
	
    namedentity_counts AS (
        SELECT 
            n.coveredtext, 
            COUNT(*) AS amount,
            n.typee AS type
        FROM namedentity n
        WHERE n.document_id IN (SELECT d.id FROM document d WHERE d.corpusid = corpusid_val)
        GROUP BY n.coveredtext, n.typee
    ),
	
    taxon_counts AS (
        SELECT 
            x.coveredtext, 
            COUNT(*) AS amount,
            'taxon' AS type
        FROM taxon x
        WHERE x.document_id IN (SELECT d.id FROM document d WHERE d.corpusid = corpusid_val)
        GROUP BY x.coveredtext
    ),
	
    combined_counts AS (
        SELECT coveredtext, amount, type FROM time_counts
        UNION ALL
        SELECT coveredtext, amount, type FROM namedentity_counts
        UNION ALL
        SELECT coveredtext, amount, type FROM taxon_counts
    ),
	
    ranked_annotations AS (
        SELECT
            cc.coveredtext,
            SUM(cc.amount)::BIGINT AS annotation_count,
            cc.type AS annotation_type,
            ROW_NUMBER() OVER (PARTITION BY cc.type ORDER BY SUM(cc.amount) DESC, cc.coveredtext ASC) AS rn
        FROM combined_counts cc
        GROUP BY cc.coveredtext, cc.type
    )
	
    SELECT
        ra.coveredtext AS annotation_text,
        ra.annotation_count,
        ra.annotation_type
    FROM ranked_annotations ra
    WHERE ra.rn > offset_count AND ra.rn <= (offset_count + take_count)
    ORDER BY ra.annotation_type, ra.annotation_count DESC, ra.coveredtext ASC;
END;
$$ LANGUAGE plpgsql;
