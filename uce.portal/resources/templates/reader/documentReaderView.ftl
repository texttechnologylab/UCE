<!DOCTYPE html>
<html lang="en">
<head>
    <link rel="stylesheet"
          href="https://cdn.jsdelivr.net/npm/bootstrap@4.3.1/dist/css/bootstrap.min.css"
          integrity="sha384-ggOyR0iXCbMQv3Xipma34MD+dH/1fQ784/j6cY/iJTQUOhcWr7x9JvoRxT2MZw1T"
          crossorigin="anonymous">
    <link
            href="https://cdnjs.cloudflare.com/ajax/libs/animate.css/4.1.1/animate.min.css"
            rel="stylesheet">
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"
          integrity="sha256-p4NxAoJBhIIN+hmNHrzRCf9tD/miZyoHS5obTRR9BMY="
          crossorigin=""/>
    <style>
        <#include "*/css/site.css">
        <#include "*/css/document-reader.css">
        <#include "*/css/custom-context-menu.css">
        <#include "*/css/wiki.css">
        <#include "*/css/drawflow.css">
        <#include "*/css/bg-anim.css">
    </style>
    <script src="https://kit.fontawesome.com/b0888ca2eb.js"
            crossorigin="anonymous"></script>
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"
            integrity="sha256-20nQCchB9co0qIjJZRGuk2/Z9VM+kNiyxNV1lvTlZBo="
            crossorigin=""></script>
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script
            src="https://cdn.jsdelivr.net/npm/popper.js@1.14.7/dist/umd/popper.min.js"
            integrity="sha384-UO2eT0CpHqdSJQ6hJty5KVphtPhzWj9WO1clHTMGa3JDZwrnQq4sF86dIHNDz0W1"
            crossorigin="anonymous"></script>
    <script
            src="https://cdn.jsdelivr.net/npm/bootstrap@4.3.1/dist/js/bootstrap.min.js"
            integrity="sha384-JjSmVgyd0p3pXB1rRibZUAYoIIy6OrQ6VrjIEaFf/nJGzIxFDsf4x0xIM+B07jRM"
            crossorigin="anonymous"></script>
    <script type="importmap">
        {
          "imports": {
            "three": "https://unpkg.com/three@v0.161.0/build/three.module.js",
            "three/addons/": "https://unpkg.com/three@v0.161.0/examples/jsm/"
          }
        }
    </script>

    <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
    <script src="js/utils.js"></script>
    <script src="js/visualization/cdns/chartjs-449.js"></script>
    <script src="js/visualization/cdns/echarts-560.js"></script>
    <script src="js/visualization/cdns/d3js-790.js"></script>
    <script src="js/visualization/cdns/drawflow-last.js"></script>
    <script type="module" src="js/md-block.js"></script>

    <title>${document.getDocumentTitle()}</title>
</head>

<body class="no-cursor">

<#include "*/messageModal.ftl">

<div class="site-container">

    <#include "*/wiki/components/wikiPageModal.ftl">

    <div class="corpus-inspector-include display-none">
    </div>

    <!-- this object must be set on any site we use within UCE -->
    <div id="prime-color-container" class="color-prime"></div>

    <div class="pages-loader-popup">
        <div class="flexed align-items-center justify-content-center h-100 w-100">
            <p class="mb-0 text">
                <i class="rotate fas fa-circle-notch mr-1"></i> ${languageResource.get("loadingPages")} <span
                        class="color-prime loaded-pages-count">0</span>/${document.getPages()?size}</p>
        </div>
    </div>

    <div class="dot" id="custom-cursor"></div>

    <ul class='custom-menu'>
        <li data-action="open-more"><i class="fab fa-readme mr-2"></i> ${languageResource.get("more")}</li>
        <!--<li data-action="search"><i class="fas fa-search mr-2"></i> ${languageResource.get("search")}</li>-->
        <li data-action="highlight" data-target=""><i
                    class="fas fa-highlighter mr-2"></i> ${languageResource.get("highlight")}</li>
    </ul>

    <div class="container-fluid">
        <div class="flexed m-0 p-0">

            <div class="w-100">
                <div class="position-relative reader-container container"
                     data-id="${document.getId()?string?replace('.', '')?replace(',', '')}"
                     data-pagescount="${document.getPages()?size?string?replace('.', '')?replace(',', '')}" data-searchtokens="${(searchTokens)!''}">

                    <!-- Topic navigation buttons (hidden by default) -->
                    <div class="topic-navigation-buttons">
                        <button class="topic-nav-button prev-topic-button" title="Previous occurrence">
                            <i class="fas fa-chevron-up"></i>
                        </button>
                        <button class="topic-nav-button next-topic-button" title="Next occurrence">
                            <i class="fas fa-chevron-down"></i>
                        </button>
                    </div>

                    <div class="header ">
                        <div class="text-center flexed align-items-center justify-content-around w-100">
                            <div class="flexed align-items-center">
                                <a class="header-btn open-wiki-page color-prime" data-wid="${document.getWikiId()}">
                                    <i class="large-font m-0 fab fa-wikipedia-w"></i>
                                </a>
                                <#if document.getMetadataTitleInfo().getScrapedUrl()?has_content>
                                    <a class="header-btn open-metadata-url-btn m-0"
                                       href="${document.getMetadataTitleInfo().getScrapedUrl()}" target="_blank">
                                        <i class="color-prime m-0 large-font fas fa-university"></i>
                                    </a>
                                </#if>
                            </div>

                            <div class="ml-2 mr-2">
                                <h5>${document.getDocumentTitle()}</h5>
                                <p class="text mb-0">${document.getMetadataTitleInfo().getPublished()}</p>
                            </div>
                            <p class="m-0 text">${document.getLanguage()?upper_case}</p>
                        </div>
                    </div>

                    <!-- metadata if exists -->
                    <#assign uceMetadata = document.getUceMetadataWithoutJson()>
                    <#if uceMetadata?has_content && uceMetadata?size gt 0>
                        <div class="w-100 pt-4 pl-4 pr-4 pb-1 mt-2">
                            <div class="small-font">
                                <#include "*/document/documentUceMetadata.ftl">
                            </div>
                            <#if document.hasJsonUceMetadata()>
                                <div class="flexed align-items-center justify-content-center text-center mt-1">
                                    <a class="btn bg-lightgray rounded light-border xsmall-font open-wiki-page
                                    align-items-center flexed hoverable"
                                       data-wid="${document.getWikiId()}">
                                        <i class="fab fa-wikipedia-w bg-light light-border rounded p-1 mr-2"></i>
                                        <span class="font-italic text-secondary"
                                              style="margin-top: 3px">${languageResource.get("showUceMetadata")}...</span>
                                    </a>
                                </div>
                            </#if>
                        </div>
                        <hr class="mb-0"/>
                    </#if>

                    <#if document.getMimeType() == "application/pdf" ||  document.getMimeType() == "pdf">
                        <#include '*/reader/components/viewerPdf.ftl' />
                    <#else>
                        <div class="document-content">
                            <#assign documentPages = document.getPages(10, 0)>
                            <#assign documentText = document.getFullText()>
                            <#assign documentAnnotations = document.getAllAnnotations(0, 10)>
                            <#include '*/reader/components/pagesList.ftl' />
                        </div>
                        <!-- Scrollbar Minimap -->
                        <div class="scrollbar-minimap">
                            <div class="minimap-markers"></div>
                            <div class="minimap-preview">
                                <div class="preview-content"></div>
                            </div>
                        </div>
                    </#if>


                </div>
            </div>

            <div class="side-bar">

                <div class="tab-header">
                    <button class="tab-btn active" data-tab="navigator-tab">Navigator</button>
                    <button class="tab-btn" data-tab="visualization-tab">Visualization</button>
                    <button class="tab-btn" data-tab="playground-tab">Playground</button>

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
                                    <a href="${document.getMetadataTitleInfo().getScrapedUrl()}" target="_blank"
                                       class="title-image mb-3">
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
                                <#if document.getMetadataTitleInfo().getScrapedUrl()?has_content>
                                    <a href="${document.getMetadataTitleInfo().getPdfUrl()}" class="btn">
                                        <i class="fas fa-file-pdf mr-2"></i> Download PDF
                                    </a>
                                </#if>
                            </div>

                            <#if (searchTokens?has_content) && (searchTokens?length gt 0)>
                                <div class="group-box search-tokens-box">
                                    <p class="title"><span>${languageResource.get("searchTokens")}</span> <i
                                                class="ml-2 rotate fas fa-spinner"></i></p>
                                    <div class="found-searchtokens-list"></div>
                                </div>
                            </#if>

                            <div class="group-box topics-box">
                                <p class="title">
                                    <span>${languageResource.get("topics")}</span>
                                    <i class="ml-2 topics-loading rotate fas fa-spinner"></i>
                                </p>
                                <div class="document-topics-list" data-document-id="${document.id}"></div>
                            </div>
                        </div>
                    </div>

                    <!-- Visualization Tab -->
                    <#assign documentTopics = document.getUnifiedTopics()![]>
                    <div class="tab-pane" id="visualization-tab">
                        <div class="visualization-wrapper">
                            <div class="visualization-content">
                                <div class="viz-panel" id="viz-panel-1">
                                    <div id="vp-1" data-document-id="${document.id}"></div>
                                </div>
                                <div class="viz-panel" id="viz-panel-2">
                                    <div id="vp-2" ></div>
                                </div>
                                <div class="viz-panel" id="viz-panel-3">
                                    <div id="vp-3"></div>
                                </div>
                                <div class="viz-panel" id="viz-panel-4">
                                    <div id="vp-4-wrapper">
                                        <div class="selector-container">
                                            <label for="similarityTypeSelector">Similarity Type:</label>
                                            <select id="similarityTypeSelector">
                                                <option value="cosine" title="Cosine: Measures angular similarity of topic-word vectors">Cosine</option>
                                                <option value="count" title="Shared Count: Simply counts overlapping words between topics">Shared Count</option>
                                            </select>
                                        </div>
                                        <div id="vp-4"></div>
                                    </div>

                                </div>
                                <div class="viz-panel" id="viz-panel-5">
                                    <div id="vp-5" ></div>
                                </div>
                            </div>

                            <div class="viz-bottom-nav">
                                <button class="viz-nav-btn active" data-target="#viz-panel-1">Semantic Density</button>
                                <button class="viz-nav-btn" data-target="#viz-panel-2">Topic-Entity</button>
                                <button class="viz-nav-btn" data-target="#viz-panel-3">Topic Landscape</button>
                                <button class="viz-nav-btn" data-target="#viz-panel-4">Topic Similarity</button>
                                <button class="viz-nav-btn" data-target="#viz-panel-5">Sentence Topic Flow</button>
                            </div>

                        </div>
                    </div>
                </div>

            </div>
        </div>

    </div>
</div>
</body>

<#--<script type="module">
    <#include "*/js/corpusUniverse.js">
</script>-->
<script type="module">
    <#include "*/js/graphViz.js">
    <#include "*/js/flowViz.js">
</script>

<script>
    <#include "*/js/site.js">
    <#include "*/js/documentReader.js">
    <#include "*/js/customContextMenu.js">
</script>

</html>