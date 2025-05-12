CREATE OR REPLACE FUNCTION get_normalized_topic_scores(corpusid BIGINT)
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
             SELECT id FROM document WHERE document.corpusid = get_normalized_topic_scores.corpusid
         )
         GROUP BY topiclabel
     ) subquery
ORDER BY normalized_score DESC;
END;
$$ LANGUAGE plpgsql;