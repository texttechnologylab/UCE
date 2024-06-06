package org.texttechnologylab.config;

import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.texttechnologylab.models.corpus.*;
import org.texttechnologylab.models.gbif.GbifOccurrence;
import org.texttechnologylab.models.rag.DocumentEmbedding;
import org.texttechnologylab.models.test.test;

import java.util.HashMap;

@Configuration
@EnableTransactionManagement
public class HibernateConf {

    public static SessionFactory buildSessionFactory() {
        var settings = getSettings();

        var serviceRegistry = new StandardServiceRegistryBuilder()
                .applySettings(settings).build();

        var metadataSources = new MetadataSources(serviceRegistry);
        metadataSources.addAnnotatedClass(test.class);
        metadataSources.addAnnotatedClass(Block.class);
        metadataSources.addAnnotatedClass(MetadataTitleInfo.class);
        metadataSources.addAnnotatedClass(Line.class);
        metadataSources.addAnnotatedClass(SrLink.class);
        metadataSources.addAnnotatedClass(Lemma.class);
        metadataSources.addAnnotatedClass(NamedEntity.class);
        metadataSources.addAnnotatedClass(Paragraph.class);
        metadataSources.addAnnotatedClass(Sentence.class);
        metadataSources.addAnnotatedClass(GbifOccurrence.class);
        metadataSources.addAnnotatedClass(Taxon.class);
        metadataSources.addAnnotatedClass(Time.class);
        metadataSources.addAnnotatedClass(WikiDataHyponym.class);
        metadataSources.addAnnotatedClass(WikipediaLink.class);
        metadataSources.addAnnotatedClass(Page.class);
        metadataSources.addAnnotatedClass(Document.class);
        metadataSources.addAnnotatedClass(Corpus.class);

        var metadata = metadataSources.buildMetadata();

        return metadata.getSessionFactoryBuilder().build();
    }

    @NotNull
    private static HashMap<Object, Object> getSettings() {
        var settings = new HashMap<>();
        var config = new CommonConfig();
        settings.put("connection.driver_class", config.getPostgresqlProperty("connection.driver_class"));
        settings.put("dialect", config.getPostgresqlProperty("dialect"));
        settings.put("hibernate.connection.url",config.getPostgresqlProperty("hibernate.connection.url"));
        settings.put("hibernate.connection.username", config.getPostgresqlProperty("hibernate.connection.username"));
        settings.put("hibernate.connection.password", config.getPostgresqlProperty("hibernate.connection.password"));
        settings.put("hibernate.current_session_context_class", config.getPostgresqlProperty("hibernate.current_session_context_class"));
        settings.put("hibernate.show_sql", config.getPostgresqlProperty("hibernate.show_sql"));
        settings.put("hibernate.format_sql", config.getPostgresqlProperty("hibernate.format_sql"));
        settings.put("hibernate.hbm2ddl.auto", config.getPostgresqlProperty("hibernate.hbm2ddl.auto"));
        return settings;
    }
}
