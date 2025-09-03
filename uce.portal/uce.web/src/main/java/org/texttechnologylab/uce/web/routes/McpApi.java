package org.texttechnologylab.uce.web.routes;

import freemarker.template.Configuration;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.uce.common.models.corpus.Document;
import org.texttechnologylab.uce.common.services.PostgresqlDataInterface_Impl;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class McpApi implements UceApi {
    private static final Logger logger = LogManager.getLogger(McpApi.class);

    private PostgresqlDataInterface_Impl db;
    private Configuration freemarkerConfig;

    public McpApi(ApplicationContext serviceContext, Configuration freemarkerConfig) {
        this.db = serviceContext.getBean(PostgresqlDataInterface_Impl.class);
        this.freemarkerConfig = freemarkerConfig;
    }

    public void registerTools(McpSyncServer mcpServer) {
        mcpServer.addTool(new McpServerFeatures.SyncToolSpecification(
                new McpSchema.Tool(
                        "get_document_by_id",
                        "Get a document by its ID.",
                        """
                          {
                            "type" : "object",
                            "id" : "urn:jsonschema:Operation",
                            "properties" : {
                              "id" : {
                                  "type" : "string"
                              }
                            }
                          }
                          """
                ),
                this.documentGetById
        ));

        mcpServer.addTool(new McpServerFeatures.SyncToolSpecification(
                new McpSchema.Tool(
                        "list_document_ids_by_title",
                        "List all document IDs matching a title. Title can use the SQL LIKE operator if enabled.",
                        """
                          {
                            "type" : "object",
                            "id" : "urn:jsonschema:Operation",
                            "properties" : {
                              "title" : {
                                  "type" : "string"
                              },
                              "like" : {
                                  "type" : "boolean"
                              }
                            }
                          }
                          """
                ),
                this.documentListIdsByTitle
        ));
    }

    public BiFunction<McpSyncServerExchange, Map<String, Object>, McpSchema.CallToolResult> documentListIdsByTitle = ((exchange, arguments) -> {
        var result = new StringBuilder();
        try {
            var title = String.valueOf(arguments.get("title"));
            var like = Boolean.parseBoolean(String.valueOf(arguments.get("like")));
            List<Long> docIds = db.findDocumentIDsByTitle(title, like);
            result.append("Document IDs for title search: \"").append(title).append("\"\n");
            for (Long docId : docIds) {
                result.append("- ").append(docId).append("\n");
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return new McpSchema.CallToolResult(result.toString(), false);
    });

    public BiFunction<McpSyncServerExchange, Map<String, Object>, McpSchema.CallToolResult> documentGetById = ((exchange, arguments) -> {
        var result = new StringBuilder();
        try {
            var doc_id = Long.parseLong(String.valueOf(arguments.get("id")));
            Document doc = db.getDocumentById(doc_id);
            result.append("Document ID: ").append(doc.getId()).append("\n");
            result.append("Title: ").append(doc.getDocumentTitle()).append("\n");
            result.append("Content: ").append("\n");
            result.append(doc.getFullText());
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return new McpSchema.CallToolResult(result.toString(), false);
    });
}
