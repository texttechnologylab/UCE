package org.texttechnologylab.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.texttechnologylab.services.DatabaseService;
import org.texttechnologylab.services.GoetheUniversityService;
import org.texttechnologylab.services.UIMAService;

@Configuration
public class SpringConfig {

    @Bean
    public DatabaseService databaseService(){
        return new DatabaseService();
    }

    @Bean
    public UIMAService uimaService(){
        return new UIMAService(goetheUniversityService());
    }

    @Bean
    public GoetheUniversityService goetheUniversityService(){
        return new GoetheUniversityService();
    }

}
