<div class="document-header">
    <div class="w-100">
        <div class="flexed align-items-center">
            <div class="flexed align-items-center">
                <a class="title-btn open-globe" data-type="document" data-trigger="hover" data-toggle="popover" data-placement="top"
                   data-content="${languageResource.get("openTaxonomyDist")}" data-id="${document.getId()?string?replace('.', '')}">
                    <i class="m-0 fas fa-globe-europe"></i></a>
                <a class="title-btn open-document" data-trigger="hover" data-toggle="popover" data-placement="top"
                   data-content="${languageResource.get("openDocumentReader")}" data-id="${document.getId()?string?replace('.', '')}">
                    <i class="m-0 fas fa-book-open"></i></a>
            </div>
            <h6 class="mb-0 title">${document.getDocumentTitle()}</h6>
        </div>
        <label class="text-secondary small-font">${document.getMetadataTitleInfo().getPublished()}</label>
    </div>

    <div class="flexed align-items-center">
        <p class="mb-0 text mr-3"> ${document.getLanguage()?upper_case}</p>
        <div class="mb-0 flexed align-items-center text">
            <i class="fas fa-file-alt"></i> <label
                    class="mb-0 ml-2">${document.getPages()?size}</label>
        </div>
    </div>
</div>

<div class="snippet-content flexed align-items-center justify-content-between h-100">
    <p class="mb-0 small-font text font-italic mr-2">
        "${document.getFullTextSnippet(85)}..."
    </p>
</div>