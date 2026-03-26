package org.texttechnologylab.uce.web.render.spec;

import com.google.gson.JsonElement;

/**
 * JSON-driven render specification for generic render modes.
 */
public final class RenderSpec {

    private PaneSpec middle;
    private PaneSpec right;

    public PaneSpec getMiddle() {
        return middle;
    }

    public PaneSpec getRight() {
        return right;
    }

    public boolean hasRight() {
        return right != null && right.getTemplate() != null && !right.getTemplate().isBlank();
    }

    public static final class PaneSpec {
        private String template;
        private JsonElement model;

        public String getTemplate() {
            return template;
        }

        public JsonElement getModel() {
            return model;
        }
    }
}

