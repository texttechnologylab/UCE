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
    <script type="module" src="https://md-block.verou.me/md-block.js"></script>
    <title>${document.getDocumentTitle()}</title>
</head>

<body class="no-cursor">

<div class="site-container">

    <#include "*/wiki/components/wikiPageModal.ftl">

    <div class="corpus-inspector-include display-none">
    </div>

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
                     data-pagescount="${document.getPages()?size}" data-searchtokens="${(searchTokens)!''}">

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

                    <div class="document-content">
                        <#assign documentPages = document.getPages(10, 0)>
                        <#assign documentText = document.getFullText()>
                        <#assign documentAnnotations = document.getAllAnnotations(0, 10)>
                        <#include '*/reader/components/pagesList.ftl' />
                    </div>

                </div>
            </div>

            <div class="side-bar">

                <div class="expander" data-expanded="true"><i class="m-0 xlarge-font fas fa-chevron-right"></i></div>

                <div class="side-bar-content">
                    <div class="header">
                        <h5 class="text-center">Navigator</h5>
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
                            <input min="10" max="21" class="font-size-range w-100 hoverable" value="16" type="range"/>
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
                            <div class="found-searchtokens-list">
                            </div>
                        </div>
                    </#if>
                </div>

            </div>
        </div>

    </div>
</div>
</body>

<script type="module">
    <#include "*/js/corpusUniverse.js">
</script>

<script>
    <#include "*/js/site.js">
    <#include "*/js/documentReader.js">
    <#include "*/js/customContextMenu.js">
</script>

</html>