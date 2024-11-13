package org.texttechnologylab.services;

import org.hibernate.*;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Service;
import org.texttechnologylab.annotations.Searchable;
import org.texttechnologylab.config.HibernateConf;
import org.texttechnologylab.exceptions.DatabaseOperationException;
import org.texttechnologylab.models.UIMAAnnotation;
import org.texttechnologylab.models.corpus.*;
import org.texttechnologylab.models.gbif.GbifOccurrence;
import org.texttechnologylab.models.globe.GlobeTaxon;
import org.texttechnologylab.models.search.*;
import org.texttechnologylab.models.util.HealthStatus;
import org.texttechnologylab.utils.SystemStatus;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PostgresqlDataInterface_Impl implements DataInterface {

    private SessionFactory sessionFactory;

    private Session getCurrentSession() {
        return sessionFactory.openSession();
    }

    public PostgresqlDataInterface_Impl() {
        try{
            sessionFactory = HibernateConf.buildSessionFactory();
            var test = getCorpusById(1);
            SystemStatus.PostgresqlDbStatus = new HealthStatus(true, "", null);
        } catch (Exception ex){
            SystemStatus.PostgresqlDbStatus = new HealthStatus(false, "Couldn't build the session factory.", ex);
        }
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
        return executeOperationSafely((session) -> {
            var criteriaBuilder = session.getCriteriaBuilder();
            var criteriaQuery = criteriaBuilder.createQuery(Taxon.class);
            var root = criteriaQuery.from(Taxon.class);

            // Adding conditions: coveredText in values and identifier not null or empty and ignore lower and upper case
            criteriaQuery.select(root).where(
                    criteriaBuilder.and(
                            criteriaBuilder.lower(root.get("coveredText")).in(tokens.stream().map(String::toLowerCase).toList()),
                            criteriaBuilder.isNotNull(root.get("identifier")),
                            criteriaBuilder.notEqual(root.get("identifier"), "")
                    )
            );

            var query = session.createQuery(criteriaQuery);
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

    public List<Document> getDocumentsByCorpusId(long corpusId, int skip, int take) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            Criteria criteria = session.createCriteria(Document.class);
            criteria.setFirstResult(skip);
            criteria.setMaxResults(take);
            criteria.add(Restrictions.eq("corpusId", corpusId));
            var documents = (List<Document>)criteria.list();
            documents.forEach(d -> Hibernate.initialize(d.getPages()));
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
            var corpus = session.get(Corpus.class, id);
            return corpus;
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
            return session.createQuery(criteriaQuery).getResultList();
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

            // HARDCODED_SQL
            query.select(root).where(root.get("id").in(documentIds));
            var q = session.createQuery(query);
            var docs = q.getResultList();

            // We show the amount of pages so init these and we want the documents to be in the same
            // order the documentIds passed in were since they could have been sorted!
            var sortedDocs = new Document[documentIds.size()];
            for (var id : documentIds) {
                var doc = docs.stream().filter(d -> d.getId() == id).findFirst().orElse(null);
                // doc cannot be null.
                Hibernate.initialize(doc.getPages());
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
                                                          List<String> searchTokens,
                                                          SearchLayer layer,
                                                          boolean countAll,
                                                          SearchOrder order,
                                                          OrderByColumn orderedByColumn,
                                                          long corpusId) throws DatabaseOperationException {

        return executeOperationSafely((session) -> session.doReturningWork((connection) -> {

            DocumentSearchResult search = null;
            try (var storedProcedure = connection.prepareCall("{call uce_search_layer_" + layer.name().toLowerCase() + "(?, ?, ?, ?, ?, ?, ?, ?)}")) {
                storedProcedure.setInt(1, (int) corpusId);
                storedProcedure.setArray(2, connection.createArrayOf("text", searchTokens.toArray()));
                storedProcedure.setString(3, String.join("|", searchTokens).trim());
                storedProcedure.setInt(4, take);
                storedProcedure.setInt(5, skip);
                storedProcedure.setBoolean(6, countAll);
                storedProcedure.setString(7, order.name());
                storedProcedure.setString(8, orderedByColumn.name().toLowerCase());

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
                    if(layer == SearchLayer.FULLTEXT){
                        var resultSet = result.getArray("snippets_found").getResultSet();
                        var foundSnippets = new HashMap<Integer, String>();
                        while (resultSet.next()) foundSnippets.put(resultSet.getInt(1) - 1, resultSet.getString(2));
                        search.setSearchSnippets(foundSnippets);
                    }
                }
                return search;
            }
        }));
    }

    public Document getDocumentById(long id) throws DatabaseOperationException {
        return executeOperationSafely((session) -> {
            var doc = session.get(Document.class, id);
            Hibernate.initialize(doc.getPages());
            return doc;
        });
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
            if (arrayElements.length == 4)
                foundNamedEntities.add(new AnnotationSearchResult(arrayElements[0],
                        Integer.parseInt(arrayElements[1]),
                        arrayElements[2],
                        Integer.parseInt(arrayElements[3])));
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

}
