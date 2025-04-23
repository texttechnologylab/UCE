CREATE TABLE IF NOT EXISTS sentencetopics (
                                               sentence_id BIGINT,
                                               document_id BIGINT,
                                               topicinstance_id BIGINT, -- refers to topicvaluebase.id
                                               topiclabel VARCHAR(255), -- refers to topicvaluebase.value
                                               thetast DOUBLE PRECISION
);
TRUNCATE TABLE sentencetopics;
INSERT INTO sentencetopics
SELECT
    ut.id AS sentence_id,
    ut.document_id,
    tvbws.id AS topicinstance_id,
    tvb.value AS topiclabel,
    tvbws.score AS thetast
FROM unifiedtopic ut
         JOIN topicvaluebase tvb ON tvb.unifiedtopic_id = ut.id
         JOIN topicvaluebasewithscore tvbws ON tvbws.id = tvb.id
WHERE tvbws.score > 0.0;