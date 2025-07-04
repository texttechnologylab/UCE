<#list documentPages as page>
    <div class="page" data-id="${page.getPageNumber()}">
        <div class="blurrer display-none" data-toggled="false"></div>
        <div>
            <#if (page.getPageKeywordDistribution())?? && (page.getPageKeywordDistribution().getYakeTopicOne())??>
                <div class="page-topic-container">
                    <div class="flexed align-items-center justify-content-between">
                        <i class="fas fa-info-circle mr-1 color-prime" data-trigger="hover" data-toggle="popover"
                           data-placement="top"
                           data-content="${languageResource.get("topicModellingInfo")}"></i>
                        <div class="text small-font font-italic text-center mb-0 flexed align-items-center justify-content-center wrapped">
                            <span data-wid="${page.getPageKeywordDistribution().getWikiId()}"
                                  data-wcovered="${page.getPageKeywordDistribution().getYakeTopicOne()}"
                                  class="open-wiki-page">
                                #${page.getPageKeywordDistribution().getYakeTopicOne()}
                            </span>
                            <span data-wid="${page.getPageKeywordDistribution().getWikiId()}"
                                  data-wcovered="${page.getPageKeywordDistribution().getYakeTopicTwo()}"
                                  class="ml-2 open-wiki-page">
                                #${page.getPageKeywordDistribution().getYakeTopicTwo()}
                            </span>
                            <span data-wid="${page.getPageKeywordDistribution().getWikiId()}"
                                  data-wcovered="${page.getPageKeywordDistribution().getYakeTopicThree()}"
                                  class="ml-2 open-wiki-page">
                                #${page.getPageKeywordDistribution().getYakeTopicThree()}
                            </span>
                            <span data-wid="${page.getPageKeywordDistribution().getWikiId()}"
                                  data-wcovered="${page.getPageKeywordDistribution().getYakeTopicFour()}"
                                  class="ml-2 open-wiki-page">
                                #${page.getPageKeywordDistribution().getYakeTopicFour()}
                            </span>
                        </div>
                        <div></div>
                    </div>
                </div>
            </#if>
            <div class="page-content">
                <#if (page.getParagraphs())?? && page.getParagraphs()?size == 0>
                    <!--<markdown-viewer> -->
                        <p class="paragraph">
                            ${page.buildHTMLString(documentAnnotations, documentText)}
                        </p>
                    <!--</markdown-viewer>-->
                <#else>
                    <#list page.getParagraphs() as paragraph>
                        <p class="paragraph" style="
                                margin-bottom: 24px;
                                text-align: ${paragraph.getAlign()!"left"};
                                font-weight: ${paragraph.getFontWeight()};
                                text-decoration: ${paragraph.getUnderlined()};">
                            ${paragraph.buildHTMLString(documentAnnotations, documentText)}
                        </p>
                    </#list>
                </#if>
            </div>
        </div>
        <p class="text-center text-dark mb-0">
            — ${page.getPageNumber()} —
        </p>
    </div>
</#list>
