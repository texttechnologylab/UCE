<div>
    <#list occurrences as occurrence>
        <div class="flexed align-items-center justify-content-between occurrence group-box">
            <p class="mb-0 text xsmall-font">${occurrence.getOccurrenceSnippetHtml()}</p>
            <div class="border-left ml-1 pl-1 h-100">
                <!-- if we have a wiki id for that anno, we put a wiki button there. Else, just the document button -->
                <#assign annotation = occurrence.getUimaAnnotation()>
                <#if annotation?has_content && annotation.getWikiId?exists && annotation.getWikiId()?has_content>
                    <a class="rounded-a open-wiki-page" data-wid="${annotation.getWikiId()}"
                       data-wcovered="${annotation.getCoveredText()}">
                        <i class="color-prime fab fa-wikipedia-w"></i>
                    </a>
                <#else>
                    <a class="rounded-a open-document"
                       data-id="${annotation.getDocumentId()?string?replace('.', '')?replace(',', '')}">
                        <i class="color-prime fas fa-book-open"></i>
                    </a>
                </#if>
                <!-- not every annotation has a connection to a page -->
                <#if occurrence.getPage()?has_content>
                    <label class="display-none page-html">
                        ${occurrence.getPage().getCoveredHtmlText()}
                    </label>
                    <a class="rounded-a" onclick="openInExpandedTextView(
                            '${languageResource.get("page")} ${occurrence.getPage().getPageNumber()}',
                            $(this).prev('.page-html').html(),
                            ['${annotation.getCoveredText()}'])">
                        <i class="text-dark fas fa-file-alt"></i>
                    </a>
                </#if>
            </div>
        </div>
    </#list>
</div>
