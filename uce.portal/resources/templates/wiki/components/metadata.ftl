<div class="flexed align-items-center justify-content-between pl-1">
    <div class="flexed align-items-center">
        <p class="mb-0 mr-2">
            <i class="fas fa-vector-square small-font color-prime mr-1"></i>
            <span class="font-italic">${vm.getAnnotationType()}</span> -
            Annotation
            <span class="text">${vm.getWikiModel().getWikiId()}:</span>
            <span class="color-prime font-italic">${vm.getCoveredText()}</span>
        </p>
    </div>

    <#if vm.getAnnotationType() != "Corpus">
        <#if vm.getAnnotationType() == "Document">
            <p class="mb-0">${languageResource.get("annotatedInCorpus")}:
                <span class="text">${vm.getCorpus().getCorpus().getName()}</span></p>
        <#else>
            <div class="flexed align-items-center">
                <p class="mb-0">${languageResource.get("annotatedInDocument")}:
                    <span class="text">${vm.getDocument().getWikiId()}</span></p>
                <#if vm.getPage()?has_content>
                    <label class="display-none">${vm.getPage().getCoveredText()}</label>
                    <a class="w-rounded-btn ml-2 mr-0"
                       onclick="openInExpandedTextView('${vm.getCoveredText()}', $(this).prev().html(), ['${vm.getCoveredText()}'], undefined, undefined)">
                        <i class="fas fa-file-alt color-prime"></i>
                    </a>
                </#if>
            </div>
        </#if>
    </#if>
</div>
