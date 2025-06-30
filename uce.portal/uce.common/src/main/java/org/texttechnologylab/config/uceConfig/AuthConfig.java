package org.texttechnologylab.config.uceConfig;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthConfig {
    private boolean isActivated;
    private String publicUrl;
    private String redirectUrl;
}
