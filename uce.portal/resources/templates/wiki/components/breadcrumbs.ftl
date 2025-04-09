
<div class="breadcrumbs w-100">
    <div class="flexed align-items-center">
        <a data-trigger="hover" data-toggle="popover" data-placement="top" data-html="true" class="open-wiki-page"
           data-wid="${vm.getCorpus().getCorpus().getWikiId()}" data-wcovered="${vm.getCorpus().getCorpus().getName()}"
           data-content="<p class='text-center mb-0'>@${vm.getCorpus().getCorpus().getAuthor()}</p>">
            <i class="fas fa-globe mr-2 color-secondary"></i> ${vm.getCorpus().getCorpus().getName()}
        </a>
        <#if vm.getAnnotationType() != "Corpus">
            <span class="ml-1 mr-1 large-font font-weight-bold">/</span>
            <a class="open-wiki-page" data-wid="${vm.getDocument().getWikiId()}" data-wcovered="">
                <i class="fas fa-book mr-2 color-secondary"></i> ${vm.getDocument().getDocumentTitle()}
            </a>
        </#if>
        <#if vm.getAnnotationType() != "Document" && vm.getAnnotationType() != "Corpus">
            <span class="ml-1 mr-1 large-font font-weight-bold">/</span>
            <a>
                <i class="fas fa-vector-square mr-2"></i> ${vm.getCoveredText()}
            </a>
        </#if>
    </div>
</div>
