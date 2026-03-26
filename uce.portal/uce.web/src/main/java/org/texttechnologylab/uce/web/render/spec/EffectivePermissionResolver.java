package org.texttechnologylab.uce.web.render.spec;

import org.texttechnologylab.models.authentication.DocumentPermission;
import org.texttechnologylab.uce.common.models.corpus.Document;

import java.util.Optional;

final class EffectivePermissionResolver {

    private EffectivePermissionResolver() {}

    static PermissionBadgeModel resolve(Document document, String principal) {
        var perms = Optional.ofNullable(document.getPermissions()).orElse(java.util.Set.of());

        if (perms.isEmpty()) {
            return new PermissionBadgeModel(PermissionBadgeModel.PermissionLevel.READ);
        }

        if (principal == null || principal.isBlank()) {
            return null;
        }

        return perms.stream()
                .filter(permission -> permission.getType() == DocumentPermission.DOCUMENT_PERMISSION_TYPE.EFFECTIVE)
                .filter(permission -> principal.equals(permission.getName()))
                .findFirst()
                .map(permission -> new PermissionBadgeModel(mapPermissionLevel(permission.getLevel())))
                .orElse(null);
    }

    private static PermissionBadgeModel.PermissionLevel mapPermissionLevel(DocumentPermission.DOCUMENT_PERMISSION_LEVEL level) {
        return switch (level) {
            case NONE -> PermissionBadgeModel.PermissionLevel.NONE;
            case READ -> PermissionBadgeModel.PermissionLevel.READ;
            case WRITE -> PermissionBadgeModel.PermissionLevel.WRITE;
            case OWNER -> PermissionBadgeModel.PermissionLevel.OWNER;
            case ADMIN -> PermissionBadgeModel.PermissionLevel.ADMIN;
        };
    }
}

