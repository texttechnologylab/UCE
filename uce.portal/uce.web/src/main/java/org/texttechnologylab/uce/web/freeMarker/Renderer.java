package org.texttechnologylab.uce.web.freeMarker;

import freemarker.template.Configuration;
import org.texttechnologylab.uce.common.models.Linkable;
import org.texttechnologylab.uce.common.models.UIMAAnnotation;
import org.texttechnologylab.uce.common.models.corpus.Document;
import org.texttechnologylab.uce.web.CustomFreeMarkerEngine;

import java.util.HashMap;

/**
 * Just a helper class for rendering some FreeMarker templates.
 */
public final class Renderer {

    public static Configuration freemarkerConfig;

    public Renderer(){}

    /**
     * Renders a linkable to an HTML component that is primarily used for the flow diagram of the Linkable network
     */
    public static String renderLinkable(Linkable linkable, Linkable from){
        String path = "defaultError.ftl";
        var uiModel = new HashMap<String, Object>();
        if(linkable instanceof Document doc){
            uiModel.put("document", doc);
            uiModel.put("searchId", "");
            uiModel.put("reduced",true);
            path = "*/search/components/documentCardContent.ftl";
            return "<div class='document-card'>" + Renderer.renderToHTML(path, uiModel) + "</div>";
        } else if (linkable instanceof UIMAAnnotation anno){
            path = "*/links/linkableAnnotation.ftl";
            uiModel.put("anno", anno);
            if(from instanceof UIMAAnnotation fromAnno) uiModel.put("sourceanno", fromAnno);
            return Renderer.renderToHTML(path, uiModel);
        }
        return Renderer.renderToHTML(path, uiModel);
    }

    public static String renderToHTML(String path, HashMap<String, Object> model){
        return new CustomFreeMarkerEngine(freemarkerConfig).render(path, model);
    }

}
