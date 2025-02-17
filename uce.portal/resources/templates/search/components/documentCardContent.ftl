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
            <a class="open-document clickable" data-id="${document.getId()?string?replace('.', '')?replace(',', '')}">
                <h6 class="title mb-0">${document.getDocumentTitle()}</h6>
            </a>
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
                    <p class="mb-0 color-prime flexed align-items-center" data-trigger="hover" data-toggle="popover" data-placement="top"
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
    <label class="text-secondary small-font mr-2">${document.getMetadataTitleInfo().getPublished()}</label>
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

<div class="snippet-content flexed align-items-center justify-content-between h-100">
    <#if searchState??>
        <p class="mb-0 small-font text font-italic mr-2">
            <#assign snippet = searchState.getPossibleSnippetOfDocumentIdx(documentIdx)!>
            <#if !snippet?has_content>
                <#assign snippet = document.getFullTextSnippet(85)!>
            </#if>

            <!-- Get the list of search tokens -->
            <!-- We used to manuall highlight the tokens here, which sucked. We now do it in the db -->
            <#-- <#assign searchTokens = searchState.getSearchTokens()!> -->
            <!-- Initialize the highlighted snippet -->
            <#-- <#assign highlightedSnippet = snippet> -->
            <!-- Loop through each search token and highlight it -->
            <#-- <#list searchTokens as searchToken>
                <#assign highlightedSnippet = highlightedSnippet?replace(searchToken, "<span class='highlighted-token'>${searchToken}</span>", "i")>
            </#list>-->

            <!-- Render the highlighted snippet -->
            ${snippet}...
        </p>
    <#else>
        <p class="mb-0 small-font text font-italic mr-2">
            ${document.getFullTextSnippet(85)}...
        </p>
    </#if>

</div>