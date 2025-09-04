package org.texttechnologylab.uce.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.texttechnologylab.uce.common.services.*;

@Configuration
public class SpringConfig {

    @Bean
    public PostgresqlDataInterface_Impl databaseService() {
        return new PostgresqlDataInterface_Impl();
    }

    @Bean
    public LexiconService lexiconService() {
        return new LexiconService(databaseService());
    }

    @Bean
    public AuthenticationService authenticationService() {return new AuthenticationService();}

    @Bean
    public MapService mapService() {
        return new MapService(databaseService());
    }

    @Bean
    public WikiService wikiService() {
        return new WikiService(databaseService(), ragService(), jenaSparqlService());
    }

    @Bean
    public GoetheUniversityService goetheUniversityService() {
        return new GoetheUniversityService();
    }

    @Bean
    public GbifService gbifService() {
        return new GbifService(jenaSparqlService());
    }

    @Bean
    public JenaSparqlService jenaSparqlService() {
        return new JenaSparqlService();
    }

    @Bean
    public RAGService ragService() {
        return new RAGService(databaseService());
    }

    @Bean
    public S3StorageService s3Storage() {
        return new S3StorageService();
    }

}
