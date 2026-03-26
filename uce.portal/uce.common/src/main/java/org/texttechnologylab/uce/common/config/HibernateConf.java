package org.texttechnologylab.uce.common.config;

import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.texttechnologylab.models.authentication.DocumentPermission;
import org.texttechnologylab.uce.common.models.biofid.BiofidTaxon;
import org.texttechnologylab.uce.common.models.biofid.GazetteerTaxon;
import org.texttechnologylab.uce.common.models.biofid.GnFinderTaxon;
import org.texttechnologylab.uce.common.models.corpus.*;
import org.texttechnologylab.uce.common.models.corpus.emotion.Emotion;
import org.texttechnologylab.uce.common.models.corpus.emotion.Feeling;
import org.texttechnologylab.uce.common.models.corpus.links.AnnotationLink;
import org.texttechnologylab.uce.common.models.corpus.links.AnnotationToDocumentLink;
import org.texttechnologylab.uce.common.models.corpus.links.DocumentLink;
import org.texttechnologylab.uce.common.models.corpus.links.DocumentToAnnotationLink;
import org.texttechnologylab.uce.common.models.gbif.GbifOccurrence;
import org.texttechnologylab.uce.common.models.imp.ImportLog;
import org.texttechnologylab.uce.common.models.imp.UCEImport;
import org.texttechnologylab.uce.common.models.negation.*;
import org.texttechnologylab.uce.common.models.topic.TopicValueBase;
import org.texttechnologylab.uce.common.models.topic.TopicValueBaseWithScore;
import org.texttechnologylab.uce.common.models.topic.TopicWord;
import org.texttechnologylab.uce.common.models.topic.UnifiedTopic;

import java.util.HashMap;

@Configuration
@EnableTransactionManagement
public class HibernateConf {

    public static SessionFactory buildSessionFactory() {
        var settings = getSettings();

        var serviceRegistry = new StandardServiceRegistryBuilder()
                .applySettings(settings).build();

        var metadataSources = new MetadataSources(serviceRegistry);
        metadataSources.addAnnotatedClass(Block.class);
        metadataSources.addAnnotatedClass(MetadataTitleInfo.class);
        metadataSources.addAnnotatedClass(UCEMetadata.class);
        metadataSources.addAnnotatedClass(UCEMetadataFilter.class);
        // Links
        metadataSources.addAnnotatedClass(DocumentLink.class);
        metadataSources.addAnnotatedClass(AnnotationLink.class);
        metadataSources.addAnnotatedClass(DocumentToAnnotationLink.class);
        metadataSources.addAnnotatedClass(AnnotationToDocumentLink.class);
        metadataSources.addAnnotatedClass(Line.class);
        metadataSources.addAnnotatedClass(SrLink.class);
        metadataSources.addAnnotatedClass(Lemma.class);
        metadataSources.addAnnotatedClass(PageKeywordDistribution.class);
        metadataSources.addAnnotatedClass(DocumentKeywordDistribution.class);
        metadataSources.addAnnotatedClass(NamedEntity.class);
        metadataSources.addAnnotatedClass(Sentiment.class);
        metadataSources.addAnnotatedClass(Emotion.class);
        metadataSources.addAnnotatedClass(Feeling.class);
        metadataSources.addAnnotatedClass(GeoName.class);
        metadataSources.addAnnotatedClass(Paragraph.class);
        metadataSources.addAnnotatedClass(Sentence.class);
        metadataSources.addAnnotatedClass(GbifOccurrence.class);
        metadataSources.addAnnotatedClass(GazetteerTaxon.class);
        metadataSources.addAnnotatedClass(GnFinderTaxon.class);
        metadataSources.addAnnotatedClass(BiofidTaxon.class);
        metadataSources.addAnnotatedClass(Time.class);
        metadataSources.addAnnotatedClass(WikiDataHyponym.class);
        metadataSources.addAnnotatedClass(WikipediaLink.class);
        metadataSources.addAnnotatedClass(LexiconEntry.class);
        metadataSources.addAnnotatedClass(Page.class);
        metadataSources.addAnnotatedClass(Document.class);
        metadataSources.addAnnotatedClass(DocumentPermission.class);
        metadataSources.addAnnotatedClass(Corpus.class);
        metadataSources.addAnnotatedClass(CorpusTsnePlot.class);
        metadataSources.addAnnotatedClass(UCELog.class);
        metadataSources.addAnnotatedClass(UCEImport.class);
        metadataSources.addAnnotatedClass(ImportLog.class);
        metadataSources.addAnnotatedClass(Image.class);
        //negations
        metadataSources.addAnnotatedClass(CompleteNegation.class);
        metadataSources.addAnnotatedClass(Cue.class);
        metadataSources.addAnnotatedClass(Event.class);
        metadataSources.addAnnotatedClass(Focus.class);
        metadataSources.addAnnotatedClass(Scope.class);
        metadataSources.addAnnotatedClass(XScope.class);
        //topics
        metadataSources.addAnnotatedClass(UnifiedTopic.class);
        metadataSources.addAnnotatedClass(TopicWord.class);
        metadataSources.addAnnotatedClass(TopicValueBase.class);
        metadataSources.addAnnotatedClass(TopicValueBaseWithScore.class);

        metadataSources.addAnnotatedClass(DocumentTopThreeTopics.class);
        var metadata = metadataSources.buildMetadata();

        return metadata.getSessionFactoryBuilder().build();
    }

    private static HashMap<Object, Object> getSettings() {
        var settings = new HashMap<>();
        var config = new CommonConfig();
        // Hibernate expects the fully-qualified keys here (hibernate.*). If these are wrong,
        // Hibernate falls back and the logs show "using driver [null]".
        settings.put("hibernate.connection.driver_class", config.getPostgresqlProperty("connection.driver_class"));
        settings.put("hibernate.dialect", config.getPostgresqlProperty("dialect"));
        settings.put("hibernate.connection.url",config.getPostgresqlProperty("hibernate.connection.url"));
        settings.put("hibernate.connection.username", config.getPostgresqlProperty("hibernate.connection.username"));
        settings.put("hibernate.connection.password", config.getPostgresqlProperty("hibernate.connection.password"));
        settings.put("hibernate.current_session_context_class", config.getPostgresqlProperty("hibernate.current_session_context_class"));
        settings.put("hibernate.show_sql", config.getPostgresqlProperty("hibernate.show_sql"));
        settings.put("hibernate.format_sql", config.getPostgresqlProperty("hibernate.format_sql"));
        settings.put("hibernate.hbm2ddl.auto", config.getPostgresqlProperty("hibernate.hbm2ddl.auto"));
        
        // Keep pool implementation internal to the PostgreSQL adapter boundary.
        settings.put("hibernate.hikari.connectionTimeout", String.valueOf(config.getPostgresqlPoolConnectionTimeoutMs()));
        settings.put("hibernate.hikari.minimumIdle", String.valueOf(config.getPostgresqlPoolMinimumIdle()));
        settings.put("hibernate.hikari.maximumPoolSize", String.valueOf(config.getPostgresqlPoolMaximumSize()));
        settings.put("hibernate.hikari.idleTimeout", String.valueOf(config.getPostgresqlPoolIdleTimeoutMs()));
        settings.put("hibernate.hikari.maxLifetime", String.valueOf(config.getPostgresqlPoolMaxLifetimeMs()));
        settings.put("hibernate.hikari.autoCommit", "false");
        settings.put("hibernate.hikari.poolName", "UCEHikariPool");
        settings.put("hibernate.hikari.leakDetectionThreshold", String.valueOf(config.getPostgresqlPoolLeakDetectionThresholdMs()));
        
        // Enable HikariCP as connection provider
        settings.put("hibernate.connection.provider_class", "com.zaxxer.hikari.hibernate.HikariConnectionProvider");
        
        return settings;
    }
}
