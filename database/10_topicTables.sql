CREATE TABLE IF NOT EXISTS sentencetopics (
                                               sentence_id BIGINT,
                                               document_id BIGINT,
                                               topicinstance_id BIGINT, -- refers to topicvaluebase.id
                                               topiclabel VARCHAR(255), -- refers to topicvaluebase.value
                                               thetast DOUBLE PRECISION
);
CREATE TABLE IF NOT EXISTS documenttopicsraw (
                                                   document_id BIGINT,
                                                   topiclabel VARCHAR(255),
                                                   rawscore DOUBLE PRECISION,
                                                   thetadt DOUBLE PRECISION
);