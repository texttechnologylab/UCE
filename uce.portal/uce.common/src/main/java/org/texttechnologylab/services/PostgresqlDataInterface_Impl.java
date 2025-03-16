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
import org.texttechnologylab.models.corpus.*;
import org.texttechnologylab.models.dto.UCEMetadataFilterDto;
import org.texttechnologylab.models.gbif.GbifOccurrence;
import org.texttechnologylab.models.globe.GlobeTaxon;
import org.texttechnologylab.models.search.*;
import org.texttechnologylab.models.util.HealthStatus;
import org.texttechnologylab.utils.SystemStatus;

import javax.persistence.criteria.Predicate;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Array;
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
        return executeOperationSafely(session -> session.createNativeQuery(sql, Void.class).getResultList());
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


    public List<Document> getNonePostprocessedDocumentsByCorpusId(long corpusId) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            Criteria criteria = session.createCriteria(Document.class);
            criteria.add(Restrictions.eq("corpusId", corpusId));
            criteria.add(Restrictions.eq("postProcessed", false));
            return criteria.list();
        });
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

                // We EAGERLY load those for now and see how that impacts performance.
                // Hibernate.initialize(doc.getPages());
                // Hibernate.initialize(doc.getUceMetadata().stream().filter(u -> u.getValueType() != UCEMetadataValueType.JSON));

                sortedDocs[documentIds.indexOf(id)] = doc;
            }

            return Arrays.stream(sortedDocs).filter(Objects::nonNull).toList();
        });
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

    public List<Document> getDocumentsByNamedEntityValue(String coveredText, int limit) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var criteriaBuilder = session.getCriteriaBuilder();
            var criteriaQuery = criteriaBuilder.createQuery(Document.class);
            var root = criteriaQuery.from(Document.class);

            // Join with NamedEntity entity via foreign key documentId
            var namedEntityJoin = root.join("namedEntities");

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

    public Document getDocumentById(long id) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var doc = session.get(Document.class, id);
            Hibernate.initialize(doc.getPages());
            Hibernate.initialize(doc.getUceMetadata());
            return doc;
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

    public NamedEntity getNamedEntityById(long id) throws DatabaseOperationException {
        return executeOperationSafely((session) -> session.get(NamedEntity.class, id));
    }

    public Taxon getTaxonById(long id) throws DatabaseOperationException {
        return executeOperationSafely((session) -> session.get(Taxon.class, id));
    }

    public Lemma getLemmaById(long id) throws DatabaseOperationException {
        return executeOperationSafely((session) -> session.get(Lemma.class, id));
    }

    public <T extends TopicDistribution> List<T> getTopicDistributionsByString(Class<T> clazz, String topic, int limit) throws DatabaseOperationException {
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

            // Initialize document pages if any result is an instance of DocumentTopicDistribution
            for (T dist : results) {
                if (dist instanceof DocumentTopicDistribution) {
                    Hibernate.initialize(((DocumentTopicDistribution) dist).getDocument().getPages());
                }
            }

            return results;
        });
    }

    public <T extends TopicDistribution> T getTopicDistributionById(Class<T> clazz, long id) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var dist = session.get(clazz, id);
            // Check if the retrieved object is an instance of DocumentTopicDistribution
            if (dist instanceof DocumentTopicDistribution) {
                Hibernate.initialize(((DocumentTopicDistribution) dist).getDocument().getPages());
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

    public void savePageTopicDistribution(Page page) throws DatabaseOperationException {
        executeOperationSafely((session) -> {
            session.saveOrUpdate(page);
            // Save or update the page's PageTopicDistribution
            if (page.getPageTopicDistribution() != null) {
                session.saveOrUpdate(page.getPageTopicDistribution());
            }
            return null;
        });
    }

    public void saveDocumentTopicDistribution(Document document) throws DatabaseOperationException {
        executeOperationSafely((session) -> {
            session.saveOrUpdate(document);
            // Save or update the page's PageTopicDistribution
            if (document.getDocumentTopicDistribution() != null) {
                session.saveOrUpdate(document.getDocumentTopicDistribution());
            }
            return null;
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
            Hibernate.initialize(page.getPageTopicDistribution());
        }

        Hibernate.initialize(doc.getDocumentTopicDistribution());
        Hibernate.initialize(doc.getSentences());
        Hibernate.initialize(doc.getNamedEntities());
        Hibernate.initialize(doc.getTaxons());
        Hibernate.initialize(doc.getTimes());
        Hibernate.initialize(doc.getWikipediaLinks());
        Hibernate.initialize(doc.getLemmas());
        Hibernate.initialize(doc.getUceMetadata());
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
