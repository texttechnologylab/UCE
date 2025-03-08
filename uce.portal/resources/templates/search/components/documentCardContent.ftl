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
                    <p class="mb-0 color-prime flexed align-items-center" data-trigger="hover" data-toggle="popover"
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
        <#if document.getDocumentTopicDistribution()?has_content>
            <label data-wid="${document.getDocumentTopicDistribution().getWikiId()}"
                   data-wcovered="${document.getDocumentTopicDistribution().getYakeTopicOne()}"
                   class="add-wiki-logo open-wiki-page">
                #${document.getDocumentTopicDistribution().getYakeTopicOne()}
            </label>
            <label data-wid="${document.getDocumentTopicDistribution().getWikiId()}"
                   data-wcovered="${document.getDocumentTopicDistribution().getYakeTopicTwo()}"
                   class="add-wiki-logo open-wiki-page">
                #${document.getDocumentTopicDistribution().getYakeTopicTwo()}
            </label>
            <label data-wid="${document.getDocumentTopicDistribution().getWikiId()}"
                   data-wcovered="${document.getDocumentTopicDistribution().getYakeTopicThree()}"
                   class="add-wiki-logo open-wiki-page">
                #${document.getDocumentTopicDistribution().getYakeTopicThree()}
            </label>
        </#if>
    </div>
</div>

<#if searchState??>
    <div class="snippets-container">
        <#assign snippets = searchState.getPossibleSnippetsOfDocumentIdx(documentIdx)!>
        <#if snippets?has_content>
            <#list snippets as snippet>
                <div class="snippet-content mt-1 mb-2 h-100 position-relative" data-id="${snippet?index}"
                        <#if snippet?index != 0> style="display: none;" </#if>>
                    <div class="small-font text font-italic mr-2 block-text">
                        ${snippet.getSnippet()}

                        <#if snippet.getPage()?has_content>
                            <input type="hidden" value="${snippet.getPage().getCoveredText()}">
                            <div class="inspect-page-btn hoverable clickable"
                                 onclick="openInExpandedTextView('Page ${snippet.getPage().getPageNumber()}', $(this).closest('.snippet-content').find('input').val())">
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
        </#if>
    </div>

<#else>
    <div class="snippet-content h-100 position-relative">
        <div class="mb-0 small-font text font-italic mr-2 block-text">
            ${document.getFullTextSnippet(85)}...
        </div>
    </div>
</#if>

<!-- metadata if it exists -->
<#if document.getUceMetadataWithoutJson()?size gt 0>
    <#assign uceMetadata = document.getUceMetadataWithoutJson()>
    <div class="metadata-div">
        <#include "*/document/documentUceMetadata.ftl">
    </div>
</#if>

