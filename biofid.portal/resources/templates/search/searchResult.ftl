<div class="mt-0 search-state" data-id="${searchState.getSearchId()}">

    <div class="header">
        <div class="flexed w-100 align-items-center justify-content-center">
            <button class="btn selected-btn">
                ${languageResource.get("searchResults")} <span class="hits">${searchState.getTotalHits()}</span>
            </button>
        </div>
    </div>

    <#include "*/search/components/foundAnnotationsModal/foundAnnotationsModal.ftl" >

    <div class="row mb-0 mr-0 ml-0 pb-5">

        <div class="col-md-3 position-relative">
            <div class="side-bar-container">
                <div class="side-bar">
                    <div class="content pb-0">
                        <h6 class="text-center underlined mb-4">${languageResource.get("taxonomy")}</h6>
                        <#include "*/search/components/taxonomyTree.ftl">
                    </div>
                </div>
            </div>
        </div>

        <div class="col-md-6">
            <div class="flexed align-items-center justify-content-between sort-container pl-3 pr-3 pt-2 pb-2 mb-3">
                <div class="flexed align-items-center">
                    <a class="btn switch-search-layer-result-btn text hoverable selected" data-layer="METADATA">
                        <i class="fas fa-search mr-1"></i> Meta</a>
                    <#if searchState.getFoundDocumentEmbeddings()?exists>
                        <a class="btn switch-search-layer-result-btn text hoverable" data-layer="EMBEDDING">
                            <i class="fab fa-searchengin mr-1"></i> Embedding</a>
                    </#if>
                </div>

                <div class="flexed">
                    <div class="flexed align-items-center w-100">
                        <p class="mb-0 mr-1">Published</p>
                        <a class="btn m-0 rounded-a small-font sort-btn" data-orderby="published" data-curorder="ASC">
                            <i class="fas fa-sort-amount-up"></i>
                        </a>
                    </div>
                    <div class="ml-2 flexed align-items-center w-100 justify-content-between">
                        <p class="mb-0 mr-1">${languageResource.get("title")}</p>
                        <a class="btn m-0 rounded-a small-font sort-btn active-sort-btn" data-orderby="title"
                           data-curorder="ASC">
                            <i class="fas fa-sort-amount-up"></i>
                        </a>
                    </div>
                </div>
            </div>

            <div>
                <#include "*/search/components/loader.ftl" >
                <div class="document-list-include list" data-layer="METADATA">
                    <#include "*/search/components/documentList.ftl" >
                </div>
                <#if searchState.getFoundDocumentEmbeddings()?exists>
                    <div class="embedding-document-list-include list display-none" data-layer="EMBEDDING">
                        <#list searchState.getFoundDocumentEmbeddings() as documentEmbedding>
                            <#assign document = documentEmbedding.getDocument()>
                            <#assign embedding = documentEmbedding.getDocumentEmbedding()>

                            <div class="document-card">
                                <div class="content">
                                    <#include '*/search/components/documentCardContent.ftl' >
                                </div>
                                <div class="mt-3">
                                    <p class="m-0 text-center w-100">
                                        <i class="color-secondary fas fa-vector-square"></i> Embedding
                                    </p>
                                    <p class="embedding-text text mt-2">
                                        ${embedding.getCoveredText()}
                                    </p>
                                </div>
                            </div>
                        </#list>
                    </div>
                </#if>
            </div>
        </div>

        <div class="col-md-3 position-relative">
            <div class="side-bar-container">
                <div class="side-bar">
                    <div class="content">

                        <h6 class="text-center underlined">Named-Entities</h6>

                        <div class="annotations">
                            <#include "*/search/components/annotations.ftl" >
                        </div>

                        <hr/>

                        <h6 class="text-center mb-3 underlined">Navigation</h6>

                        <div class="navigation-include">
                            <#include "*/search/components/navigation.ftl">
                        </div>

                    </div>
                </div>
            </div>
        </div>
    </div>

</div>