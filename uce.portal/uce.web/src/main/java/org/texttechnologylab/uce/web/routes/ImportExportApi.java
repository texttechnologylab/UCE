package org.texttechnologylab.uce.web.routes;

import io.javalin.http.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.uce.common.exceptions.DatabaseOperationException;
import org.texttechnologylab.uce.common.exceptions.ExceptionUtils;
import org.texttechnologylab.uce.common.services.PostgresqlDataInterface_Impl;
import org.texttechnologylab.uce.common.services.S3StorageService;
import org.texttechnologylab.uce.common.utils.StringUtils;
import org.texttechnologylab.uce.corpusimporter.Importer;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
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

    public void downloadUIMA(Context ctx) {
        var objectName = ExceptionUtils.tryCatchLog(
                () -> ctx.queryParam("objectName"),
                (ex) -> logger.error("Error getting a cas object from the storage to download, the objectName parameter wasn't set.", ex));
        if (objectName == null || objectName.isBlank()) {
            ctx.status(400);
            ctx.result("Missing required query parameter: objectName");
            return;
        }

        try (
                var s3Stream = s3StorageService.downloadObject(objectName);
                var out = ctx.res().getOutputStream()
        ) {
            var contentType = s3StorageService.getContentTypeOfObject(objectName);
            ctx.res().setContentType(contentType);
            ctx.res().setHeader("Content-Disposition", "attachment; filename=\"" + objectName + "." + StringUtils.getExtensionByContentType(contentType) + "\"");

            var buffer  = new byte[8192];
            int bytesRead;
            while ((bytesRead = s3Stream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }

            out.flush();
            //return ctx.res(); // required to return the raw response stream
        } catch (Exception e) {
            logger.error("Failed to download object: " + objectName, e);
            ctx.status(500);
            ctx.result("Error downloading object: " + e.getMessage());
        }
    }

    public void uploadUIMA(Context ctx) {
        try {
            // First, we need to know which corpus this document should be added to.
            var corpusId = ExceptionUtils.tryCatchLog(
                    () -> Long.parseLong(new String(ctx.req().getPart("corpusId").getInputStream().readAllBytes(), StandardCharsets.UTF_8)),
                    (ex) -> logger.error("Error getting the corpusId this document should be added to. Aborting.", ex));
            if (corpusId == null) {
                ctx.result("Parameter corpusId didn't exist. Without it, the document cannot be uploaded.");
                return;
            }

            var casView = ExceptionUtils.tryCatchLog(
                    () -> new String(ctx.req().getPart("casView").getInputStream().readAllBytes(), StandardCharsets.UTF_8),
                    (ex) -> logger.error("Error getting the casView that should be imported from this document. Using default view.", ex));

            // If we import a document with multiple views, we can optionally override the documentId that is used to check for existing documents
            var documentId = ExceptionUtils.tryCatchLog(
                    () -> new String(ctx.req().getPart("documentId").getInputStream().readAllBytes(), StandardCharsets.UTF_8),
                    (ex) -> logger.error("Error getting the documentId that should be used as a name/id for this document. Aborting.", ex));

            var corpus = ExceptionUtils.tryCatchLog(
                    () -> db.getCorpusById(corpusId),
                    (ex) -> logger.error("Couldn't fetch corpus when uploading new document to corpusId " + corpusId, ex));
            if (corpus == null) {
                ctx.result("Corpus with id " + corpusId + " wasn't found in the database; can't upload document.");
                return;
            }

            // TODO just use 1 as default? will throw an error if this is null otherwise...
            var importerNumber = 1;
            var importer = new Importer(this.serviceContext, importerNumber, casView);
            try (var input = ctx.req().getPart("file").getInputStream()) {
                var fileName = ctx.req().getPart("file").getSubmittedFileName();
                // Import the doc in the background
                var importFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        return importer.storeUploadedXMIToCorpusAsync(input, corpus, fileName, documentId);
                    } catch (DatabaseOperationException e) {
                        throw new RuntimeException(e);
                    }
                });
                Long newDocumentId = importFuture.get();
                // TODO check that this new document id is not null, could this happen?

                ctx.status(200);

                var acceptedContentType = ctx.header("Accept");
                if (acceptedContentType != null && acceptedContentType.equals("application/json")) {
                    Map<String, Object> apiResult = new HashMap<>();
                    apiResult.put("document_id", newDocumentId);
                    ctx.json(apiResult);
                    return;
                }

                ctx.result("File uploaded successfully!");
            }
        } catch (Exception e) {
            ctx.status(500);
            ctx.result("Error uploading a file: " + e.getMessage());
        }
    };

}
