<div class="wiki-page container" data-corpusid="${vm.getCorpus().getCorpus().getId()}"
     data-center="[0.0,0.0,0.0]">

    <!-- breadcrumbs -->
    <div class="mb-3">
        <#include "*/wiki/components/breadcrumbs.ftl">
    </div>

    <!-- metadata header -->
    <div>
        <#include "*/wiki/components/metadata.ftl">
    </div>

    <hr class="mt-2 mb-4"/>

    <!-- the document this is from -->
    <div class="mt-4 mb-2 w-100 p-0 m-0 justify-content-center flexed align-items-start">
        <div class="document-card w-100">
            <#assign document = vm.getDocument()>
            <#assign searchId = "">
            <#include '*/search/components/documentCardContent.ftl' >
        </div>
    </div>

    <!-- possible metadata of this document -->
    <div class="mt-2 mb-4 w-100 p-0 m-0">
        <h5 class="text-center mb-2">Metadata</h5>
        <div class="light-border rounded p-3 bg-light card-shadow">
            <#assign uceMetadata = vm.getUceMetadata()>
            <#include "*/document/documentUceMetadata.ftl">
        </div>
    </div>

    <!-- Corpus Universe -->
    <div class="mt-3 mb-4">
        <h6 class="text-dark text-center w-100 mb-2">
            ${languageResource.get("documentInSemanticSpace")}
        </h6>
        <div id="wiki-corpus-universe-include">
            <a class="open-corpus-universe-btn">
                <i class="fas fa-external-link-alt"></i>
            </a>
            <div id="wiki-universe-container" class="corpus-universe-container bg-light">
            </div>
        </div>
    </div>

</div>

<script>
    $(document).ready(function () {
        // Upon loading the document annotation page, we want to init a small corpus universe.
        const center = $('.wiki-page').data('center');
        const corpusId = $('.wiki-page').data('corpusid');
        window.wikiHandler.addUniverseToDocumentWikiPage(corpusId, center);
    })
</script>
