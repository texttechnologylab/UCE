package org.texttechnologylab.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.texttechnologylab.services.*;

@Configuration
public class SpringConfig {

    @Bean
    public PostgresqlDataInterface_Impl databaseService(){
        return new PostgresqlDataInterface_Impl();
    }

    @Bean
    public UIMAService uimaService(){
        return new UIMAService(goetheUniversityService(), databaseService(), gbifService(), ragService(), jenaSparqlService());
    }
    @Bean
    public WikiService wikiService(){
        return new WikiService(databaseService(), ragService());
    }

    @Bean
    public GoetheUniversityService goetheUniversityService(){
        return new GoetheUniversityService();
    }

    @Bean
    public GbifService gbifService(){
        return new GbifService(jenaSparqlService());
    }

    @Bean
    public JenaSparqlService jenaSparqlService() {return new JenaSparqlService();}

    @Bean
    public RAGService ragService() {return new RAGService(databaseService());}

}
