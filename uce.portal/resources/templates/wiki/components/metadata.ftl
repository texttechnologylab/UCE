<div class="flexed align-items-center justify-content-between pl-1 pr-1">
    <p class="mb-0 mr-2">
        <i class="fas fa-vector-square small-font color-prime mr-1"></i>
        <span class="font-italic">${vm.getAnnotationType()}</span> -
        Annotation
        <span class="text">${vm.getWikiModel().getWikiId()}:</span>
        <span class="color-prime font-italic">${vm.getCoveredText()}</span>
    </p>
    <#if vm.getAnnotationType() == "Document">
        <p class="mb-0">${languageResource.get("annotatedInCorpus")}:
            <span class="text">${vm.getCorpus().getCorpus().getName()}</span></p>
    <#else>
        <p class="mb-0">${languageResource.get("annotatedInDocument")}:
            <span class="text">${vm.getDocument().getWikiId()}</span></p>
    </#if>
</div>
