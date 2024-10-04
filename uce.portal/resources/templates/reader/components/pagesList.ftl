<div>
    <#list documentPages as page>
        <div class="page" data-id="${page.getPageNumber() + 1}">
            <div class="blurrer display-none" data-toggled="false"></div>
            <div>
                <div class="page-topic-container">
                    <div class="flexed align-items-center justify-content-between">
                        <i class="fas fa-info-circle mr-1 color-prime" data-trigger="hover" data-toggle="popover"
                           data-placement="top"
                           data-content="${languageResource.get("topicModellingInfo")}"></i>
                        <p class="text font-italic text-center mb-0">
                            <span class="text-dark">#</span>${page.getPageTopicDistribution().getYakeTopicOne()}
                            <span class="ml-2 text-dark">#</span>${page.getPageTopicDistribution().getYakeTopicTwo()}
                            <span class="ml-2 text-dark">#</span>${page.getPageTopicDistribution().getYakeTopicThree()}
                            <span class="ml-2 text-dark">#</span>${page.getPageTopicDistribution().getYakeTopicFour()}
                        </p>
                        <div></div>
                    </div>
                </div>
                <#if (page.getParagraphs())?? && page.getParagraphs()?size == 0>
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