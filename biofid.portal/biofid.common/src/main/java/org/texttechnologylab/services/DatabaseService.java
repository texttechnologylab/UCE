package org.texttechnologylab.services;

import org.apache.http.annotation.Obsolete;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.stereotype.Service;
import org.texttechnologylab.config.HibernateConf;
import org.texttechnologylab.models.corpus.*;
import org.texttechnologylab.models.search.DocumentSearchResult;
import org.texttechnologylab.models.test.test;

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
     * @return
     */
    public List<Document> getAllCompleteDocuments(){
        return executeOperationSafely((session) -> {
            var criteriaQuery = session.getCriteriaBuilder().createQuery(Document.class);
            criteriaQuery.from(Document.class);
            var docs = session.createQuery(criteriaQuery).getResultList();
            for(var doc: docs){
                initializeCompleteDocument(doc);
            }

            return docs;
        });
    }

    /**
     * Returns a list of documents by a list of ids
     * @return
     */
    public List<Document> getManyDocumentsByIds(List<Integer> documentIds){
        return executeOperationSafely((session) -> {
            var builder = session.getCriteriaBuilder();
            var query = builder.createQuery(Document.class);
            var root = query.from(Document.class);

            // HARDCODED_SQL
            query.select(root).where(root.get("id").in(documentIds));

            var q = session.createQuery(query);
            var docs = q.getResultList();
            // We show the amount of pages so init these
            for(var doc: docs){
                Hibernate.initialize(doc.getPages());
            }
            return docs;
        });
    }

    /**
     * Searches for documents with a variety of criterias. It's the main db search of the biofid portal
     * @param skip
     * @param take
     * @param searchTokens
     * @return
     */
    public DocumentSearchResult searchForDocuments(int skip, int take, List<String> searchTokens, String layer){

        return executeOperationSafely((session) -> {

            var searchResult = session.doReturningWork((connection) -> {
                DocumentSearchResult search = null;
                try(var storedProcedure = connection.prepareCall("{call biofid_search_layer_" + layer + "(?, ?, ?, ?)}")){
                    storedProcedure.setArray(1, connection.createArrayOf("text", searchTokens.toArray()));
                    storedProcedure.setString(2, String.join("|", searchTokens).trim());
                    storedProcedure.setInt(3, 10);
                    storedProcedure.setInt(4, 0);

                    var result = storedProcedure.executeQuery();
                    while(result.next()){
                        var documentCount = result.getInt("total_count_out");
                        var documentIds = new ArrayList<Integer>();
                        var documentIdsResult = result.getArray("document_ids");
                        if (documentIdsResult != null) {
                            var ids = (Integer[]) documentIdsResult.getArray();
                            documentIds.addAll(Arrays.asList(ids));
                        }
                        search = new DocumentSearchResult(documentCount, documentIds);
                    }
                    return search;
                }
            });

            return searchResult;

            /*var queryString = "SELECT * FROM (\n" +
                    "    SELECT DISTINCT d.* \n" +
                    "    FROM document d \n" +
                    "    JOIN namedentity ne ON d.document_id = ne.document_id \n" +
                    "    WHERE LOWER(ne.coveredtext) IN :searchTokens \n" +
                    "    UNION\n" +
                    "    SELECT DISTINCT d.* \n" +
                    "    FROM document d \n" +
                    "    JOIN time t ON d.document_id = t.document_id\n" +
                    "    WHERE LOWER(t.coveredtext) IN :searchTokens \n" +
                    "    UNION\n" +
                    "    SELECT DISTINCT d.* \n" +
                    "    FROM document d \n" +
                    "    JOIN taxon ta ON d.document_id = ta.document_id\n" +
                    "    WHERE LOWER(ta.coveredtext) IN :searchTokens \n" +
                    ") AS combined_result\n" +
                    "OFFSET :skip LIMIT :take ;";

            NativeQuery<Document> query = session.createNativeQuery(queryString, Document.class);
            query.setParameter("searchTokens", searchTokens);
            query.setParameter("skip", skip);
            query.setParameter("take", take);

            var docs = query.getResultList();
            // In the search view, for now we show the amount of pages so init them
            for(var doc: docs){
                Hibernate.initialize(doc.getPages());
            }
            return docs; */
        });
    }

    /**
     * Generic operation that fetches documents given the paramters
     * @return
     */
    public List<Document> searchForDocuments(int skip,
                                             int take){
        return executeOperationSafely((session) -> {
            var criteriaQuery = session.getCriteriaBuilder().createQuery(Document.class);
            var docRoot = criteriaQuery.from(Document.class);

            var query = session.createQuery(criteriaQuery);
            query.setFirstResult(skip);
            query.setMaxResults(take);

            var docs = query.getResultList();
            // In the search view, for now we show the amount of pages so init them
            for(var doc: docs){
                Hibernate.initialize(doc.getPages());
            }
            return docs;
        });
    }

    /**
     * Gets a single document without any lists.
     * @param id
     * @return
     */
    public Document getDocumentById(long id){
        return executeOperationSafely((session) -> {
            return session.get(Document.class, id);
        });
    }

    /**
     * Gets a complete document, alongside its lists, from the database.
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
     * @param document
     */
    public void saveDocument(Document document) {
        executeOperationSafely((session) -> {
            session.save(document);
            return null; // No return value needed for this write operation
        });
    }

    /**
     * Gets all sublists and complete properties of a document.
     * @param doc
     * @return
     */
    private Document initializeCompleteDocument(Document doc){
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
     * @return
     * @param <T>
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
     * @param doc
     */
    public void test(Document doc){
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
