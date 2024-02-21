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
    </style>
    <title>${document.getDocumentTitle()}</title>
</head>

<body class="no-cursor">
<div class="site-container">

    <div class="dot" id="custom-cursor"></div>

    <ul class='custom-menu'>
        <li data-action="open-more"><i class="fab fa-readme mr-2"></i> Mehr dazu</li>
        <li data-action="search"><i class="fas fa-search mr-2"></i> Suchen</li>
        <li data-action="highlight" data-target=""><i class="fas fa-highlighter mr-2"></i> Hervorheben</li>
    </ul>

    <div class="container-fluid">
        <div class="flexed m-0 p-0">

            <div class="w-100">
                <div class="reader-container container">

                    <div class="header text-center flexed align-items-center justify-content-around">
                        <a class="open-goethe-url-btn m-0" href="${document.getGoetheTitleInfo().getScrapedUrl()}"
                           target="_blank">
                            <i class="color-prime m-0 large-font fas fa-university"></i>
                        </a>
                        <div>
                            <h5>${document.getDocumentTitle()}</h5>
                            <p class="text mb-0">${document.getGoetheTitleInfo().getPublished()}</p>
                        </div>
                        <p class="m-0 text">${document.getLanguage()?upper_case}</p>
                    </div>

                    <div class="document-content">

                        <#list document.getPages(10, 0) as page>
                            <div class="page" data-id="${page.getPageNumber() + 1}">
                                <div class="blurrer display-none" data-toggled="false"></div>
                                <div>
                                    <#list page.getParagraphs() as paragraph>
                                        <p class="text paragraph" style="
                                                text-align: ${paragraph.getAlign()};
                                                font-weight: ${paragraph.getFontWeight()};
                                                text-decoration: ${paragraph.getUnderlined()};">
                                            ${paragraph.buildHTMLString(document.getAllAnnotations())}
                                        </p>
                                    </#list>
                                </div>
                                <p class="text-center text-dark mb-0">
                                    — ${page.getPageNumber() + 1} —
                                </p>
                            </div>
                        </#list>
                    </div>

                </div>
            </div>

            <div class="side-bar">

                <div class="expander" data-expanded="true"><i class="m-0 xlarge-font fas fa-chevron-right"></i></div>

                <div class="side-bar-content">
                    <div class="header">
                        <h5 class="text-center">Navigator</h5>
                    </div>

                    <div class="group-box">
                        <p class="title">Original Dokument</p>
                        <a href="${document.getGoetheTitleInfo().getScrapedUrl()}" target="_blank"
                           class="title-image mb-3">
                            <img src="${document.getGoetheTitleInfo().getTitleImageUrl()}"/>
                        </a>
                    </div>

                    <div class="group-box">
                        <p class="title">Einstellungen</p>
                        <div class="flexed align-items-center">
                            <i class="fas fa-text-height mr-2"></i>
                            <input min="10" max="21" class="font-size-range w-100 hoverable" value="16" type="range"/>
                        </div>
                    </div>

                    <div class="group-box">
                        <p class="title">Seite <span class="current-page">1</span></p>
                        <a class="btn open-goethe-url-page-btn" target="_blank"
                           data-href="${document.getGoetheTitleInfo().getPageViewStartUrl()}"
                           href="${document.getGoetheTitleInfo().getPageViewStartUrl()}">
                            <i class="mr-2 fas fa-university"></i> Original
                        </a>
                    </div>

                    <div class="buttons group-box">
                        <p class="title">Funktionen</p>
                        <button class="btn toggle-focus-btn">
                            <i class="fas fa-satellite-dish mr-2"></i> Toggle Fokus
                        </button>
                        <a href="${document.getGoetheTitleInfo().getPdfUrl()}" class="btn">
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