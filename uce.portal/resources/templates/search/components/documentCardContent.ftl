<div class="document-header">
    <div class="w-100">
        <div class="flexed align-items-center">
            <div class="flexed align-items-center">
                <!-- We only show the 3d taxonomy dist if we have them annotated in the current corpus -->
                <#if searchState.getCorpusConfig()?has_content && searchState.getCorpusConfig().getAnnotations().getTaxon().isBiofidOnthologyAnnotated()>
                    <a class="title-btn open-globe" data-type="document" data-trigger="hover" data-toggle="popover"
                       data-placement="top"
                       data-content="${languageResource.get("openTaxonomyDist")}"
                       data-id="${document.getId()?string?replace('.', '')?replace(',', '')}">
                        <i class="m-0 fas fa-globe-europe"></i></a>
                </#if>
                <a class="title-btn open-document" data-trigger="hover" data-toggle="popover" data-placement="top"
                   data-content="${languageResource.get("openDocumentReader")}"
                   data-id="${document.getId()?string?replace('.', '')?replace(',', '')}">
                    <i class="m-0 fas fa-book-open"></i></a>
            </div>
            <h6 class="mb-0 title">${document.getDocumentTitle()}</h6>
        </div>
    </div>

    <div class="flexed align-items-center">
        <p class="mb-0 text mr-3"> ${document.getLanguage()?upper_case}</p>
        <div class="mb-0 flexed align-items-center text">
            <i class="fas fa-file-alt color-secondary"></i> <label
                    class="mb-0 ml-2">${document.getPages()?size}</label>
        </div>
    </div>
</div>

<!-- topics -->
<div class="flexed align-items-center justify-content-between w-100">
    <label class="text-secondary small-font mr-2">${document.getMetadataTitleInfo().getPublished()}</label>
    <div class="flexed align-items-center topic-list">
        <#if document.getDocumentTopicDistribution()?has_content>
            <label>#${document.getDocumentTopicDistribution().getYakeTopicOne()}</label>
            <label>#${document.getDocumentTopicDistribution().getYakeTopicTwo()}</label>
            <label>#${document.getDocumentTopicDistribution().getYakeTopicThree()}</label>
        </#if>
    </div>
</div>

<div class="snippet-content flexed align-items-center justify-content-between h-100">
    <p class="mb-0 small-font text font-italic mr-2">
        <#assign snippet = searchState.getPossibleSnippetOfDocumentIdx(documentIdx)!>
        <#if !snippet?has_content>
            <#assign snippet = document.getFullTextSnippet(85)!>
        </#if>

        <!-- Get the list of search tokens -->
        <#assign searchTokens = searchState.getSearchTokens()!>

        <!-- Initialize the highlighted snippet -->
        <#assign highlightedSnippet = snippet>

        <!-- Loop through each search token and highlight it -->
        <#list searchTokens as searchToken>
            <#assign highlightedSnippet = highlightedSnippet?replace(searchToken, "<span class='highlighted-token'>${searchToken}</span>", "i")>
        </#list>

        <!-- Render the highlighted snippet -->
        ${highlightedSnippet}...
    </p>
</div>