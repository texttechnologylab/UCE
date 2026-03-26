-- DISABLED: Safe tsquery construction functions for robust full-text search
-- Prevents tsquery syntax errors and handles large term expansions gracefully

-- Function: safe_to_tsquery
-- Creates a tsquery from an array of terms with safety limits and error handling
-- DISABLED: Use baseline PostgreSQL tsquery functions instead
/*
CREATE OR REPLACE FUNCTION safe_to_tsquery(
    config regconfig DEFAULT 'simple',
    terms text[] DEFAULT NULL,
    max_terms integer DEFAULT 100,
    max_term_length integer DEFAULT 100,
    max_total_length integer DEFAULT 100000
) RETURNS tsquery AS $$
DECLARE
    safe_terms text[];
    term text;
    query_text text;
    term_count integer;
BEGIN
    -- Return NULL if no terms
    IF terms IS NULL OR array_length(terms, 1) = 0 THEN
        RETURN NULL;
    END IF;
    
    -- Limit number of terms
    term_count := LEAST(array_length(terms, 1), max_terms);
    safe_terms := terms[1:term_count];
    
    -- Process each term: trim, limit length, escape
    FOR i IN 1..array_length(safe_terms, 1) LOOP
        term := safe_terms[i];
        
        -- Trim whitespace
        term := trim(term);
        
        -- Skip empty terms
        IF term = '' THEN
            safe_terms[i] := NULL;
            CONTINUE;
        END IF;
        
        -- Limit term length
        IF length(term) > max_term_length THEN
            term := substring(term from 1 for max_term_length);
        END IF;
        
        -- Escape special tsquery characters: !, &, |, *, :, (, ), <, >
        -- PostgreSQL tsquery expects single quotes around terms with special chars
        term := replace(term, '''', ''''''); -- Escape single quotes
        term := replace(term, '\', '\\'); -- Escape backslashes
        
        -- Check if term needs quoting (contains special characters or spaces)
        IF term ~ '[!&|*:()<>''\s]' THEN
            term := '''' || term || '''';
        END IF;
        
        safe_terms[i] := term;
    END LOOP;
    
    -- Remove NULL terms
    safe_terms := array_remove(safe_terms, NULL);
    
    -- Check if we have any terms left
    IF array_length(safe_terms, 1) = 0 THEN
        RETURN NULL;
    END IF;
    
    -- Build query text with OR operator
    query_text := array_to_string(safe_terms, ' | ');
    
    -- Check total length
    IF length(query_text) > max_total_length THEN
        RAISE WARNING 'tsquery too long (% bytes), truncating to % bytes', 
            length(query_text), max_total_length;
        query_text := substring(query_text from 1 for max_total_length);
    END IF;
    
    -- Attempt to create tsquery
    BEGIN
        RETURN to_tsquery(config, query_text);
    EXCEPTION
        WHEN OTHERS THEN
            -- Log error and fallback to simple search with first few terms
            RAISE WARNING 'Failed to create tsquery: % (query: %)', SQLERRM, query_text;
            
            -- Try with just the first 10 terms
            IF array_length(safe_terms, 1) > 10 THEN
                query_text := array_to_string(safe_terms[1:10], ' | ');
                BEGIN
                    RETURN to_tsquery(config, query_text);
                EXCEPTION
                    WHEN OTHERS THEN
                        -- Ultimate fallback: empty query
                        RETURN to_tsquery(config, '');
                END;
            ELSE
                -- Ultimate fallback: empty query
                RETURN to_tsquery(config, '');
            END IF;
    END;
END;
$$ LANGUAGE plpgsql IMMUTABLE STRICT;

-- Function: safe_websearch_to_tsquery
-- Wrapper for websearch_to_tsquery with safety limits
CREATE OR REPLACE FUNCTION safe_websearch_to_tsquery(
    config regconfig DEFAULT 'simple',
    query_text text DEFAULT NULL,
    max_length integer DEFAULT 100000
) RETURNS tsquery AS $$
BEGIN
    -- Return NULL if no query
    IF query_text IS NULL OR trim(query_text) = '' THEN
        RETURN NULL;
    END IF;
    
    -- Limit query length
    IF length(query_text) > max_length THEN
        RAISE WARNING 'websearch query too long (% bytes), truncating to % bytes', 
            length(query_text), max_length;
        query_text := substring(query_text from 1 for max_length);
    END IF;
    
    -- Attempt to create tsquery
    BEGIN
        RETURN websearch_to_tsquery(config, query_text);
    EXCEPTION
        WHEN OTHERS THEN
            -- Log error and fallback
            RAISE WARNING 'Failed to create websearch tsquery: % (query: %)', SQLERRM, query_text;
            
            -- Try with simplified query (remove problematic characters)
            query_text := regexp_replace(query_text, '[!&|*:()<>''"]', ' ', 'g');
            query_text := regexp_replace(query_text, '\s+', ' ', 'g');
            query_text := trim(query_text);
            
            IF query_text = '' THEN
                RETURN to_tsquery(config, '');
            END IF;
            
            BEGIN
                RETURN websearch_to_tsquery(config, query_text);
            EXCEPTION
                WHEN OTHERS THEN
                    -- Ultimate fallback: empty query
                    RETURN to_tsquery(config, '');
            END;
    END;
END;
$$ LANGUAGE plpgsql IMMUTABLE STRICT;

-- Function: build_safe_search_query
-- Builds a search query from expanded terms with proper handling for pro mode
CREATE OR REPLACE FUNCTION build_safe_search_query(
    config regconfig DEFAULT 'simple',
    expanded_terms text[] DEFAULT NULL,
    original_query text DEFAULT NULL,
    pro_mode boolean DEFAULT false,
    max_terms integer DEFAULT 100
) RETURNS tsquery AS $$
DECLARE
    effective_terms text[];
    query_text text;
BEGIN
    -- Use expanded terms if available, otherwise use original query
    IF expanded_terms IS NOT NULL AND array_length(expanded_terms, 1) > 0 THEN
        -- For pro mode with expanded terms, we need to handle differently
        IF pro_mode THEN
            -- In pro mode with expansions, we need to preserve the original query structure
            -- but replace terms with their expansions
            IF original_query IS NOT NULL AND original_query != '' THEN
                -- This is complex - for now, use safe_to_tsquery with OR logic
                -- TODO: Implement proper AST-based expansion for pro mode
                RETURN safe_to_tsquery(config, expanded_terms, max_terms);
            ELSE
                -- No original query structure, use OR logic
                RETURN safe_to_tsquery(config, expanded_terms, max_terms);
            END IF;
        ELSE
            -- Non-pro mode: use websearch_to_tsquery for robustness
            query_text := array_to_string(expanded_terms, ' OR ');
            RETURN safe_websearch_to_tsquery(config, query_text);
        END IF;
    ELSE
        -- No expanded terms, use original query
        IF pro_mode THEN
            -- Pro mode: use to_tsquery (supports operators)
            BEGIN
                RETURN to_tsquery(config, original_query);
            EXCEPTION
                WHEN OTHERS THEN
                    -- Fallback to websearch for malformed pro mode queries
                    RAISE WARNING 'Pro mode query failed, falling back to websearch: %', SQLERRM;
                    RETURN safe_websearch_to_tsquery(config, original_query);
            END;
        ELSE
            -- Non-pro mode: use websearch_to_tsquery
            RETURN safe_websearch_to_tsquery(config, original_query);
        END IF;
    END IF;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Function: chunk_array
-- Utility function to split array into chunks
CREATE OR REPLACE FUNCTION chunk_array(
    arr text[],
    chunk_size integer
) RETURNS text[][] AS $$
DECLARE
    chunks text[][];
    num_chunks integer;
    i integer;
BEGIN
    IF arr IS NULL OR array_length(arr, 1) IS NULL THEN
        RETURN ARRAY[]::text[][];
    END IF;
    
    num_chunks := ceil(array_length(arr, 1)::float / chunk_size);
    chunks := array_fill(NULL::text[], ARRAY[num_chunks]);
    
    FOR i IN 1..num_chunks LOOP
        chunks[i] := arr[((i-1)*chunk_size + 1):LEAST(i*chunk_size, array_length(arr, 1))];
    END LOOP;
    
    RETURN chunks;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Test the functions
COMMENT ON FUNCTION safe_to_tsquery(regconfig, text[], integer, integer, integer) IS 
    'Creates a tsquery from terms with safety limits and error handling';

COMMENT ON FUNCTION safe_websearch_to_tsquery(regconfig, text, integer) IS 
    'Wrapper for websearch_to_tsquery with safety limits';

COMMENT ON FUNCTION build_safe_search_query(regconfig, text[], text, boolean, integer) IS 
    'Builds search query from expanded terms with pro mode support';

COMMENT ON FUNCTION chunk_array(text[], integer) IS 
    'Splits text array into chunks of specified size';