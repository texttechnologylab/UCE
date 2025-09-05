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

    <div id="document-topic-word-cloud-container" class="col-md-8 mx-auto"></div>
    <div id="document-topic-distribution-container"></div>
    <div id="document-similar-documents-container"></div>


    <!-- the document this is from -->
    <div class="mt-4 mb-2 w-100 p-0 m-0 justify-content-center flexed align-items-start">
        <div class="document-card w-100">
            <#assign document = vm.getDocument()>
            <#assign searchId = "">
            <#assign reduced = true>
            <#include '*/search/components/documentCardContent.ftl' >
        </div>
    </div>

    <!-- all permissions of this document -->
    <div class="mt-2 mb-4 w-100 p-0 m-0">
        <h5 class="text-center mb-2">Document Permissions</h5>
        <#assign uceDocumentPermissions = vm.getDocument().getPermissions()!>
        <#include "*/document/documentPermissions.ftl">
    </div>

    <!-- possible metadata of this document -->
    <div class="mt-2 mb-4 w-100 p-0 m-0">
        <h5 class="text-center mb-2">Document Metadata</h5>
        <div class="light-border rounded p-3 bg-light card-shadow">
            <#assign uceMetadata = vm.getUceMetadata()!>
            <#include "*/document/documentUceMetadata.ftl">
        </div>
    </div>

    <!-- linkable space -->
    <div class="mt-2 mb-2">
        <#assign unique = (vm.getWikiModel().getUnique())!"none">
        <#assign height = 500>
        <#if unique != "none">
            <div class="w-100">
                <#include "*/wiki/components/linkableSpace.ftl">
            </div>
        </#if>
    </div>

    <!-- Corpus Universe -->
    <#if vm.getCorpus().getCorpusConfig().getOther().isEnableEmbeddings()>
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
    </#if>

</div>

<script>
    $(document).ready(function () {
        // Upon loading the document annotation page, we want to init a small corpus universe.
        const center = $('.wiki-page').data('center');
        const corpusId = $('.wiki-page').data('corpusid');
        window.wikiHandler.addUniverseToDocumentWikiPage(corpusId, center);

        var topicsData = {
            "labels": [
                <#list vm.getTopicDistribution() as topic>
                "${topic[0]?js_string}"<#if topic_has_next>,</#if>
                </#list>
            ],
            "data": [
                <#list vm.getTopicDistribution() as topic>
                ${topic[1]?c}<#if topic_has_next>,</#if>
                </#list>
            ],
            "labelName": "Topic Weights"
        };

        var wordData = [
            <#list vm.getTopicWords() as wordItem>
            {
                "term": "${wordItem.getWord()?js_string}",
                "weight": ${wordItem.getProbability()?c}
            }<#if wordItem_has_next>,</#if>
            </#list>
        ];

        var similarDocumentsData = {
            "labels": [
                <#list vm.getSimilarDocuments() as item>
                "${item[0]?js_string}"<#if item_has_next>,</#if>
                </#list>
            ],
            "data": [
                <#list vm.getSimilarDocuments() as item>
                ${item[1]?c}<#if item_has_next>,</#if>
                </#list>
            ],
            "labelName": "Shared Words"
        };

        if (topicsData.data.length > 0) {
            window.graphVizHandler.createBasicChart(
                document.getElementById('document-topic-distribution-container'),
                'Topic Distribution',
                topicsData,
                'pie'
            );
        } else {
            document.getElementById('document-topic-distribution-container').style.display = 'none';
        }

        if (wordData.length > 0) {
            window.graphVizHandler.createWordCloud(
                document.getElementById('document-topic-word-cloud-container'),
                "${languageResource.get("documentWords")}",
                wordData
            );
        } else {
            document.getElementById('document-topic-word-cloud-container').style.display = 'none';
        }

        if (similarDocumentsData.data.length > 0) {
            window.graphVizHandler.createBasicChart(
                document.getElementById('document-similar-documents-container'),
                'Similar documents based on shared words',
                similarDocumentsData,
                'polarArea'
            );
        } else {
            document.getElementById('document-similar-documents-container').style.display = 'none';
        }

    })
</script>
