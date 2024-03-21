package org.texttechnologylab.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.texttechnologylab.services.*;

@Configuration
public class SpringConfig {

    @Bean
    public DatabaseService databaseService(){
        return new DatabaseService();
    }

    @Bean
    public UIMAService uimaService(){
        return new UIMAService(goetheUniversityService(), databaseService(), gbifService(), ragService());
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
