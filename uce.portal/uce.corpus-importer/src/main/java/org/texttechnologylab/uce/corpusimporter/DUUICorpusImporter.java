package org.texttechnologylab.uce.corpusimporter;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.cli.DefaultParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.cas.impl.XmiSerializationSharedData;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.testing.util.DisableLogging;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.texttechnologylab.annotation.AnnotationComment;
import org.texttechnologylab.annotation.GNMetaData;
import org.texttechnologylab.annotation.biofid.gnfinder.Taxon;
import org.texttechnologylab.annotation.biofid.gnfinder.VerifiedTaxon;
import org.texttechnologylab.uce.common.config.CommonConfig;
import org.texttechnologylab.uce.common.config.SpringConfig;
import org.texttechnologylab.uce.common.exceptions.ExceptionUtils;
import org.texttechnologylab.uce.common.security.DocumentAccessManager;
import org.texttechnologylab.uce.common.services.PostgresqlDataInterface_Impl;
import org.texttechnologylab.uce.common.utils.SystemStatus;
import org.xml.sax.SAXException;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.logging.Level;

public class DUUICorpusImporter {
    private static final Logger logger = LogManager.getLogger(DUUICorpusImporter.class);

    public static void main(String[] args) throws Exception {
        DisableLogging.enableLogging(Level.SEVERE);
        HttpServer server = HttpServer.create(new InetSocketAddress(9714), 0);
        server.createContext("/v1/communication_layer", new CommunicationLayer());
        server.createContext("/v1/typesystem", new TypesystemHandler());
        server.createContext("/v1/process", new ProcessHandler());
        server.createContext("/v1/details/input_output", new IOHandler());

        server.setExecutor(null); // creates a default executor
        server.start();
    }

    static class ProcessHandler implements HttpHandler {
        static JCas jc;

        static {
            try {
                jc = JCasFactory.createJCas();
            } catch (UIMAException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            File tf = null;
            try {
                jc.reset();

                XmiSerializationSharedData sharedData = new XmiSerializationSharedData();

                String body = new String(t.getRequestBody().readAllBytes());
                String[] bodies = body.split("\"}", 2);
                String args = bodies[0].split("args\":\"")[1];

                InputStream casBody = new ByteArrayInputStream(bodies[1].getBytes(StandardCharsets.UTF_8));
                XmiCasDeserializer.deserialize(casBody, jc.getCas(), true, sharedData);

                //dump the common into commonEmpty.conf in the resources folder, in that case the given configuration will be loaded by the Corpus Importer instead of the default common.conf
                if(args.contains("-c")) {
                    String commonConf = args.split("-c ")[1].split(" -")[0];
                    try (FileWriter writer = new FileWriter("./uce.common/src/main/resources/commonEmpty.conf")) {
                        writer.write(commonConf);
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                // raise exception if the given conf is not found, otherwise the default common.conf will be loaded, which is not wanted in that case.
                else
                {
                    throw new RuntimeException("No common conf given in the arguments, or the given common conf is not found. Canceling. Please provide a common conf with the -c argument, and make sure the path is correct.");
                }

                //Dokument conf is needed to load the correct configuration into the Database, instead of the given CorpusConf in the folder of the input files.
                if(args.contains("-d")) {
                    String corpusConf = args.split("-d ")[1].split(" -")[0];
//                    try (FileWriter writer = new FileWriter("./uce.corpus-importer/src/main/resources/corpusConfig.json")) {
//                        writer.write(corpusConf);
//                    }
//                    catch (IOException e) {
//                        e.printStackTrace();
//                    }
                }
                else
                {
                    throw new RuntimeException("No corpus conf given in the arguments, or the given corpus conf is not found. Canceling. Please provide a corpus conf with the -d argument, and make sure the path is correct.");
                }

                var context = new AnnotationConfigApplicationContext(SpringConfig.class);

                var accessManager = context.getBean(DocumentAccessManager.class);

                try (var guard = accessManager.asAdmin()) {
                    var commonConfig = new CommonConfig();
                    ExceptionUtils.tryCatchLog(
                            () -> SystemStatus.executeExternalDatabaseScripts(commonConfig.getDatabaseScriptsLocation(), context.getBean(PostgresqlDataInterface_Impl.class)),
                            (ex) -> logger.warn("Couldn't read the db scripts in the external database scripts folder; path wasn't found or other IO problems. ", ex));
                    var importId = UUID.randomUUID().toString();
                    var importer = new Importer(context, null, 1, importId, null);

                }


            }
            catch (Exception e) {
                e.printStackTrace();
                t.sendResponseHeaders(404, -1);
                return;
            }
        }

    }

    static class TypesystemHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            try {
                TypeSystemDescription desc = TypeSystemDescriptionFactory.createTypeSystemDescription();
                StringWriter writer = new StringWriter();
                desc.toXML(writer);
                String response = writer.getBuffer().toString();

                t.sendResponseHeaders(200, response.getBytes(Charset.defaultCharset()).length);

                OutputStream os = t.getResponseBody();
                os.write(response.getBytes(Charset.defaultCharset()));

            } catch (ResourceInitializationException e) {
                e.printStackTrace();
                t.sendResponseHeaders(404, -1);
                return;
            } catch (SAXException e) {
                e.printStackTrace();
            } finally {
                t.getResponseBody().close();
            }

        }
    }

    static class IOHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            try {
                JSONObject rObject = new JSONObject();
                rObject.put("input", new JSONArray());
                rObject.put("output", new JSONArray().put("org.texttechnologylab.annotation.type.Taxon").put("org.texttechnologylab.annotation.AnnotationComment"));
                String response = rObject.toString();
                t.sendResponseHeaders(200, response.getBytes(Charset.defaultCharset()).length);

                OutputStream os = t.getResponseBody();
                os.write(response.getBytes(Charset.defaultCharset()));

            } catch (JSONException e) {
                e.printStackTrace();
                t.sendResponseHeaders(404, -1);
                return;
            } finally {
                t.getResponseBody().close();
            }

        }
    }

    static class CommunicationLayer implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            BufferedReader reader = new BufferedReader(new FileReader("./uce.corpus-importer/communication.lua"));
            StringBuilder stringBuilder = new StringBuilder();
            String line = null;
            String ls = System.getProperty("line.separator");
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append(ls);
            }
            // delete the last new line separator
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
            reader.close();

            String response = stringBuilder.toString();

            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}
