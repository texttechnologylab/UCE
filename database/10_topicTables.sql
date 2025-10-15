CREATE TABLE IF NOT EXISTS sentencetopics (
                                               unifiedtopic_id BIGINT,
                                               document_id BIGINT,
                                               sentence_id BIGINT,
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
CREATE TABLE IF NOT EXISTS documenttopicwords (
                                                  document_id BIGINT,
                                                  topiclabel VARCHAR(255),
    word VARCHAR(255),
    probability DOUBLE PRECISION
    );
CREATE TABLE IF NOT EXISTS corpustopicwords (
                                                corpus_id BIGINT,
                                                topiclabel VARCHAR(255),
    word VARCHAR(255),
    probability DOUBLE PRECISION
    );