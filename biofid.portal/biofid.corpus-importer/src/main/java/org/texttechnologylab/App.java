package org.texttechnologylab;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.texttechnologylab.config.SpringConfig;
import org.texttechnologylab.services.UIMAService;

import static org.texttechnologylab.utilities.uima.jcas.SanitizingJCasFactory.createJCas;

/**
 * Entry of the corpus importer!
 */
public class App {
    public static void main(String[] args) {
        // Init DI
        var context = new AnnotationConfigApplicationContext(SpringConfig.class);
        var uimaService = context.getBean(UIMAService.class);
        // Decomment if you want to import test documents
        //uimaService.storeCorpusFromFolder("C:\\kevin\\projects\\biofid\\test_data\\2020_02_10");
        //uimaService.storeCorpusFromFolder("C:\\kevin\\projects\\biofid\\test_data\\zobodat");
        uimaService.storeCorpusFromFolder("C:\\kevin\\projects\\biofid\\test_data\\CORE\\texts_xmi_spacy\\unzipped");
        //uimaService.storeCorpusFromFolder("C:\\kevin\\projects\\biofid\\test_data\\bhl\\2023_01_25");
    }
}
