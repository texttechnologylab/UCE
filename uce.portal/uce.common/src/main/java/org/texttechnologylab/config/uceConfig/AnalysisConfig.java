package org.texttechnologylab.config.uceConfig;

import lombok.Getter;
import lombok.Setter;

public class AnalysisConfig {

    private boolean enableAnalysisEngine;

    public AnalysisConfig() {
    }

    public boolean isEnableAnalysisEngine() {
        return this.enableAnalysisEngine;
    }


    public void setEnableAnalysisEngine(boolean enableAnalysisEngine) {
        this.enableAnalysisEngine = enableAnalysisEngine;
    }

}
