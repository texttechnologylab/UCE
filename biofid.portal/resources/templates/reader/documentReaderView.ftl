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
    </style>
    <title>${document.getDocumentTitle()}</title>
</head>

<body>
<div class="site-container">

    <div class="container reader-container">

        <div class="header text-center flexed align-items-center justify-content-around">
            <a class="open-goethe-url-btn m-0" href="${document.getGoetheTitleInfo().getScrapedUrl()}" target="_blank">
                <i class="color-prime m-0 large-font fas fa-university"></i>
            </a>
            <div>
                <h5>${document.getDocumentTitle()}</h5>
                <p class="text mb-0">${document.getGoetheTitleInfo().getPublished()}</p>
            </div>
            <p class="m-0 text">${document.getLanguage()?upper_case}</p>
        </div>

        <div class="document-content">

            <#list document.getPages() as page>
                <div class="page">
                    <div>
                        <#list page.getParagraphs() as paragraph>
                            <p class="text paragraph" style="text-align: ${paragraph.getAlign()};">
                                ${paragraph.buildHTMLString(document.getNamedEntities())}
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
</script>

</html>