DROP FUNCTION IF EXISTS get_normalized_topic_scores(BIGINT, text, integer);

CREATE OR REPLACE FUNCTION get_normalized_topic_scores(
    p_corpusid BIGINT,
    p_user_name text DEFAULT NULL,
    p_min_level integer DEFAULT 1
    )
RETURNS TABLE(topic VARCHAR(255), normalized_score DOUBLE PRECISION) AS $$
BEGIN
RETURN QUERY
SELECT
    subquery.topiclabel,
    avg_thetadt / SUM(avg_thetadt) OVER () AS normalized_score
FROM (
         SELECT
             topiclabel AS topiclabel,
             AVG(thetadt) AS avg_thetadt
         FROM documenttopicsraw
         WHERE document_id IN (
             SELECT id
             FROM permitted_documents(get_normalized_topic_scores.p_user_name, get_normalized_topic_scores.p_min_level) pd
             WHERE pd.corpusid = get_normalized_topic_scores.p_corpusid
         )
         GROUP BY topiclabel
     ) subquery
ORDER BY normalized_score DESC;
END;
$$ LANGUAGE plpgsql;
