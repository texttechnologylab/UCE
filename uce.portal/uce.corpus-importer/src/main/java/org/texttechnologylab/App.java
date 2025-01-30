package org.texttechnologylab;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.uima.fit.testing.util.DisableLogging;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.texttechnologylab.config.SpringConfig;
import org.texttechnologylab.config.UceConfig;
import org.texttechnologylab.exceptions.DatabaseOperationException;
import org.texttechnologylab.models.imp.ImportStatus;
import org.texttechnologylab.models.imp.UCEImport;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.security.InvalidParameterException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

/**
 * Entry of the corpus importer!
 */
public class App {
    private static final Logger logger = LogManager.getLogger();

    public static void main(String[] args) throws DatabaseOperationException, ParseException {
        // Disable the warning and other junk logs from the UIMA project.
        DisableLogging.enableLogging(Level.SEVERE);

        // Init DI
        var context = new AnnotationConfigApplicationContext(SpringConfig.class);

        // Read the import path from the CLI
        var options = getOptions();
        var parser = new DefaultParser();
        var cmd = parser.parse(options, args);

        var importDirPath = cmd.getOptionValue("importSrc");
        var importId = cmd.getOptionValue("importId");
        var importerNumber = Integer.parseInt(cmd.getOptionValue("importerNumber"));
        var numThreadsStr = cmd.getOptionValue("numThreads");
        var numThreads = 1;
        if(numThreadsStr != null) numThreads = Integer.parseInt(numThreadsStr);

        if(importId == null && importerNumber != 1){
            throw new InvalidParameterException("When no -importId is given, the -importerNumber must be 1, since this will be the only instance. Canceling.");
        }

        var importer = new Importer(context, importDirPath, importerNumber, importId);

        // If this is the number 1 importer, he will create a Database entry for this import. The other importers will wait for that db entry.
        if(importerNumber == 1){
            var uceImport = new UCEImport();
            uceImport.setBasePath(importDirPath);
            uceImport.setImportId(importId);
            uceImport.setStatus(ImportStatus.STARTING);
        } else{
            ;
        }

        importer.start(numThreads);

        // Decomment if you want to import test documents
        //importer.storeCorpusFromFolder("C:\\kevin\\projects\\biofid\\test_data\\2020_02_10");
        // importer.storeCorpusFromFolder("C:\\kevin\\projects\\uce\\test_data\\zobodat");
        //importer.storeCorpusFromFolder("C:\\kevin\\projects\\biofid\\test_data\\CORE\\srl_ht_tests\\_dataset");
        //importer.storeCorpusFromFolder("C:\\kevin\\projects\\biofid\\test_data\\bhl\\2023_01_25");
        //importer.storeCorpusFromFolder("C:\\kevin\\projects\\uce\\test_data\\GerParCor__Bundestag__18_19");
    }

    @NotNull
    private static Options getOptions() {
        var options = new Options();
        options.addOption("src", "importSrc", true, "The path to the import source where the UIMA-annotated files are stored.");
        options.addOption("iid", "importId", true, "When starting multiple importers, assign an id to each instance by counting up from 1 to n.");
        options.addOption("num", "importerNumber", true, "When starting multiple importers, assign an id to each instance by counting up from 1 to n.");
        options.addOption("t", "numThreads", true, "We do the import asynchronous. Decide with how many threads, e.g. 4-8. By default, this is single threaded.");
        return options;
    }
}
