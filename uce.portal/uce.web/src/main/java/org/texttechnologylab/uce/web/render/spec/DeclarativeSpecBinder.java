package org.texttechnologylab.uce.web.render.spec;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.texttechnologylab.uce.common.models.corpus.Document;
import org.texttechnologylab.uce.common.models.corpus.Image;
import org.texttechnologylab.uce.common.models.corpus.UCEMetadata;
import org.texttechnologylab.uce.common.models.corpus.UCEMetadataValueType;
import org.texttechnologylab.uce.web.render.RenderException;
import org.texttechnologylab.uce.web.render.RenderPrincipal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Builds a pane model from a JSON tree that only supports declarative references.
 * <p>
 * A reference is expressed as an object with a single field {@code "$ref"} whose
 * value is a dot-separated path into the runtime context:
 * <ul>
 *     <li>{@code document.*} (JavaBean properties on {@link Document})</li>
 *     <li>{@code metadata.<key>} (values from {@link UCEMetadata})</li>
 *     <li>{@code images} (list of image maps)</li>
 *     <li>{@code effectivePermission} (permission badge model)</li>
 * </ul>
 */
final class DeclarativeSpecBinder {

    private static final String REF_KEY = "$ref";

    private final Gson gson = new Gson();
    private final Document document;
    private final String principal;

    private final Map<String, Object> metadata;
    private final List<Map<String, Object>> images;
    private final PermissionBadgeModel effectivePermission;

    DeclarativeSpecBinder(Document document, RenderPrincipal principal) {
        this.document = document;
        this.principal = principal != null ? principal.name() : null;
        this.metadata = buildMetadataMap(document);
        this.images = buildImages(document);
        this.effectivePermission = EffectivePermissionResolver.resolve(document, this.principal);
    }

    Map<String, Object> bindModel(JsonElement element) throws RenderException {
        var value = bind(element);
        if (value == null) {
            return Map.of();
        }
        if (value instanceof Map<?, ?> map) {
            var out = new HashMap<String, Object>();
            for (var entry : map.entrySet()) {
                out.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return out;
        }
        throw new RenderException("Spec pane model must evaluate to an object/map.");
    }

    private Object bind(JsonElement element) throws RenderException {
        if (element == null || element instanceof JsonNull) {
            return null;
        }
        if (element.isJsonPrimitive()) {
            return bindPrimitive(element.getAsJsonPrimitive());
        }
        if (element.isJsonArray()) {
            return bindArray(element.getAsJsonArray());
        }
        if (element.isJsonObject()) {
            return bindObject(element.getAsJsonObject());
        }
        return null;
    }

    private Object bindPrimitive(JsonPrimitive primitive) {
        if (primitive.isBoolean()) {
            return primitive.getAsBoolean();
        }
        if (primitive.isNumber()) {
            return primitive.getAsNumber();
        }
        return primitive.getAsString();
    }

    private List<Object> bindArray(JsonArray array) throws RenderException {
        var out = new ArrayList<Object>(array.size());
        for (var item : array) {
            out.add(bind(item));
        }
        return out;
    }

    private Map<String, Object> bindObject(JsonObject obj) throws RenderException {
        if (obj.size() == 1 && obj.has(REF_KEY)) {
            return Map.of("value", resolveRef(obj.get(REF_KEY).getAsString()));
        }
        var out = new HashMap<String, Object>();
        for (var entry : obj.entrySet()) {
            out.put(entry.getKey(), unwrapValue(bind(entry.getValue())));
        }
        return out;
    }

    private static Object unwrapValue(Object maybeWrapped) {
        if (maybeWrapped instanceof Map<?, ?> map && map.size() == 1 && map.containsKey("value")) {
            return map.get("value");
        }
        return maybeWrapped;
    }

    private Object resolveRef(String ref) throws RenderException {
        if (ref == null || ref.isBlank()) {
            return null;
        }
        if ("document".equals(ref)) {
            return document;
        }
        if ("metadata".equals(ref)) {
            return metadata;
        }
        if ("images".equals(ref)) {
            return images;
        }
        if ("effectivePermission".equals(ref)) {
            return effectivePermission;
        }

        if (ref.startsWith("metadata.")) {
            var key = ref.substring("metadata.".length());
            return metadata.get(key);
        }

        if (ref.startsWith("document.")) {
            var prop = ref.substring("document.".length());
            return readDocumentProperty(prop);
        }

        throw new RenderException("Unsupported $ref path: " + ref);
    }

    private Object readDocumentProperty(String prop) throws RenderException {
        return switch (prop) {
            case "id" -> document.getId();
            case "documentId" -> document.getDocumentId();
            case "documentTitle" -> safeDocumentTitle();
            case "language" -> document.getLanguage();
            case "mimeType" -> document.getMimeType();
            case "fullText" -> document.getFullText();
            case "fullTextCleaned" -> document.getFullTextCleaned();
            case "corpusId" -> document.getCorpusId();
            default -> throw new RenderException("Unsupported document property: " + prop);
        };
    }

    private String safeDocumentTitle() {
        try {
            return document.getDocumentTitle();
        } catch (RuntimeException ex) {
            return "";
        }
    }

    private Map<String, Object> buildMetadataMap(Document document) {
        var out = new HashMap<String, Object>();
        for (var meta : Optional.ofNullable(document.getUceMetadata()).orElse(List.of())) {
            if (meta == null || meta.getKey() == null) {
                continue;
            }
            out.put(meta.getKey(), coerceMetadataValue(meta));
        }
        return out;
    }

    private Object coerceMetadataValue(UCEMetadata meta) {
        var value = meta.getValue();
        if (value == null) {
            return null;
        }

        UCEMetadataValueType type = meta.getValueType();
        if (type == null) {
            return value;
        }

        return switch (type) {
            case NUMBER -> {
                try {
                    yield Double.parseDouble(value.replace(",", "."));
                } catch (NumberFormatException ignored) {
                    yield 0d;
                }
            }
            case JSON -> {
                try {
                    yield gson.fromJson(value, Object.class);
                } catch (Exception ignored) {
                    yield value;
                }
            }
            default -> value;
        };
    }

    private static List<Map<String, Object>> buildImages(Document document) {
        var out = new ArrayList<Map<String, Object>>();
        for (Image image : Optional.ofNullable(document.getImages()).orElse(List.of())) {
            if (image == null) {
                continue;
            }
            out.add(Map.of(
                    "mimeType", nullToEmpty(image.getMimeType()),
                    "width", image.getWidth(),
                    "height", image.getHeight(),
                    "src", nullToEmpty(image.getSrc()),
                    "htmlImgSrc", image.getHTMLImgSrc()
            ));
        }
        return out;
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
