package org.texttechnologylab.uce.web.render.spec;

import org.texttechnologylab.uce.common.config.corpusConfig.RenderModeConfig;
import org.texttechnologylab.uce.web.render.PaneRenderer;
import org.texttechnologylab.uce.web.render.RenderContext;
import org.texttechnologylab.uce.web.render.RenderException;
import org.texttechnologylab.uce.web.render.RenderPrincipal;
import org.texttechnologylab.uce.web.render.RenderResult;

/**
 * Generic renderer that loads a JSON spec file and evaluates it into a FreeMarker model.
 */
public final class SpecPaneRenderer implements PaneRenderer {

    public static final String HANDLER_KEY = "document_reader_template_view";

    private final SpecLoader loader = new SpecLoader();

    @Override
    public RenderResult render(RenderContext context) throws RenderException {
        var mode = context.requirePayload(RenderModeConfig.class);
        var principal = context.payload(RenderPrincipal.class).orElse(null);

        var spec = loader.load(mode.getSpecPath());

        var binder = new DeclarativeSpecBinder(context.document(), principal);
        var middleModel = binder.bindModel(spec.getMiddle().getModel());

        var builder = RenderResult.builder()
                .middlePane(spec.getMiddle().getTemplate(), middleModel);

        if (spec.hasRight()) {
            var rightModel = binder.bindModel(spec.getRight().getModel());
            builder.rightPane(spec.getRight().getTemplate(), rightModel);
        }

        return builder.build();
    }
}
