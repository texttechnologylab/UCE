package org.texttechnologylab.uce.common.config.uceConfig;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CorporateConfig {
    private ContactConfig contact;
    private String website;
    private String logo;
    private String name;
    private String primaryColor;
    private String secondaryColor;
    private TeamConfig team;
    private String imprint;

    public CorporateConfig(){}
}
