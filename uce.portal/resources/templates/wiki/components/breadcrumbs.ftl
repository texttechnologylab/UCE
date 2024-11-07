<div class="breadcrumbs w-100">
    <div class="flexed align-items-center">
        <a data-trigger="hover" data-toggle="popover" data-placement="top" data-html="true"
           data-content="<p class='text-center mb-0'>@${vm.getCorpus().getCorpus().getAuthor()}</p><hr class='mt-1 mb-1'/> <p class='mb-0 text small-font'>${vm.getCorpus().getCorpusConfig().getDescription()!languageResource.get("noCorpusDescription")}</p>">
            ${vm.getCorpus().getCorpus().getName()}
        </a>
        <span class="ml-1 mr-1 large-font font-weight-bold">/</span>
        <a>${vm.getDocument().getDocumentTitle()}</a>
    </div>
</div>