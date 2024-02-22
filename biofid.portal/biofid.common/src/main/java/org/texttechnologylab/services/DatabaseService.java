package org.texttechnologylab.services;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.stereotype.Service;
import org.texttechnologylab.config.HibernateConf;
import org.texttechnologylab.models.corpus.*;
import org.texttechnologylab.models.test.test;
import org.texttechnologylab.utils.StringUtils;

import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;
import javax.print.Doc;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

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
     * Generic operation that fetches documents given the paramters
     * @return
     */
    public List<Document> searchForDocuments(int take,
                                             int skip){
        return executeOperationSafely((session) -> {
            var criteriaQuery = session.getCriteriaBuilder().createQuery(Document.class);
            var docRoot = criteriaQuery.from(Document.class);

            // Here, we can later insert more search critieras if we want
            /*if (searchTerm != null && !searchTerm.isEmpty()) {
                // Assuming you want to search by some property of Document
                Predicate searchPredicate = criteriaBuilder.like(docRoot.get("propertyName"), "%" + searchTerm + "%");
                criteriaQuery.where(searchPredicate);
            }*/

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
