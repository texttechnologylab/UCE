<div>
    <div class="header">
    </div>

    <div>
        <#list documents as document>
            <div class="document-card open-document" data-id="${document.getModelId()}">

                <div>
                    <div class="hoverable-content">
                        <div class="readme">
                            <h5 class="m-0"><i class="text-dark large-font fab fa-readme"></i></h5>
                        </div>
                    </div>

                    <div class="document-header">
                        <p class="mb-0"><i class="fas fa-book color-secondary mr-1"></i> ${document.getDocumentTitle()}
                            <span class="text-secondary small-font">(${document.getGoetheTitleInfo().getPublished()})</span></p>
                        <p class="mb-0 underlined"> ${document.getLanguage()?upper_case}</p>
                    </div>

                    <div class="snippet-content mt-1 flexed align-items-center justify-content-between">
                        <p class="mb-0 small-font text font-italic mr-2">
                            "${document.getFullTextCleanedSnippet(75)}..."
                        </p>
                        <div class="mb-0 flexed align-items-center">
                            <i class="fas fa-file-alt color-secondary"></i> <label class="mb-0 ml-2">${document.getPages(10000, 0)?size}</label>
                        </div>
                    </div>
                </div>


            </div>
        </#list>
    </div>
</div>