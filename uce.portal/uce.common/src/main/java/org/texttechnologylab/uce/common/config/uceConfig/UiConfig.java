package org.texttechnologylab.uce.common.config.uceConfig;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UiConfig {
    private DocumentReaderUiConfig documentReader = new DocumentReaderUiConfig();
    private MainPageUiConfig mainPage = new MainPageUiConfig();
    private CorpusInspectorUiConfig corpusInspector = new CorpusInspectorUiConfig();
}
