-- Add the 'location' column if it doesn't exist
-- Add a routine that builds the special geopgrahy types of our geonames
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'geoname' AND column_name = 'location_geog'
    ) THEN
        ALTER TABLE geoname ADD COLUMN location_geog geography(Point, 4326);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'geoname' AND column_name = 'location_geom'
    ) THEN
        ALTER TABLE geoname ADD COLUMN location_geom geometry(Point, 4326);
    END IF;
END $$;


-- Create or replace the function to update the geoname.location column
CREATE OR REPLACE FUNCTION update_geoname_locations()
RETURNS integer AS $$
DECLARE
    updated_count integer;
BEGIN
    -- Update location where it is NULL and latitude/longitude are not NULL
    UPDATE geoname
    SET
        location_geog = ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography,
        location_geom = ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geometry
    WHERE location_geog IS NULL
      AND latitude IS NOT NULL
      AND longitude IS NOT NULL;

    GET DIAGNOSTICS updated_count = ROW_COUNT;

    RETURN updated_count;
END;
$$ LANGUAGE plpgsql;

-- Create a cached materialized view of logical links and geoname/time annotations
-- To refresh it, use "REFRESH MATERIALIZED VIEW [NAME]]"
CREATE MATERIALIZED VIEW IF NOT EXISTS geoname_context_timeline_cache AS
SELECT
    g.id AS geoname_id,
    g.name AS geoname_name,
    g.location_geom,
    al.corpusid,
    t.date,
	al.fromannotationtypetable,
    COUNT(*) AS context_count
FROM annotationlink al
JOIN geoname g ON g.id = al.toid AND al.toannotationtypetable = 'geoname'
INNER JOIN (
    SELECT al1.fromid, t.date
    FROM annotationlink al1
    JOIN time t ON al1.toid = t.id and al1.toannotationtypetable = 'time'
    WHERE al1.linkid = 'context' AND t.date IS NOT NULL
) t ON al.fromid = t.fromid
WHERE al.linkid = 'context' -- and al.fromannotationtypetable != 'namedEntity' 
GROUP BY
    g.id, g.name, g.location_geom, al.corpusid, t.date, al.fromannotationtypetable;
	

-- Query geoname markers on the premise of the annotationlink connection geoname -> annotation -> time
CREATE OR REPLACE FUNCTION uce_query_geoname_timeline_links(
    min_lng DOUBLE PRECISION,
    min_lat DOUBLE PRECISION,
    max_lng DOUBLE PRECISION,
    max_lat DOUBLE PRECISION,
    from_date DATE DEFAULT NULL,
    to_date DATE DEFAULT NULL,
    corpus BIGINT DEFAULT NULL,
    skip INTEGER DEFAULT 0,
    take INTEGER DEFAULT NULL,
    from_annotation_type_table TEXT DEFAULT NULL
)
RETURNS TABLE (
    lat DOUBLE PRECISION,
    lng DOUBLE PRECISION,
    fromcoveredtext TEXT,
    id BIGINT,
    annotationId BIGINT,
    annotationType TEXT,
    locationcoveredtext TEXT,
    location TEXT,
    date DATE,
    datecoveredtext TEXT
)
AS $$
BEGIN
    RETURN QUERY
    SELECT
        ST_Y(g.location_geom) AS lat,
        ST_X(g.location_geom) AS lng,
        al.fromcoveredtext,
        al.id AS id,
        al.fromid AS annotationId,
        al.fromannotationtype AS annotationType,
        al.tocoveredtext AS locationcoveredtext,
        g.name AS location,
        t.date,
        t.coveredtext AS datecoveredtext
    FROM geoname g
    JOIN annotationlink al 
		ON al.toid = g.id 
		AND al.toannotationtypetable = 'geoname'
		AND al.corpusid = corpus
    INNER JOIN (
        SELECT al1.fromid, t.date, t.coveredtext 
        FROM annotationlink al1
        JOIN time t ON al1.toid = t.id and al1.toannotationtypetable = 'time'
        WHERE al1.linkid = 'context' AND t.date IS NOT NULL AND al1.corpusid = corpus
    ) t ON al.fromid = t.fromid
    WHERE al.linkid = 'context' 
      --AND al.fromannotationtypetable != 'namedEntity'
      AND (
        from_annotation_type_table IS NULL 
        OR
        al.fromannotationtypetable = from_annotation_type_table 
      ) 
      AND ST_Within(
            g.location_geom,
            ST_MakeEnvelope(min_lng, min_lat, max_lng, max_lat, 4326)
          )
      AND (
            (from_date IS NULL AND to_date IS NULL)
            OR (t.date BETWEEN from_date AND to_date)
          )
    OFFSET skip
    LIMIT take;
END;
$$ LANGUAGE plpgsql STABLE;

-- Similar function used to query the context timelinecache we built in a materialized view. Example call:
CREATE OR REPLACE FUNCTION uce_query_clustered_geoname_timeline_cache(
    min_lng DOUBLE PRECISION,   -- Minimum longitude (left boundary of the map viewport)
    min_lat DOUBLE PRECISION,   -- Minimum latitude (bottom boundary of the map viewport)
    max_lng DOUBLE PRECISION,   -- Maximum longitude (right boundary of the map viewport)
    max_lat DOUBLE PRECISION,   -- Maximum latitude (top boundary of the map viewport)
    grid_size DOUBLE PRECISION, -- Spatial granularity: larger = fewer clusters, smaller = more detailed clusters (e.g., 0.1 ≈ ~11km)
    date_from DATE,             -- Start of the date range for filtering timeline annotations
    date_to DATE,               -- End of the date range for filtering timeline annotations
    corpus BIGINT               -- Corpus ID
)
RETURNS TABLE (
    count BIGINT,               -- Total number of context-linked annotations in the cluster
    lat DOUBLE PRECISION,       -- Latitude of the cluster centroid
    lng DOUBLE PRECISION        -- Longitude of the cluster centroid
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        -- Sum up the context counts from the materialized view, cast to BIGINT for consistency with function return type
        SUM(context_count)::BIGINT AS count,

        -- Calculate latitude and longitude of the cluster's centroid using PostGIS functions
        ST_Y(ST_Centroid(ST_Collect(location_geom))) AS lat,
        ST_X(ST_Centroid(ST_Collect(location_geom))) AS lng

    FROM geoname_context_timeline_cache

    WHERE
        -- Filter points within the specified map viewport
        ST_Within(
            location_geom,
            ST_MakeEnvelope(min_lng, min_lat, max_lng, max_lat, 4326)  -- Bounding box in WGS84 (EPSG:4326)
        )

        -- For now, no named entities
        --AND fromannotationtypetable != 'namedEntity' 

        -- Filter by date range
        AND (
            (date >= date_from AND date_from is not null AND date <= date_to AND date_to is not null)
            OR (date_from is null OR date_to is null)
        )

        -- Filter by specific corpus ID
        AND corpusid = corpus

    -- Group nearby points based on a fixed spatial grid to create clusters
    GROUP BY ST_SnapToGrid(location_geom, grid_size, grid_size);
END;
$$ LANGUAGE plpgsql;
