CREATE TABLE IF NOT EXISTS documenttopicsraw (
                                                   document_id BIGINT,
                                                   topiclabel VARCHAR(255),
    rawscore DOUBLE PRECISION,
    thetadt DOUBLE PRECISION
    );
TRUNCATE TABLE documenttopicsraw;
INSERT INTO documenttopicsraw
SELECT
    st.document_id,
    st.topiclabel,
    SUM(st.thetast) AS raw_score,
    SUM(st.thetast)
        / SUM(SUM(st.thetast)) OVER (PARTITION BY st.document_id) AS thetadt
FROM sentencetopics AS st
GROUP BY st.document_id, st.topiclabel;