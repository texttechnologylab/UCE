<div class="wiki-page container">

    <div class="mb-3">
        <#include "*/wiki/components/breadcrumbs.ftl">
    </div>

    <!-- metadata header -->
    <div>
        <#include "*/wiki/components/metadata.ftl">
    </div>

    <hr class="mt-2 mb-4"/>

    <!-- special for topic annotation -->
    <div class="row m-0 p-0 align-items-start justify-content-between">
        <div class="col-md-4 p-0 m-0">
            <h6 class="ml-2">
                Keywords
                <#if vm.getPage()?has_content>
                    <span class="text">(${languageResource.get("page")} ${vm.getPage().getPageNumber() + 1})</span>
                </#if>
            </h6>
            <div class="keywords-container card-shadow mb-3">
                <h6 class="color-prime ml-2 mr-1">${vm.getKeywordDistribution().getYakeTopicOne()}</h6>
                <h6 class="color-prime ml-2 mr-1">${vm.getKeywordDistribution().getYakeTopicTwo()}</h6>
                <h6 class="color-prime ml-2 mr-1">${vm.getKeywordDistribution().getYakeTopicThree()}</h6>
                <h6 class="color-prime ml-2 mr-1">${vm.getKeywordDistribution().getYakeTopicFour()}</h6>
                <hr class="mt-2 mb-2"/>
                <div class="mb-2">
                    <div onclick="$(this).next().toggle(100)"
                         class="clickable flexed align-items-center ml-2 mr-2 justify-content-between">
                        <label class="mb-0 mr-2 text">Phrases</label>
                        <i class="fas fa-angle-down text"></i>
                    </div>
                    <div class="display-none">
                        <hr class="mt-2 mb-2"/>
                        <h6 class="text-dark ml-2 mr-1 small-font">${vm.getKeywordDistribution().getRakeTopicOne()}</h6>
                        <h6 class="text-dark small-font ml-2 mr-1">${vm.getKeywordDistribution().getRakeTopicTwo()}</h6>
                        <h6 class="text-dark small-font ml-2 mr-1">${vm.getKeywordDistribution().getRakeTopicThree()}</h6>
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
            <#if vm.getSimilarKeywordDistributions()?has_content>
                <#list vm.getSimilarKeywordDistributions() as topic>
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
    <div class="mt-4">
        <#include "*/wiki/components/kwic.ftl">
    </div>
</div>
