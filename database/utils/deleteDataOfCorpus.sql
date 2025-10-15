SET session_replication_role = 'replica';

DO $$
DECLARE 
    corpus_id INTEGER := 5;
BEGIN
    DELETE FROM documentchunkembeddings 
    USING document 
    WHERE documentchunkembeddings.document_id = document.id AND document.corpusid = corpus_id;

	DELETE FROM documentembeddings 
    USING document 
    WHERE documentembeddings.document_id = document.id AND document.corpusid = corpus_id;

	DELETE FROM ucemetadata 
    USING document 
    WHERE ucemetadata.document_id = document.id AND document.corpusid = corpus_id;

	DELETE FROM ucemetadatafilter 
    WHERE ucemetadatafilter.corpusid = corpus_id;

	DELETE FROM documentsentenceembeddings 
    USING document 
    WHERE documentsentenceembeddings.document_id = document.id AND document.corpusid = corpus_id;

    DELETE FROM biofidtaxon 
    USING document 
    WHERE biofidtaxon.document_id = document.id AND document.corpusid = corpus_id;

    DELETE FROM lemma 
    USING document 
    WHERE lemma.document_id = document.id AND document.corpusid = corpus_id;

    DELETE FROM namedentity 
    USING document 
    WHERE namedentity.document_id = document.id AND document.corpusid = corpus_id;

    DELETE FROM geoname 
    USING document 
    WHERE geoname.document_id = document.id AND document.corpusid = corpus_id;

    DELETE FROM time 
    USING document 
    WHERE time.document_id = document.id AND document.corpusid = corpus_id;

    DELETE FROM sentence 
    USING document 
    WHERE sentence.document_id = document.id AND document.corpusid = corpus_id;

    DELETE FROM srlink 
    USING document 
    WHERE srlink.document_id = document.id AND document.corpusid = corpus_id;

    DELETE FROM gazetteertaxon 
    USING document 
    WHERE gazetteertaxon.document_id = document.id AND document.corpusid = corpus_id;

    DELETE FROM gnfindertaxon 
    USING document 
    WHERE gnfindertaxon.document_id = document.id AND document.corpusid = corpus_id;

    DELETE FROM page 
    USING document 
    WHERE page.document_id = document.id AND document.corpusid = corpus_id;

    DELETE FROM documentlink 
    WHERE documentlink.corpusid = corpus_id;
	
	DELETE FROM annotationlink 
    WHERE annotationlink.corpusid = corpus_id;

    DELETE FROM documenttoannotationlink 
    WHERE documenttoannotationlink.corpusid = corpus_id;

    DELETE FROM annotationtodocumentlink 
    WHERE annotationtodocumentlink.corpusid = corpus_id;

    DELETE FROM document 
    WHERE corpusid = corpus_id;
END $$;

SET session_replication_role = 'origin';