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

    <!-- Lemma information -->
    <div class="mt-1 mb-3 lemma-properties-container">
        <#assign lemma = vm.getWikiModel()>
        <div class="row m-0 p-0">
            <#list lemma.loopThroughProperties() as propertyPair>
                <div class="col-md-4 p-0 m-0 custom-col">
                    <div class="pl-2 pr-2 pb-1 pt-1 flexed align-items-center justify-content-between">
                        <label class="small-font mb-0 mr-1">
                            ${propertyPair.getLeft()}:
                        </label>
                        <label class="small-font text mb-0 ml-1">
                            ${propertyPair.getRight()}
                        </label>
                    </div>
                </div>
            </#list>
        </div>
    </div>

    <!-- the document this is from -->
    <div class="mt-4 mb-3 w-100 p-0 m-0 justify-content-center flexed align-items-start">
        <div class="document-card w-100">
            <#assign document = vm.getDocument()>
            <#assign searchId = "">
            <#include '*/search/components/documentCardContent.ftl' >
        </div>
    </div>

    <!-- kwic view -->
    <div class="mt-3">
        <#include "*/wiki/components/kwic.ftl">
    </div>

</div>
