DO
$$
DECLARE
    rec RECORD;
BEGIN
    -- Disable triggers to avoid issues with foreign key constraints
    PERFORM pg_catalog.set_config('session_replication_role', 'replica', false);

    -- Loop through each table in the public schema
    FOR rec IN
        SELECT tablename
        FROM pg_tables
        WHERE schemaname = 'public'
    LOOP
        EXECUTE 'DELETE FROM ' || quote_ident(rec.tablename) || ' CASCADE';
    END LOOP;

    -- Re-enable triggers
    PERFORM pg_catalog.set_config('session_replication_role', 'origin', false);
END
$$;
