SET session_replication_role = 'replica';

DELETE FROM biofidtaxon 
USING document 
WHERE biofidtaxon.document_id = document.id AND document.corpusid = 4;

DELETE FROM lemma 
USING document 
WHERE lemma.document_id = document.id AND document.corpusid = 4;

DELETE FROM namedentity 
USING document 
WHERE namedentity.document_id = document.id AND document.corpusid = 4;

DELETE FROM time 
USING document 
WHERE time.document_id = document.id AND document.corpusid = 4;

DELETE FROM sentence 
USING document 
WHERE sentence.document_id = document.id AND document.corpusid = 4;

DELETE FROM srlink 
USING document 
WHERE srlink.document_id = document.id AND document.corpusid = 4;

DELETE FROM taxon 
USING document 
WHERE taxon.document_id = document.id AND document.corpusid = 4;

DELETE FROM page 
USING document 
WHERE page.document_id = document.id AND document.corpusid = 4;

DELETE FROM document WHERE corpusid = 4;

SET session_replication_role = 'origin';