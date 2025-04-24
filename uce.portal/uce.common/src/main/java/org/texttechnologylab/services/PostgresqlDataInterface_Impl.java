package org.texttechnologylab.services;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.hibernate.*;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Service;
import org.texttechnologylab.annotations.Searchable;
import org.texttechnologylab.config.HibernateConf;
import org.texttechnologylab.exceptions.DatabaseOperationException;
import org.texttechnologylab.exceptions.ExceptionUtils;
import org.texttechnologylab.models.Linkable;
import org.texttechnologylab.models.ModelBase;
import org.texttechnologylab.models.UIMAAnnotation;
import org.texttechnologylab.models.corpus.*;
import org.texttechnologylab.models.corpus.links.DocumentLink;
import org.texttechnologylab.models.corpus.links.Link;
import org.texttechnologylab.models.dto.UCEMetadataFilterDto;
import org.texttechnologylab.models.gbif.GbifOccurrence;
import org.texttechnologylab.models.globe.GlobeTaxon;
import org.texttechnologylab.models.imp.ImportLog;
import org.texttechnologylab.models.imp.UCEImport;
import org.texttechnologylab.models.negation.CompleteNegation;
import org.texttechnologylab.models.search.*;
import org.texttechnologylab.models.topic.TopicValueBase;
import org.texttechnologylab.models.topic.TopicWord;
import org.texttechnologylab.models.topic.UnifiedTopic;
import org.texttechnologylab.models.util.HealthStatus;
import org.texttechnologylab.utils.ClassUtils;
import org.texttechnologylab.utils.StringUtils;
import org.texttechnologylab.utils.SystemStatus;

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

    private final SessionFactory sessionFactory;

    private final Gson gson = new Gson();

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

    public List<Taxon> getIdentifiableTaxonsByValues(List<String> tokens) throws DatabaseOperationException {
        var tokensAsOneString = String.join(" ", tokens);
        var finalTokens = new ArrayList<String>(tokens);
        finalTokens.add(tokensAsOneString);

        // TODO: Hardcoded SQL, since writing this in Hibernate is waaaay more painful
        return executeOperationSafely((session) -> {
            String sql = "SELECT * FROM taxon t " +
                    "WHERE (lower(t.coveredText) IN :tokens " +
                    "OR EXISTS ( " +
                    "    SELECT 1 FROM unnest(t.value_array) AS val " +
                    "    WHERE TRIM(LOWER(val)) IN :tokens " +
                    ")) " +
                    "AND t.identifier IS NOT NULL " +
                    "AND t.identifier <> '' ";

            // Create a query with the native SQL
            var query = session.createNativeQuery(sql, Taxon.class);
            query.setParameter("tokens", finalTokens.stream().map(String::toLowerCase).toList());

            return query.getResultList();
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

    public List<Document> getDocumentsByCorpusId(long corpusId, int skip, int take) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            // TODO: Hardcoded sql, but another instance where hibernate is fucking unusable. This SQL in HQL or whatever
            // crooked syntax is a million times slower. I'll just leave the raw sql here then.
            var sql = "SELECT * FROM document WHERE corpusid = :corpusId ORDER BY id LIMIT :take OFFSET :skip";
            var query = session.createNativeQuery(sql, Document.class)
                    .setParameter("corpusId", corpusId)
                    .setParameter("take", take)
                    .setParameter("skip", skip);

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

    public List<Link> getAllLinksOfLinkable(long id, List<Class<? extends ModelBase>> possibleLinkTypes) throws DatabaseOperationException {
        // A linkable object can have multiple links that reference different tables (document, namedentity, token...)
        var links = new ArrayList<Link>();

        for(var type:possibleLinkTypes){
            links.addAll(getLinksOfLinkableByType(id, type));
        }
        return links;
    }

    public List<Link> getLinksOfLinkableByType(long id, Class<? extends ModelBase> type) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var criteria = session.createCriteria(type);
            criteria.add(Restrictions.or(
                    Restrictions.eq("fromId", id),
                    Restrictions.eq("toId", id)
            ));
            return criteria.list();
        });
    }

    public Linkable getLinkableById(long id, Class<? extends Linkable> clazz) throws DatabaseOperationException {
        return executeOperationSafely(session -> {
            var linkable = session.get(clazz, id);
            if(linkable instanceof Document doc) Hibernate.initialize(doc.getPages());
            return linkable;
        });
    }

    public Linkable getLinkable(long id, Class<? extends Linkable> clazz) throws DatabaseOperationException {
        var linkable = getLinkableById(id, clazz);
        linkable.initLinkableViewModel(this);
        return linkable;
    }

    public Linkable getLinkable(long id, String className) throws ClassNotFoundException, DatabaseOperationException {
        var clazz = ClassUtils.getClassFromClassName(className, Linkable.class);
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
            var taxonCommand = "SELECT DISTINCT t " +
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

            return documents;
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

    public List<Document> getManyDocumentsByIds(List<Integer> documentIds) throws DatabaseOperationException {
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
                                                                   List<UCEMetadataFilterDto> filters) throws DatabaseOperationException {
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
            }
            else {
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
                        Document doc = getCompleteDocumentById(negComp.getDocumentId(), 0, 9999999);
                        PageSnippet pageSnippet = new PageSnippet();

                        String snippet = doc.getFullTextSnippetCharOffset(Math.max(minBegin - 100, 0), Math.min(maxEnd + 100, minBegin + 500));
                        pageSnippet.setSnippet(StringUtils.mergeBoldTags(StringUtils.addBoldTags(snippet, offsetList)).replaceAll("\n", "<br/>").replaceAll(" ", "&nbsp;"));
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
                    var applicableFilters = uceMetadataFilters.stream().filter(f -> !(f.getValue().isEmpty() || f.getValue().equals("{ANY}"))).toList();
                    if (applicableFilters.isEmpty()) storedProcedure.setString(9, null);
                    else storedProcedure.setString(9, gson.toJson(applicableFilters)
                            .replaceAll("\"valueType\"", "\"valueType::text\""));
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
                                snippet.setSnippet(snippet.getSnippet().replaceAll("\n", "<br/>").replaceAll(" ", "&nbsp;"));
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
            var namedEntityJoin = root.join(annotationName);

            criteriaQuery.select(root).distinct(true) // Ensure distinct documents
                    .where(criteriaBuilder.equal(namedEntityJoin.get("coveredText"), coveredText));

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
        return executeOperationSafely((session) -> {
            var doc = session.get(Document.class, id);
            Hibernate.initialize(doc.getPages());
            Hibernate.initialize(doc.getUceMetadata());
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
                Hibernate.initialize(doc.getPages());
                Hibernate.initialize(doc.getUceMetadata());
            }
            return doc;
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

    public Time getTimeAnnotationById(long id) throws DatabaseOperationException {
        return executeOperationSafely((session) -> session.get(Time.class, id));
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
        return executeOperationSafely((session) -> session.get(NamedEntity.class, id));
    }

    public Taxon getTaxonById(long id) throws DatabaseOperationException {
        return executeOperationSafely((session) -> session.get(Taxon.class, id));
    }

    public Lemma getLemmaById(long id) throws DatabaseOperationException {
        return executeOperationSafely((session) -> session.get(Lemma.class, id));
    }

    public CompleteNegation getCompleteNegationById(long id) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var neg = session.get(CompleteNegation.class, id);
            Hibernate.initialize(neg);
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

    public Document getCompleteDocumentById(long id, int skipPages, int pageLimit) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var doc = session.get(Document.class, id);

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
        Hibernate.initialize(doc.getTaxons());
        Hibernate.initialize(doc.getTimes());
        Hibernate.initialize(doc.getWikipediaLinks());
        Hibernate.initialize(doc.getLemmas());
        Hibernate.initialize(doc.getUceMetadata());
        // init negations
        Hibernate.initialize(doc.getCompleteNegations());
        Hibernate.initialize(doc.getCues());
        Hibernate.initialize(doc.getScopes());
        Hibernate.initialize(doc.getFocuses());
        Hibernate.initialize(doc.getXscopes());
        Hibernate.initialize(doc.getEvents());

        // unified topic
        Hibernate.initialize(doc.getUnifiedTopics());

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
