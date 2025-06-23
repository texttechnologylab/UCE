package org.texttechnologylab.config;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import org.texttechnologylab.config.uceConfig.CorporateConfig;
import org.texttechnologylab.config.uceConfig.MetaConfig;
import org.texttechnologylab.config.uceConfig.SettingsConfig;

@Getter
@Setter
public class UceConfig {
    private MetaConfig meta;
    private CorporateConfig corporate;
    private SettingsConfig settings;

    public boolean authIsEnabled(){
        return settings.getAuthentication().isActivated();
    }

    public static UceConfig fromJson(String uceConfigJson){
        var gson = new Gson();
        var config = gson.fromJson(uceConfigJson, UceConfig.class);
        return config;
    }

    public UceConfig(){}
}
