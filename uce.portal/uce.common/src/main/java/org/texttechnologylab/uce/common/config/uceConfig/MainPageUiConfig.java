package org.texttechnologylab.uce.common.config.uceConfig;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MainPageUiConfig {
    private boolean showSystemStatus = true;
    private boolean showCorpusSelector = true;
    private boolean showNavButtons = true;
    private boolean showLanguageSelector = true;
    private boolean showAuthButton = true;
    private boolean showWikiModal = true;
    private boolean showRagbotChat = true;
}

