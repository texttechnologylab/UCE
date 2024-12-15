package org.texttechnologylab;

import org.apache.uima.fit.testing.util.DisableLogging;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.texttechnologylab.config.SpringConfig;
import org.texttechnologylab.exceptions.DatabaseOperationException;
import org.texttechnologylab.services.UIMAService;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.texttechnologylab.utilities.uima.jcas.SanitizingJCasFactory.createJCas;

/**
 * Entry of the corpus importer!
 */
public class App {
    public static void main(String[] args) throws DatabaseOperationException {
        // Disable the warning and other junk logs from the UIMA project.
        DisableLogging.enableLogging(Level.SEVERE);

        // Init DI
        var context = new AnnotationConfigApplicationContext(SpringConfig.class);
        var uimaService = context.getBean(UIMAService.class);
        // Decomment if you want to import test documents
        //uimaService.storeCorpusFromFolder("C:\\kevin\\projects\\biofid\\test_data\\2020_02_10");
        uimaService.storeCorpusFromFolder("C:\\kevin\\projects\\uce\\test_data\\zobodat");
        //uimaService.storeCorpusFromFolder("C:\\kevin\\projects\\biofid\\test_data\\CORE\\srl_ht_tests\\_dataset");
        //uimaService.storeCorpusFromFolder("C:\\kevin\\projects\\biofid\\test_data\\bhl\\2023_01_25");
        //uimaService.storeCorpusFromFolder("C:\\kevin\\projects\\uce\\test_data\\GerParCor__Bundestag__18_19");
    }
}
