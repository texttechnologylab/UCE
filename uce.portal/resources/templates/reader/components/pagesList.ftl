<#list documentPages as page>
    <div class="page" data-id="${page.getPageNumber()}">
        <div class="blurrer display-none" data-toggled="false"></div>
        <div>
            <div class="page-topic-container">
                <div class="flexed align-items-center justify-content-between">
                    <i class="fas fa-info-circle mr-1 color-prime" data-trigger="hover" data-toggle="popover"
                       data-placement="top"
                       data-content="${languageResource.get("topicModellingInfo")}"></i>
                    <#if (page.getPageTopicDistribution())?? && (page.getPageTopicDistribution().getYakeTopicOne())??>
                        <p class="text font-italic text-center mb-0">
                            <span data-wid="${page.getPageTopicDistribution().getId()?string?replace('.', '')?replace(',', '')}" data-wtype="P-TOPIC"
                                  data-wcovered="${page.getPageTopicDistribution().getYakeTopicOne()}"
                                  class="open-wiki-page">
                                #${page.getPageTopicDistribution().getYakeTopicOne()}
                            </span>
                            <span data-wid="${page.getPageTopicDistribution().getId()?string?replace('.', '')?replace(',', '')}" data-wtype="P-TOPIC"
                                  data-wcovered="${page.getPageTopicDistribution().getYakeTopicTwo()}"
                                  class="ml-2 open-wiki-page">
                                #${page.getPageTopicDistribution().getYakeTopicTwo()}
                            </span>
                            <span data-wid="${page.getPageTopicDistribution().getId()?string?replace('.', '')?replace(',', '')}" data-wtype="P-TOPIC"
                                  data-wcovered="${page.getPageTopicDistribution().getYakeTopicThree()}"
                                  class="ml-2 open-wiki-page">
                                #${page.getPageTopicDistribution().getYakeTopicThree()}
                            </span>
                            <span data-wid="${page.getPageTopicDistribution().getId()?string?replace('.', '')?replace(',', '')}" data-wtype="P-TOPIC"
                                  data-wcovered="${page.getPageTopicDistribution().getYakeTopicFour()}"
                                  class="ml-2 open-wiki-page">
                                #${page.getPageTopicDistribution().getYakeTopicFour()}
                            </span>
                        </p>
                    </#if>
                    <div></div>
                </div>
            </div>
            <div class="page-content">
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
        </div>
        <p class="text-center text-dark mb-0">
            — ${page.getPageNumber() + 1} —
        </p>
    </div>
</#list>
