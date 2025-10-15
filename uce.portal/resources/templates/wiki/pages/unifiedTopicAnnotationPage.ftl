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

    <div class="topic-container">
        <!-- Dropdown Section -->
        <div>
            <h5>Choose a Topic</h5>
            <select id="topicSelect" name="Topics" onchange="showWords()">
                <option value="" disabled selected>Select a topic</option>

                <!-- Sort topics by score in descending order -->
                <#assign sortedTopics = vm.getTopics()?sort_by("score")?reverse>

                <#list sortedTopics as topic>
                    <!-- Sort words inside each topic by probability -->
                    <#assign sortedWords = topic.getWords()?sort_by("probability")?reverse>

                    <option value="${topic.getValue()}"
                            data-words="<#list sortedWords as word>${word.getWord()} (${word.getProbability()}), </#list>">
                        ${topic.getValue()} (Score: ${topic.getScore()})
                    </option>
                </#list>
            </select>
        </div>

        <!-- Words Display Section -->
        <div id="wordsContainer" class="words-container">
            <ul id="wordsList"  class="word-labels"></ul>
        </div>
    </div>

</div>
