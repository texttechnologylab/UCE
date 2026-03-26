<#import "*/reader/components/middlePaneHeader.ftl" as middleHeader>

<div class="reader-view-layout default-reader-view">
    <div class="reader-middle-pane">
        <div class="position-relative reader-container reader-news-column"
             data-id="${document.getId()?string?replace('.', '')?replace(',', '')}"
             data-pagescount="${document.getPages()?size?string?replace('.', '')?replace(',', '')}"
             data-searchtokens="${(searchTokens)!''}">

            <#if (uceConfig.settings.ui.documentReader.showTopicNavigationButtons)!true>
                <div class="topic-navigation-buttons">
                    <button class="topic-nav-button prev-topic-button" title="Previous occurrence">
                        <i class="fas fa-chevron-up"></i>
                    </button>
                    <button class="topic-nav-button next-topic-button" title="Next occurrence">
                        <i class="fas fa-chevron-down"></i>
                    </button>
                </div>
            </#if>

            <#if (uceConfig.settings.ui.documentReader.showHeader)!true>
                <@middleHeader.render
                    title=document.getDocumentTitle()
                    published=document.getMetadataTitleInfo().getPublished()!""
                    language=document.getLanguage()!""
                    wikiId=document.getWikiId()!""
                    wikiCovered=document.getDocumentTitle()!""
                    externalUrl=document.getMetadataTitleInfo().getScrapedUrl()!""
                />
            </#if>

            <#if (uceConfig.settings.ui.documentReader.showUceMetadata)!true>
                <#assign uceMetadata = document.getUceMetadataWithoutJson()>
                <#if uceMetadata?has_content && uceMetadata?size gt 0>
                    <div class="w-100 pt-4 pl-4 pr-4 pb-1 mt-2">
                        <div class="small-font">
                            <#include "*/document/documentUceMetadata.ftl">
                        </div>
                        <#if document.hasJsonUceMetadata()>
                            <div class="flexed align-items-center justify-content-center text-center mt-1">
                                <#if (uceConfig.settings.ui.mainPage.showWikiModal)!true>
                                    <a class="btn bg-lightgray rounded light-border xsmall-font open-wiki-page align-items-center flexed hoverable"
                                       data-wid="${document.getWikiId()}">
                                        <i class="fab fa-wikipedia-w bg-light light-border rounded p-1 mr-2"></i>
                                        <span class="font-italic text-secondary" style="margin-top: 3px">
                                            ${languageResource.get("showUceMetadata")}...
                                        </span>
                                    </a>
                                </#if>
                            </div>
                        </#if>
                    </div>
                    <hr class="mb-0"/>
                </#if>
            </#if>

            <div class="document-content"></div>

            <div class="scrollbar-minimap">
                <div class="minimap-markers"></div>
                <div class="minimap-preview">
                    <div class="preview-content"></div>
                </div>
            </div>
        </div>
    </div>

    <#if (uceConfig.settings.ui.documentReader.showSidebar)!true>
        <button class="sidebar-drawer-toggle" type="button" aria-label="Toggle sidebar">
            <i class="fas fa-sliders-h"></i>
        </button>
        <div class="sidebar-drawer-backdrop"></div>

        <aside class="side-bar reader-side-panel">
            <div class="tab-header">
                <button class="tab-btn active" data-tab="navigator-tab">${languageResource.get("controlPanelTab")}</button>
                <#if (uceConfig.settings.ui.documentReader.showVisualizationTab)!true>
                    <button class="tab-btn" data-tab="visualization-tab">${languageResource.get("visualizationTab")}</button>
                </#if>
            </div>

            <div class="tab-content">
                <div class="tab-pane active" id="navigator-tab">
                    <div class="expander" data-expanded="true"><i class="m-0 xlarge-font fas fa-chevron-right"></i></div>
                    <div class="side-bar-content">
                        <div class="group-box">
                            <p class="text-center mb-0"><i class="fas fa-id-card-alt mr-1"></i> ${document.getDocumentId()}</p>
                        </div>

                        <#if document.getMetadataTitleInfo().getScrapedUrl()?has_content>
                            <div class="group-box">
                                <p class="title">${languageResource.get("ogDocument")}</p>
                                <a href="${document.getMetadataTitleInfo().getScrapedUrl()}" target="_blank" class="title-image mb-3">
                                    <img src="${document.getMetadataTitleInfo().getTitleImageUrl()}"/>
                                </a>
                            </div>
                        </#if>

                        <div class="group-box">
                            <p class="title">${languageResource.get("settings")}</p>
                            <div class="flexed align-items-center">
                                <i class="fas fa-text-height mr-2"></i>
                                <input min="10" max="30" class="font-size-range w-100 hoverable" value="20" type="range"/>
                            </div>
                        </div>

                        <#if document.getMetadataTitleInfo().getScrapedUrl()?has_content>
                            <div class="group-box">
                                <p class="title">${languageResource.get("page")} <span class="current-page">1</span></p>
                                <a class="btn open-metadata-url-page-btn" target="_blank"
                                   data-href="${document.getMetadataTitleInfo().getPageViewStartUrl()}"
                                   href="${document.getMetadataTitleInfo().getPageViewStartUrl()}">
                                    <i class="mr-2 fas fa-university"></i> Original
                                </a>
                            </div>
                        </#if>

                        <div class="buttons group-box">
                            <p class="title">${languageResource.get("functions")}</p>
                            <button class="btn toggle-focus-btn">
                                <i class="fas fa-satellite-dish mr-2"></i> Toggle Focus
                            </button>
                            <button class="btn toggle-highlighting-btn" data-highlighted="true">
                                <i class="fas fa-highlighter mr-2"></i> Toggle Highlighting
                            </button>
                            <#if casDownloadName?has_content && casDownloadName != "">
                                <a href="/api/ie/download/uima?objectName=${casDownloadName}" class="btn">
                                    <i class="fas fa-file-download mr-2"></i> Download XMI
                                </a>
                            </#if>
                        </div>

                        <#if (searchTokens?has_content) && (searchTokens?length gt 0)>
                            <div class="group-box search-tokens-box">
                                <p class="title"><span>${languageResource.get("searchTokens")}</span> <i class="ml-2 rotate fas fa-spinner"></i></p>
                                <div class="found-searchtokens-list"></div>
                            </div>
                        </#if>

                        <div class="group-box topics-box">
                            <div class="key-topics-title d-flex align-items-center justify-content-between mb-3">
                                <span class="title mx-auto" style="flex:1; text-align:center;">${languageResource.get("topics")}</span>
                                <i class="ml-2 fas fa-cog key-topics-settings" title="Settings"></i>
                            </div>
                            <div class="document-topics-list" data-document-id="${document.id}"></div>
                        </div>
                    </div>
                </div>

                <#if (uceConfig.settings.ui.documentReader.showVisualizationTab)!true>
                    <div class="tab-pane" id="visualization-tab">
                        <div class="visualization-wrapper">
                            <div class="visualization-spinner">
                                <div class="visualization-spinner__icon">
                                    <i class="fa fa-spinner fa-spin"></i>
                                </div>
                                <div class="visualization-spinner__text">Loading visualization&hellip;</div>
                            </div>
                            <div class="visualization-content" id="viz-content" data-message="${languageResource.get('noDataAvailable')}">
                                <div class="viz-panel" id="viz-panel-1"><div id="vp-1"></div></div>
                                <div class="viz-panel" id="viz-panel-2"><div id="vp-2"></div></div>
                                <div class="viz-panel" id="viz-panel-3"><div id="vp-3"></div></div>
                                <div class="viz-panel" id="viz-panel-4">
                                    <div id="vp-4-wrapper">
                                        <div class="selector-container">
                                            <label for="similarityTypeSelector">Similarity Type:</label>
                                            <select id="similarityTypeSelector">
                                                <option value="cosine" title="${languageResource.get('cosine')}">Cosine</option>
                                                <option value="count" title="${languageResource.get('overlap')}">Shared Count</option>
                                            </select>
                                        </div>
                                        <div id="vp-4"></div>
                                    </div>
                                </div>
                                <div class="viz-panel" id="viz-panel-5"><div id="vp-5"></div></div>
                            </div>

                            <div class="viz-bottom-nav">
                                <button class="viz-nav-btn active" data-target="#viz-panel-1">${languageResource.get("semanticDensity")}</button>
                                <button class="viz-nav-btn" data-target="#viz-panel-2">${languageResource.get("topicEntity")}</button>
                                <button class="viz-nav-btn" data-target="#viz-panel-3">${languageResource.get("topicLandscape")}</button>
                                <button class="viz-nav-btn" data-target="#viz-panel-4">${languageResource.get("topicSimilarity")}</button>
                                <button class="viz-nav-btn" data-target="#viz-panel-5">${languageResource.get("sentenceTopicFlow")}</button>
                            </div>
                        </div>
                    </div>
                </#if>
            </div>
        </aside>

        <#if (uceConfig.settings.ui.documentReader.showTopicSettingsPanel)!true>
            <div class="key-topic-settings-panel" data-id="${document.getCorpusId()}">
                <div class="d-flex align-items-center justify-content-between mb-3">
                    <h4 class="mb-0">${languageResource.get("topicSettings")}</h4>
                    <div>
                        <button class="save-topic-setting btn btn-light btn-sm mr-2" title="Save" data-toggle="tooltip" data-placement="top" data-original-title="${languageResource.get("saveTopicSettings")}"><i class="fas fa-save"></i></button>
                        <button class="upload-topic-setting btn btn-light btn-sm" title="Upload" data-toggle="tooltip" data-placement="top" data-original-title="${languageResource.get("uploadTopicSettings")}"><i class="fas fa-upload"></i></button>
                    </div>
                </div>

                <div class="setting-group">
                    <label for="topic-count">${languageResource.get("numTopics")}</label>
                    <select id="topic-count" class="form-control"></select>
                </div>

                <div class="setting-group">
                    <label>${languageResource.get("topicColorMode")}</label>
                    <div class="color-option">
                        <input type="radio" id="per-topic-colors" name="color-mode" value="per-topic">
                        <label for="per-topic-colors">${languageResource.get("perTopic")}</label>
                    </div>
                    <div class="color-option">
                        <input type="radio" id="gradient-range" name="color-mode" value="gradient">
                        <label for="gradient-range">Gradient range</label>
                    </div>

                    <div class="color-pickers" style="display:none;">
                        <div>
                            <input type="color" id="gradient-start-color">
                            <div class="color-label">Min</div>
                        </div>
                        <div>
                            <input type="color" id="gradient-end-color">
                            <div class="color-label">Max</div>
                        </div>
                    </div>

                    <div class="key-topic-color-grid" style="display:none;"></div>
                </div>

                <div style="display: flex; gap: 10px;">
                    <button class="key-topics-setting-apply-btn">${languageResource.get("apply")}</button>
                    <button class="key-topics-setting-reset-btn">${languageResource.get("reset")}</button>
                </div>
            </div>
        </#if>
    </#if>
</div>
