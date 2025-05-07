<div class="wiki-page container">

    <div class="mb-3 flexed align-items-center justify-content-between">
        <div>
            <!-- breadcrumbs -->
            <div class="mb-3">
                <#include "*/wiki/components/breadcrumbs.ftl">
            </div>
            <!-- metadata header -->
            <#include "*/wiki/components/metadata.ftl">
        </div>
        <h5 class="mb-0 font-italic">${vm.getCorpus().getCorpus().getName()}</h5>
    </div>

    <hr class="mt-2 mb-3"/>

    <div class="mt-4 mb-3 card-shadow">
        <!-- corpusConfig raw -->
        <div class="corpus-config-json">
            <div class="group-box bg-ghost mb-0 flexed align-items-center justify-content-between rounded-0 pt-2 pb-2">
                <h6 class="mr-1 mb-0">corpusConfig.json</h6>
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

        <!-- annotations and meta -->
        <div class="annotations-metadata-container">
            <div class="group-box border-top-0 bg-ghost mb-0 flexed align-items-center justify-content-between rounded-0 pt-2 pb-2">
                <h6 class="mb-0 text-center">Annotations and Metadata</h6>
                <a class="rounded-a ml-1 mb-0 p-0 mt-0 mr-0"
                   onclick="$(this).closest('.annotations-metadata-container').find('.expanded').toggle(75)">
                    <i class="fas fa-angle-down"></i>
                </a>
            </div>
            <div class="expanded display-none">
                <div class="corpus-inspector">
                    <#assign corpus = vm.getCorpus().getCorpus()>
                    <#assign corpusConfig = vm.getCorpus().getCorpusConfig()>
                    <#assign documentsCount = vm.getDocumentsCount()>


                    <div class="group-box rounded-0 bg-lightgray card-shadow mb-0 border-bottom-0 border-top-0">
                        <#include "*/corpus/components/corpusMetadata.ftl"/>
                    </div>

                    <div class="group-box bg-ghost rounded-0 card-shadow">
                        <#include "*/corpus/components/corpusAnnotations.ftl"/>
                    </div>
                </div>
            </div>
        </div>
    </div>

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

    <div class="text-center mb-3">
        <h6 class="mb-3">${languageResource.get("corpusWords")}</h6>
        <!-- Topic Word Cloud -->
        <div class="col-md-6 mx-auto">
            <div id="topicWordCloud"></div>
        </div>
    </div>

    <div id="corpus-topic-distribution-container"></div>


    <!-- Documents -->
    <!--<h5 class="text-center">Documents</h5>
    <div class="group-box card-shadow bg-lightgray">
        <div class="corpus-documents-list-include h-100 w-100 position-relative pr-3">
            <div class="simple-loader"></div>
        </div>
    </div>-->

</div>

<script>
    $(document).ready(function () {
        // After that, we load documentsListView
        //loadCorpusDocuments(${vm.getCorpus().getCorpus().getId()}, $('.wiki-page .corpus-documents-list-include'));

        var wordData = [
            <#if vm.getNormalizedTopicWords()?? && vm.getNormalizedTopicWords()?has_content>
            <#list vm.getNormalizedTopicWords() as term>
            {
                "term": "${term.getWord()?js_string}",
                "weight": ${term.getProbability()?c}
            }<#if term_has_next>, </#if>
            </#list>
            </#if>
        ];

        var topicDistData = {
            "labels": [
                <#if vm.getTopicDistributions()?? && vm.getTopicDistributions()?has_content>
                <#list vm.getTopicDistributions()?keys as topicLabel>
                "${topicLabel?js_string}"<#if topicLabel_has_next>, </#if>
                </#list>
                </#if>
            ],
            "data": [
                <#if vm.getTopicDistributions()?? && vm.getTopicDistributions()?has_content>
                <#list vm.getTopicDistributions()?values as weight>
                ${weight?c}<#if weight_has_next>, </#if>
                </#list>
                </#if>
            ],
            "labelName": "Topic Distribution"
        };

        window.graphVizHandler.createWordCloud(document.getElementById('topicWordCloud'), 'Word Cloud', wordData);

        window.graphVizHandler.createBasicChart(document.getElementById('corpus-topic-distribution-container'),
            'Topic Distribution in Corpus',
            topicDistData,
            'pie'
        );
    })
</script>

