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


    <!-- the document this is from -->
    <div class="mt-4 mb-3 w-100 p-0 m-0 justify-content-center flexed align-items-start">
        <div class="document-card w-100">
            <#assign document = vm.getDocument()>
            <#assign searchId = "">
            <#include '*/search/components/documentCardContent.ftl' >
        </div>
    </div>

    <#list vm.getMatchingTopics() as topic>
        <div class="topic-value-base-spans">
            <p>
                ${vm.getHighlightedText(topic)}
            </p>
            <div class="topic-metadata">
                Doc ID: ${topic.getDocument().getId()} |
                <#if vm.hasScore()>
                    Relevance: ${vm.getTopic().getScore()?string("0.00")}
                <#else>
                    Relevance: N/A
                </#if>
            </div>
        </div>
    </#list>

</div>
