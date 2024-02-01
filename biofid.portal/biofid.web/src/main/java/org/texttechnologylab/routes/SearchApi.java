package org.texttechnologylab.routes;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.texttechnologylab.models.corpus.Document;
import org.texttechnologylab.services.UIMAService;
import spark.Route;

import java.util.List;

public class SearchApi {

    private final UIMAService uimaService;
    private List<Document> testDocs;

    public SearchApi(ApplicationContext serviceContext){
        this.uimaService = serviceContext.getBean(UIMAService.class);
        // TODO: This is only testing for now.
        this.testDocs = uimaService.XMIFolderToDocuments("C:\\kevin\\projects\\biofid\\test_data\\test_data");
    }

    public Route search = ((request, response) -> {
        return String.join(", ", this.testDocs.stream().map(d -> d.getDocumentTitle()).toList());
    });
}
