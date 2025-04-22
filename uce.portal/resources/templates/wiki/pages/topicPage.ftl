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

    <!-- Topic Summary -->
    <div class="text-center mb-3">
        <h3 class="mb-2">Topic: "${vm.getCoveredText()}"</h3>
        <p class="text-muted">
            Top Keywords:
            <#if vm.getTopicTerms()?has_content>
                <#assign terms = vm.getTopicTerms()>
                <#if terms?size gte 1>${terms[0].word}</#if><#if terms?size gte 2>, ${terms[1].word}</#if><#if terms?size gte 3>, ${terms[2].word}...</#if>
            <#else>
                No keywords available
            </#if>
        </p>
    </div>
    <hr class="mt-2 mb-4"/>

    <div class="row m-0 p-0" style="height: 500px; width:100%">
        <div id="document-distribution-container" class="col-6 m-0 p-2"></div>
        <div id="similar-topics-container" class="col-6 m-0 p-2"></div>
    </div>

    <!-- the document this is from -->
    <div class="mt-4 mb-3 w-100 p-0 m-0 justify-content-center flexed align-items-start">
        <div class="document-card w-100">
            <#assign document = vm.getDocument()>
            <#assign searchId = "">
            <#include '*/search/components/documentCardContent.ftl' >
        </div>
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

    window.graphVizHandler.createBasicChart(document.getElementById('document-distribution-container'),
        'Document Distribution of the topic',
        documentDistData,
        'bar',
    );

    window.graphVizHandler.createBasicChart(document.getElementById('similar-topics-container'),
        'Similar topic based on shared words',
        similarTopicsData,
        'polarArea',
    );


</script>

