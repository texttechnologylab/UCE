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

@Import(SpringConfig.class)
/**
 * Entry of the corpus importer!
 */
public class App {
    public static void main(String[] args) {

        // Init DI
        var context = new AnnotationConfigApplicationContext(SpringConfig.class);
        var uimaService = context.getBean(UIMAService.class);

        var test = uimaService.XMIToDocument("C:\\kevin\\projects\\biofid\\test_data\\064_10801712.xml.gz.xmi");

        // Let's test a bit
        try {
            // An empty jcas
            var jCas = JCasFactory.createJCas();

            // Read in the contents of a single xmi cas
            var unique = new HashSet<String>();
            JCasUtil.select(jCas, Annotation.class).stream().forEach(a -> {
                unique.add(a.getType().getName());
            });
            unique.forEach(a -> System.out.println(a));

            JCasUtil.select(jCas, org.texttechnologylab.annotation.type.Taxon.class).stream().forEach(a -> {
                System.out.println(a.getCoveredText());
                System.out.println(a.getType().toString());
                JCasUtil.selectCovered(Annotation.class, a).stream().forEach(b -> {
                    System.out.println(b);
                });
            });

            System.out.print("Ok.");
        } catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
            ex.printStackTrace();
        }

    }
}
