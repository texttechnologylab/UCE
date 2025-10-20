package org.texttechnologylab.uce.common.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.*;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.LongType;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.stereotype.Service;
import org.texttechnologylab.models.authentication.DocumentPermission;
import org.texttechnologylab.uce.common.annotations.Searchable;
import org.texttechnologylab.uce.common.config.HibernateConf;
import org.texttechnologylab.uce.common.exceptions.DatabaseOperationException;
import org.texttechnologylab.uce.common.exceptions.ExceptionUtils;
import org.texttechnologylab.uce.common.models.Linkable;
import org.texttechnologylab.uce.common.models.ModelBase;
import org.texttechnologylab.uce.common.models.UIMAAnnotation;
import org.texttechnologylab.uce.common.models.authentication.UceUser;
import org.texttechnologylab.uce.common.models.biofid.BiofidTaxon;
import org.texttechnologylab.uce.common.models.biofid.GazetteerTaxon;
import org.texttechnologylab.uce.common.models.biofid.GnFinderTaxon;
import org.texttechnologylab.uce.common.models.corpus.*;
import org.texttechnologylab.uce.common.models.corpus.links.*;
import org.texttechnologylab.uce.common.models.dto.UCEMetadataFilterDto;
import org.texttechnologylab.uce.common.models.dto.map.MapClusterDto;
import org.texttechnologylab.uce.common.models.dto.map.PointDto;
import org.texttechnologylab.uce.common.models.gbif.GbifOccurrence;
import org.texttechnologylab.uce.common.models.globe.GlobeTaxon;
import org.texttechnologylab.uce.common.models.imp.ImportLog;
import org.texttechnologylab.uce.common.models.imp.UCEImport;
import org.texttechnologylab.uce.common.models.negation.CompleteNegation;
import org.texttechnologylab.uce.common.models.search.*;
import org.texttechnologylab.uce.common.models.topic.TopicValueBase;
import org.texttechnologylab.uce.common.models.topic.TopicWord;
import org.texttechnologylab.uce.common.models.topic.UnifiedTopic;
import org.texttechnologylab.uce.common.models.util.HealthStatus;
import org.texttechnologylab.uce.common.utils.ReflectionUtils;
import org.texttechnologylab.uce.common.utils.StringUtils;
import org.texttechnologylab.uce.common.utils.SystemStatus;

import javax.persistence.NoResultException;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PostgresqlDataInterface_Impl implements DataInterface {
    private static final Logger logger = LogManager.getLogger(PostgresqlDataInterface_Impl.class);

    private final SessionFactory sessionFactory;

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(UCEMetadataValueType.class, new UCEMetadataValueTypeOrdinalAdapter())
            .create();

    private Session getCurrentSession() {
        return sessionFactory.openSession();
    }

    public PostgresqlDataInterface_Impl() {
        sessionFactory = HibernateConf.buildSessionFactory();
        TestConnection();
    }

    public void TestConnection() {
        try {
            var log = new UCELog("localhost", "TEST", "/", "Testing DB Connection", "/");
            saveUceLog(log);
            SystemStatus.PostgresqlDbStatus = new HealthStatus(true, "", null);
        } catch (Exception ex) {
            SystemStatus.PostgresqlDbStatus = new HealthStatus(false, "Couldn't build the session factory.", ex);
        }
    }

    public void calculateEffectivePermissions(String username, Set<String> groups) throws DatabaseOperationException {
        executeOperationSafely(session -> {
            // Build a Postgres array literal from the groups set
            // Escape single quotes to prevent SQL injection
            String groupArrayLiteral = groups.stream()
                    .map(s -> "'" + s.replace("'", "''") + "'")
                    .collect(Collectors.joining(", "));

            // If the set is empty, just use ARRAY[]::text[]
            if (groupArrayLiteral.isEmpty()) {
                groupArrayLiteral = ""; // Postgres accepts ARRAY[]::text[] as empty
            }

            String sql = String.format("""
            WITH all_permissions AS (
                SELECT dp.document_id, dp.level
                FROM documentpermissions dp
                WHERE dp.type = 1
                  AND dp.name = :username
                UNION ALL
                SELECT dp.document_id, dp.level
                FROM documentpermissions dp
                WHERE dp.type = 0
                  AND dp.name = ANY(ARRAY[%s])
            ),
            ranked AS (
                SELECT document_id, MAX(level) AS max_level
                FROM all_permissions
                GROUP BY document_id
            )
            INSERT INTO documentpermissions (document_id, name, type, level)
            SELECT r.document_id, :username, 2, r.max_level
            FROM ranked r
            ON CONFLICT (document_id, name, type)
            DO UPDATE SET level = EXCLUDED.level;
            """, groupArrayLiteral);

            var query = session.createNativeQuery(sql);
            query.setParameter("username", username);

            query.executeUpdate();
            return null;
        });
    }

    public void executeSqlWithoutReturn(String sql) throws DatabaseOperationException {
        executeOperationSafely(session -> {
            session.doWork(connection -> {
                try (var stmt = connection.prepareStatement(sql)) {
                    stmt.executeUpdate();
                }
            });
            return null;
        });
    }

    public List executeSqlWithReturn(String sql) throws DatabaseOperationException {
        return executeOperationSafely(session -> session.createNativeQuery(sql).getResultList());
    }

    public ArrayList<AnnotationSearchResult> getAnnotationsOfCorpus(long corpusId, int skip, int take) throws DatabaseOperationException {
        return executeOperationSafely((session) -> session.doReturningWork((connection) -> {

            DocumentSearchResult search = null;
            try (var storedProcedure = connection.prepareCall("{call get_corpus_annotations" + "(?, ?, ?)}")) {
                storedProcedure.setInt(1, (int) corpusId);
                storedProcedure.setInt(2, take);
                storedProcedure.setInt(3, skip);

                var result = storedProcedure.executeQuery();
                var annotations = new ArrayList<AnnotationSearchResult>();
                while (result.next()) {
                    var annotationSearchResult = new AnnotationSearchResult();
                    annotationSearchResult.setCoveredText(result.getString("annotation_text"));
                    annotationSearchResult.setOccurrences(result.getInt("annotation_count"));
                    annotationSearchResult.setInfo(result.getString("annotation_type"));
                    annotations.add(annotationSearchResult);
                }
                return annotations;
            }
        }));
    }

    public ArrayList<PointDto> getGeonameTimelineLinks(double minLng,
                                                       double minLat,
                                                       double maxLng,
                                                       double maxLat,
                                                       java.sql.Date fromDate,
                                                       java.sql.Date toDate,
                                                       long corpusId,
                                                       int skip,
                                                       int take,
                                                       String fromAnnotationTypeTable) throws DatabaseOperationException {
        return executeOperationSafely((session) -> session.doReturningWork((connection) -> {
            try (var storedProcedure = connection.prepareCall("{call uce_query_geoname_timeline_links" + "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}")) {
                storedProcedure.setDouble(1, minLng);
                storedProcedure.setDouble(2, minLat);
                storedProcedure.setDouble(3, maxLng);
                storedProcedure.setDouble(4, maxLat);
                storedProcedure.setDate(5, fromDate);
                storedProcedure.setDate(6, toDate);
                storedProcedure.setInt(7, (int) corpusId);
                storedProcedure.setInt(8, skip);
                storedProcedure.setInt(9, take);
                storedProcedure.setString(10, fromAnnotationTypeTable);

                var result = storedProcedure.executeQuery();
                var points = new ArrayList<PointDto>();
                while (result.next()) {
                    var pointDto = new PointDto();
                    pointDto.setId(result.getLong("id"));
                    pointDto.setAnnotationId(result.getLong("annotationId"));
                    pointDto.setAnnotationType(result.getString("annotationType"));
                    pointDto.setLocationCoveredText(result.getString("locationcoveredtext"));
                    pointDto.setLocation(result.getString("location"));
                    pointDto.setDateCoveredText(result.getString("datecoveredtext"));
                    var date = result.getDate("date");
                    pointDto.setDate(date != null ? date.toString() : null);
                    pointDto.setLabel(result.getString("fromcoveredtext"));
                    pointDto.setLongitude(result.getDouble("lng"));
                    pointDto.setLatitude(result.getDouble("lat"));
                    points.add(pointDto);
                }
                return points;
            }
        }));
    }

    public ArrayList<MapClusterDto> getGeonameClustersFromTimelineMap(double minLng,
                                                                      double minLat,
                                                                      double maxLng,
                                                                      double maxLat,
                                                                      double gridSize,
                                                                      java.sql.Date fromDate,
                                                                      java.sql.Date toDate,
                                                                      long corpusId) throws DatabaseOperationException {
        return executeOperationSafely((session) -> session.doReturningWork((connection) -> {
            try (var storedProcedure = connection.prepareCall("{call uce_query_clustered_geoname_timeline_cache" + "(?, ?, ?, ?, ?, ?, ?, ?)}")) {
                storedProcedure.setDouble(1, minLng);
                storedProcedure.setDouble(2, minLat);
                storedProcedure.setDouble(3, maxLng);
                storedProcedure.setDouble(4, maxLat);
                storedProcedure.setDouble(5, gridSize);
                storedProcedure.setDate(6, fromDate);
                storedProcedure.setDate(7, toDate);
                storedProcedure.setInt(8, (int) corpusId);

                var result = storedProcedure.executeQuery();
                var clusters = new ArrayList<MapClusterDto>();
                while (result.next()) {
                    var clusterDto = new MapClusterDto();
                    clusterDto.setCount(result.getInt("count"));
                    clusterDto.setLongitude(result.getDouble("lng"));
                    clusterDto.setLatitude(result.getDouble("lat"));
                    clusters.add(clusterDto);
                }
                return clusters;
            }
        }));
    }

    public List<String> getIdentifiableTaxonsByValue(String token) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            String sql = "SELECT DISTINCT biofidurl FROM biofidtaxon WHERE primaryname ILIKE :token LIMIT 100";

            var query = session.createNativeQuery(sql); // No type/class here
            query.setParameter("token", "%" + token + "%");

            @SuppressWarnings("unchecked")
            List<Object> result = query.getResultList();
            return result.stream()
                    .map(Object::toString)
                    .toList();
        });
    }

    public int countDocumentsInCorpus(long id) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var criteria = session.createCriteria(Document.class);
            criteria.add(Restrictions.eq("corpusId", id));
            criteria.setProjection(Projections.rowCount());
            return Math.toIntExact((Long) criteria.uniqueResult());
        });
    }

    @SuppressWarnings("deprecation")
    public int countPagesInCorpus(long corpusId) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var criteria = session.createCriteria(Page.class, "page");
            criteria.createAlias("page.document", "document");
            criteria.add(Restrictions.eq("document.corpusId", corpusId));
            criteria.setProjection(Projections.rowCount());
            return Math.toIntExact((Long) criteria.uniqueResult());
        });
    }

    public boolean documentExists(long corpusId, String documentId) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var criteriaBuilder = session.getCriteriaBuilder();
            var criteriaQuery = criteriaBuilder.createQuery(Long.class);
            var root = criteriaQuery.from(Document.class);

            var corpusIdPredicate = criteriaBuilder.equal(root.get("corpusId"), corpusId);
            var documentIdPredicate = criteriaBuilder.equal(root.get("documentId"), documentId);
            criteriaQuery.select(criteriaBuilder.count(root)).where(criteriaBuilder.and(corpusIdPredicate, documentIdPredicate));

            Long count = session.createQuery(criteriaQuery).getSingleResult();
            return count > 0;
        });
    }

    public CorpusTsnePlot getCorpusTsnePlotByCorpusId(long corpusId) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var cb = session.getCriteriaBuilder();
            var query = cb.createQuery(CorpusTsnePlot.class);
            var root = query.from(CorpusTsnePlot.class);
            query.select(root).where(cb.equal(root.get("corpus").get("id"), corpusId));
            var typedQuery = session.createQuery(query);
            return typedQuery.getSingleResult();
        });
    }

    public List<UCEMetadataFilter> getUCEMetadataFiltersByCorpusId(long corpusId) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            Criteria criteria = session.createCriteria(UCEMetadataFilter.class);
            criteria.add(Restrictions.eq("corpusId", corpusId));
            return (List<UCEMetadataFilter>) criteria.list();
        });
    }

    public List<UCEMetadata> getUCEMetadataByDocumentId(long documentId) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            Criteria criteria = session.createCriteria(UCEMetadata.class);
            criteria.add(Restrictions.eq("documentId", documentId));
            // I want the JSON value types to be last in list.
            return ((List<UCEMetadata>) criteria.list())
                    .stream()
                    .sorted(Comparator.comparing((UCEMetadata m) -> "JSON".equals(m.getValueType().name()) ? 1 : 0)
                            .thenComparingInt(m -> m.getValueType().ordinal()))
                    .toList();
        });
    }

    public List<Document> getDocumentsByCorpusId(long corpusId, int skip, int take, UceUser user) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            // TODO: Hardcoded sql, but another instance where hibernate is fucking unusable. This SQL in HQL or whatever
            // crooked syntax is a million times slower. I'll just leave the raw sql here then.
            var sql = "SELECT * FROM document WHERE corpusid = :corpusId ORDER BY id LIMIT :take OFFSET :skip";

            if (user != null) {
                // only show documents where the effective permissions (2) for this user are at least READ (1)
                // or allow access if there are no permissions set at all
                sql = """
                    SELECT * FROM document doc
                    LEFT JOIN documentpermissions dp ON dp.document_id = doc.id
                    WHERE doc.corpusid = :corpusId
                        AND (
                            dp.document_id IS NULL
                            OR (dp.type = 2 AND dp.name = :user AND dp.level >= 1)
                        )
                    ORDER BY doc.id
                    LIMIT :take
                    OFFSET :skip
                    """;
            }

            var query = session.createNativeQuery(sql, Document.class)
                    .setParameter("corpusId", corpusId)
                    .setParameter("take", take)
                    .setParameter("skip", skip);
            if (user != null) {
                query.setParameter("user", user.getUsername());
            }

            var documents = query.getResultList();

            documents.forEach(d -> Hibernate.initialize(d.getPages()));
            documents.forEach(d -> Hibernate.initialize(
                    d.getUceMetadata().stream()
                            .filter(u -> u.getValueType() != UCEMetadataValueType.JSON)
                            .toList()
            ));

            return documents;
        });
    }

    public List<DocumentLink> getManyDocumentLinksOfDocument(long id) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var criteria = session.createCriteria(DocumentLink.class);
            criteria.add(Restrictions.or(
                    Restrictions.eq("fromId", id),
                    Restrictions.eq("toId", id)
            ));
            return criteria.list();
        });
    }

    public List<DocumentLink> getManyDocumentLinksByDocumentId(String documentId, long corpusId) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var criteria = session.createCriteria(DocumentLink.class);
            criteria.add(Restrictions.eq("corpusId", corpusId));
            criteria.add(Restrictions.or(
                    Restrictions.eq("from", documentId),
                    Restrictions.eq("to", documentId)
            ));
            return criteria.list();
        });
    }

    public List<Document> getNonePostprocessedDocumentsByCorpusId(long corpusId) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var criteria = session.createCriteria(Document.class);
            criteria.add(Restrictions.eq("corpusId", corpusId));
            criteria.add(Restrictions.eq("postProcessed", false));
            return criteria.list();
        });
    }

    public List<Link> getAllLinksOfLinkable(long id, Class<? extends Linkable> linkableType, List<Class<? extends ModelBase>> possibleLinkTypes) throws DatabaseOperationException {
        // A linkable object can have multiple links that reference different tables (document, namedentity, token...)
        var links = new ArrayList<Link>();

        for (var type : possibleLinkTypes) {
            links.addAll(getLinksOfLinkableByType(id, linkableType, type));
        }
        return links;
    }

    public List<Link> getLinksOfLinkableByType(long id, Class<? extends Linkable> linkableType, Class<? extends ModelBase> type) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var criteria = session.createCriteria(type);
            criteria.add(Restrictions.or(
                    Restrictions.and(
                            Restrictions.eq("fromId", id),
                            Restrictions.eq("fromAnnotationType", linkableType.getName())
                    ),
                    Restrictions.and(
                            Restrictions.eq("toId", id),
                            Restrictions.eq("toAnnotationType", linkableType.getName())
                    )
            ));
            return criteria.list();
        });
    }

    public Linkable getLinkableById(long id, Class<? extends Linkable> clazz) throws DatabaseOperationException {
        return executeOperationSafely(session -> {
            var linkable = session.get(clazz, id);
            if (linkable instanceof Document doc) Hibernate.initialize(doc.getPages());
            if (clazz != Document.class && clazz != Page.class && linkable instanceof UIMAAnnotation anno)
                Hibernate.initialize(anno.getPage());
            return linkable;
        });
    }

    public Linkable getLinkable(long id, Class<? extends Linkable> clazz) throws DatabaseOperationException {
        var linkable = getLinkableById(id, clazz);
        if(linkable == null) return null;
        linkable.initLinkableViewModel(this);
        return linkable;
    }

    public Linkable getLinkable(long id, String className) throws ClassNotFoundException, DatabaseOperationException {
        var clazz = ReflectionUtils.getClassFromClassName(className, Linkable.class);
        return getLinkable(id, clazz);
    }

    @SuppressWarnings("unchecked")
    public List<UIMAAnnotation> getManyUIMAAnnotationsByCoveredText(String coveredText, Class<? extends UIMAAnnotation> clazz, int skip, int take) throws DatabaseOperationException {
        return (List<UIMAAnnotation>) executeOperationSafely((session -> {
            String sql = String.format(
                    "SELECT * FROM %s WHERE coveredtext = :coveredText ORDER BY id LIMIT :take OFFSET :skip",
                    clazz.getSimpleName().toLowerCase()
            );
            var query = session.createNativeQuery(sql, clazz)
                    .setParameter("coveredText", coveredText)
                    .setParameter("take", take)
                    .setParameter("skip", skip);
            query.stream().forEach(Hibernate::initialize);
            return query.getResultList();
        }));
    }

    public Corpus getCorpusById(long id) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            return session.get(Corpus.class, id);
        });
    }

    public Corpus getCorpusByName(String name) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var criteriaBuilder = session.getCriteriaBuilder();
            var criteriaQuery = criteriaBuilder.createQuery(Corpus.class);
            var root = criteriaQuery.from(Corpus.class);
            criteriaQuery.select(root).where(criteriaBuilder.equal(root.get("name"), name));
            var query = session.createQuery(criteriaQuery);
            return query.uniqueResult();
        });
    }

    public List<Corpus> getAllCorpora() throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var criteriaQuery = session.getCriteriaBuilder().createQuery(Corpus.class);
            criteriaQuery.from(Corpus.class);
            var corpora = session.createQuery(criteriaQuery).getResultList();
            for (var corpus : corpora) {
                Hibernate.initialize(corpus.getUceMetadataFilters());
            }
            return corpora;
        });
    }

    public List<GlobeTaxon> getGlobeDataForDocument(long documentId) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var doc = session.get(Document.class, documentId);
            Hibernate.initialize(doc.getPages());
            Hibernate.initialize(doc.getAllTaxa());
            Hibernate.initialize(doc.getBiofidTaxons());
            Hibernate.initialize(doc.getGeoNames());
            var globeTaxa = new ArrayList<GlobeTaxon>();
            for (var taxon : doc.getAllTaxa()) {
                var links = ExceptionUtils.tryCatchLog(
                        () -> getAllLinksOfLinkable(taxon.getId(), taxon.getClass(), List.of(AnnotationLink.class))
                                .stream()
                                .filter(l -> l.getLinkId().equals("context") && l.getToAnnotationType().equals(GeoName.class.getName())).toList(),
                        (ex) -> { });
                // Foreach taxa, fetch a possible geoname link.
                if (links != null)
                    for (var link : links) {
                        var geoname = doc.getGeoNames().stream().filter(g -> g.getId() == link.getToId()).findFirst();
                        if(geoname.isEmpty()) continue;
                        var globeTaxon = new GlobeTaxon();
                        globeTaxon.setLongitude(geoname.get().getLongitude());
                        globeTaxon.setLatitude(geoname.get().getLatitude());
                        globeTaxon.setTaxonId(String.valueOf(taxon.getRecordId()));
                        globeTaxon.setName(taxon.getCoveredText());
                        globeTaxon.setValue(taxon.getValue());
                        globeTaxon.setCountry(geoname.get().getCountryCode());
                        globeTaxon.setRegion(geoname.get().getName());
                        globeTaxa.add(globeTaxon);
                    }
            }
            return globeTaxa;

            // TODO: CLEANUP this is obsolete probably now.
            /*var taxonCommand = "SELECT DISTINCT t " +
                    "FROM Document d " +
                    "JOIN d.taxons t " +
                    "JOIN GbifOccurrence go ON go.gbifTaxonId = t.gbifTaxonId " +
                    "WHERE d.id = :documentId AND t.gbifTaxonId != 0 AND go.longitude <> -1000.0 AND go.latitude <> -1000.0";
            var query = session.createQuery(taxonCommand, Taxon.class);
            query.setParameter("documentId", documentId);
            var taxons = query.getResultList();
            // These are the unique taxon ids from the given corpus
            var taxonIds = taxons.stream().map(Taxon::getGbifTaxonId).collect(Collectors.toSet());

            // Now fetch all unique occurrences of these taxonids
            var occurrenceCommand = "SELECT DISTINCT gbif FROM GbifOccurrence gbif WHERE gbif.gbifTaxonId IN :taxonIds";
            var query2 = session.createQuery(occurrenceCommand, GbifOccurrence.class);
            query2.setParameter("taxonIds", taxonIds);
            var occurrences = query2.getResultList();

            var documents = new ArrayList<GlobeTaxon>();
            for (var occurrence : occurrences) {
                if (occurrence.getLatitude() == -1000) continue;

                var doc = new GlobeTaxon();
                var taxon = taxons.stream().filter(t -> t.getGbifTaxonId() == occurrence.getGbifTaxonId()).findFirst().get();

                doc.setLongitude(occurrence.getLongitude());
                doc.setLatitude(occurrence.getLatitude());
                doc.setName(taxon.getCoveredText());
                doc.setValue(taxon.getValue());
                doc.setCountry(occurrence.getCountry());
                doc.setRegion(occurrence.getRegion());
                doc.setImage(occurrence.getImageUrl());
                doc.setTaxonId(Long.toString(occurrence.getGbifTaxonId()));
                documents.add(doc);
            }

            return documents;*/
        });
    }

    public List<Document> getAllCompleteDocuments() throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var criteriaQuery = session.getCriteriaBuilder().createQuery(Document.class);
            criteriaQuery.from(Document.class);
            var docs = session.createQuery(criteriaQuery).getResultList();
            for (var doc : docs) {
                initializeCompleteDocument(doc, 0, 9999);
            }

            return docs;
        });
    }

    private void initializeDocument(Document doc, Set<String> hibernateInit) {
        // initialize additional Hibernate based properties
        if (hibernateInit != null) {
            for (String init : hibernateInit) {
                switch (init) {
                    // TODO can this be done more elegantly?
                    case "image" -> Hibernate.initialize(doc.getImages());
                    default -> System.err.println("getDocumentById: Unknown initialization option: " + init);
                    // Add more cases as needed for other initializations
                }
            }
        }
    }

    public List<Document> getManyDocumentsByIds(List<Integer> documentIds) throws DatabaseOperationException {
        return getManyDocumentsByIds(documentIds, null);
    }

    public List<Document> getManyDocumentsByIds(List<Integer> documentIds, Set<String> hibernateInit) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var builder = session.getCriteriaBuilder();
            var query = builder.createQuery(Document.class);
            var root = query.from(Document.class);

            query.select(root).where(root.get("id").in(documentIds));
            var q = session.createQuery(query);
            var docs = q.getResultList();

            // We show the amount of pages so init these and we want the documents to be in the same
            // order the documentIds passed in were since they could have been sorted!
            var sortedDocs = new Document[documentIds.size()];
            for (var id : documentIds) {
                // doc cannot be null.
                var doc = docs.stream().filter(d -> d.getId() == id).findFirst().orElse(null);
                if (doc == null) continue;
                doc.initLinkableViewModel(this);

                // We EAGERLY load those for now and see how that impacts performance.
                // Hibernate.initialize(doc.getPages());
                // Hibernate.initialize(doc.getUceMetadata().stream().filter(u -> u.getValueType() != UCEMetadataValueType.JSON));
                sortedDocs[documentIds.indexOf(id)] = doc;

                initializeDocument(doc, hibernateInit);
            }

            return Arrays.stream(sortedDocs).filter(Objects::nonNull).toList();
        });
    }

    public List<LexiconEntry> getManyLexiconEntries(int skip, int take, List<String> alphabet,
                                                    List<String> annotationFilters, String sortColumn,
                                                    String sortOrder, String searchInput)
            throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var builder = session.getCriteriaBuilder();
            var criteriaQuery = builder.createQuery(LexiconEntry.class);
            var root = criteriaQuery.from(LexiconEntry.class);
            criteriaQuery.select(root);

            // Collect predicates for dynamic where clause
            List<Predicate> predicates = new ArrayList<>();

            if (alphabet != null && !alphabet.isEmpty()) {
                predicates.add(root.get("startCharacter").in(alphabet.stream().map(String::toLowerCase).toList()));
            }

            if (annotationFilters != null && !annotationFilters.isEmpty()) {
                predicates.add(root.get("id").get("type").in(
                        annotationFilters.stream().map(String::toLowerCase).toList()));
            }

            // TODO: This is probably rather slow. Gotta watch this.
            if (searchInput != null && !searchInput.isBlank()) {
                predicates.add(
                        builder.like(
                                builder.lower(root.get("id").get("coveredText")),
                                "%" + searchInput.toLowerCase() + "%"
                        )
                );
            }

            if (!predicates.isEmpty()) {
                criteriaQuery.where(builder.and(predicates.toArray(new Predicate[0])));
            }

            // Determine sorting
            Path<?> sortPath;
            if ("occurrence".equalsIgnoreCase(sortColumn)) {
                sortPath = root.get("count");
            } else {
                // Default to sorting by coveredText of ID
                sortPath = root.get("id").get("coveredText");
            }

            Order order = "DESC".equalsIgnoreCase(sortOrder)
                    ? builder.desc(sortPath)
                    : builder.asc(sortPath);

            // Secondary sort by type for stability
            criteriaQuery.orderBy(order, builder.asc(root.get("id").get("type")));

            return session.createQuery(criteriaQuery)
                    .setFirstResult(skip)
                    .setMaxResults(take)
                    .getResultList();
        });
    }

    public int callLexiconRefresh(ArrayList<String> tables, boolean force) throws DatabaseOperationException {
        return executeOperationSafely((session) -> session.doReturningWork((connection) -> {
            var insertedLex = 0;
            try (var storedProcedure = connection.prepareCall("{call refresh_lexicon" + "(?, ?)}")) {
                storedProcedure.setArray(1, connection.createArrayOf("text", tables.toArray(new String[0])));
                storedProcedure.setBoolean(2, force);
                var result = storedProcedure.executeQuery();
                while (result.next()) {
                    insertedLex = result.getInt(1);
                }
            }
            return insertedLex;
        }));
    }

    public int callLogicalLinksRefresh() throws DatabaseOperationException {
        return executeOperationSafely((session) -> session.doReturningWork((connection) -> {
            var insertedLex = 0;
            try (var storedProcedure = connection.prepareCall("{call refresh_links()}")) {
                var result = storedProcedure.executeQuery();
                while (result.next()) {
                    insertedLex = result.getInt(1);
                }
            }
            return insertedLex;
        }));
    }

    public int callGeonameLocationRefresh() throws DatabaseOperationException {
        return executeOperationSafely((session) -> session.doReturningWork((connection) -> {
            var insertedLex = 0;
            try (var storedProcedure = connection.prepareCall("{call update_geoname_locations()}")) {
                var result = storedProcedure.executeQuery();
                while (result.next()) {
                    insertedLex = result.getInt(1);
                }
            }
            return insertedLex;
        }));
    }

    @Override
    public DocumentSearchResult semanticRoleSearchForDocuments(int skip,
                                                               int take,
                                                               List<String> arg0,
                                                               List<String> arg1,
                                                               List<String> arg2,
                                                               List<String> argm,
                                                               String verb,
                                                               boolean countAll,
                                                               SearchOrder order,
                                                               OrderByColumn orderedByColumn,
                                                               long corpusId) throws DatabaseOperationException {
        return executeOperationSafely((session) -> session.doReturningWork((connection) -> {

            DocumentSearchResult search = null;
            try (var storedProcedure = connection.prepareCall("{call uce_semantic_role_search" + "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}")) {
                storedProcedure.setInt(1, (int) corpusId);
                storedProcedure.setArray(2, connection.createArrayOf("text", arg0.toArray()));
                storedProcedure.setArray(3, connection.createArrayOf("text", arg1.toArray()));
                storedProcedure.setArray(4, connection.createArrayOf("text", arg2.toArray()));
                storedProcedure.setArray(5, connection.createArrayOf("text", argm.toArray()));
                storedProcedure.setString(6, verb);
                storedProcedure.setInt(7, take);
                storedProcedure.setInt(8, skip);
                storedProcedure.setBoolean(9, countAll);
                storedProcedure.setString(10, order.name());
                storedProcedure.setString(11, orderedByColumn.name().toLowerCase());

                var result = storedProcedure.executeQuery();
                while (result.next()) {
                    var documentCount = result.getInt("total_count_out");
                    var documentIds = new ArrayList<Integer>();
                    var documentIdsResult = result.getArray("document_ids");
                    if (documentIdsResult != null) {
                        var ids = (Integer[]) documentIdsResult.getArray();
                        documentIds.addAll(Arrays.asList(ids));
                    }
                    search = new DocumentSearchResult(documentCount, documentIds);
                    // Also parse the found entities and all outputs the query returns.
                    search.setFoundNamedEntities(parseAnnotationOccurrences(result.getArray("named_entities_found").getResultSet()));
                    search.setFoundTaxons(parseAnnotationOccurrences(result.getArray("taxons_found").getResultSet()));
                    search.setFoundTimes(parseAnnotationOccurrences(result.getArray("time_found").getResultSet()));
                }
                return search;
            }
        }));
    }

    @Override
    public DocumentSearchResult completeNegationSearchForDocuments(int skip,
                                                                   int take,
                                                                   List<String> cue,
                                                                   List<String> event,
                                                                   List<String> focus,
                                                                   List<String> scope,
                                                                   List<String> xscope,
                                                                   boolean countAll,
                                                                   SearchOrder order,
                                                                   OrderByColumn orderedByColumn,
                                                                   long corpusId,
                                                                   List<UCEMetadataFilterDto> filters,
                                                                   UceUser user) throws DatabaseOperationException {
        return executeOperationSafely((session) -> session.doReturningWork((connection) -> {
            HashMap<String, List<String>> tableSubstrings = new HashMap<>();
            tableSubstrings.put("cue", cue);
            tableSubstrings.put("event", event);
            tableSubstrings.put("focus", focus);
            tableSubstrings.put("scope", scope);
            tableSubstrings.put("xscope", xscope);

            DocumentSearchResult search = null;

            TreeMap<Long, TreeMap<Long, List<AnnotationSearchResult>>> cuesByDocID = new TreeMap<>();
            TreeMap<Long, TreeMap<Long, List<AnnotationSearchResult>>> scopesByDocID = new TreeMap<>();
            TreeMap<Long, TreeMap<Long, List<AnnotationSearchResult>>> xscopesByDocID = new TreeMap<>();
            TreeMap<Long, TreeMap<Long, List<AnnotationSearchResult>>> eventsByDocID = new TreeMap<>();
            TreeMap<Long, TreeMap<Long, List<AnnotationSearchResult>>> fociByDocID = new TreeMap<>();
            HashMap<String, TreeMap<Long, TreeMap<Long, List<AnnotationSearchResult>>>> annoMap = new HashMap<>();
            annoMap.put("cue", cuesByDocID);
            annoMap.put("event", eventsByDocID);
            annoMap.put("focus", fociByDocID);
            annoMap.put("scope", scopesByDocID);
            annoMap.put("xscope", xscopesByDocID);

            TreeMap<String, Boolean> skipMap = new TreeMap<String, Boolean>();
            skipMap.put("cue", false);
            skipMap.put("event", false);
            skipMap.put("focus", false);
            skipMap.put("scope", false);
            skipMap.put("xscope", false);

            boolean useFilters = true;
            List<ArrayList<String>> kvList = new ArrayList<>();
            if (filters == null || filters.isEmpty()) {
                useFilters = false;
            } else {
                var applicableFilters = filters.stream().filter(f -> !(f.getValue().isEmpty() || f.getValue().equals("{ANY}"))).toList();
                if (applicableFilters.isEmpty()) {
                    useFilters = false;
                }
                kvList = applicableFilters.stream()
                        .map(o -> new ArrayList<>(Arrays.asList(o.getKey(), o.getValue())))
                        .toList();
            }

            for (Map.Entry<String, List<String>> entry : tableSubstrings.entrySet()) {
                String table = entry.getKey();
                List<String> substrings = entry.getValue();
                if (substrings == null || substrings.isEmpty()) {
                    skipMap.put(table, true);
                    continue;
                }

                StringBuilder sql = new StringBuilder();
                sql.append("SELECT c.* FROM ").append(table).append(" c WHERE ");

                // WHERE conditions (ANDed ILIKEs)
                for (int i = 0; i < substrings.size(); i++) {
                    if (i > 0) sql.append(" AND ");
                    sql.append("c.coveredtext ILIKE ?");
                }
                if (useFilters) {
                    sql.append(" AND EXISTS (SELECT 1 FROM ucemetadata m WHERE m.document_id = c.document_id GROUP BY m.document_id HAVING COUNT(*) FILTER (WHERE (m.key, m.value) IN (");
                    // Add placeholders for key-value pairs
                    List<String> placeholders = new ArrayList<>();
                    for (int i = 0; i < kvList.size(); i++) {
                        placeholders.add("(?, ?)");
                    }
                    sql.append(String.join(", ", placeholders)).append(")) = ?)");
                }


                try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
                    int paramIndex = 1;

                    // Set parameters for each substring in WHERE clause
                    for (String s : substrings) {
                        stmt.setString(paramIndex++, "%" + s + "%");
                    }
                    if (useFilters) {
                        for (ArrayList<String> filter : kvList) {
                            stmt.setString(paramIndex++, filter.getFirst());
                            stmt.setString(paramIndex++, filter.getLast());
                        }
                        stmt.setInt(paramIndex, kvList.size());
                    }

                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        Long docId = rs.getLong("document_id");
                        Long negId = rs.getLong("negation_id");
                        Long annoId = rs.getLong("id");
                        Long pageId = rs.getLong("page_id");
                        int begin = rs.getInt("beginn");
                        int end = rs.getInt("endd");
                        String coveredText = rs.getString("coveredtext");
                        if (annoMap.get(table).get(docId) == null) {
                            List<AnnotationSearchResult> offsets = new ArrayList<>();
                            offsets.add(new AnnotationSearchResult(annoId, coveredText, 1, String.join("@", substrings), docId.intValue(), negId, begin, end, pageId));
                            TreeMap<Long, List<AnnotationSearchResult>> negMap = new TreeMap<>();
                            negMap.put(negId, offsets);
                            annoMap.get(table).put(docId, negMap);
                        } else {
                            if (annoMap.get(table).get(docId).get(negId) == null) {
                                List<AnnotationSearchResult> offsets = new ArrayList<>();
                                offsets.add(new AnnotationSearchResult(annoId, coveredText, 1, String.join("@", substrings), docId.intValue(), negId, begin, end, pageId));
                                annoMap.get(table).get(docId).put(negId, offsets);
                            } else {
                                annoMap.get(table).get(docId).get(negId).add(new AnnotationSearchResult(annoId, coveredText, 1, String.join("@", substrings), docId.intValue(), negId, begin, end, pageId));
                            }

                        }
                    }
                }
            }
            ArrayList<AnnotationSearchResult> cues = new ArrayList<>();
            ArrayList<AnnotationSearchResult> scopes = new ArrayList<>();
            ArrayList<AnnotationSearchResult> xscopes = new ArrayList<>();
            ArrayList<AnnotationSearchResult> events = new ArrayList<>();
            ArrayList<AnnotationSearchResult> foci = new ArrayList<>();
            List<Long> docIds = new ArrayList<>();

            int docCount = 0;
            int doc_found = 0;
            String mainKey = "cue";
            for (String possKey : skipMap.keySet()) {
                if (!skipMap.get(possKey)) {
                    mainKey = possKey;
                    break;
                }
            }
            for (Map.Entry<Long, TreeMap<Long, List<AnnotationSearchResult>>> entry2 : annoMap.get(mainKey).entrySet()) {
                Long docId = entry2.getKey();
                TreeMap<Long, List<AnnotationSearchResult>> negMap = entry2.getValue();
                boolean docPresent = true;
                for (String table : annoMap.keySet()) {
                    if (skipMap.get(table)) {
                        continue;
                    } else {
                        if (!annoMap.get(table).containsKey(docId)) {
                            docPresent = false;
                            break;
                        }
                    }
                }
                if (docPresent) {
                    boolean wasNegPresent = false;
                    for (Long negId : negMap.keySet()) {
                        boolean negPresent = true;
                        for (String table : annoMap.keySet()) {
                            if (skipMap.get(table)) {
                                continue;
                            } else {
                                if (!annoMap.get(table).get(docId).containsKey(negId)) {
                                    negPresent = false;
                                    break;
                                }
                            }
                        }
                        if (negPresent) {
                            wasNegPresent = true;
                            if ((docCount >= skip && doc_found < take)) {
                                for (String table : annoMap.keySet()) {
                                    if (!skipMap.get(table)) {
                                        if (Objects.equals(table, "cue"))
                                            cues.addAll(annoMap.get(table).get(docId).get(negId));
                                        if (Objects.equals(table, "scope"))
                                            scopes.addAll(annoMap.get(table).get(docId).get(negId));
                                        if (Objects.equals(table, "xscope"))
                                            xscopes.addAll(annoMap.get(table).get(docId).get(negId));
                                        if (Objects.equals(table, "focus"))
                                            foci.addAll(annoMap.get(table).get(docId).get(negId));
                                        if (Objects.equals(table, "event"))
                                            events.addAll(annoMap.get(table).get(docId).get(negId));
                                    }
                                }
                            }
                        }
                    }
                    if (wasNegPresent) {
                        if ((docCount >= skip && doc_found < take)) {
                            doc_found++;
                            docIds.add(docId);
                        }
                        docCount++;


                    }
                }
            }
            if (docIds.isEmpty()) {
                return search;
            } else {
                var documentIds = new ArrayList<Integer>();
                for (Long docId : docIds) {
                    documentIds.add(docId.intValue()); // Convert Long to Integer
                }
                search = new DocumentSearchResult(docCount, documentIds);
                search.setFoundCues(cues);
                search.setFoundEvents(events);
                search.setFoundXscopes(xscopes);
                search.setFoundFoci(foci);
                search.setFoundScopes(scopes);

                ArrayList<AnnotationSearchResult> allAnnos = new ArrayList<>();
                allAnnos.addAll(cues);
                allAnnos.addAll(foci);
                allAnnos.addAll(events);
                allAnnos.addAll(xscopes);
                allAnnos.addAll(scopes);

                HashMap<Long, ArrayList<PageSnippet>> foundSnippets = new HashMap<>();
                TreeMap<Long, List<AnnotationSearchResult>> negSorted = new TreeMap<>();
                for (AnnotationSearchResult anno : allAnnos) {
                    if (negSorted.get(anno.getAdditionalId()) == null) {
                        negSorted.put(anno.getAdditionalId(), new ArrayList<>());
                        negSorted.get(anno.getAdditionalId()).add(anno);
                    } else {
                        negSorted.get(anno.getAdditionalId()).add(anno);
                    }
                }
                for (Long negId : negSorted.keySet()) {
                    List<ArrayList<Integer>> offsetList = new ArrayList<>();
                    int minBegin = 999999999;
                    int maxEnd = 0;
                    for (AnnotationSearchResult anno : negSorted.get(negId)) {
                        if (minBegin > anno.getBegin()) {
                            minBegin = anno.getBegin();
                        }
                        if (maxEnd < anno.getEnd()) {
                            maxEnd = anno.getEnd();
                        }
                        ArrayList<Integer> offsetsTemp = new ArrayList<>();
                        offsetsTemp.add(anno.getBegin());
                        offsetsTemp.add(anno.getEnd());
                        offsetList.add(offsetsTemp);
                    }
                    try {
                        for (ArrayList<Integer> pair : offsetList) {
                            for (int i = 0; i < pair.size(); i++) {
                                pair.set(i, pair.get(i) - Math.max(minBegin - 100, 0));
                            }
                        }
                        CompleteNegation negComp = getCompleteNegationById(negId);
                        //Document doc = getCompleteDocumentById((long) negSorted.get(negId).getFirst().getDocumentId(), 0, 9999999);
                        Document doc = getCompleteDocumentById(negComp.getDocumentId(), 0, 9999999, user);
                        PageSnippet pageSnippet = new PageSnippet();

                        String snippet = doc.getFullTextSnippetCharOffset(Math.max(minBegin - 100, 0), Math.min(maxEnd + 100, minBegin + 500));
                        pageSnippet.setSnippet(StringUtils.getHtmlText(StringUtils.mergeBoldTags(StringUtils.addBoldTags(snippet, offsetList))));
                        pageSnippet.setPage(getPageById(negComp.getCue().getPage().getId()));
                        pageSnippet.setPageId((int) negComp.getCue().getPage().getId());
                        if (foundSnippets.containsKey(doc.getId())) {
                            foundSnippets.get(doc.getId()).add(pageSnippet);
                        } else {
                            foundSnippets.put(doc.getId(), new ArrayList<>());
                            foundSnippets.get(doc.getId()).add(pageSnippet);
                        }
                    } catch (DatabaseOperationException e) {
                        throw new RuntimeException(e);
                    }

                }
                search.setSearchSnippetsDocIdToSnippet(foundSnippets);
                return search;
            }

        }));
    }

    public DocumentSearchResult defaultSearchForDocuments(int skip,
                                                          int take,
                                                          String ogSearchQuery,
                                                          List<String> searchTokens,
                                                          SearchLayer layer,
                                                          boolean countAll,
                                                          SearchOrder order,
                                                          OrderByColumn orderedByColumn,
                                                          long corpusId,
                                                          List<UCEMetadataFilterDto> uceMetadataFilters,
                                                          boolean useTsVectorSearch,
                                                          String schema,
                                                          String sourceTable) throws DatabaseOperationException {

        return executeOperationSafely((session) -> session.doReturningWork((connection) -> {
            DocumentSearchResult search = null;
            try (var storedProcedure = connection.prepareCall("{call uce_search_layer_" + layer.name().toLowerCase() +
                                                              "(?::bigint, ?::text[], ?::text, ?::integer, ?::integer, ?::boolean, ?::text, ?::text, ?::jsonb, ?::boolean, ?::text, ?::text)}")) {
                storedProcedure.setInt(1, (int) corpusId);
                storedProcedure.setArray(2, connection.createArrayOf("text", searchTokens.stream().map(this::escapeSql).toArray()));
                storedProcedure.setString(3, ogSearchQuery);
                storedProcedure.setInt(4, take);
                storedProcedure.setInt(5, skip);
                storedProcedure.setBoolean(6, countAll);
                storedProcedure.setString(7, order.name());
                storedProcedure.setString(8, orderedByColumn.name().toLowerCase());
                if (uceMetadataFilters == null || uceMetadataFilters.isEmpty())
                    storedProcedure.setString(9, null);
                else {
                    var applicableFilters = uceMetadataFilters.stream().filter(f -> (!(f.getValue().isEmpty() || f.getValue().equals("{ANY}"))) || (f.getMax() != null || f.getMin() != null)).toList();
                    if (applicableFilters.isEmpty()) storedProcedure.setString(9, null);
                    else storedProcedure.setString(9, gson.toJson(applicableFilters));
                }
                storedProcedure.setBoolean(10, useTsVectorSearch);
                storedProcedure.setString(11, sourceTable);
                storedProcedure.setString(12, schema);

                var result = storedProcedure.executeQuery();
                while (result.next()) {
                    var documentCount = result.getInt("total_count_out");
                    var documentIds = new ArrayList<Integer>();
                    var documentIdsResult = result.getArray("document_ids");
                    if (documentIdsResult != null) {
                        var ids = (Integer[]) documentIdsResult.getArray();
                        documentIds.addAll(Arrays.asList(ids));
                    }
                    search = new DocumentSearchResult(documentCount, documentIds);
                    // Also parse the found entities and all outputs the query returns.
                    search.setFoundNamedEntities(parseAnnotationOccurrences(result.getArray("named_entities_found").getResultSet()));
                    search.setFoundTaxons(parseAnnotationOccurrences(result.getArray("taxons_found").getResultSet()));
                    search.setFoundTimes(parseAnnotationOccurrences(result.getArray("time_found").getResultSet()));

                    // Finally, parse the found snippets of the search
                    // This is only done for the fulltext search
                    if (layer == SearchLayer.FULLTEXT) {
                        // The found text snippets.
                        var gson = new Gson();
                        var resultSet = result.getArray("snippets_found").getResultSet();
                        var foundSnippets = new HashMap<Integer, ArrayList<PageSnippet>>();
                        // Snippets are the snippet text and the page_id to which this snippet belongs. They are json objects
                        while (resultSet.next()) {
                            var idx = resultSet.getInt(1) - 1;
                            ArrayList<ArrayList<PageSnippet>> pageSnippet = gson.fromJson(
                                    resultSet.getString(2),
                                    new TypeToken<ArrayList<ArrayList<PageSnippet>>>() {
                                    }.getType());
                            foundSnippets.put(idx, pageSnippet.getFirst());
                        }
                        foundSnippets.forEach((key, snippetList) -> {
                            for (PageSnippet snippet : snippetList) {
                                // Modify the value (example: changing content)
                                snippet.setSnippet(snippet.getSnippet());
                            }
                        });
                        search.setSearchSnippets(foundSnippets);

                        // And the ranks of each document.
                        var rankResultSet = result.getArray("document_ranks").getResultSet();
                        var documentRanks = new HashMap<Integer, Float>();
                        while (rankResultSet.next())
                            documentRanks.put(rankResultSet.getInt(1) - 1, rankResultSet.getFloat(2));
                        search.setSearchRanks(documentRanks);
                    }
                }

                return search;
            }
        }));
    }

    public List<Document> getDocumentsByAnnotationCoveredText(String coveredText, int limit, String annotationName) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var criteriaBuilder = session.getCriteriaBuilder();
            var criteriaQuery = criteriaBuilder.createQuery(Document.class);
            var root = criteriaQuery.from(Document.class);

            // Join with NamedEntity or other annotation entities via foreign key documentId
            var annotationJoin = root.join(annotationName);

            criteriaQuery.select(root).distinct(true) // Ensure distinct documents
                    .where(criteriaBuilder.equal(annotationJoin.get("coveredText"), coveredText));

            // set the limit
            var docs = session.createQuery(criteriaQuery)
                    .setMaxResults(limit)
                    .getResultList();

            // Initialize each document if needed
            for (var doc : docs) {
                // as always, I want the pages count
                Hibernate.initialize(doc.getPages());
            }

            return docs;
        });
    }

    public List<Lemma> getLemmasWithinBeginAndEndOfDocument(int begin, int end, long documentId) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var cb = session.getCriteriaBuilder();
            var query = cb.createQuery(Lemma.class);
            var lemmaRoot = query.from(Lemma.class);

            // Predicate for begin and end range
            Predicate beginPredicate = cb.greaterThanOrEqualTo(lemmaRoot.get("begin"), begin);
            Predicate endPredicate = cb.lessThanOrEqualTo(lemmaRoot.get("end"), end);

            // Combine predicates for range and documentId
            Predicate combinedPredicate = cb.and(beginPredicate, endPredicate);

            Predicate documentIdPredicate = cb.equal(lemmaRoot.get("documentId"), documentId);
            combinedPredicate = cb.and(combinedPredicate, documentIdPredicate);
            query.where(combinedPredicate);

            var resultQuery = session.createQuery(query);
            return resultQuery.getResultList();
        });
    }

    public List<Lemma> getLemmasByValue(String covered, int limit, long documentId) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var cb = session.getCriteriaBuilder();
            var query = cb.createQuery(Lemma.class);
            var lemmaRoot = query.from(Lemma.class);

            Predicate valuePredicate = cb.equal(lemmaRoot.get("coveredText"), covered);

            // If documentId is not -1, add a where clause to filter by documentId
            if (documentId != -1) {
                Predicate documentIdPredicate = cb.equal(lemmaRoot.get("document").get("id"), documentId);
                query.where(cb.and(valuePredicate, documentIdPredicate));
            } else {
                query.where(valuePredicate);
            }

            var resultQuery = session.createQuery(query);
            if (limit > 0) {
                resultQuery.setMaxResults(limit);
            }

            return resultQuery.getResultList();
        });
    }

    public UCEImport getUceImportByImportId(String importId) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var criteria = session.createCriteria(UCEImport.class);
            criteria.add(Restrictions.eq("importId", importId));
            return (UCEImport) criteria.list().getFirst();
        });
    }

    public Document getDocumentById(long id) throws DatabaseOperationException {
        return getDocumentById(id, null);
    }

    public Document getDocumentById(long id, Set<String> hibernateInit) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var doc = session.get(Document.class, id);
            Hibernate.initialize(doc.getPages());
            Hibernate.initialize(doc.getUceMetadata());
            initializeDocument(doc, hibernateInit);
            return doc;
        });
    }

    public List<Long> findDocumentIdsByMetadata(String key, String value, UCEMetadataValueType valueType) throws DatabaseOperationException {
        // Search for a document based on a metadata key/value pair
        return executeOperationSafely((session) -> {
            var cb = session.getCriteriaBuilder();
            var cq = cb.createQuery(Long.class);
            var root = cq.from(UCEMetadata.class);

            var predicate = cb.and(
                    cb.equal(root.get("key"), key),
                    cb.equal(root.get("value"), value),
                    cb.equal(root.get("valueType"), valueType.ordinal())
            );

            cq.select(root.get("documentId")).where(predicate);

            var query = session.createQuery(cq);
            return query.getResultList();
        });
    }

    public void deleteDocumentById(long id) throws DatabaseOperationException {
        // NOTE this only cleans up everything directly connected to the document
        // TODO also remove embeddings and other data
        executeOperationSafely((session) -> {
            var doc = session.get(Document.class, id);
            if (doc != null) {
                session.delete(doc);
            }
            return null;
        });
    }

    public List<Long> findDocumentIDsByTitle(String title, boolean like) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var cb = session.getCriteriaBuilder();
            var cq = cb.createQuery(Long.class);
            var root = cq.from(Document.class);

            Predicate predicate;
            if (like) {
                predicate = cb.like(root.get("documentTitle"), "%" + title + "%");
            }
            else{
                predicate = cb.equal(root.get("documentTitle"), title);
            }

            cq.select(root.get("id")).where(predicate);

            var query = session.createQuery(cq);
            return query.getResultList();
        });
    }

    public Document getFirstDocumentByTitle(String title, boolean like) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var cb = session.getCriteriaBuilder();
            var cq = cb.createQuery(Document.class);
            var root = cq.from(Document.class);

            if (like) {
                cq.select(root).where(cb.like(root.get("documentTitle"), "%" + title + "%"));
            }
            else{
                cq.select(root).where(cb.equal(root.get("documentTitle"), title));
            }

            var query = session.createQuery(cq);
            query.setMaxResults(1);
            var doc = query.uniqueResult();
            if (doc != null) {
                Hibernate.initialize(doc.getPages());
                Hibernate.initialize(doc.getUceMetadata());
            }
            return doc;
        });
    }

    public Page getPageById(long id) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var page = session.get(Page.class, id);
            Hibernate.initialize(page);
            return page;
        });
    }

    public Page getPageByDocumentIdAndBeginEnd(long documentId, int begin, int end, boolean initialize) throws DatabaseOperationException {
        return executeOperationSafely(session -> {
            var builder = session.getCriteriaBuilder();
            var criteria = builder.createQuery(Page.class);
            var root = criteria.from(Page.class);

            criteria.select(root).where(
                    builder.equal(root.get("documentId"), documentId),
                    builder.lessThanOrEqualTo(root.get("begin"), begin),
                    builder.greaterThanOrEqualTo(root.get("end"), end)
            );

            var page = session.createQuery(criteria)
                    .setMaxResults(1)
                    .uniqueResult();
            if (initialize) Hibernate.initialize(page);
            return page;
        });
    }

    public Document getDocumentByCorpusAndDocumentId(long corpusId, String documentId) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var cb = session.getCriteriaBuilder();
            var criteriaQuery = cb.createQuery(Document.class);
            var root = criteriaQuery.from(Document.class);

            criteriaQuery.select(root)
                    .where(
                            cb.and(
                                    cb.equal(root.get("corpusId"), corpusId),
                                    cb.equal(root.get("documentId"), documentId)
                            )
                    );

            Document doc = session.createQuery(criteriaQuery).uniqueResult();

            if (doc != null) {
                //initializeCompleteDocument(doc, 0, 999999);
            }
            return doc;
        });
    }

    public List<String> getDistinctTimesByCondition(String condition, long corpusId, int limit) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            // Construct HQL dynamically (THIS IS UNSAFE BECAUSE OF THE CONDITION INSERTION)
            String hql = "SELECT DISTINCT t.coveredText " +
                         "FROM Time t " +
                         "JOIN Document d ON t.documentId = d.id " +
                         "WHERE " + condition + " AND d.corpusId = :corpusId";

            var query = session.createQuery(hql, String.class);
            query.setParameter("corpusId", corpusId);
            query.setMaxResults(limit);

            return query.getResultList();
        });
    }

    public List<String> getDistinctGeonamesNamesByFeatureCode(GeoNameFeatureClass featureClass, String featureCode, long corpusId, int limit) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            // This with hibernate query builder doesn't work.
            String hql = """
                        SELECT DISTINCT g.name
                        FROM GeoName g
                        JOIN Document d ON g.documentId = d.id
                        WHERE g.featureClass = :featureClass
                          AND (:featureCode IS NULL OR g.featureCode = :featureCode)
                          AND d.corpusId = :corpusId
                    """;

            var query = session.createQuery(hql, String.class);
            query.setParameter("featureClass", featureClass);
            query.setParameter("featureCode", featureCode.isEmpty() ? null : featureCode);
            query.setParameter("corpusId", corpusId);
            query.setMaxResults(limit);

            return query.getResultList();
        });
    }

    public List<String> getDistinctGeonamesNamesByRadius(double longitude, double latitude, double radius, long corpusId, int limit) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            // This with hibernate query builder doesn't work since we use Postgis location queries.
            String sql = """
                        SELECT DISTINCT g.name
                        FROM geoname g
                        JOIN document d ON g.document_id = d.id
                        WHERE ST_DWithin(location_geog, CAST(ST_MakePoint(:longitude,:latitude) AS geography), :radius)
                        AND d.corpusId = :corpusId
                        LIMIT :limit
                    """;

            var query = session.createNativeQuery(sql);
            query.setParameter("longitude", longitude);
            query.setParameter("latitude", latitude);
            query.setParameter("radius", radius); // in meters
            query.setParameter("corpusId", corpusId);
            query.setParameter("limit", limit);

            return query.getResultList();
        });
    }

    public List<GbifOccurrence> getGbifOccurrencesByGbifTaxonId(long gbifTaxonId) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var criteriaBuilder = session.getCriteriaBuilder();
            var criteriaQuery = criteriaBuilder.createQuery(GbifOccurrence.class);

            var root = criteriaQuery.from(GbifOccurrence.class);
            var gbifTaxonIdPredicate = criteriaBuilder.equal(root.get("gbifTaxonId"), gbifTaxonId);
            var longitudePredicate = criteriaBuilder.notEqual(root.get("longitude"), -1000);
            var latitudePredicate = criteriaBuilder.notEqual(root.get("latitude"), -1000);

            var combinedPredicate = criteriaBuilder.and(gbifTaxonIdPredicate, longitudePredicate, latitudePredicate);

            criteriaQuery.where(combinedPredicate);

            return session.createQuery(criteriaQuery).getResultList();
        });
    }

    public long countLexiconEntries() throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var builder = session.getCriteriaBuilder();
            var criteria = builder.createQuery(Long.class);
            var root = criteria.from(LexiconEntry.class);
            criteria.select(builder.count(root));
            return session.createQuery(criteria).getSingleResult();
        });
    }

    public LexiconEntry getLexiconEntryId(LexiconEntryId id) throws DatabaseOperationException {
        return executeOperationSafely((session) -> session.get(LexiconEntry.class, id));
    }

    public NamedEntity getNamedEntityById(long id) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var entity = session.get(NamedEntity.class, id);
            Hibernate.initialize(entity.getPage());
            return entity;
        });
    }

    public GazetteerTaxon getGazetteerTaxonById(long id) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var taxon = session.get(GazetteerTaxon.class, id);
            Hibernate.initialize(taxon.getPage());
            return taxon;
        });
    }

    public GnFinderTaxon getGnFinderTaxonById(long id) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var taxon = session.get(GnFinderTaxon.class, id);
            Hibernate.initialize(taxon.getPage());
            return taxon;
        });
    }

    public BiofidTaxon getBiofidTaxonById(long id) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var taxon = session.get(BiofidTaxon.class, id);
            Hibernate.initialize(taxon.getPage());
            return taxon;
        });
    }

    public GeoName getGeoNameAnnotationById(long id) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var geo = session.get(GeoName.class, id);
            Hibernate.initialize(geo.getPage());
            return geo;
        });
    }

    public Time getTimeAnnotationById(long id) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var time = session.get(Time.class, id);
            Hibernate.initialize(time.getPage());
            return time;
        });
    }

    public Sentence getSentenceAnnotationById(long id) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var sentence = session.get(Sentence.class, id);
            Hibernate.initialize(sentence.getPage());
            return sentence;
        });
    }

    public Lemma getLemmaById(long id) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var lemma = session.get(Lemma.class, id);
            Hibernate.initialize(lemma.getPage());
            return lemma;
        });
    }

    public CompleteNegation getCompleteNegationById(long id) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var neg = session.get(CompleteNegation.class, id);
            Hibernate.initialize(neg);
            Hibernate.initialize(neg.getPage());
            return neg;
        });
    }

    public CompleteNegation getCompleteNegationByCueId(long id) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            String sql = "SELECT * FROM completenegation WHERE cue_id = :id";

            // Create a query with the native SQL
            var query = session.createNativeQuery(sql, CompleteNegation.class);
            query.setParameter("id", id);

            try {
                CompleteNegation neg = query.getSingleResult();
                Hibernate.initialize(neg); // Ensure lazy-loaded properties are initialized
                Hibernate.initialize(neg.getPage());
                if (neg.getCue() != null) {
                    Hibernate.initialize(neg.getCue());     // initialize cue
                    if (neg.getCue().getPage() != null) {
                        Hibernate.initialize(neg.getCue().getPage()); // initialize cue.page
                    }
                }
                return neg;
            } catch (NoResultException e) {
                return null; // Or throw an exception if no result is an error case
            }
        });
    }

    public TopicValueBase getTopicValueBaseById(long id) throws DatabaseOperationException {
        return executeOperationSafely((session) -> session.get(TopicValueBase.class, id));
    }

    public UnifiedTopic getUnifiedTopicById(long id) throws DatabaseOperationException {
        return executeOperationSafely((session) -> session.get(UnifiedTopic.class, id));
    }

    public UnifiedTopic getInitializedUnifiedTopicById(long id) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var topic = session.get(UnifiedTopic.class, id);
            Hibernate.initialize(topic.getTopics());
            Hibernate.initialize(topic.getPage());
            for(var t:topic.getTopics()){
                Hibernate.initialize(t.getWords());
            }
            return topic;
        });
    }

    public <T extends KeywordDistribution> List<T> getKeywordDistributionsByString(Class<T> clazz, String topic, int limit) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var builder = session.getCriteriaBuilder();
            var query = builder.createQuery(clazz);
            var root = query.from(clazz);

            // Convert the search topic to lowercase for case-insensitive matching
            var searchTopic = topic.toLowerCase();
            var predicates = new ArrayList<>();

            // Use reflection to find fields annotated with @Searchable. Otherwise, we'd have to
            // hardocde the SQL columns for every topic field in here which I really dont wanna do. If the reflection
            // is too costly, then think about changing it.
            // PS: Reflection in java sucks. (as well as ORM)
            Class<?> currentClass = clazz;
            while (currentClass != null) {
                for (var field : currentClass.getDeclaredFields()) {
                    if (field.isAnnotationPresent(Searchable.class)) {
                        // Build a case-insensitive equality predicate for each searchable field
                        predicates.add(
                                builder.equal(
                                        builder.lower(root.get(field.getName())),
                                        searchTopic
                                )
                        );
                    }
                }
                currentClass = currentClass.getSuperclass();
            }

            // Combine all predicates with OR condition
            var combinedPredicate = builder.or(predicates.toArray(new Predicate[0]));
            query.select(root).where(combinedPredicate);

            var finalQuery = session.createQuery(query);
            finalQuery.setMaxResults(limit);

            var results = finalQuery.getResultList();

            // Initialize document pages if any result is an instance of DocumentKeywordDistribution
            for (T dist : results) {
                if (dist instanceof DocumentKeywordDistribution) {
                    Hibernate.initialize(((DocumentKeywordDistribution) dist).getDocument().getPages());
                }
            }

            return results;
        });
    }

    public <T extends KeywordDistribution> T getKeywordDistributionById(Class<T> clazz, long id) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var dist = session.get(clazz, id);
            // Check if the retrieved object is an instance of DocumentKeywordDistribution
            if (dist instanceof DocumentKeywordDistribution) {
                Hibernate.initialize(((DocumentKeywordDistribution) dist).getDocument().getPages());
            }
            return dist;
        });
    }

    public boolean checkIfGbifOccurrencesExist(long gbifTaxonId) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var builder = session.getCriteriaBuilder();
            var criteriaQuery = builder.createQuery(Long.class);
            var root = criteriaQuery.from(GbifOccurrence.class);
            criteriaQuery.select(builder.count(root));
            // Add predicate to check for taxonId
            criteriaQuery.where(builder.equal(root.get("gbifTaxonId"), gbifTaxonId));
            // Execute query
            var count = session.createQuery(criteriaQuery).getSingleResult();
            return count > 0;
        });
    }

    public boolean userHasPermission(Document document, UceUser user, DocumentPermission.DOCUMENT_PERMISSION_LEVEL level) {
        Set<DocumentPermission> documentPermissions = document.getPermissions();

        // check for permissions if the document has any
        // if not, assume this is a "public" document and allow read access
        // this will keep backwards compatibility with older versions
        if (documentPermissions == null || documentPermissions.isEmpty()) {
            logger.info("Document {} has no permissions, assuming public read access", document.getId());
            return true;
        }

        // if not public, a user is required
        if (user == null) {
            logger.warn("No user provided for permission check on document {}, denying access", document.getId());
            return false;
        }

        // else check if this specific user has the required permission level
        for (DocumentPermission permission : documentPermissions) {
            // only need to check effective permissions
            if (permission.getType() == DocumentPermission.DOCUMENT_PERMISSION_TYPE.EFFECTIVE) {
                // permission level is sufficient
                if (permission.getLevel().ordinal() >= level.ordinal()) {
                    // check if the permission applies to the user
                    if (permission.getName().equals(user.getUsername())) {
                        return true;
                    }
                }
            }
        }

        logger.warn("User {} does not have sufficient permissions on document {}, denying access", user.getUsername(), document.getId());
        return false;
    }

    public Document getCompleteDocumentById(long id, int skipPages, int pageLimit, UceUser user) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var doc = session.get(Document.class, id);

            if (!userHasPermission(doc, user, DocumentPermission.DOCUMENT_PERMISSION_LEVEL.READ)) {
                return null;
            }

            return initializeCompleteDocument(doc, skipPages, pageLimit);
        });
    }

    public void saveOrUpdateCorpusTsnePlot(CorpusTsnePlot corpusTsnePlot, Corpus corpus) throws DatabaseOperationException {
        executeOperationSafely((session) -> {
            session.saveOrUpdate(corpus);
            // Save or update the corpus tsne plot
            if (corpus.getCorpusTsnePlot() != null) {
                session.saveOrUpdate(corpus.getCorpusTsnePlot());
            }
            return null;
        });
    }

    public void saveOrUpdateManyAnnotationLinks(List<AnnotationLink> links) throws DatabaseOperationException {
        final int BATCH_SIZE = 1000;
        // Since the links go in the hundred of millions for giant documents, we have to chunk the bulk inserts...
        executeOperationSafely(session -> {
            for (int i = 0; i < links.size(); i++) {
                session.saveOrUpdate(links.get(i));

                // Flush and clear the session every BATCH_SIZE records
                if (i % BATCH_SIZE == 0 && i > 0) {
                    session.flush();
                    session.clear();
                }
            }

            session.flush();
            session.clear();
            return null;
        });
    }

    public void saveOrUpdateManyDocumentToAnnotationLinks(List<DocumentToAnnotationLink> links) throws DatabaseOperationException {
        executeOperationSafely((session -> {
            for (var link : links) {
                session.saveOrUpdate(link);
            }
            return null;
        }));
    }

    public void saveOrUpdateManyAnnotationToDocumentLinks(List<AnnotationToDocumentLink> links) throws DatabaseOperationException {
        executeOperationSafely((session -> {
            for (var link : links) {
                session.saveOrUpdate(link);
            }
            return null;
        }));
    }

    public void saveOrUpdateManyDocumentLinks(List<DocumentLink> documentLinks) throws DatabaseOperationException {
        executeOperationSafely((session -> {
            for (var link : documentLinks) {
                session.saveOrUpdate(link);
            }
            return null;
        }));
    }

    public void saveOrUpdateUceImport(UCEImport uceImport) throws DatabaseOperationException {
        executeOperationSafely((session) -> {
            session.saveOrUpdate(uceImport);
            return null;
        });
    }

    public void saveOrUpdateImportLog(ImportLog importLog) throws DatabaseOperationException {
        executeOperationSafely((session) -> {
            session.saveOrUpdate(importLog);
            return null;
        });
    }

    public void saveOrUpdateUCEMetadataFilter(UCEMetadataFilter filter) throws DatabaseOperationException {
        executeOperationSafely((session) -> {
            session.saveOrUpdate(filter);
            return null;
        });
    }

    public void saveUCEMetadataFilter(UCEMetadataFilter filter) throws DatabaseOperationException {
        executeOperationSafely((session) -> {
            session.save(filter);
            return null;
        });
    }

    public void saveDocument(Document document) throws DatabaseOperationException {
        executeOperationSafely((session) -> {
            session.save(document);
            return null;
        });
    }

    public void updateDocument(Document document) throws DatabaseOperationException {
        executeOperationSafely((session) -> {
            session.update(document);
            return null;
        });
    }

    public void saveUceLog(UCELog log) throws DatabaseOperationException {
        executeOperationSafely((session) -> {
            session.saveOrUpdate(log);
            return null;
        });
    }

    public void saveCorpus(Corpus corpus) throws DatabaseOperationException {
        executeOperationSafely((session) -> {
            session.save(corpus);
            return null;
        });
    }

    public void savePageKeywordDistribution(Page page) throws DatabaseOperationException {
        executeOperationSafely((session) -> {
            session.saveOrUpdate(page);
            // Save or update the page's PageKeywordDistribution
            if (page.getPageKeywordDistribution() != null) {
                session.saveOrUpdate(page.getPageKeywordDistribution());
            }
            return null;
        });
    }

    public void saveDocumentKeywordDistribution(Document document) throws DatabaseOperationException {
        executeOperationSafely((session) -> {
            session.saveOrUpdate(document);
            // Save or update the page's PageKeywordDistribution
            if (document.getDocumentKeywordDistribution() != null) {
                session.saveOrUpdate(document.getDocumentKeywordDistribution());
            }
            return null;
        });
    }

    public void saveDocumentTopThreeTopics(Document document) throws DatabaseOperationException {
        executeOperationSafely((session) -> {
            session.saveOrUpdate(document);
            // Save or update the document's TopicDistribution
            if (document.getDocumentTopThreeTopics() != null) {
                session.saveOrUpdate(document.getDocumentTopThreeTopics());
            }
            return null;
        });
    }

    public DocumentTopThreeTopics getDocumentTopThreeTopicsById(long id) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var dist = session.get(DocumentTopThreeTopics.class, id);
            return dist;
        });
    }

    public List<Object[]> getTopTopicsByDocument(long documentId, int limit) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            // Use native SQL to query the document_topics_raw table
            String sql = "SELECT topiclabel, thetadt FROM documenttopicsraw " +
                         "WHERE document_id = :documentId " +
                         "ORDER BY thetadt DESC " +
                         "LIMIT :limit";

            var query = session.createNativeQuery(sql)
                    .setParameter("documentId", documentId)
                    .setParameter("limit", limit);

            return query.getResultList();
        });
    }

    public List<Object[]> getTopTopicsBySentence(long sentenceId, int limit) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            // Direct query using sentence_id
            String sql = "SELECT topiclabel, thetast FROM sentencetopics " +
                         "WHERE sentence_id = :sentenceId " +
                         "ORDER BY thetast DESC " +
                         "LIMIT :limit";

            var query = session.createNativeQuery(sql)
                    .setParameter("sentenceId", sentenceId)
                    .setParameter("limit", limit);

            return query.getResultList();
        });
    }

    public List<Object[]> getTopDocumentsByTopicLabel(String topicValue, long corpusId, int limit) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            String sql = "SELECT d.id, d.documentid, dtr.thetadt " +
                         "FROM document d " +
                         "JOIN documenttopicsraw dtr ON d.id = dtr.document_id " +
                         "WHERE dtr.topiclabel = :topicValue " +
                         "AND d.corpusid = :corpusId " +
                         "ORDER BY dtr.thetadt DESC " +
                         "LIMIT :limit";

            var query = session.createNativeQuery(sql)
                    .setParameter("topicValue", topicValue)
                    .setParameter("corpusId", corpusId)
                    .setParameter("limit", limit);

            return query.getResultList();
        });
    }

    public List<TopicWord> getTopicWordsByTopicLabel(String topicValue, long corpusId) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            String sql = "SELECT word, probability " +
                         "FROM corpustopicwords " +
                         "WHERE topiclabel = :topicValue AND corpus_id = :corpusId " +
                         "ORDER BY probability DESC " +
                         "LIMIT 20";

            var query = session.createNativeQuery(sql);
            query.setParameter("topicValue", topicValue);
            query.setParameter("corpusId", corpusId);

            List<Object[]> results = query.getResultList();

            List<TopicWord> topicWords = new ArrayList<>();
            for (Object[] row : results) {
                TopicWord tw = new TopicWord();
                tw.setWord((String) row[0]);
                tw.setProbability((Double) row[1]);
                topicWords.add(tw);
            }

            return topicWords;
        });
    }

    public List<Object[]> getSimilarTopicsbyTopicLabel(String topicValue, long corpusId, int minSharedWords, int result_limit) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            String sql = "SELECT * FROM find_similar_topics(:topicValue, :minSharedWords, :result_limit, :corpusId)";

            var query = session.createNativeQuery(sql)
                    .setParameter("topicValue", topicValue)
                    .setParameter("minSharedWords", minSharedWords)
                    .setParameter("result_limit", result_limit)
                    .setParameter("corpusId", corpusId);

            return query.getResultList();
        });
    }

    public List<TopicWord> getNormalizedTopicWordsForCorpus(long corpusId) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            String sql = "SELECT word, " +
                         "AVG(probability) AS avg_probability, " +
                         "AVG(probability) / SUM(AVG(probability)) OVER () AS normalized_probability " +
                         "FROM corpustopicwords " +
                         "WHERE corpus_id = :corpusId " +
                         "GROUP BY word " +
                         "ORDER BY normalized_probability DESC";

            var query = session.createNativeQuery(sql);
            query.setParameter("corpusId", corpusId);

            List<Object[]> results = query.getResultList();

            List<TopicWord> topicWords = new ArrayList<>();
            for (Object[] row : results) {
                TopicWord tw = new TopicWord();
                tw.setWord((String) row[0]);
                tw.setProbability((Double) row[2]); // Use normalized probability
                topicWords.add(tw);
            }

            return topicWords.size() > 20 ? topicWords.subList(0, 20) : topicWords;
        });
    }

    public Map<String, Double> getTopNormalizedTopicsByCorpusId(long corpusId) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            String sql = """
                        SELECT topic, normalized_score
                        FROM get_normalized_topic_scores(:corpusId)
                        ORDER BY normalized_score DESC
                        LIMIT 20
                    """;

            var query = session.createNativeQuery(sql)
                    .setParameter("corpusId", corpusId);

            List<Object[]> results = query.getResultList();

            Map<String, Double> topicDistributions = new HashMap<>();
            for (Object[] row : results) {
                String topicLabel = (String) row[0];
                Double probability = (Double) row[1];
                topicDistributions.put(topicLabel, probability);
            }

            return topicDistributions;
        });
    }

    public List<TopicWord> getDocumentWordDistribution(long documentId) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            String sql = "SELECT word, AVG(probability) AS avg_probability " +
                         "FROM documenttopicwords " +
                         "WHERE document_id = :documentId " +
                         "GROUP BY word " +
                         "ORDER BY avg_probability DESC " +
                         "LIMIT 20";

            var query = session.createNativeQuery(sql);
            query.setParameter("documentId", documentId);

            List<Object[]> results = query.getResultList();

            List<TopicWord> topicWords = new ArrayList<>();
            double totalProbability = results.stream()
                    .mapToDouble(row -> (Double) row[1])
                    .sum();

            if (totalProbability > 0) {
                for (Object[] row : results) {
                    String word = (String) row[0];
                    Double avgProbability = (Double) row[1];
                    TopicWord topicWord = new TopicWord();
                    topicWord.setWord(word);
                    topicWord.setProbability(avgProbability / totalProbability);
                    topicWords.add(topicWord);
                }
            }

            return topicWords;
        });
    }

    // Similar documents based on the shared topic words
    public List<Object[]> getSimilarDocumentbyDocumentId(long documentId) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            String sql = "WITH sourcewords AS (" +
                         "    SELECT word " +
                         "    FROM documenttopicwords " +
                         "    WHERE document_id = :documentId " +
                         "    GROUP BY word" +
                         "), " +
                         "similardocs AS (" +
                         "    SELECT " +
                         "        dtw.document_id, " +
                         "        COUNT(DISTINCT dtw.word) AS sharedwords " +
                         "    FROM documenttopicwords dtw " +
                         "    JOIN sourcewords sw ON sw.word = dtw.word " +
                         "    WHERE dtw.document_id != :documentId " +
                         "    GROUP BY dtw.document_id " +
                         "    ORDER BY sharedwords DESC" +
                         ") " +
                         "SELECT d.documentid, s.sharedwords " +
                         "FROM similardocs s " +
                         "JOIN document d ON s.document_id = d.id " +
                         "LIMIT 20";

            var query = session.createNativeQuery(sql)
                    .setParameter("documentId", documentId);

            return query.getResultList();
        });
    }

    public List<Object[]> getTaxonValuesAndCountByPageId(long documentId) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            List<String> taxonTypes = ReflectionUtils.getTaxonSystemTypes(Taxon.class);

            StringBuilder sqlBuilder = new StringBuilder();
            for (int i = 0; i < taxonTypes.size(); i++) {
                String tableName = taxonTypes.get(i);
                sqlBuilder.append("SELECT t.page_id, t.valuee ")
                        .append("FROM ").append(tableName).append(" t ")
                        .append("WHERE t.document_id = :documentId ");
                if (i < taxonTypes.size() - 1) {
                    sqlBuilder.append("UNION ALL ");
                }
            }

            // Outer query: group by page_id, aggregate values and count
            String finalSql = "SELECT page_id, valuee AS taxon_value " +
                              "FROM (" + sqlBuilder.toString() + ") AS combined_taxon ";

            var query = session.createNativeQuery(finalSql)
                    .setParameter("documentId", documentId)
                    .unwrap(org.hibernate.query.NativeQuery.class)
                    .addScalar("page_id", LongType.INSTANCE)
                    .addScalar("taxon_value", StandardBasicTypes.TEXT);

            return query.getResultList();
        });
    }

    public List<Object[]> getNamedEntityValuesAndCountByPage(long documentId) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            // Construct the SQL query to select page_id and coveredtext from the namedentity table
            String sql = "SELECT ne.page_id, ne.coveredtext AS named_entity_value, ne.typee AS named_entity_type " +
                         "FROM namedentity ne " +
                         "WHERE ne.document_id = :documentId";

            var query = session.createNativeQuery(sql)
                    .setParameter("documentId", documentId);

            return query.getResultList();
        });
    }

    public List<Object[]> getLemmaByPage(long documentId) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            // Construct the SQL query to select page_id and coveredtext from the namedentity table
            String sql = "SELECT lemma.page_id, lemma.coveredtext AS lemma_value, lemma.coarsevalue AS coarsevalue " +
                    "FROM lemma " +
                    "WHERE lemma.document_id = :documentId";

            var query = session.createNativeQuery(sql)
                    .setParameter("documentId", documentId);

            return query.getResultList();
        });
    }

    public List<Object[]> getGeonameByPage(long documentId) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            String sql = "SELECT gn.page_id, gn.coveredtext AS geoname_value " +
                    "FROM geoname gn " +
                    "WHERE gn.document_id = :documentId";

            var query = session.createNativeQuery(sql)
                    .setParameter("documentId", documentId);

            return query.getResultList();
        });
    }


    public List<Object[]> getTopicDistributionByPageForDocument(long documentId) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            String sql = """
                    WITH best_topic_per_sentence AS (
                        SELECT DISTINCT ON (st.document_id, st.sentence_id)
                            st.unifiedtopic_id,
                            st.document_id,
                            st.sentence_id,
                            st.topiclabel,
                            st.thetast
                        FROM 
                            sentencetopics st
                        WHERE 
                            st.document_id = :documentId
                        ORDER BY 
                            st.document_id, st.sentence_id, st.thetast DESC
                    )
                    SELECT 
                        ut.page_id,
                        btp.topiclabel
                    FROM 
                        best_topic_per_sentence btp
                    JOIN 
                        unifiedtopic ut ON btp.unifiedtopic_id = ut.id
                    WHERE 
                        ut.document_id = :documentId
                    ORDER BY 
                        ut.page_id, btp.topiclabel
                    """;

            var query = session.createNativeQuery(sql)
                    .setParameter("documentId", documentId);

            return query.getResultList();
        });
    }

    public List<Object[]> getSentenceTopicsWithEntitiesByPageForDocument(long documentId) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            String sql = """
                    WITH best_topic_per_sentence AS (
                        SELECT DISTINCT ON (st.document_id, st.sentence_id)
                            st.sentence_id,
                            st.topiclabel
                        FROM 
                            sentencetopics st
                        WHERE 
                            st.document_id = :document_id
                        ORDER BY 
                            st.document_id, st.sentence_id, st.thetast DESC
                    ),
                    entities_in_sentences AS (
                        SELECT DISTINCT
                            s.id AS sentence_id,
                            ne.typee AS entity_type
                        FROM
                            sentence s
                            JOIN namedentity ne ON 
                                ne.document_id = s.document_id AND
                                ne.beginn >= s.beginn AND 
                                ne.endd <= s.endd
                        WHERE
                            s.document_id = :document_id
                    )
                    SELECT
                        btps.topiclabel,
                        eis.entity_type
                    FROM
                        best_topic_per_sentence btps
                        JOIN entities_in_sentences eis ON btps.sentence_id = eis.sentence_id
                    ORDER BY
                        btps.sentence_id, eis.entity_type
                    """;

            Query<Object[]> query = session.createNativeQuery(sql)
                    .setParameter("document_id", documentId);

            return query.getResultList();
        });
    }

    public List<Object[]> getTopicWordsByDocumentId(long documentId) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            String sql = "SELECT topiclabel, word, AVG(probability) AS avg_probability " +
                         "FROM documenttopicwords " +
                         "WHERE document_id = :documentId " +
                         "GROUP BY topiclabel, word " +
                         "ORDER BY avg_probability DESC";

            var query = session.createNativeQuery(sql);
            query.setParameter("documentId", documentId);

            List<Object[]> results = query.getResultList();

            return results;
        });
    }

    public Map<Long, Long> getUnifiedTopicToSentenceMap(long documentId) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            String sql = "SELECT unifiedtopic_id, sentence_id FROM sentencetopics WHERE document_id = :documentId";

            var query = session.createNativeQuery(sql)
                    .setParameter("documentId", documentId);

            List<Object[]> rows = query.getResultList();

            Map<Long, Long> map = new HashMap<>();
            for (Object[] row : rows) {
                Long unifiedTopicId = ((Number) row[0]).longValue();
                Long sentenceId = ((Number) row[1]).longValue();

                if (map.containsKey(unifiedTopicId)) {
                    continue;
                }

                map.put(unifiedTopicId, sentenceId);
            }

            return map;
        });
    }


    /**
     * Parses the annotation occurrences that our search query outputs. This is so scuffed because hibernate freaking sucks, it's so nested.
     *
     * @param resultSet
     * @return
     * @throws SQLException
     */
    private ArrayList<AnnotationSearchResult> parseAnnotationOccurrences(ResultSet resultSet) throws SQLException {
        var foundNamedEntities = new ArrayList<AnnotationSearchResult>();
        while (resultSet.next()) {
            var resultArray = (Array) resultSet.getArray(2);
            var arrayElements = (String[]) resultArray.getArray();
            // The search query should return a quadruple of data
            if (arrayElements.length == 5)
                foundNamedEntities.add(new AnnotationSearchResult(
                        Long.parseLong(arrayElements[0]),
                        arrayElements[1],
                        Integer.parseInt(arrayElements[2]),
                        arrayElements[3],
                        Integer.parseInt(arrayElements[4])));
        }
        return foundNamedEntities;
    }

    /**
     * Gets all sublist and complete properties of a document.
     *
     * @param doc
     * @return
     */
    private Document initializeCompleteDocument(Document doc, int skipPages, int pageLimit) {
        Hibernate.initialize(doc.getPages());

        // The documents are too large to fetch all pages and all annotations at once, it would take to long.
        // So we initialize only through a window.
        for (var page : doc.getPages()
                .stream()
                .sorted(Comparator.comparing(Page::getPageNumber))
                .skip(skipPages)
                .limit(pageLimit)
                .toList()) {
            Hibernate.initialize(page.getBlocks());
            Hibernate.initialize(page.getParagraphs());
            Hibernate.initialize(page.getLines());
            Hibernate.initialize(page.getPageKeywordDistribution());
        }

        Hibernate.initialize(doc.getDocumentKeywordDistribution());
        Hibernate.initialize(doc.getSentences());
        Hibernate.initialize(doc.getNamedEntities());
        Hibernate.initialize(doc.getGeoNames());
        Hibernate.initialize(doc.getSentiments());
        Hibernate.initialize(doc.getEmotions());
        for(var emote:doc.getEmotions()) Hibernate.initialize(emote.getFeelings());
        Hibernate.initialize(doc.getBiofidTaxons());
        Hibernate.initialize(doc.getGazetteerTaxons());
        Hibernate.initialize(doc.getGnFinderTaxons());
        Hibernate.initialize(doc.getTimes());
        Hibernate.initialize(doc.getWikipediaLinks());
        Hibernate.initialize(doc.getLemmas());
        Hibernate.initialize(doc.getUceMetadata());
        Hibernate.initialize(doc.getImages());
        // init negations
        Hibernate.initialize(doc.getCompleteNegations());
        Hibernate.initialize(doc.getCues());
        Hibernate.initialize(doc.getScopes());
        Hibernate.initialize(doc.getFocuses());
        Hibernate.initialize(doc.getXscopes());
        Hibernate.initialize(doc.getEvents());

        // unified topic
        Hibernate.initialize(doc.getUnifiedTopics());

        for (var topic : doc.getUnifiedTopics()) {
            Hibernate.initialize(topic.getTopics());
        }

        for (var link : doc.getWikipediaLinks()) {
            Hibernate.initialize(link.getWikiDataHyponyms());
        }

        return doc;
    }

    /**
     * Since we need to handle exceptions, closing the session properly, rollback etc., we write a generic function
     * which is to be used whenever we call the database.
     *
     * @param <T>
     * @return
     */
    private <T> T executeOperationSafely(Function<Session, T> operation) throws DatabaseOperationException {
        Session session = null;
        Transaction transaction = null;
        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();
            T result = operation.apply(session);
            transaction.commit();
            return result;
        } catch (Exception ex) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new DatabaseOperationException(
                    "Error while executing database operation. All possible db transactions have been rolled back and state has been restored.",
                    ex);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    private String escapeSql(String input) {
        return input.replace("(", "\\(").replace(")", "\\)").replace(":", "\\:").replace("|", "\\|");
    }

}
