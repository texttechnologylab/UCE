package org.texttechnologylab.uce.web.render;

/**
 * Request-scoped identity information a renderer/spec can use, e.g. to derive
 * effective permissions. This keeps renderers decoupled from the HTTP layer.
 */
public record RenderPrincipal(String name) {}

