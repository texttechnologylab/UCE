CREATE OR REPLACE FUNCTION find_similar_topics(
    label_value TEXT,
    minSharedWords INTEGER,
    result_limit INTEGER,
    target_corpusid BIGINT
)
RETURNS TABLE (
    topiclabel VARCHAR(255),
    sharedwords BIGINT
)
AS $$
BEGIN
RETURN QUERY
    WITH sourcewords AS (
        SELECT tw.word, tw.probability
        FROM topicvaluebase tvb
        JOIN topicword tw ON tw.topic_id = tvb.id
        JOIN document d ON tvb.document_id = d.id
        WHERE tvb.value = label_value
          AND d.corpusid = target_corpusid
        ORDER BY tw.probability DESC
        LIMIT 10
    ),
    similartopicsraw AS (
        SELECT
            tvb.value AS topiclabel,
            COUNT(DISTINCT tw.word) AS sharedwords
        FROM topicvaluebase tvb
        JOIN topicword tw ON tw.topic_id = tvb.id
        JOIN document d ON tvb.document_id = d.id
        JOIN sourcewords sw ON sw.word = tw.word
        WHERE tvb.value != label_value
          AND d.corpusid = target_corpusid
        GROUP BY tvb.value
        HAVING COUNT(DISTINCT tw.word) >= minSharedWords
        ORDER BY sharedwords DESC
    )
SELECT * FROM similartopicsraw
                  LIMIT result_limit;
END;
$$ LANGUAGE plpgsql;
