<div class="breadcrumbs w-100">
    <div class="flexed align-items-center">
        <a data-trigger="hover" data-toggle="popover" data-placement="top" data-html="true"
           data-id="${vm.getCorpus().getCorpus().getId()}" class="open-corpus-inspector-btn"
           data-content="<p class='text-center mb-0'>@${vm.getCorpus().getCorpus().getAuthor()}</p><hr class='mt-1 mb-1'/> <p class='mb-0 text small-font'>${vm.getCorpus().getCorpusConfig().getDescription()!languageResource.get("noCorpusDescription")}</p>">
            <i class="fas fa-globe mr-1 color-secondary"></i> ${vm.getCorpus().getCorpus().getName()}
        </a>
        <span class="ml-1 mr-1 large-font font-weight-bold">/</span>
        <a class="open-wiki-page" data-wid="${vm.getDocument().getWikiId()}" data-wcovered="">
            <i class="fas fa-book mr-1 color-secondary"></i> ${vm.getDocument().getDocumentTitle()}
        </a>
        <#if vm.getAnnotationType() != "Document">
            <span class="ml-1 mr-1 large-font font-weight-bold">/</span>
            <a>
                <i class="fas fa-vector-square mr-1"></i> ${vm.getCoveredText()}
            </a>
        </#if>
    </div>
</div>