<div class="p-2">
    <div class="snippet-content small-font p-1 font-italic">${anno.getCoveredText()}</div>
    <div class="flexed mt-2 justify-content-between">
        <a class="w-rounded-btn m-0 w-100 rounded-0 pb-1 pt-1 border-right-0 open-wiki-page"
        data-wid="${anno.getWikiId()}" data-wcovered="${anno.getCoveredText()}">
            <i class="text-dark fab fa-wikipedia-w"></i>
        </a>
        <a class="w-rounded-btn m-0 w-100 rounded-0 pb-1 pt-1 open-document" data-id="${anno.getDocumentId()?string?replace('.', '')?replace(',', '')}">
            <i class="color-prime fas fa-book"></i>
        </a>
    </div>
</div>