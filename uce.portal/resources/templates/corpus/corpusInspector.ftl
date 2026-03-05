<style>
    <#include "*/css/corpus-inspector.css">
</style>
<div class="row m-0 p-0 w-100">

    <!-- corpus statistics -->
    <div class="col-md-4 p-0 m-0">
        <#if (uceConfig.settings.ui.corpusInspector.showHeader)!true>
            <div class="cheader w-100 flexed align-items-center p-4">
                <div class="cheader-side">
                    <a class="btn" onclick="$('.corpus-inspector-include').hide(150)">
                        <i class="fas fa-long-arrow-alt-left m-0 color-prime"></i>
                    </a>
                </div>
                <div class="cheader-center text-center">
                    <h5 class="mb-1 color-prime">${corpus.getName()}</h5>
                    <hr class="mt-0 mb-1 text"/>
                    <p class="text mb-0 font-italic">${languageResource.get("corpusInspector")}</p>
                </div>
                <div class="cheader-side">
                    <#if (uceConfig.settings.ui.mainPage.showWikiModal)!true>
                        <a class="w-rounded-btn open-wiki-page" data-wid="${corpus.getWikiId()}" data-wcovered="${corpus.getName()}">
                            <i class="fab fa-wikipedia-w m-0 color-prime large-font"></i>
                        </a>
                    </#if>
                </div>
            </div>
        </#if>

        <div class="ccontent">

            <!-- Meta -->
            <#if (uceConfig.settings.ui.corpusInspector.showMeta)!true>
                <h6 class="large-font mt-2 mb-2 text-center color-prime">Meta</h6>
                <div class="group-box">
                    <div>
                        <#include "*/corpus/components/corpusMetadata.ftl" />
                    </div>
                </div>
            </#if>

            <!-- Annotations -->
            <#if (uceConfig.settings.ui.corpusInspector.showAnnotations)!true>
                <h6 class="large-font mb-2 mt-4 text-center color-prime">Annotations</h6>
                <div class="group-box">
                    <#include "*/corpus/components/corpusAnnotations.ftl"/>
                </div>
            </#if>
        </div>
    </div>

    <!-- corpus documents -->
    <#if (uceConfig.settings.ui.corpusInspector.showDocuments)!true>
        <div class="col-md-8 w-100 m-0 p-0 border-left position-relative">
            <div class="documents-list-header card-shadow bg-default">
                <div class="flexed w-100 justify-content-between pl-3 pr-3">
                    <h6 class="mb-0 mr-1">${languageResource.get("corpusDocuments")}</h6>
                    <#if (uceConfig.settings.ui.corpusInspector.showSearchHint)!true>
                        <a class="clickable color-prime mb-0 text small ml-1" onclick="navigateToView('search')">
                            <i class="fas fa-search mr-1"></i> ${languageResource.get("callForSearch")}
                        </a>
                    </#if>
                </div>
            </div>
            <div class="corpus-documents-list-include h-100 w-100 position-relative pr-3">
                <div class="simple-loader"><h5 class="mb-0 text">Loading...</h5></div>
            </div>
        </div>
    </#if>

</div>
