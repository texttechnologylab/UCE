package org.texttechnologylab.freeMarker;

import freemarker.template.Configuration;
import org.texttechnologylab.CustomFreeMarkerEngine;
import org.texttechnologylab.models.Linkable;
import org.texttechnologylab.models.corpus.Document;
import org.texttechnologylab.models.viewModels.link.LinkableViewModel;
import spark.ModelAndView;

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
    public static String renderLinkable(Linkable linkable){
        String path = "defaultError.ftl";
        var uiModel = new HashMap<String, Object>();
        if(linkable instanceof Document doc){
            uiModel.put("document", doc);
            uiModel.put("searchId", "");
            uiModel.put("reduced",true);
            path = "*/search/components/documentCardContent.ftl";
        }
        return "<div class='document-card'>" + Renderer.renderToHTML(path, uiModel) + "</div>";
    }

    public static String renderToHTML(String path, HashMap<String, Object> model){
        return new CustomFreeMarkerEngine(freemarkerConfig).render(new ModelAndView(model, path));
    }

}
