package org.texttechnologylab.services;

import org.hibernate.*;
import org.springframework.stereotype.Service;
import org.texttechnologylab.config.HibernateConf;
import org.texttechnologylab.models.corpus.*;
import org.texttechnologylab.models.gbif.GbifOccurrence;
import org.texttechnologylab.models.globe.GlobeTaxon;
import org.texttechnologylab.models.search.*;
import org.texttechnologylab.models.test.test;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DatabaseService {

    private final SessionFactory sessionFactory;

    private Session getCurrentSession() {
        return sessionFactory.openSession();
    }

    public DatabaseService() {
        sessionFactory = HibernateConf.buildSessionFactory();
    }

    /**
     * Gets all corpora from the database
     *
     * @return
     */
    public List<Corpus> getAllCorpora() {
        return executeOperationSafely((session) -> {
            var criteriaQuery = session.getCriteriaBuilder().createQuery(Corpus.class);
            criteriaQuery.from(Corpus.class);
            return session.createQuery(criteriaQuery).getResultList();
        });
    }

    /**
     * Gets the data required for the world globus to render correctly.
     * @param documentId
     * @return
     */
    public List<GlobeTaxon> getGlobeDataForDocument(long documentId) {
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
            for(var occurrence:occurrences){
                if(occurrence.getLatitude() == -1000) continue;;

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

    /**
     * Gets all complete documents in the database
     * CAUTION: Do you _really_ want this? It's extremely heavy.
     *
     * @return
     */
    public List<Document> getAllCompleteDocuments() {
        return executeOperationSafely((session) -> {
            var criteriaQuery = session.getCriteriaBuilder().createQuery(Document.class);
            criteriaQuery.from(Document.class);
            var docs = session.createQuery(criteriaQuery).getResultList();
            for (var doc : docs) {
                initializeCompleteDocument(doc, 9999);
            }

            return docs;
        });
    }

    /**
     * Returns a list of documents by a list of ids
     *
     * @return
     */
    public List<Document> getManyDocumentsByIds(List<Integer> documentIds) {
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

    /**
     * Searches for documents with a variety of criterias. It's the main db search of the biofid portal
     * The function calls a variety of stored procedures in the database.
     *
     * @param skip
     * @param take
     * @param searchTokens
     * @return
     */
    public DocumentSearchResult searchForDocuments(int skip,
                                                   int take,
                                                   List<String> searchTokens,
                                                   SearchLayer layer,
                                                   boolean countAll,
                                                   SearchOrder order,
                                                   OrderByColumn orderedByColumn,
                                                   long corpusId) {

        return executeOperationSafely((session) -> session.doReturningWork((connection) -> {

            DocumentSearchResult search = null;
            try (var storedProcedure = connection.prepareCall("{call biofid_search_layer_" + layer.name().toLowerCase() + "(?, ?, ?, ?, ?, ?, ?, ?)}")) {
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
                }
                return search;
            }
        }));
    }

    /**
     * Generic operation that fetches documents given the paramters
     *
     * @return
     */
    public List<Document> searchForDocuments(int skip,
                                             int take) {
        return executeOperationSafely((session) -> {
            var criteriaQuery = session.getCriteriaBuilder().createQuery(Document.class);
            var docRoot = criteriaQuery.from(Document.class);

            var query = session.createQuery(criteriaQuery);
            query.setFirstResult(skip);
            query.setMaxResults(take);

            var docs = query.getResultList();
            // In the search view, for now we show the amount of pages so init them
            for (var doc : docs) {
                Hibernate.initialize(doc.getPages());
            }
            return docs;
        });
    }

    /**
     * Gets a single document without any lists.
     *
     * @param id
     * @return
     */
    public Document getDocumentById(long id) {
        return executeOperationSafely((session) -> session.get(Document.class, id));
    }

    /**
     * Returns true if one instance of a Gbif occurrence exists in the database
     *
     * @return
     */
    public boolean checkIfGbifOccurrencesExist(long gbifTaxonId) {
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

    /**
     * Gets a complete document, alongside its lists, from the database.
     *
     * @param id
     * @return
     */
    public Document getCompleteDocumentById(long id, int pageLimit) {
        return executeOperationSafely((session) -> {
            var doc = session.get(Document.class, id);

            return initializeCompleteDocument(doc, pageLimit);
        });
    }

    /**
     * Stores the complete document with all its lists in the database.
     *
     * @param document
     */
    public void saveDocument(Document document) {
        executeOperationSafely((session) -> {
            session.save(document);
            return null; // No return value needed for this write operation
        });
    }

    /**
     * Stores a corpus in the database.
     *
     * @param corpus
     */
    public void saveCorpus(Corpus corpus) {
        executeOperationSafely((session) -> {
            session.save(corpus);
            return null; // No return value needed for this write operation
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
    private Document initializeCompleteDocument(Document doc, int pageLimit) {
        Hibernate.initialize(doc.getPages());
        for (var page : doc.getPages().stream().limit(pageLimit).toList()) {
            Hibernate.initialize(page.getBlocks());
            Hibernate.initialize(page.getParagraphs());
            Hibernate.initialize(page.getLines());
        }
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
    private <T> T executeOperationSafely(Function<Session, T> operation) {
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
            throw new RuntimeException("Error executing operation", ex);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    /**
     * Test method, can be ignored.
     *
     * @param doc
     */
    public void test(Document doc) {
        var currentSession = sessionFactory.openSession();
        var tes = new test();
        tes.setId(UUID.randomUUID());
        tes.setName("Test");
        var trans = currentSession.beginTransaction();
        currentSession.save(tes);
        trans.commit();
        currentSession.close();
    }

}
