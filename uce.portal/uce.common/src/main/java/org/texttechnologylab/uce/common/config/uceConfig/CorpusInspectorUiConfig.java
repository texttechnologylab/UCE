package org.texttechnologylab.uce.common.config.uceConfig;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CorpusInspectorUiConfig {
    private boolean showHeader = true;
    private boolean showMeta = true;
    private boolean showAnnotations = true;
    private boolean showDocuments = true;
    private boolean showSearchHint = true;
}

