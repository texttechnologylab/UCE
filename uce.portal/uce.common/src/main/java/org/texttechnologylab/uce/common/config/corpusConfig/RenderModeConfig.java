package org.texttechnologylab.uce.common.config.corpusConfig;

import lombok.Getter;
import lombok.Setter;

/**
 * Declarative description of an additional render mode for a corpus.
 * <p>
 * The configuration is intentionally minimal so that the web layer can
 * decide how to resolve the handler identifier into an actual renderer
 * implementation.
 */
@Getter
@Setter
public class RenderModeConfig {
    /**
     * Human readable label shown in the UI navigation.
     */
    private String name;

    /**
     * Machine readable key used in URLs (e.g. {@code mode=feedback}).
     */
    private String key;

    /**
     * Identifier that maps to a {@code PaneRenderer} implementation.
     */
    private String handler;

    /**
     * Optional description so UIs can surface more context about the mode.
     */
    private String description;

    /**
     * Optional pointer to a JSON render specification used by generic renderers.
     * <p>
     * Supported prefixes:
     * <ul>
     *     <li>{@code FILE::/absolute/or/relative/path.json}</li>
     *     <li>{@code CLASSPATH::render-specs/mySpec.json}</li>
     * </ul>
     */
    private String specPath;
}
