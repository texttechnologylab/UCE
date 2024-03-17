<div class="mt-0 search-state" data-id="${searchState.getSearchId()}">

    <div class="header">
        <div class="flexed w-100 align-items-center justify-content-center">
            <button class="btn selected-btn">
                Suchergebnisse <span class="hits">${searchState.getTotalHits()}</span>
            </button>
        </div>
    </div>

    <div class="row mb-0 mr-0 ml-0 pb-5">

        <div class="col-md-3 position-relative">
            <div class="side-bar-container">
                <div class="side-bar">
                    <div class="content pb-0">
                        <h6 class="text-center underlined mb-4">Taxonomie</h6>
                        <#include "*/search/components/taxonomyTree.ftl">
                    </div>
                </div>
            </div>
        </div>


        <div class="col-md-6">
            <div class="flexed align-items-center justify-content-between sort-container pl-3 pr-3 pt-2 pb-2 mb-3">
                <h6 class="m-0">Sortierung</h6>
                <div class="flexed">
                    <div class="flexed align-items-center w-100">
                        <p class="mb-0 mr-1">Published</p>
                        <a class="btn m-0 rounded-a small-font sort-btn" data-orderby="published" data-curorder="ASC">
                            <i class="fas fa-sort-amount-up"></i>
                        </a>
                    </div>
                    <div class="ml-2 flexed align-items-center w-100 justify-content-between">
                        <p class="mb-0 mr-1">Titel</p>
                        <a class="btn m-0 rounded-a small-font sort-btn active-sort-btn" data-orderby="title"
                           data-curorder="ASC">
                            <i class="fas fa-sort-amount-up"></i>
                        </a>
                    </div>
                </div>
            </div>

            <div>
                <#include "*/search/components/loader.ftl" >
                <div class="document-list-include">
                    <#include "*/search/components/documentList.ftl" >
                </div>
            </div>
        </div>

        <div class="col-md-3 position-relative">
            <div class="side-bar-container">
                <div class="side-bar">
                    <div class="content">

                        <h6 class="text-center underlined">Eigennamen</h6>

                        <#include "*/search/components/annotations.ftl" >

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