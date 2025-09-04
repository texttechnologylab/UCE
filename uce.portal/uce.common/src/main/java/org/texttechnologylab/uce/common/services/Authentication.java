package org.texttechnologylab.uce.common.services;

public interface Authentication {

    boolean hasPermission(String token, String resource, String scope);

}
