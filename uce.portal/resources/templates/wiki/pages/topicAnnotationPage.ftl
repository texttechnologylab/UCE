<div class="wiki-page container">

    <div class="mb-4">
        <#include "*/wiki/components/breadcrumbs.ftl">
    </div>

    <div class="row m-0 p-0 align-items-start justify-content-between">
        <div class="col-md-4 p-0 m-0">
            <h5 class="ml-2">Keywords</h5>
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
        <div class="col-md-8 p-0 m-0">
            <div class="document-card w-100">
                <#assign document = vm.getDocument()>
                <#assign searchId = "">
                <#include '*/search/components/documentCardContent.ftl' >
            </div>
        </div>
    </div>
</div>