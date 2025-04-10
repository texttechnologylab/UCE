<style>
    <#include "*/css/corpus-inspector.css">
</style>
<div class="row m-0 p-0 w-100">

    <!-- corpus statistics -->
    <div class="col-md-4 p-0 m-0">
        <div class="cheader w-100 flexed align-items-center justify-content-between p-4">
            <a class="btn" onclick="$('.corpus-inspector-include').hide(150)">
                <i class="fas fa-long-arrow-alt-left m-0 color-prime"></i>
            </a>
            <div class="text-center">
                <h5 class="mb-1 color-prime">${corpus.getName()}</h5>
                <hr class="mt-0 mb-1 text"/>
                <p class="text mb-0 font-italic">${languageResource.get("corpusInspector")}</p>
            </div>
            <a class="w-rounded-btn open-wiki-page" data-wid="${corpus.getWikiId()}" data-wcovered="${corpus.getName()}">
                <i class="fab fa-wikipedia-w m-0 color-prime large-font"></i>
            </a>
        </div>

        <div class="ccontent">

            <!-- Meta -->
            <h6 class="large-font mt-2 mb-2 text-center color-prime">Meta</h6>
            <div class="group-box">
                <div>
                    <#include "*/corpus/components/corpusMetadata.ftl" />
                </div>
            </div>

            <!-- Annotations -->
            <h6 class="large-font mb-2 mt-4 text-center color-prime">Annotations</h6>
            <div class="group-box">
                <#include "*/corpus/components/corpusAnnotations.ftl"/>
            </div>
        </div>
    </div>

    <!-- corpus documents -->
    <div class="col-md-8 w-100 m-0 p-0 border-left position-relative">
        <div class="documents-list-header card-shadow bg-default">
            <div class="flexed w-100 justify-content-between pl-3 pr-3">
                <h6 class="mb-0 mr-1">${languageResource.get("corpusDocuments")}</h6>
                <a class="clickable color-prime mb-0 text small ml-1" onclick="navigateToView('search')">
                    <i class="fas fa-search mr-1"></i> ${languageResource.get("callForSearch")}
                </a>
            </div>
        </div>
        <div class="corpus-documents-list-include h-100 w-100 position-relative pr-3">
            <div class="simple-loader"></div>
        </div>
    </div>

</div>