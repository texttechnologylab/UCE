package org.texttechnologylab.config;

import com.google.gson.Gson;
import org.texttechnologylab.config.uceConfig.CorporateConfig;
import org.texttechnologylab.config.uceConfig.MetaConfig;
import org.texttechnologylab.config.uceConfig.SettingsConfig;

public class UceConfig {
    private MetaConfig meta;
    private CorporateConfig corporate;

    private SettingsConfig settings;

    public static UceConfig fromJson(String uceConfigJson){
        var gson = new Gson();
        var config = gson.fromJson(uceConfigJson, UceConfig.class);
        return config;
    }

    public UceConfig(){}

    public SettingsConfig getSettings() {
        return settings;
    }

    public void setSettings(SettingsConfig settings) {
        this.settings = settings;
    }

    public MetaConfig getMeta() {
        return meta;
    }

    public void setMeta(MetaConfig meta) {
        this.meta = meta;
    }

    public CorporateConfig getCorporate() {
        return corporate;
    }

    public void setCorporate(CorporateConfig corporate) {
        this.corporate = corporate;
    }
}
