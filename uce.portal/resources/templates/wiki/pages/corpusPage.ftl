<div class="wiki-page container">

    <!-- breadcrumbs -->
    <div class="mb-3 flexed align-items-center justify-content-between">
        <#include "*/wiki/components/breadcrumbs.ftl">
        <h5 class="mb-0">${vm.getCorpus().getCorpus().getName()}</h5>
    </div>

    <hr class="mt-2 mb-3"/>

    <!-- description -->
    <div class="mt-0 mb-4">
        <h5 class="text-center">Description</h5>
        <div class="mb-0 bg-lightgray p-3 rounded text card-shadow">
            <#if vm.getCorpus().getCorpusConfig().getDescription()?has_content>
                ${vm.getCorpus().getCorpusConfig().getDescription()}
            <#else>
                ${languageResource.get("noCorpusDescription")}
            </#if>
        </div>
    </div>

    <!-- corpusConfig raw -->
    <div class="mt-4 mb-4">
        <div class="corpus-config-json card-shadow">
            <div class="group-box bg-ghost mb-0 flexed align-items-center justify-content-between rounded-0 pt-2 pb-2">
                <h5 class="font-italic mr-1 mb-0">corpusConfig.json</h5>
                <a class="rounded-a ml-1 mb-0 p-0 mt-0 mr-0"
                   onclick="$(this).closest('.corpus-config-json').find('.expanded').toggle(75)">
                    <i class="fas fa-angle-down"></i>
                </a>
            </div>
            <div class="expanded display-none">
                <#assign jsonValueAsIterable = vm.getCorpusConfigJsonAsIterable()>
                <#include "*/document/jsonBeautifier.ftl">
            </div>
        </div>
    </div>

    <!-- corpus inspector -->
    <div class="corpus-inspector">
        <#assign corpus = vm.getCorpus().getCorpus()>
        <#assign corpusConfig = vm.getCorpus().getCorpusConfig()>
        <#assign documentsCount = vm.getDocumentsCount()>
        <h5 class="text-center">Annotations</h5>
        <div class="group-box bg-ghost card-shadow">
            <#include "*/corpus/components/corpusAnnotations.ftl"/>
        </div>
    </div>

</div>

