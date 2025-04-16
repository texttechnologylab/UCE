package org.texttechnologylab;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.uima.fit.testing.util.DisableLogging;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.texttechnologylab.config.CommonConfig;
import org.texttechnologylab.config.SpringConfig;
import org.texttechnologylab.config.UceConfig;
import org.texttechnologylab.exceptions.DatabaseOperationException;
import org.texttechnologylab.exceptions.ExceptionUtils;
import org.texttechnologylab.models.imp.ImportStatus;
import org.texttechnologylab.models.imp.UCEImport;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.texttechnologylab.services.PostgresqlDataInterface_Impl;
import org.texttechnologylab.utils.SystemStatus;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

/**
 * Entry of the corpus importer!
 */
public class App {
    private static final Logger logger = LogManager.getLogger(App.class);

    public static void main(String[] args) throws DatabaseOperationException, ParseException {
        // Disable the warning and other junk logs from the UIMA project.
        DisableLogging.enableLogging(Level.SEVERE);

        // Init DI
        var context = new AnnotationConfigApplicationContext(SpringConfig.class);

        // Execute the external database scripts
        var commonConfig = new CommonConfig();
        logger.info("Executing external database scripts from " + commonConfig.getDatabaseScriptsLocation());
        ExceptionUtils.tryCatchLog(
                () -> SystemStatus.ExecuteExternalDatabaseScripts(commonConfig.getDatabaseScriptsLocation(), context.getBean(PostgresqlDataInterface_Impl.class)),
                (ex) -> logger.warn("Couldn't read the db scripts in the external database scripts folder; path wasn't found or other IO problems. ", ex));
        logger.info("Finished with executing external database scripts.");

        // Read the import path from the CLI
        var options = getOptions();
        var parser = new DefaultParser();
        var cmd = parser.parse(options, args);

        var importSrcPath = cmd.getOptionValue("importSrc");
        var importDirPath = cmd.getOptionValue("importDir");
        var importId = UUID.randomUUID().toString();
        var importerNumber = Integer.parseInt(cmd.getOptionValue("importerNumber"));
        var numThreadsStr = cmd.getOptionValue("numThreads");
        var numThreads = 1;
        if (numThreadsStr != null) numThreads = Integer.parseInt(numThreadsStr);

        if (importerNumber != 1) {
            throw new InvalidParameterException("For now, the -importerNumber must always be 1, since this will be the only instance. Canceling.");
        }

        var importablePaths = new ArrayList<String>();
        // If no parent directory was given, we simply import the single src path
        if (importDirPath == null) importablePaths.add(importSrcPath);
        else {
            // Else, we want to check EACH folder in the parent directory and try to import them each.
            var dir = new File(importDirPath);
            for (var file : Objects.requireNonNull(dir.listFiles())) {
                if (file.isDirectory()) importablePaths.add(file.getPath());
            }
        }

        for (var path : importablePaths) {
            var importer = new Importer(context, path, importerNumber, importId);

            // If this is the number 1 importer, he will create a Database entry for this import. The other importers will wait for that db entry.
            if (importerNumber == 1) {
                var uceImport = new UCEImport(importId, path, ImportStatus.STARTING);
                var fileCount = ExceptionUtils.tryCatchLog(importer::getXMICountInPath,
                        (ex) -> logger.warn("There was an IO error counting the importable UIMA files - the import will probably fail at some point.", ex));
                uceImport.setTotalDocuments(fileCount == null ? -1 : fileCount);
                context.getBean(PostgresqlDataInterface_Impl.class).saveOrUpdateUceImport(uceImport);
            } else {
                // TODO: This was meant to be prepared for the incorporation into DUUI, but this isn't decided yet.
                ;
            }

            importer.start(numThreads);
        }

    }

    private static Options getOptions() {
        var options = new Options();
        options.addOption("srcDir", "importDir", true, "Unlike '-src', '-srcDir' is the path to a directory that holds multiple importable 'src' paths. " +
                "The importer will check for folders within this directory, where each folder should be an importable corpus with a corpusConfig.json and its input UIMA-files. Those are then imported.");
        options.addOption("src", "importSrc", true, "The path to the import source where the UIMA-annotated files are stored.");
        options.addOption("num", "importerNumber", true, "When starting multiple importers, assign an id to each instance by counting up from 1 to n.");
        options.addOption("t", "numThreads", true, "We do the import asynchronous. Decide with how many threads, e.g. 4-8. By default, this is single threaded.");
        return options;
    }
}
