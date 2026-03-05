package org.texttechnologylab.uce.web.render;

import java.util.Map;

/**
 * Thin wrapper around the legacy template so it can participate in the new
 * render mode pipeline without changing existing behaviour.
 */
public final class DefaultPaneRenderer implements PaneRenderer {

    public static final String HANDLER_KEY = "document_reader_pdf_view";
    private static final String TEMPLATE = "reader/modes/defaultMiddlePane.ftl";

    @Override
    public RenderResult render(RenderContext context) {
        // The traditional UI expects a pre-populated model. Until the HTTP
        // routes adopt the new approach we simply emit the template name so
        // callers can continue wiring the legacy data structures.
        return RenderResult.middleOnly(TEMPLATE, Map.of(
                "document", context.document(),
                "corpus", context.corpus()
        ));
    }
}
