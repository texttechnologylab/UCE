<div class="wiki-page container">

    <!-- breadcrumbs -->
    <div class="mb-3">
        <#include "*/wiki/components/breadcrumbs.ftl">
    </div>

    <!-- metadata header -->
    <div>
        <#include "*/wiki/components/metadata.ftl">
    </div>

    <hr class="mt-2 mb-4"/>

    <div id="topic-word-cloud-container" class="col-md-8 mx-auto"></div>
    <div id="topic-document-distribution-container"></div>
    <div id="topic-similar-topics-container"></div>

    <!-- the document this is from -->
    <div class="mt-4 mb-3 w-100 p-0 m-0 justify-content-center flexed align-items-start">
        <div class="document-card w-100">
            <#assign document = vm.getDocument()>
            <#assign searchId = "">
            <#assign reduced = true>
            <#include '*/search/components/documentCardContent.ftl' >
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


</div>

<script>

    var wordData = [
        <#list vm.getWordCloudData() as term>
        {
            "term": "${term.term?js_string}",
            "weight": ${term.weight?c}
        }<#if term_has_next>,</#if>
        </#list>
    ];

    var documentDistData = {
        "labels": [
            <#list vm.getDocumentDistributionData() as item>
            "${item.documentTitle?js_string}"<#if item_has_next>,</#if>
            </#list>
        ],
        "data": [
            <#list vm.getDocumentDistributionData() as item>
            ${item.weight?c}<#if item_has_next>,</#if>
            </#list>
        ],
        "labelName": "Topic Weights"
    };

    var similarTopicsData = {
        "labels": [
            <#list vm.getSimilarTopicsData() as item>
            "${item.topic?js_string}"<#if item_has_next>,</#if>
            </#list>
        ],
        "data": [
            <#list vm.getSimilarTopicsData() as item>
            ${item.overlap?c}<#if item_has_next>,</#if>
            </#list>
        ],
        "labelName": "Shared Words"
    };

    if (documentDistData.data.length > 0) {
        window.graphVizHandler.createBasicChart(document.getElementById('topic-document-distribution-container'),
            'Document Distribution of the topic',
            documentDistData,
            'bar',
        );
    } else {
        document.getElementById('topic-document-distribution-container').style.display = 'none';
    }

    if (similarTopicsData.data.length > 0) {
        window.graphVizHandler.createBasicChart(document.getElementById('topic-similar-topics-container'),
            'Similar topic based on shared words',
            similarTopicsData,
            'polarArea',
        );
    } else {
        document.getElementById('topic-similar-topics-container').style.display = 'none';
    }

    if (wordData.length > 0) {
        window.graphVizHandler.createWordCloud(document.getElementById('topic-word-cloud-container'), "${languageResource.get("topicWords")}", wordData);
    } else {
        document.getElementById('topic-word-cloud-container').style.display = 'none';
    }



</script>

