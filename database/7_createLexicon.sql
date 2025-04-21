-- Now create the refresh_lexicon stored procedure
CREATE OR REPLACE FUNCTION refresh_lexicon(tables TEXT[], force BOOLEAN DEFAULT FALSE)
RETURNS INTEGER
LANGUAGE plpgsql AS $$
DECLARE
    table_name TEXT;
    dyn_sql TEXT;
    total_new_entries INTEGER := 0;
    inserted_count INTEGER;
BEGIN

    -- If we force, delete all entries from the lexicon
    IF force THEN
        EXECUTE 'DELETE FROM lexicon';
    END IF;

    FOREACH table_name IN ARRAY tables
    LOOP
        -- Dynamically build SQL based on the value of 'force'
        dyn_sql := format($f$
            WITH new_lex AS (
                SELECT
                    coveredtext,
                    COUNT(*) AS cnt,
                    LEFT(coveredtext, 1) AS startchar
                FROM %I
                WHERE coveredtext IS NOT NULL
                %s
                GROUP BY coveredtext
            ),
            inserted AS (
                INSERT INTO lexicon (coveredtext, typee, count, startcharacter)
                SELECT nl.coveredtext, %L, nl.cnt, LOWER(nl.startchar)
                FROM new_lex nl
                ON CONFLICT (coveredtext, typee) DO NOTHING
                RETURNING 1
            )
            SELECT COUNT(*) FROM inserted;
        $f$,
        table_name,
        CASE WHEN force THEN '' ELSE 'AND (NOT isLexicalized OR isLexicalized IS NULL)' END,
        table_name);

        EXECUTE dyn_sql INTO inserted_count;
        total_new_entries := total_new_entries + COALESCE(inserted_count, 0);

        -- Update isLexicalized flag if not in force mode
        IF NOT force THEN
            dyn_sql := format($f$
                UPDATE %I
                SET isLexicalized = TRUE
                WHERE coveredtext IS NOT NULL AND NOT isLexicalized;
            $f$, table_name);

            EXECUTE dyn_sql;
        END IF;
    END LOOP;

    RETURN total_new_entries;
END;
$$;


