-- We want to build a lexicon and for that, we need distinct values of all our annotations.
-- We do this by adding a trigger that, upon changing a list of tables, checks of that coveredtext 
-- has already been added to our "lexicon" table and if not, we add it there.
CREATE TABLE IF NOT EXISTS lexicon (
    coveredtext TEXT,
    typee VARCHAR,
    count INT DEFAULT 1,
    PRIMARY KEY (coveredtext, typee)
);

-- And create the trigger:
CREATE OR REPLACE FUNCTION update_lexicon()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.coveredtext IS NOT NULL THEN
        INSERT INTO lexicon (coveredtext, typee, count)
        VALUES (NEW.coveredtext, TG_TABLE_NAME, 1)
        -- If a duplicate is being inserted, it conflicts. In that case, count up.
        ON CONFLICT (coveredtext, typee)
        DO UPDATE SET count = lexicon.count + 1;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- TRIGGER_TEMPLATE
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_trigger
        WHERE tgname = 'trg_update_lexicon_-TABLE-'
    ) THEN
        CREATE TRIGGER trg_update_lexicon_-TABLE-
        AFTER INSERT OR UPDATE ON -TABLE-
        FOR EACH ROW
        EXECUTE FUNCTION update_lexicon();
    END IF;
END;
-- TRIGGER_TEMPLATE_END

-- This placeholder will be filled in programmatically by UCE when building the lexicon
[TRIGGERS]