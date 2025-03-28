-- Whenever a new page entity is added. We need that column for faster fulltext searches.

ALTER TABLE page ADD COLUMN IF NOT EXISTS textsearch tsvector;

CREATE OR REPLACE FUNCTION update_textsearch()
RETURNS TRIGGER AS $$
BEGIN
    NEW.textsearch := to_tsvector(
        'simple',
        (SELECT documenttitle FROM document WHERE id = NEW.document_id) || ' ' || NEW.coveredtext
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$ 
DECLARE v_exists INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_exists FROM pg_trigger WHERE tgname = 'trigger_update_textsearch';

    IF v_exists = 0 THEN
        EXECUTE 'CREATE TRIGGER trigger_update_textsearch
                 BEFORE INSERT OR UPDATE ON page
                 FOR EACH ROW
                 EXECUTE FUNCTION update_textsearch();';
    END IF;
END $$;
