INSERT INTO corpustopicwords
SELECT
    d.corpusid,
    dtw.topiclabel,
    dtw.word,
    SUM(dtw.probability)
        / SUM(SUM(dtw.probability)) OVER (PARTITION BY d.corpusid, dtw.topiclabel) AS probability
FROM documenttopicwords dtw
         JOIN document d ON dtw.document_id = d.document_id
GROUP BY d.corpusid, dtw.topiclabel, dtw.word
ORDER BY d.corpusid, dtw.topiclabel, probability DESC;