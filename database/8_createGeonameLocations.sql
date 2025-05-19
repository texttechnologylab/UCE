-- We go through geonames and build the more complex geography type for faster 
-- postgis queries. I tried doing this with hibernate but it didn't work really.
CREATE OR REPLACE FUNCTION update_geoname_locations()
RETURNS integer AS $$
DECLARE
    updated_count integer;
BEGIN
    -- Add the 'location' column if it doesn't exist
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'geoname' AND column_name = 'location'
    ) THEN
        ALTER TABLE geoname
        ADD COLUMN location geography(Point, 4326);
    END IF;

    -- Update location where it is NULL and latitude/longitude are not NULL
    UPDATE geoname
    SET location = ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)
    WHERE location IS NULL
      AND latitude IS NOT NULL
      AND longitude IS NOT NULL;

    GET DIAGNOSTICS updated_count = ROW_COUNT;

    RETURN updated_count;
END;
$$ LANGUAGE plpgsql;
