<div class="wiki-page container">

    <div class="mb-3">
        <#include "*/wiki/components/breadcrumbs.ftl">
    </div>

    <!-- metadata header -->
    <div class="flexed align-items-center justify-content-between pl-1 pr-1">
        <p class="mb-0 mr-2">
            <i class="fas fa-vector-square small-font color-prime mr-1"></i>
            <span class="font-italic">${vm.getAnnotationType()}</span> -
            Annotation
            <span class="text">${vm.getTopicDistribution().getWikiId()}:</span>
            <span class="color-prime font-italic">${vm.getCoveredText()}</span>
        </p>
        <p class="mb-0">${languageResource.get("annotatedInDocument")} <span
                    class="text">${vm.getDocument().getId()}</span></p>
    </div>

    <hr class="mt-2 mb-4"/>

    <div class="row m-0 p-0 align-items-start justify-content-between">
        <div class="col-md-4 p-0 m-0">
            <h6 class="ml-2">
                Keywords
                <#if vm.getPage()?has_content>
                    <span class="text">(${languageResource.get("page")} ${vm.getPage().getPageNumber() + 1})</span>
                </#if>
            </h6>
            <div class="keywords-container card-shadow mb-3">
                <h6 class="color-prime ml-2 mr-1">${vm.getTopicDistribution().getYakeTopicOne()}</h6>
                <h6 class="color-prime ml-2 mr-1">${vm.getTopicDistribution().getYakeTopicTwo()}</h6>
                <h6 class="color-prime ml-2 mr-1">${vm.getTopicDistribution().getYakeTopicThree()}</h6>
                <h6 class="color-prime ml-2 mr-1">${vm.getTopicDistribution().getYakeTopicFour()}</h6>
                <hr class="mt-2 mb-2"/>
                <div class="mb-2">
                    <div onclick="$(this).next().toggle(100)"
                         class="clickable flexed align-items-center ml-2 mr-2 justify-content-between">
                        <label class="mb-0 mr-2 text">Phrases</label>
                        <i class="fas fa-angle-down text"></i>
                    </div>
                    <div class="display-none">
                        <hr class="mt-2 mb-2"/>
                        <h6 class="text-dark ml-2 mr-1 small-font">${vm.getTopicDistribution().getRakeTopicOne()}</h6>
                        <h6 class="text-dark small-font ml-2 mr-1">${vm.getTopicDistribution().getRakeTopicTwo()}</h6>
                        <h6 class="text-dark small-font ml-2 mr-1">${vm.getTopicDistribution().getRakeTopicThree()}</h6>
                    </div>
                </div>
            </div>
        </div>

        <div class="col-md-8 p-0 m-0 justify-content-center">
            <div class="document-card w-100">
                <#assign document = vm.getDocument()>
                <#assign searchId = "">
                <#include '*/search/components/documentCardContent.ftl' >
            </div>
        </div>
    </div>

    <!-- similar topics -->
    <div>
        <h6 class="mb-2 text-center">${languageResource.get("similarAnnotations")}: </h6>
        <div class="flexed align-items-center justify-content-center wrapped similar-topics p-2">
            <#if vm.getSimilarTopicDistributions()?has_content>
                <#list vm.getSimilarTopicDistributions() as topic>
                    <a class=" ml-0 mr-2 mb-0 mb-1 mt-1 small-font text-wrap open-wiki-page clickable"
                       data-wid="${topic.getWikiId()}"
                       data-wcovered="${topic.getYakeTopicOne()}">
                        <i class="fas fa-vector-square color-prime mr-1 small-font"></i> ${topic.getYakeTopicOne()}
                    </a>
                </#list>
            <#else>
                <p class="small text text-center mb-0 w-100">${languageResource.get("noneFound")}</p>
            </#if>
        </div>
    </div>

    <!-- kwic view -->
    <div class="kwic-include mt-4">
        <header class="pb-2 pt-2 pl-3 pr-3">
            <h6 class="mb-0 text-dark">Keyword-in-Context</h6>
        </header>
        <div class="p-2">
            <#assign contextState = vm.getKwicState()>
            <#assign showHeader = false>
            <#include "*/search/components/keywordInContext.ftl">
        </div>
    </div>
</div>
