CREATE OR REPLACE FUNCTION refresh_links()
RETURNS INTEGER
LANGUAGE plpgsql AS $$
DECLARE
    updated_count INTEGER := 0;
    temp_count INTEGER := 0;
    rec RECORD;
    annotation_id INTEGER;
    doc_internal_id INTEGER;
BEGIN
    ----------- Update fromId if null or 0 foreach link table we have
    UPDATE documentlink dl
    SET fromId = d.id
    FROM document d
    WHERE (dl.fromId IS NULL OR dl.fromId = 0)
      AND dl.corpusid = d.corpusid
      AND dl.fromm = d.documentid;

    GET DIAGNOSTICS temp_count = ROW_COUNT;
    updated_count := updated_count + temp_count;

    UPDATE documenttoannotationlink dl
    SET fromId = d.id
    FROM document d
    WHERE (dl.fromId IS NULL OR dl.fromId = 0)
      AND dl.corpusid = d.corpusid
      AND dl.fromm = d.documentid;

    GET DIAGNOSTICS temp_count = ROW_COUNT;
    updated_count := updated_count + temp_count;

    ----------- Update toId if null or 0 foreach link table we have
    UPDATE documentlink dl
    SET toId = d.id
    FROM document d
    WHERE (dl.toId IS NULL OR dl.toId = 0)
      AND dl.corpusid = d.corpusid
      AND dl.too = d.documentid;

    GET DIAGNOSTICS temp_count = ROW_COUNT;
    updated_count := updated_count + temp_count;

    ----------- Update toId in documenttoannotationlink using dynamic table lookup
    FOR rec IN
        SELECT id, corpusid, toannotationtypetable, tobegin, toend, too
        FROM documenttoannotationlink
        WHERE (toId IS NULL OR toId = 0)
    LOOP
        -- First, try to get the internal document ID
        SELECT id INTO doc_internal_id
        FROM document
        WHERE corpusid = rec.corpusid AND documentid = rec.too;

        IF doc_internal_id IS NOT NULL THEN
            BEGIN
                -- Now fetch the annotation ID using dynamic SQL
                EXECUTE format(
                    'SELECT id FROM %I WHERE document_id = $1 AND beginn = $2 AND endd = $3 LIMIT 1',
                    rec.toannotationtypetable
                )
                INTO annotation_id
                USING doc_internal_id, rec.tobegin, rec.toend;

                IF annotation_id IS NOT NULL THEN
                    UPDATE documenttoannotationlink
                    SET toId = annotation_id
                    WHERE id = rec.id;

                    updated_count := updated_count + 1;
                END IF;
            EXCEPTION WHEN OTHERS THEN
                RAISE NOTICE 'Failed dynamic annotation lookup for rec.id=%, table=%.', rec.id, rec.toannotationtypetable;
            END;
        ELSE
            RAISE NOTICE 'Document not found for corpusid=%, documentid=%', rec.corpusid, rec.too;
        END IF;
    END LOOP;

    RETURN updated_count;
END;
$$;
