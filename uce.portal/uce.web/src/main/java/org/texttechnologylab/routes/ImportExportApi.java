package org.texttechnologylab.routes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.Importer;
import org.texttechnologylab.exceptions.DatabaseOperationException;
import org.texttechnologylab.exceptions.ExceptionUtils;
import org.texttechnologylab.services.PostgresqlDataInterface_Impl;
import org.texttechnologylab.services.S3StorageService;
import org.texttechnologylab.utils.StringUtils;
import spark.Route;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class ImportExportApi implements UceApi {

    private S3StorageService s3StorageService;
    private PostgresqlDataInterface_Impl db;
    private ApplicationContext serviceContext;

    private static final Logger logger = LogManager.getLogger(PostgresqlDataInterface_Impl.class);

    public ImportExportApi(ApplicationContext serviceContext) {
        this.serviceContext = serviceContext;
        this.s3StorageService = serviceContext.getBean(S3StorageService.class);
        this.db = serviceContext.getBean(PostgresqlDataInterface_Impl.class);
    }

    public Route downloadUIMA = ((request, response) -> {
        var objectName = ExceptionUtils.tryCatchLog(
                () -> request.queryParams("objectName"),
                (ex) -> logger.error("Error getting a cas object from the storage to download, the objectName parameter wasn't set.", ex));
        if (objectName == null || objectName.isBlank()) {
            response.status(400);
            return "Missing required query parameter: objectName";
        }

        try (
                var s3Stream = s3StorageService.downloadObject(objectName);
                var out = response.raw().getOutputStream()
        ) {
            var contentType = s3StorageService.getContentTypeOfObject(objectName);
            response.raw().setContentType(contentType);
            response.raw().setHeader("Content-Disposition", "attachment; filename=\"" + objectName + "." + StringUtils.getExtensionByContentType(contentType) + "\"");

            var buffer  = new byte[8192];
            int bytesRead;
            while ((bytesRead = s3Stream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }

            out.flush();
            return response.raw(); // required to return the raw response stream
        } catch (Exception e) {
            logger.error("Failed to download object: " + objectName, e);
            response.status(500);
            return "Error downloading object: " + e.getMessage();
        }
    });

    public Route uploadUIMA = ((request, response) -> {
        try {
            // First, we need to know which corpus this document should be added to.
            var corpusId = ExceptionUtils.tryCatchLog(
                    () -> Long.parseLong(new String(request.raw().getPart("corpusId").getInputStream().readAllBytes(), StandardCharsets.UTF_8)),
                    (ex) -> logger.error("Error getting the corpusId this document should be added to. Aborting.", ex));
            if (corpusId == null)
                return "Parameter corpusId didn't exist. Without it, the document cannot be uploaded.";

            var casView = ExceptionUtils.tryCatchLog(
                    () -> new String(request.raw().getPart("casView").getInputStream().readAllBytes(), StandardCharsets.UTF_8),
                    (ex) -> logger.error("Error getting the casView that should be imported from this document. Using default view.", ex));

            // If we import a document with multiple views, we can optionally override the documentId that is used to check for existing documents
            var documentId = ExceptionUtils.tryCatchLog(
                    () -> new String(request.raw().getPart("documentId").getInputStream().readAllBytes(), StandardCharsets.UTF_8),
                    (ex) -> logger.error("Error getting the documentId that should be used as a name/id for this document. Aborting.", ex));

            var corpus = ExceptionUtils.tryCatchLog(
                    () -> db.getCorpusById(corpusId),
                    (ex) -> logger.error("Couldn't fetch corpus when uploading new document to corpusId " + corpusId, ex));
            if (corpus == null)
                return "Corpus with id " + corpusId + " wasn't found in the database; can't upload document.";

            // TODO just use 1 as default? will throw an error if this is null otherwise...
            var importerNumber = 1;
            var importer = new Importer(this.serviceContext, importerNumber, casView);
            try (var input = request.raw().getPart("file").getInputStream()) {
                var fileName = request.raw().getPart("file").getSubmittedFileName();
                // Import the doc in the background
                var importFuture = CompletableFuture.runAsync(() -> {
                    try {
                        importer.storeUploadedXMIToCorpusAsync(input, corpus, fileName, documentId);
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

}
