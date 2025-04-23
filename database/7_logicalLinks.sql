-- This function goes through the logical links that haven't been correctly connected by their
-- respective ids of e.g. documents and updates those. Since we can't know the order of the importing
-- documents and links, we need to do this periodically.
CREATE OR REPLACE FUNCTION refresh_links()
RETURNS INTEGER
LANGUAGE plpgsql AS $$
DECLARE
    updated_count INTEGER := 0;
    temp_count INTEGER := 0;
BEGIN
    -- Update fromId if null or 0
    UPDATE documentlink dl
    SET fromId = d.id
    FROM document d
    WHERE (dl.fromId IS NULL OR dl.fromId = 0)
      AND dl.corpusid = d.corpusid
      AND dl.fromm = d.documentid;

    GET DIAGNOSTICS temp_count = ROW_COUNT;
    updated_count := updated_count + temp_count;

    -- Update toId if null or 0
    UPDATE documentlink dl
    SET toId = d.id
    FROM document d
    WHERE (dl.toId IS NULL OR dl.toId = 0)
      AND dl.corpusid = d.corpusid
      AND dl.too = d.documentid;

    GET DIAGNOSTICS temp_count = ROW_COUNT;
    updated_count := updated_count + temp_count;

    RETURN updated_count;
END;
$$;

