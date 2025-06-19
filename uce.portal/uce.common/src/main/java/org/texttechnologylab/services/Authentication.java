package org.texttechnologylab.services;

public interface Authentication {

    boolean hasPermission(String token, String resource, String scope);

}
