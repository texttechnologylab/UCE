package org.texttechnologylab.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.texttechnologylab.exceptions.DatabaseOperationException;
import org.texttechnologylab.exceptions.ExceptionUtils;
import org.texttechnologylab.models.dto.map.MapClusterDto;
import org.texttechnologylab.models.dto.map.PointDto;

import java.awt.*;
import java.util.List;

@Service
public class MapService {
    private static final Logger logger = LogManager.getLogger(MapService.class);

    private final PostgresqlDataInterface_Impl db;

    public MapService(PostgresqlDataInterface_Impl db) {
        this.db = db;
    }

    /**
     * This fetches single points of {Geoname,Time} -> Annotation links, such that you can query occurrences
     * of annotations (taxa, ner e.g.) by a given location viewport and a time range.
     */
    public List<PointDto> getGeoNameTimelineLinks(double minLng,
                                              double minLat,
                                              double maxLng,
                                              double maxLat,
                                              java.sql.Date fromDate,
                                              java.sql.Date toDate,
                                              long corpusId) throws DatabaseOperationException {
        return db.getGeonameTimelineLinks(minLng, minLat, maxLng, maxLat, fromDate, toDate, corpusId);
    }

    /**
     * This fetches pre-cached clusters of GeoNames that have an annotationlink to other annotations. It also
     * checks for Time links, such that you can not only query by a location viewport, but also by time.
     */
    public List<MapClusterDto> getTimelineMapClusters(double minLng,
                                              double minLat,
                                              double maxLng,
                                              double maxLat,
                                              double gridSize,
                                              java.sql.Date fromDate,
                                              java.sql.Date toDate,
                                              long corpusId) throws DatabaseOperationException {
        return db.getGeonameClustersFromTimelineMap(minLng, minLat, maxLng, maxLat, gridSize, fromDate, toDate, corpusId);
    }

    public boolean cachedTimelineMapHasEntries() {
        var entries = ExceptionUtils.tryCatchLog(
                () -> db.executeSqlWithReturn("SELECT context_count from geoname_context_timeline_cache LIMIT 1"),
                (ex) -> logger.error("Error getting the count of the cached timeline map.", ex));
        return entries != null && !entries.isEmpty();
    }

    /**
     * Refreshes the Materialized View that acts as a cache between GeoName,Time -> annotationlinks
     */
    public void refreshCachedTimelineMap(boolean force) throws DatabaseOperationException {
        if (force || !cachedTimelineMapHasEntries())
            db.executeSqlWithoutReturn("REFRESH MATERIALIZED VIEW geoname_context_timeline_cache");
    }
}
