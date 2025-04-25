SET session_replication_role = 'replica';

DELETE FROM biofidtaxon 
USING document 
WHERE biofidtaxon.document_id = document.id AND document.corpusid = 8;

DELETE FROM lemma 
USING document 
WHERE lemma.document_id = document.id AND document.corpusid = 8;

DELETE FROM namedentity 
USING document 
WHERE namedentity.document_id = document.id AND document.corpusid = 8;

DELETE FROM time 
USING document 
WHERE time.document_id = document.id AND document.corpusid = 8;

DELETE FROM sentence 
USING document 
WHERE sentence.document_id = document.id AND document.corpusid = 8;

DELETE FROM srlink 
USING document 
WHERE srlink.document_id = document.id AND document.corpusid = 8;

DELETE FROM taxon 
USING document 
WHERE taxon.document_id = document.id AND document.corpusid = 8;

DELETE FROM page 
USING document 
WHERE page.document_id = document.id AND document.corpusid = 8;

DELETE FROM documentlink 
WHERE documentlink.corpusid = 8;

DELETE FROM documenttoannotationlink 
WHERE documenttoannotationlink.corpusid = 8;

DELETE FROM document WHERE corpusid = 8;

SET session_replication_role = 'origin';