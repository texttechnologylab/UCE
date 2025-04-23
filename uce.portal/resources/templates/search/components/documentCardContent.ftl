<div class="flexed align-items-stretch h-100">

    <#assign isReducedView = reduced??>

    <!-- incoming links -->
    <#if !isReducedView && document.getLinkableViewModel()?has_content && document.getLinkableViewModel().getIncomingLinks()?size gt 0>
        <div class="links-container">
            <div class="links-div" data-kind="incoming">
                <div class="link border-radius-0 mt-0 mb-4 "><i class="large-font fab fa-hubspot"></i>
                </div>
                <div class="h-100 pb-1 flexed align-items-center">
                    <div class="position-relative pb-4">
                        <#assign popoverTitle = "<span class='text-center w-100 small-font'><i class='mr-2 small-font fas fa-long-arrow-alt-right'></i>" + languageResource.get("incomingLinks") + "</span>">
                        <#assign popupText = "">
                        <#list document.getLinkableViewModel().getIncomingLinks() as link>
                            <#assign popupText += "<i class='small-font fas fa-link mr-1 color-prime'></i>" + link.getLink().getType() + "<br/>">
                        </#list>
                        <div class="link lines open-linkable-node"
                             data-unique="${document.getUnique()}"
                             data-trigger="hover"
                             data-placement="left"
                             data-toggle="popover"
                             data-html="true"
                             data-original-title="${popoverTitle}"
                             data-content="${popupText}">
                            ${document.getLinkableViewModel().getIncomingLinks()?size}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </#if>

    <div class="pl-3 pr-3 pb-2 pt-3 w-100">
        <div class="document-header">
            <div class="w-100">
                <div class="flexed align-items-center">
                    <div class="flexed align-items-center">
                        <!-- We only show the 3d taxonomy dist if we have them annotated in the current corpus -->
                        <#if corpusConfig?? && corpusConfig.getAnnotations().getTaxon().isBiofidOnthologyAnnotated()>
                            <a class="title-btn open-globe color-prime" data-type="document" data-trigger="hover"
                               data-toggle="popover"
                               data-placement="top"
                               data-content="${languageResource.get("openTaxonomyDist")}"
                               data-searchid="${searchId}"
                               data-id="${document.getId()?string?replace('.', '')?replace(',', '')}">
                                <i class="m-0 fas fa-globe-europe"></i></a>
                        </#if>
                        <a class="title-btn open-wiki-page color-prime" data-trigger="hover" data-toggle="popover"
                           data-wcovered="" data-wid="${document.getWikiId()}">
                            <i class="m-0 fab fa-wikipedia-w"></i></a>
                        <a class="title-btn open-document color-prime" data-trigger="hover" data-toggle="popover"
                           data-placement="top"
                           data-content="${languageResource.get("openDocumentReader")}"
                           data-searchid="${searchId}"
                           data-id="${document.getId()?string?replace('.', '')?replace(',', '')}">
                            <i class="m-0 fas fa-book-open"></i></a>
                    </div>
                    <div class="flexed align-items-center wrapped ml-2 w-100">
                        <div class="open-document mr-2 clickable flexed align-items-center wrapped mb-1"
                             data-id="${document.getId()?string?replace('.', '')?replace(',', '')}">
                            <h6 class="title mb-0">${document.getDocumentTitle()}</h6>
                        </div>
                        <label class="xsmall-font text mb-1 font-italic"><i class="fas fa-id-card-alt"></i>
                            (${document.getDocumentId()})</label>
                    </div>

                </div>
            </div>

            <div class="flexed align-items-center justify-content-end">
                <p class="mb-0 text mr-3"> ${document.getLanguage()?upper_case}</p>
                <div class="mb-0 flexed align-items-center text">
                    <i class="fas fa-file-alt"></i> <label
                            class="mb-0 ml-2">${document.getPages()?size}</label>
                </div>
                <#if searchState??>
                    <div class="ml-3 mb-0 flexed align-items-center text">
                        <#assign rank = searchState.getPossibleRankOfDocumentIdx(documentIdx)!>
                        <#if rank gt -1>
                            <p class="mb-0 color-prime flexed align-items-center" data-trigger="hover"
                               data-toggle="popover"
                               data-placement="top"
                               data-content="${languageResource.get("searchRankDescription")}">
                                <i class="fab fa-hackerrank mr-1"></i> ${rank}
                            </p>
                        </#if>
                    </div>
                </#if>
            </div>
        </div>

        <!-- topics -->
        <div class="flexed align-items-center justify-content-between w-100">
            <div class="flexed align-items-center">
                <label class="text-secondary small-font mr-2"><i
                            class="far fa-clock mr-1"></i> ${document.getMetadataTitleInfo().getPublished()}</label>
                <label class="text-secondary small-font mr-2"><i
                            class="fas fa-pen-nib mr-1"></i> ${document.getMetadataTitleInfo().getAuthor()}</label>
            </div>
            <div class="flexed align-items-center topic-list">
                <#if document.getDocumentKeywordDistribution()?has_content>
                    <label data-wid="${document.getDocumentKeywordDistribution().getWikiId()}"
                           data-wcovered="${document.getDocumentKeywordDistribution().getYakeTopicOne()}"
                           class="add-wiki-logo open-wiki-page">
                        #${document.getDocumentKeywordDistribution().getYakeTopicOne()}
                    </label>
                    <label data-wid="${document.getDocumentKeywordDistribution().getWikiId()}"
                           data-wcovered="${document.getDocumentKeywordDistribution().getYakeTopicTwo()}"
                           class="add-wiki-logo open-wiki-page">
                        #${document.getDocumentKeywordDistribution().getYakeTopicTwo()}
                    </label>
                    <label data-wid="${document.getDocumentKeywordDistribution().getWikiId()}"
                           data-wcovered="${document.getDocumentKeywordDistribution().getYakeTopicThree()}"
                           class="add-wiki-logo open-wiki-page">
                        #${document.getDocumentKeywordDistribution().getYakeTopicThree()}
                    </label>
                    <label data-wid="${document.getDocumentKeywordDistribution().getWikiId()}"
                           data-wcovered="${document.getDocumentKeywordDistribution().getYakeTopicThree()}"
                           class="add-wiki-logo open-wiki-page">
                        #${document.getDocumentKeywordDistribution().getYakeTopicThree()}
                    </label>
                </#if>
                <#if document.getDocumentUnifiedTopicDistribution(3)?has_content>
                    <#list document.getDocumentUnifiedTopicDistribution(3) as topic>
                        <label data-wid="${topic.getWikiId()}"
                               data-wcovered="${topic.getValue()}"
                               class="add-wiki-logo open-wiki-page">
                            ${topic.getValue()}
                        </label>
                    </#list>
                </#if>
            </div>
        </div>

        <#macro renderSnippets snippets>
            <#if snippets?has_content>
                <#list snippets as snippet>
                    <#assign displayStyle = (snippet?index != 0)?then('display: none;', '')>
                    <div class="snippet-content mt-1 mb-2 h-100 position-relative"
                         data-id="${snippet?index}" style="${displayStyle}">
                        <div class="small-font text font-italic mr-2 word-break-word">
                            ${snippet.getSnippet()}
                            <#if snippet.getPage()?has_content>
                                <label class="display-none page-html">
                                    ${snippet.getPage().getCoveredHtmlText()}
                                </label>
                                <div class="inspect-page-btn hoverable clickable"
                                     onclick="openInExpandedTextView('${languageResource.get('page')} ${snippet.getPage().getPageNumber()}', $(this).closest('.snippet-content').find('.page-html').html())">
                                    ${snippet.getPage().getPageNumber()}.<i class="ml-1 fas fa-file-alt"></i>
                                </div>
                            </#if>
                        </div>
                    </div>
                </#list>

                <#if snippets?size gt 1>
                    <button class="toggle-snippets-btn btn small-font light-border w-100 mt-1 mb-2 color-prime">
                        ${languageResource.get("more")} <i class="ml-1 fas fa-file-alt"></i>
                    </button>
                </#if>
            <#else>
                <@renderFallback document/>
            </#if>
        </#macro>

        <#macro renderFallback document>
            <#if document?has_content>
                <div class="snippet-content position-relative">
                    <div class="mb-0 small-font text font-italic mr-2 block-text">
                        ${document.getFullTextSnippet(85)}...
                    </div>
                </div>
            </#if>
        </#macro>

        <#if searchState?? && searchState.getSearchType()??>
            <#if searchState.getSearchType() == "DEFAULT">
                <div class="snippets-container">
                    <#assign snippets = searchState.getPossibleSnippetsOfDocumentIdx(documentIdx)!>
                    <@renderSnippets snippets />
                </div>
            <#elseif searchState.getSearchType() == "NEG">
                <div class="snippets-container">
                    <#assign snippets = searchState.getPossibleSnippetsOfDocumentId(document.getId())!>
                    <@renderSnippets snippets />
                </div>
            <#else>
                <@renderFallback document/>
            </#if>
        <#else>
            <@renderFallback document/>
        </#if>

        <#macro renderFallback document>
            <#if document?has_content>
                <#if mainAnno??>
                    <#if offsetList??>
                        <div class="snippet-content h-100 position-relative">
                            <div class="mb-0 small-font text font-italic mr-2 word-break-word">
                                ${document.getFullTextSnippetOffsetList(offsetList)}...
                            </div>
                        </div>
                    <#else>
                        <div class="snippet-content h-100 position-relative">
                            <div class="mb-0 small-font text font-italic mr-2 word-break-word">
                                ${document.getFullTextSnippetAnnotationOffset(mainAnno)}...
                            </div>
                        </div>
                    </#if>
                <#else>
                    <div class="snippet-content h-100 position-relative">
                        <div class="mb-0 small-font text font-italic mr-2 word-break-word">
                            ${document.getFullTextSnippet(85)}...
                        </div>
                    </div>
                </#if>
            </#if>
        </#macro>

        <!-- metadata if it exists (and its not reduced view) -->
        <#if !isReducedView && document.getUceMetadataWithoutJson()?size gt 0>
            <#assign uceMetadata = document.getUceMetadataWithoutJson()>
            <div class="metadata-div">
                <#include "*/document/documentUceMetadata.ftl">
            </div>
        </#if>
    </div>

    <!-- outgoing links -->
    <#if !isReducedView && document.getLinkableViewModel()?has_content && document.getLinkableViewModel().getOutgoingLinks()?size gt 0>
        <div class="links-container">
            <div class="links-div" data-kind="outgoing">
                <div class="link border-radius-0 mt-0 mb-4 "><i class="turn-135 large-font fab fa-hubspot"></i></div>
                <div class="h-100 pb-1 flexed align-items-center">
                    <div class="position-relative pb-4">
                        <#assign popoverTitle = "<span class='text-center w-100 small-font'>" + languageResource.get("outgoingLinks") + "<i class='ml-2 small-font fas fa-long-arrow-alt-right'></i></span>">
                        <#assign popupText = "">
                        <#list document.getLinkableViewModel().getOutgoingLinks() as link>
                            <#assign popupText += "<i class='small-font fas fa-link mr-1 color-prime'></i>" + link.getLink().getType() + "<br/>">
                        </#list>
                        <div class="link lines open-linkable-node"
                             data-unique="${document.getUnique()}"
                             data-trigger="hover"
                             data-placement="right"
                             data-toggle="popover"
                             data-html="true"
                             data-original-title="${popoverTitle}"
                             data-content="${popupText}">
                            ${document.getLinkableViewModel().getOutgoingLinks()?size}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </#if>

</div>
