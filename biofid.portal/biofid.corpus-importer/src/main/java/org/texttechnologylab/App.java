package org.texttechnologylab;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.tcas.Annotation;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Import;
import org.texttechnologylab.config.SpringConfig;
import org.texttechnologylab.services.UIMAService;

import java.util.HashSet;

import static org.texttechnologylab.utilities.uima.jcas.SanitizingJCasFactory.createJCas;

/**
 * Entry of the corpus importer!
 */
public class App {
    public static void main(String[] args) {

        // Init DI
        var context = new AnnotationConfigApplicationContext(SpringConfig.class);
        var uimaService = context.getBean(UIMAService.class);

        var test = uimaService.XMIToDocument("C:\\kevin\\projects\\biofid\\test_data\\064_10801712.xml.gz.xmi");
    }
}
