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
        <h4 class="mb-3">Topic: ${vm.getCoveredText()}</h4>
        <!-- Topic Word Cloud -->
        <div class="col-md-6 mx-auto">
            <div id="topicWordCloud"></div>
        </div>
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

    drawTopicWordCloud('topicWordCloud', wordData);


    function drawTopicWordCloud(elementId, wordData) {
        console.log('Drawing topic word cloud:', elementId, wordData);

        if (!wordData || !Array.isArray(wordData) || wordData.length === 0) {
            console.error('Invalid data provided to drawTopicWordCloud:', wordData);
            return;
        }

        const container = document.getElementById(elementId);
        if (!container) {
            console.error("Element with id " + elementId + " not found");
            return;
        }

        container.innerHTML = '';

        const cloudContainer = document.createElement('div');
        cloudContainer.className = 'word-cloud-container';

        const maxWeight = Math.max(...wordData.map(item => item.weight));
        const minWeight = Math.min(...wordData.map(item => item.weight));
        const weightRange = maxWeight - minWeight;

        wordData.forEach(item => {
            const word = document.createElement('div');
            word.className = 'word-cloud-item';
            word.textContent = item.term;

            const normalizedWeight = weightRange === 0 ? 1 : (item.weight - minWeight) / weightRange;
            const fontSize = 12 + (normalizedWeight * 24);
            word.style.fontSize = fontSize + "px";
            word.style.color = getColorForWeight(normalizedWeight);

            word.addEventListener('mouseover', () => {
                word.classList.add('hovered');
                const tooltip = document.createElement('div');
                tooltip.className = 'word-tooltip';
                tooltip.textContent = "Weight: " + item.weight.toFixed(4);
                document.body.appendChild(tooltip);

                const rect = word.getBoundingClientRect();
                tooltip.style.top = (rect.top + word.offsetHeight + 50) + "px";
                tooltip.style.left = (rect.left + (word.offsetWidth / 5)) + "px";
                tooltip.style.transform = 'translateX(-50%)';
                word._tooltip = tooltip;
            });

            word.addEventListener('mouseout', () => {
                word.classList.remove('hovered');
                if (word._tooltip) {
                    word._tooltip.remove();
                    word._tooltip = null;
                }
            });

            cloudContainer.appendChild(word);
        });

        container.appendChild(cloudContainer);
    }

    function getColorForWeight(weight) {
        const r = Math.floor(50 + (weight * 205));
        const g = Math.floor(50 + ((1 - weight) * 150));
        const b = Math.floor(150 + ((1 - weight) * 105));
        return "rgb(" + r + ", " + g + ", " + b + ")";
    }

</script>

