package org.texttechnologylab.uce.web.routes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.uce.common.config.CorpusConfig;
import org.texttechnologylab.uce.common.config.corpusConfig.CorpusAnnotationConfig;
import org.texttechnologylab.uce.common.config.corpusConfig.OtherConfig;
import org.texttechnologylab.uce.common.config.corpusConfig.TaxonConfig;
import org.texttechnologylab.uce.common.exceptions.DatabaseOperationException;
import org.texttechnologylab.uce.common.exceptions.ExceptionUtils;
import org.texttechnologylab.uce.common.models.corpus.Corpus;
import org.texttechnologylab.uce.common.models.imp.ImportStatus;
import org.texttechnologylab.uce.common.models.imp.UCEImport;
import org.texttechnologylab.uce.common.services.PostgresqlDataInterface_Impl;
import org.texttechnologylab.uce.common.services.S3StorageService;
import org.texttechnologylab.uce.common.utils.StringUtils;
import org.texttechnologylab.uce.corpusimporter.Importer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


public class ImportExportApi implements UceApi {

    private static final Logger logger = LogManager.getLogger(PostgresqlDataInterface_Impl.class);
    private S3StorageService s3StorageService;
    private PostgresqlDataInterface_Impl db;
    private ApplicationContext serviceContext;

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

            var buffer = new byte[8192];
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
                    (ex) -> logger.error("Error getting corpusId from request.", ex));

            if (corpusId == null) {
                ctx.status(400);
                ctx.result("Parameter corpusId didn't exist; cannot upload document.");
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
                    (ex) -> logger.error("Couldn't fetch corpus with id " + corpusId, ex));

            if (corpus == null) {
                ctx.status(404);
                ctx.result("Corpus with id " + corpusId + " wasn't found in the database.");
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
//                    ctx.contentType("application/json"); //redundant
                    ctx.json(apiResult);
                    return;
                }

                ctx.result("File uploaded successfully!");
            }
        } catch (Exception e) {
            ctx.status(500);
            ctx.result("Error uploading a file: " + e.getMessage());
        }
    }


    public void importCorpusFromPath(Context ctx) {
        try {
            String path = ctx.formParam("path");
            String numThreadStr = ctx.formParam("numThreads");
            int numThreads = (numThreadStr != null && !numThreadStr.isBlank()) ? Integer.parseInt(numThreadStr) : 1;
            String casView = ctx.formParam("casView");

            if (casView != null && casView.isBlank()) {
                casView = null;
            }

            if (path == null || path.isBlank()) {
                ctx.status(400).result("Path is required");
                return;
            }

            String importId = UUID.randomUUID().toString();
            int importerNumber = 1;
            Importer importer = new Importer(serviceContext, path, importerNumber, importId, casView);
            UCEImport uceImport = new UCEImport(importId, path, ImportStatus.STARTING);
            Integer fileCount = ExceptionUtils.tryCatchLog(importer::getXMICountInPath,
                    (ex) -> logger.warn("There was an IO error counting the importable UIMA files - the import will probably fail at some point.", ex));
            uceImport.setTotalDocuments(fileCount == null ? -1 : fileCount);
            db.saveOrUpdateUceImport(uceImport);
            CompletableFuture.runAsync(() -> {
                try {
                    importer.start(numThreads);
                } catch (DatabaseOperationException e) {
                    logger.error("Error during asynchronous corpus import", e);
                }
            });
            ctx.status(200).result("Import started. Import ID: " + importId);
        } catch (DatabaseOperationException e) {
            logger.error("Error when creating saving/updating to database" + e);
            ctx.status(500).result("Database error initiating corpus import" + e.getMessage());

        } catch (Exception e) {
            logger.error("Error initiating corpus import", e);
            ctx.status(500).result("Error initiating import: " + e.getMessage());
        }

    }
    
    public void importCorpusFromUpload(Context ctx){
        try{
            String importId = UUID.randomUUID().toString();
            Path rootDir = java.nio.file.Paths.get(System.getProperty("java.io.tmpdir"), "uce_uploads", importId);
            Path inputDir = rootDir.resolve("input");
            Files.createDirectories(inputDir);

            var validFiles = ctx.uploadedFiles("files").stream()
                    .filter(f -> f.size() > 0 && f.filename() != null && !f.filename().isBlank())
                    .toList();

            if (validFiles.isEmpty()) {
                ctx.status(400).result("No files selected. Please select at least one XMI file or archive.");
                return;
            }
            
            for(UploadedFile uploadedFile : ctx.uploadedFiles("files")){
                try(InputStream input = uploadedFile.content()){
                    Files.copy(input,inputDir.resolve(uploadedFile.filename()), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            
            String name = ctx.formParam("name");
            if (name == null || name.isBlank()){
                ctx.status(400).result("No corpus name given");
            }
            String addToExistingParam = ctx.formParam("addToExistingCorpus");
            boolean addToExisting = addToExistingParam != null && Boolean.parseBoolean(addToExistingParam);
            CorpusConfig config = null;
            if(addToExisting){
                Corpus existingCorpus = ExceptionUtils.tryCatchLog(() -> 
                        db.getCorpusByName(name),
                        (ex) -> logger.warn("Could not fetch existing corpus config for merging",ex)
                        );
                if (existingCorpus != null && existingCorpus.getCorpusJsonConfig() != null) 
                    config = CorpusConfig.fromJson(existingCorpus.getCorpusJsonConfig());
            }
            if (config == null){
                config = new CorpusConfig();
                config.setAnnotations(new CorpusAnnotationConfig());
                config.getAnnotations().setTaxon(new TaxonConfig());
                config.setOther(new OtherConfig());
            } else{
                if(config.getAnnotations() == null) config.setAnnotations(new CorpusAnnotationConfig());
                if(config.getAnnotations().getTaxon() == null) config.getAnnotations().setTaxon(new TaxonConfig());
                if(config.getOther() == null) config.setOther(new OtherConfig());
            }
            
            config.setName(name);
            config.setAddToExistingCorpus(addToExisting);
            String author = ctx.formParam("author");
            if (author != null && !author.isBlank()) config.setAuthor(author);
            else if (config.getAuthor() == null) {
                ctx.status(400).result("Corpus Author is required");
                return;
            }
            String language = ctx.formParam("language");
            if (language != null && !language.isBlank()) config.setLanguage(language);
            else if (config.getLanguage() == null) {
                ctx.status(400).result("Corpus Language is required.");
                return;
            }
            String description = ctx.formParam("description");
            if (description != null && !description.isBlank()) config.setDescription(description);
//             Annotations
             CorpusAnnotationConfig ann = config.getAnnotations();
             ann.setSentence(ann.isSentence() || ctx.formParam("sentence") != null);
             ann.setLemma(ann.isLemma() || ctx.formParam("lemma") != null);
             ann.setNamedEntity(ann.isNamedEntity() || ctx.formParam("namedEntity") != null);
             ann.setTopic(ann.isNamedEntity() || ctx.formParam("topic") != null);
             ann.setSentiment(ann.isSentiment() || ctx.formParam("sentiment") != null);
             ann.setEmotion(ann.isEmotion() || ctx.formParam("emotion") != null);
             ann.setTime(ann.isTime() || ctx.formParam("time") != null);
             ann.setGeoNames(ann.isGeoNames() || ctx.formParam("geoNames") != null);
             ann.setWikipediaLink(ann.isWikipediaLink() || ctx.formParam("wikipediaLink") != null);
             ann.setImage(ann.isImage() || ctx.formParam("image") != null);
             ann.setAnnotatorMetadata(ann.isAnnotatorMetadata() || ctx.formParam("annotatorMetadata") != null);
             ann.setUceMetadata(ann.isUceMetadata() || ctx.formParam("uceMetadata") != null);
             ann.setLogicalLinks(ann.isLogicalLinks() || ctx.formParam("logicalLinks") != null);
             ann.setSrLink(ann.isSrLink() || ctx.formParam("srLink") != null);
             ann.setUnifiedTopic(ann.isUnifiedTopic() || ctx.formParam("unifiedTopic") != null);
             ann.setOCRPage(ann.isOCRPage() || ctx.formParam("OCRPage") != null);
             ann.setOCRParagraph(ann.isOCRParagraph() || ctx.formParam("OCRParagraph") != null);
             ann.setOCRBlock(ann.isOCRBlock() || ctx.formParam("OCRBlock") != null);
             ann.setOCRLine(ann.isOCRLine() || ctx.formParam("OCRLine") != null);
             ann.setCompleteNegation(ann.isCompleteNegation() || ctx.formParam("completeNegation") != null);
             ann.setCue(ann.isCue() || ctx.formParam("cue") != null);
             ann.setEvent(ann.isEvent() || ctx.formParam("event") != null);
             ann.setFocus(ann.isFocus() || ctx.formParam("focus") != null);
             ann.setScope(ann.isScope() || ctx.formParam("scope") != null);
             ann.setXscope(ann.isXscope() || ctx.formParam("xscope") != null);


            TaxonConfig taxonConfig = ann.getTaxon();
            taxonConfig.setAnnotated(taxonConfig.isAnnotated() || ctx.formParam("taxonAnnotated") != null);
            taxonConfig.setBiofidOnthologyAnnotated(taxonConfig.isBiofidOnthologyAnnotated() || ctx.formParam("biofidOnthologyAnnotated") != null);
//          Other Settings
            OtherConfig otherConfig = config.getOther();
            otherConfig.setEnableEmbeddings(otherConfig.isEnableEmbeddings() || ctx.formParam("enableEmbeddings") != null);
            otherConfig.setEnableRAGBot(otherConfig.isEnableRAGBot() ||  ctx.formParam("enableRAGBot") != null);
            otherConfig.setIncludeKeywordDistribution(otherConfig.isIncludeKeywordDistribution()|| ctx.formParam("includeKeywordDistribution") != null);
            otherConfig.setEnableS3Storage(otherConfig.isEnableS3Storage() || ctx.formParam("enableS3Storage") != null);
            otherConfig.setAvailableOnFrankfurtUniversityCollection(otherConfig.isAvailableOnFrankfurtUniversityCollection() || ctx.formParam("availableOnFrankfurtUniversityCollection") != null);
            
            config.setAnnotations(ann);
            config.setOther(otherConfig);
            
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonString = gson.toJson(config);
            Files.writeString(rootDir.resolve("corpusConfig.json"),jsonString,StandardCharsets.UTF_8);

            String numThreadStr = ctx.formParam("numThreads");
            int numThreads = (numThreadStr != null && !numThreadStr.isBlank()) ? Integer.parseInt(numThreadStr) : 1;
            String casView = ctx.formParam("casView");
            if(casView != null && casView.isBlank()) casView = null;
            int importerNumber = 1;
            Importer importer = new Importer(serviceContext,rootDir.toString(),importerNumber,importId,casView);
            
            String logTitle = (addToExisting ? "ADD_TO:" : "UPLOAD_NEW:") + name;
            UCEImport uceImport = new UCEImport(importId,logTitle,ImportStatus.STARTING);
            Integer fileCount = ExceptionUtils.tryCatchLog(importer::getXMICountInPath,
                    (ex) -> logger.warn("IO Error counting upload files.",ex));
            uceImport.setTotalDocuments(fileCount == null ? -1 : fileCount);
            db.saveOrUpdateUceImport(uceImport);
            CompletableFuture.runAsync(() -> {
                try{
                    importer.start(numThreads);
                } catch (DatabaseOperationException e) {
                    logger.error("Error during asynchronous corpus uplaod import",e);
                }finally {
                    try {
                        org.apache.commons.io.FileUtils.deleteDirectory(rootDir.toFile());
                    } catch (IOException e) {
                        logger.warn("Could not delete temp upload dir: " + rootDir,e);
                    }
                }
            });
            
            ctx.status(200).result("Upload sucessfull. Import started with ID: " + importId);
            
        } catch (IOException e) {
            logger.error("Error handling file upload import", e);
            ctx.status(500).result("Error during upload " + e.getMessage());
        } catch (DatabaseOperationException e) {
            logger.error("Error saving/updating database during Uce Import", e);
            ctx.status(500).result("Error during saving/updating database " + e.getMessage());
        }
    }

}
