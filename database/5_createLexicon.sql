-- We want to build a lexicon and for that, we need distinct values of all our annotations.
-- We do this by adding a procedure, that checks if that coveredtext 
-- has already been added to our "lexicon" table and if not, we add it there.
CREATE TABLE IF NOT EXISTS lexicon (
    coveredtext TEXT,
    typee VARCHAR,
    count INT DEFAULT 1,
    PRIMARY KEY (coveredtext, typee)
);

CREATE OR REPLACE FUNCTION refresh_lexicon(tables TEXT[])
RETURNS INTEGER
LANGUAGE plpgsql AS $$
DECLARE
    table_name TEXT;
    dyn_sql TEXT;
    total_new_entries INTEGER := 0;
    inserted_count INTEGER;
BEGIN
    FOREACH table_name IN ARRAY tables
    LOOP
        dyn_sql := format($f$
            WITH new_lex AS (
                SELECT coveredtext, COUNT(*) AS cnt
                FROM %I
                WHERE coveredtext IS NOT NULL AND NOT isLexicalized
                GROUP BY coveredtext
            ),
            inserted AS (
                INSERT INTO lexicon (coveredtext, typee, count)
                SELECT nl.coveredtext, %L, nl.cnt
                FROM new_lex nl
                ON CONFLICT (coveredtext, typee) DO NOTHING
                RETURNING 1
            )
            SELECT COUNT(*) FROM inserted;
        $f$, table_name, table_name);

        EXECUTE dyn_sql INTO inserted_count;
        total_new_entries := total_new_entries + COALESCE(inserted_count, 0);

        -- Now update isLexicalized flag
        dyn_sql := format($f$
            UPDATE %I
            SET isLexicalized = TRUE
            WHERE coveredtext IS NOT NULL AND NOT isLexicalized;
        $f$, table_name);

        EXECUTE dyn_sql;
    END LOOP;

    RETURN total_new_entries;
END;
$$;


