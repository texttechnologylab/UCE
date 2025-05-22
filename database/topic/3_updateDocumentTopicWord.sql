INSERT INTO documenttopicwords
SELECT
    st.document_id,
    tvb.value AS topiclabel,
    tw.word,
    SUM(tw.probability * st.thetast)
        / SUM(SUM(tw.probability * st.thetast)) OVER (PARTITION BY st.document_id, tvb.value) AS probability
FROM sentencetopics AS st
         JOIN topicvaluebase tvb ON tvb.id = st.topicinstance_id
         JOIN topicword tw ON tw.topic_id = tvb.id
GROUP BY st.document_id, tvb.value, tw.word
ORDER BY st.document_id, tvb.value, probability DESC;