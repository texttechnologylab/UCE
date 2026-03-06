package org.texttechnologylab.uce.corpusimporter;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;

import org.apache.uima.util.CasIOUtils;
import org.apache.uima.util.CasLoadMode;
import org.apache.uima.util.XmlCasSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.texttechnologylab.DockerUnifiedUIMAInterface.DUUIComposer;
import org.texttechnologylab.DockerUnifiedUIMAInterface.driver.DUUIRemoteDriver;
import org.texttechnologylab.DockerUnifiedUIMAInterface.lua.DUUILuaContext;
import org.xml.sax.SAXException;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Unit test for simple App.
 */
public class DUUImporterDokumentTest {
    static String common= """
            uce.version=1.0.4-debug
            
            # University scraping and bib properties
            university.botanik.base.url=https://sammlungen.ub.uni-frankfurt.de/botanik/periodical/titleinfo/{ID}
            university.collection.base.url=https://sammlungen.ub.uni-frankfurt.de
            
            # Gbif scraping properties
            gbif.occurrences.search.url=https://api.gbif.org/v1/occurrence/search?limit=10&media_type=stillImage&taxon_key={TAXON_ID}
            
            # RAG Webserver properties
            ; rag.webserver.base.url=http://localhost:5678/
            rag.webserver.base.url=http://isengart.hucompute.org:5678/
            embedding.webserver.base.url=http://isengart.hucompute.org:5678/
            
            # JenaSparql properties
            ; sparql.host=http://localhost:5430/
            sparql.host=http://isengart.hucompute.org:8098/
            sparql.endpoint=biofid-search/sparql
            sparql.max.enrichment=100
            
            templates.location=uce.portal/resources/templates/
            # We want to use an external path for developing to enable hot reloading
            external.public.use=true
            external.public.location=../uce.web/src/main/resources/public
            database.scripts.location=/app/database/
            ; database.scripts.location=/storage/projects/bagci/BioFID/UCE_Releases/2026-02-06_BioFID/database
            log.db=false
            # Define the interval in seconds (3600s = 1 hour)
            session.job.interval = 3600
            system.job.interval = 10
            
            postgresql.connection.driver_class=org.postgresql.Driver
            postgresql.dialect=org.hibernate.dialect.PostgreSQLDialect
            postgresql.hibernate.connection.url=jdbc:postgresql://uce-postgresql-db:5432/uce
            ; postgresql.hibernate.connection.url=jdbc:postgresql://isengart.hucompute.org:8217/uce
            postgresql.hibernate.connection.username=postgres
            postgresql.hibernate.connection.password=1234
            postgresql.hibernate.current_session_context_class=thread
            postgresql.hibernate.show_sql=false
            postgresql.hibernate.format_sql=true
            # !!! If you put this on "create" it will wipe the database (other is "update") !!!
            postgresql.hibernate.hbm2ddl.auto=update
            postgresql.enrichment.location.max=200
            
            # s3 storage
            minio.endpoint = http://localhost:9000
            minio.username = admin
            minio.pwd = 12345678
            minio.bucket = cas
            
            # Keycloak authentication
            keycloak.realm=uce
            keycloak.auth_server_url=http://localhost:8080
            keycloak.client=uce-web
            keycloak.credentials.secret=**********
            """;
    static String commonRelease= """
            uce.version=1.0.4-debug
            
            # University scraping and bib properties
            university.botanik.base.url=https://sammlungen.ub.uni-frankfurt.de/botanik/periodical/titleinfo/{ID}
            university.collection.base.url=https://sammlungen.ub.uni-frankfurt.de
            
            # Gbif scraping properties
            gbif.occurrences.search.url=https://api.gbif.org/v1/occurrence/search?limit=10&media_type=stillImage&taxon_key={TAXON_ID}
            
            # RAG Webserver properties
            ; rag.webserver.base.url=http://localhost:5678/
            rag.webserver.base.url=http://isengart.hucompute.org:5678/
            embedding.webserver.base.url=http://isengart.hucompute.org:5678/
            
            # JenaSparql properties
            ; sparql.host=http://localhost:5430/
            sparql.host=http://isengart.hucompute.org:8098/
            sparql.endpoint=biofid-search/sparql
            sparql.max.enrichment=100
            
            templates.location=uce.portal/resources/templates/
            # We want to use an external path for developing to enable hot reloading
            external.public.use=true
            external.public.location=../uce.web/src/main/resources/public
            database.scripts.location=../database/
            ; database.scripts.location=/storage/projects/bagci/BioFID/UCE_Releases/2026-02-06_BioFID/database
            log.db=false
            # Define the interval in seconds (3600s = 1 hour)
            session.job.interval = 3600
            system.job.interval = 10
            
            postgresql.connection.driver_class=org.postgresql.Driver
            postgresql.dialect=org.hibernate.dialect.PostgreSQLDialect
            postgresql.hibernate.connection.url=jdbc:postgresql://141.2.108.201:5432/uce
            ; postgresql.hibernate.connection.url=jdbc:postgresql://isengart.hucompute.org:8217/uce
            postgresql.hibernate.connection.username=postgres
            postgresql.hibernate.connection.password=1234
            postgresql.hibernate.current_session_context_class=thread
            postgresql.hibernate.show_sql=false
            postgresql.hibernate.format_sql=true
            # !!! If you put this on "create" it will wipe the database (other is "update") !!!
            postgresql.hibernate.hbm2ddl.auto=update
            postgresql.enrichment.location.max=200
            
            # s3 storage
            minio.endpoint = http://141.2.108.201:9000
            minio.username = admin
            minio.pwd = 12345678
            minio.bucket = cas
            
            # Keycloak authentication
            keycloak.realm=uce
            keycloak.auth_server_url=http://141.2.108.201:8080
            keycloak.client=uce-web
            keycloak.credentials.secret=**********
            """;
    static String corpusConfig = """
            {
              "name": "biofid-mini-kubernetes",
              "author": "Fachinformationsdienst Biodiversitätsforschung (BIOfid)",
              "language": "de-DE",
              "description": "<b>Im Aufbau!</b><br/><br/>Im Rahmen des <b>Fachinformationsdienstes (FID) Biodiversitätsforschung</b> digitalisiert die <b>Universitätsbibliothek Johann Christian Senckenberg</b> (Frankfurt am Main) Literatur zur Biodiversität mit Schwerpunkt auf <i>Zeitschriften des 20. Jahrhunderts</i>.<br/><br/>Zu geringen Anteilen umfasst die <b>Sammlung Biodiversität</b> auch Teile der älteren, <i>urheberrechtsfreien Literatur</i>, die bislang noch nirgends digitalisiert wurde.<br/><br/>Ein wesentlicher Zweck der Digitalisierung im FID ist – neben einer besseren Verfügbarkeit der Inhalte – die <b>Schaffung eines Korpus</b> für ein <i>Pilotvorhaben zum Text-Mining</i> in Biodiversitäts-Literatur. Dabei steht Literatur zur Diversität von <b>Gefäßpflanzen, Schmetterlingen und Vögeln in Mitteleuropa</b> im Vordergrund.<br/><br/>Der FID Biodiversitätsforschung wird durch die <b>Universitätsbibliothek Johann Christian Senckenberg</b> gemeinsam mit der <b>Senckenberg Gesellschaft für Naturforschung</b> und der <b>AG Texttechnologie</b> am <b>Institut für Informatik der Goethe-Universität</b> in den Jahren <i>2017 bis 2020</i> aufgebaut.<br/><br/>Der FID einschließlich der Digitalisierung von Biodiversitäts-Literatur wird von der <b>Deutschen Forschungsgemeinschaft (DFG)</b> gefördert.",
              "annotations": {
                "annotatorMetadata": false,
                "uceMetadata": true,
                "logicalLinks": false,
                "OCRPage": true,
                "OCRParagraph": true,
                "OCRBlock": true,
                "OCRLine": true,
                "taxon": {
                  "annotated": true,
                  "biofidOnthologyAnnotated": true
                },
                "srLink": false,
                "lemma": false,
                "namedEntity": true,
                "geoNames": true,
                "sentence": true,
                "time": true,
                "sentiment": false,
                "emotion": false,
                "wikipediaLink": false,
                "completeNegation": false,
                "cue": false,
                "event": false,
                "focus": false,
                "scope": false,
                "xscope": false,
                "unifiedTopic": false
              },
              "addToExistingCorpus": true,
              "other": {
                "availableOnFrankfurtUniversityCollection": false,
                "includeKeywordDistribution": false,
                "enableEmbeddings": false,
                "enableRAGBot": false,
                "enableS3Storage": false
              }
            }
            """;
    static DUUIComposer composer;
    static JCas cas;

    static String url = "http://127.0.0.1:9714";

    @BeforeAll
    static void beforeAll() throws URISyntaxException, IOException, UIMAException, SAXException, CompressorException {
        composer = new DUUIComposer()
                .withSkipVerification(true)
                .withLuaContext(new DUUILuaContext().withJsonLibrary());

        DUUIRemoteDriver remoteDriver = new DUUIRemoteDriver();
        composer.addDriver(remoteDriver);
//        DUUIDockerDriver docker_driver = new DUUIDockerDriver();
//        composer.addDriver(docker_driver);


        cas = JCasFactory.createJCas();
    }

    @AfterAll
    static void afterAll() throws UnknownHostException {
        composer.shutdown();
    }

    @AfterEach
    public void afterEach() throws IOException, SAXException {
        composer.resetPipeline();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        XmlCasSerializer.serialize(cas.getCas(), null, stream);
        System.out.println(stream.toString(StandardCharsets.UTF_8));

        cas.reset();
    }

    @Test
    public void DokumentInputTest() throws Exception {
        // Load document cas
//        String file = "/home/bagci/data/biofid-mini/input/3664127.xmi.bz2";
        String file = "/home/staff_homes/bagci/data/biofid-mini/input/3664127.xmi.bz2";
        var inputStream =  new BZip2CompressorInputStream(new FileInputStream(file));
        CasIOUtils.load(inputStream, null, cas.getCas(), CasLoadMode.LENIENT);
        composer.add(
                new DUUIRemoteDriver.Component(url)
                        .withParameter("common", common)
                        .withParameter("dokumentConf", corpusConfig)
//                        .withParameter("selection", "text")
        );

        composer.run(cas);
        String model_name = "";

    }




}
