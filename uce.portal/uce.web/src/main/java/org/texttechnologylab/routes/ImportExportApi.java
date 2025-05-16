package org.texttechnologylab.routes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.Importer;
import org.texttechnologylab.exceptions.DatabaseOperationException;
import org.texttechnologylab.exceptions.ExceptionUtils;
import org.texttechnologylab.models.imp.ImportStatus;
import org.texttechnologylab.models.imp.UCEImport;
import org.texttechnologylab.services.PostgresqlDataInterface_Impl;
import spark.Route;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ImportExportApi {

    private PostgresqlDataInterface_Impl db;
    private ApplicationContext serviceContext;

    private static final Logger logger = LogManager.getLogger(PostgresqlDataInterface_Impl.class);

    public ImportExportApi(ApplicationContext serviceContext) {
        this.serviceContext = serviceContext;
        this.db = serviceContext.getBean(PostgresqlDataInterface_Impl.class);
    }

    public Route uploadUIMA = ((request, response) -> {
        try {
            // First, we need to know which corpus this document should be added to.
            var corpusId = ExceptionUtils.tryCatchLog(
                    () -> Long.parseLong(new String(request.raw().getPart("corpusId").getInputStream().readAllBytes(), StandardCharsets.UTF_8)),
                    (ex) -> logger.error("Error getting the corpusId this document should be added to. Aborting.", ex));
            if (corpusId == null)
                return "Parameter corpusId didn't exist. Without it, the document cannot be uploaded.";

            var corpus = ExceptionUtils.tryCatchLog(
                    () -> db.getCorpusById(corpusId),
                    (ex) -> logger.error("Couldn't fetch corpus when uploading new document to corpusId " + corpusId, ex));
            if (corpus == null)
                return "Corpus with id " + corpusId + " wasn't found in the database; can't upload document.";

            var importer = new Importer(this.serviceContext);
            try (var input = request.raw().getPart("file").getInputStream()) {
                // Import the doc in the background
                var importFuture = CompletableFuture.runAsync(() -> {
                    try {
                        importer.storeUploadedXMIToCorpusAsync(input, corpus);
                    } catch (DatabaseOperationException e) {
                        throw new RuntimeException(e);
                    }
                });
                importFuture.get();
                response.status(200);
                return "File uploaded successfully!";
            }
        } catch (Exception e) {
            response.status(500);
            return "Error uploading a file: " + e.getMessage();
        }
    });


    public Route uploadCorpus = ((request, response) -> {
        try {

            try (InputStream folderPathStream = request.raw().getPart("corpusPath").getInputStream()) {
                String folderPath = new String(folderPathStream.readAllBytes(), StandardCharsets.UTF_8);

                var importId = UUID.randomUUID().toString();
                var importerNumber = 1;
                var numThreads = 1;
                var importer = new Importer(this.serviceContext, folderPath,importerNumber,importId,null)  ;
                // If this is the number 1 importer, he will create a Database entry for this import. The other importers will wait for that db entry.

                var uceImport = new UCEImport(importId, folderPath, ImportStatus.STARTING);
                var fileCount = ExceptionUtils.tryCatchLog(importer::getXMICountInPath,
                        (ex) -> logger.warn("There was an IO error counting the importable UIMA files - the import will probably fail at some point.", ex));
                uceImport.setTotalDocuments(fileCount == null ? -1 : fileCount);
                serviceContext.getBean(PostgresqlDataInterface_Impl.class).saveOrUpdateUceImport(uceImport);

                // Import the doc in the background
                var importFuture = CompletableFuture.runAsync(() -> {
                    try {
                        importer.start(numThreads);
                    } catch (DatabaseOperationException e) {
                        throw new RuntimeException(e);
                    }
                });
                importFuture.get();
                response.status(200);
                return "Corpus added successfully!";
            }
        } catch (Exception e) {
            response.status(500);
            return "Error uploading the corpus: " + e.getMessage();
        }
    });
}
