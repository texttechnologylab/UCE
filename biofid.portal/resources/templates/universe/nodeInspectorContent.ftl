<div>
    <div class="text-center">
        <p class="mb-0">${document.getDocumentTitle()}</p>
        <p class="mb-0 small-font text">${document.getMetadataTitleInfo().getPublished()}</p>
    </div>

    <p class="text-snippet text mt-2">
        "${document.getFullTextSnippet(150)}..."
    </p>
</div>