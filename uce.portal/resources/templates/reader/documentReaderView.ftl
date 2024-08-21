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
    <script src="https://kit.fontawesome.com/b0888ca2eb.js"
            crossorigin="anonymous"></script>
    <style>
        <#include "../css/site.css">
        <#include "../css/document-reader.css">
        <#include "../css/custom-context-menu.css">
        <#include "../css/bg-anim.css">
    </style>
    <title>${document.getDocumentTitle()}</title>
</head>

<body class="no-cursor">

<link href='https://fonts.googleapis.com/css?family=Lato:300,400,700' rel='stylesheet' type='text/css'>
<div id='stars'></div>
<div id='stars2'></div>
<div id='stars3'></div>

<div class="site-container">

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
        <li data-action="search"><i class="fas fa-search mr-2"></i> ${languageResource.get("search")}</li>
        <li data-action="highlight" data-target=""><i class="fas fa-highlighter mr-2"></i> ${languageResource.get("highlight")}</li>
    </ul>

    <div class="container-fluid">
        <div class="flexed m-0 p-0">

            <div class="w-100">
                <div class="position-relative reader-container container"
                     data-id="${document.getId()?string?replace('.', '')?replace(',', '')}"
                     data-pagescount="${document.getPages()?size}">

                    <div class="header text-center flexed align-items-center justify-content-around">

                        <#if document.getMetadataTitleInfo().getScrapedUrl()?has_content>
                            <a class="open-metadata-url-btn m-0"
                               href="${document.getMetadataTitleInfo().getScrapedUrl()}" target="_blank">
                                <i class="color-prime m-0 large-font fas fa-university"></i>
                            </a>
                        </#if>
                        <div>
                            <h5>${document.getDocumentTitle()}</h5>
                            <p class="text mb-0">${document.getMetadataTitleInfo().getPublished()}</p>
                        </div>
                        <p class="m-0 text">${document.getLanguage()?upper_case}</p>
                    </div>

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

                    <div class="group-box">
                        <p class="title">${languageResource.get("page")} <span class="current-page">1</span></p>
                        <a class="btn open-metadata-url-page-btn" target="_blank"
                           data-href="${document.getMetadataTitleInfo().getPageViewStartUrl()}"
                           href="${document.getMetadataTitleInfo().getPageViewStartUrl()}">
                            <i class="mr-2 fas fa-university"></i> Original
                        </a>
                    </div>

                    <div class="buttons group-box">
                        <p class="title">${languageResource.get("functions")}</p>
                        <button class="btn toggle-focus-btn">
                            <i class="fas fa-satellite-dish mr-2"></i> Toggle Focus
                        </button>
                        <a href="${document.getMetadataTitleInfo().getPdfUrl()}" class="btn">
                            <i class="fas fa-file-pdf mr-2"></i> Download PDF
                        </a>
                    </div>

                </div>

            </div>
        </div>

    </div>


</div>
</body>

<script src="https://code.jquery.com/jquery-3.3.1.slim.min.js"
        integrity="sha384-q8i/X+965DzO0rT7abK41JStQIAqVgRVzpbzo5smXKp4YfRvH+8abtTE1Pi6jizo"
        crossorigin="anonymous"></script>
<script
        src="https://cdn.jsdelivr.net/npm/popper.js@1.14.7/dist/umd/popper.min.js"
        integrity="sha384-UO2eT0CpHqdSJQ6hJty5KVphtPhzWj9WO1clHTMGa3JDZwrnQq4sF86dIHNDz0W1"
        crossorigin="anonymous"></script>
<script
        src="https://cdn.jsdelivr.net/npm/bootstrap@4.3.1/dist/js/bootstrap.min.js"
        integrity="sha384-JjSmVgyd0p3pXB1rRibZUAYoIIy6OrQ6VrjIEaFf/nJGzIxFDsf4x0xIM+B07jRM"
        crossorigin="anonymous"></script>
<script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>

<script>
    <#include "../js/site.js">
    <#include "../js/documentReader.js">
    <#include "../js/customContextMenu.js">
</script>

</html>