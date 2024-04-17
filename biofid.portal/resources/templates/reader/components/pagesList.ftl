<div>
    <#list documentPages as page>
        <div class="page" data-id="${page.getPageNumber() + 1}">
            <div class="blurrer display-none" data-toggled="false"></div>
            <div>
                <#if page.getParagraphs()?size == 0>
                    <p class="text paragraph">
                        ${page.buildHTMLString(documentAnnotations, documentText)}
                    </p>
                <#else>
                    <#list page.getParagraphs() as paragraph>
                        <p class="text paragraph" style="
                                text-align: ${paragraph.getAlign()};
                                font-weight: ${paragraph.getFontWeight()};
                                text-decoration: ${paragraph.getUnderlined()};">
                            ${paragraph.buildHTMLString(documentAnnotations)}
                        </p>
                    </#list>
                </#if>
            </div>
            <p class="text-center text-dark mb-0">
                — ${page.getPageNumber() + 1} —
            </p>
        </div>
    </#list>
</div>