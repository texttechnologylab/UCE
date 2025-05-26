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

    <!-- list the lemmas that form this named entity -->
    <div class="mt-3 mb-3">
        <#assign neType = "GeoName">
        <#include "*/wiki/components/lemmaTree.ftl">
    </div>

    <!-- the document this is from -->
    <div class="mt-4 mb-3 w-100 p-0 m-0 justify-content-center flexed align-items-start">
        <div class="document-card w-100">
            <#assign document = vm.getDocument()>
            <#assign searchId = "">
            <#assign reduced = true>
            <#include '*/search/components/documentCardContent.ftl' >
        </div>
    </div>

    <!-- map -->
    <div class="mt-2 mb-4">
        <div class="group-box card-shadow w-100 h-100 m-0 p-0">
            <div class="w-100 bg-lightgray pb-2 pt-2 pr-3 pl-3"><label class="text-dark mb-0">Map</label></div>
            <div class="w-100 uce-map-container" style="height: 400px"></div>
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

    <!-- kwic view -->
    <div class="mt-3">
        <#include "*/wiki/components/kwic.ftl">
    </div>

    <!-- similar documents as in the sense of where this ne also exists -->
    <#if (vm.getSimilarDocuments()?has_content) && (vm.getSimilarDocuments()?size > 0)>
        <div class="mt-4">
            <#assign similarDocuments = vm.getSimilarDocuments()>
            <h6 class="text-center">${languageResource.get("foundInDocuments")}</h6>
            <div class="similar-documents">
                <div class="row m-0 p-0">
                    <#list similarDocuments as document>
                        <div class="col-md-4 m-0 p-2">
                            <div class="item">
                                <div class="p-2">
                                    <h6 data-wid="${document.getWikiId()}" data-wcovered=""
                                        class="open-wiki-page color-prime mb-0 text-center flexed align-items-center justify-content-center">
                                        ${document.getDocumentTitle()}
                                    </h6>
                                </div>
                                <hr class="mb-1 mt-1"/>
                                <div class="p-1">
                                    <p class="font-italic mb-0 small-font block-text text normal-line-height">
                                        ${document.getFullTextSnippet(30)}...
                                    </p>
                                </div>
                            </div>
                        </div>
                    </#list>
                </div>
            </div>
        </div>
    </#if>
</div>

<script>
    $(document).ready(function () {
        const map = window.graphVizHandler.createUceMap($('.wiki-page .uce-map-container').get(0), true);

        const marker = {
            lat: ${vm.getWikiModel().getLatitude()?c},
            lng: ${vm.getWikiModel().getLongitude()?c},
            label: "${vm.getWikiModel().getName()?js_string}"
        };

        map.placeNodes([marker]);
        map.translateTo(marker);
    });
</script>

