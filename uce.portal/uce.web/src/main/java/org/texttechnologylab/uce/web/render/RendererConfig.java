package org.texttechnologylab.uce.web.render;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.texttechnologylab.uce.web.render.feedback.FeedbackPaneRenderer;
import org.texttechnologylab.uce.web.render.spec.SpecPaneRenderer;

@Configuration
public class RendererConfig {

    @Bean
    public RendererRegistry rendererRegistry() {
        return new RendererRegistry()
                .register(DefaultPaneRenderer.HANDLER_KEY, new DefaultPaneRenderer())
                .register(FeedbackPaneRenderer.HANDLER_KEY, new FeedbackPaneRenderer())
                .register(SpecPaneRenderer.HANDLER_KEY, new SpecPaneRenderer());
    }
}
