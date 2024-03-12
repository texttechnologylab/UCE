package org.texttechnologylab.services;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.stereotype.Service;
import org.texttechnologylab.config.HibernateConf;
import org.texttechnologylab.models.corpus.*;
import org.texttechnologylab.models.search.*;
import org.texttechnologylab.models.test.test;
import org.texttechnologylab.models.utils.CustomTuple;

import java.lang.annotation.Annotation;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

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
                initializeCompleteDocument(doc);
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
            return Arrays.stream(sortedDocs).toList();
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
                                                   OrderByColumn orderedByColumn) {

        return executeOperationSafely((session) -> session.doReturningWork((connection) -> {

            DocumentSearchResult search = null;
            try (var storedProcedure = connection.prepareCall("{call biofid_search_layer_" + layer.name().toLowerCase() + "(?, ?, ?, ?, ?, ?, ?)}")) {
                storedProcedure.setArray(1, connection.createArrayOf("text", searchTokens.toArray()));
                storedProcedure.setString(2, String.join("|", searchTokens).trim());
                storedProcedure.setInt(3, take);
                storedProcedure.setInt(4, skip);
                storedProcedure.setBoolean(5, countAll);
                storedProcedure.setString(6, order.name());
                storedProcedure.setString(7, orderedByColumn.name().toLowerCase());

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
        return executeOperationSafely((session) -> {
            return session.get(Document.class, id);
        });
    }

    /**
     * Gets a complete document, alongside its lists, from the database.
     *
     * @param id
     * @return
     */
    public Document getCompleteDocumentById(long id) {
        return executeOperationSafely((session) -> {
            var doc = session.get(Document.class, id);

            return initializeCompleteDocument(doc);
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
     * Parses the annotation occurrences that our search query outputs. This is so scuffed because hibernate freaking sucks, it's so nested.
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
    private Document initializeCompleteDocument(Document doc) {
        Hibernate.initialize(doc.getPages());
        for (var page : doc.getPages()) {
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
