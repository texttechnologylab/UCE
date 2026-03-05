package org.texttechnologylab.uce.web.render.spec;

/**
 * Minimal permission view model tailored to {@code permissionBadge.ftl}.
 */
public record PermissionBadgeModel(PermissionLevel level) {

    public enum PermissionLevel { NONE, READ, WRITE, OWNER, ADMIN }
}

