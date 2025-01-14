-- Create a trigger function that automatically creates a new tsvector fulltextsearch vector, 
-- whenever a new page entity is added. We need that column for faster fulltext searches.
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

CREATE TRIGGER trigger_update_textsearch
BEFORE INSERT OR UPDATE ON page
FOR EACH ROW
EXECUTE FUNCTION update_textsearch();
