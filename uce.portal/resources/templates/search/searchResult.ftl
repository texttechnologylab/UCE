<div class="mt-0 search-state" data-id="${searchState.getSearchId()}">

    <div class="header">
        <div class="flexed w-100 align-items-center justify-content-center">
            <div class="btn rounded-0 selected-btn" data-trigger="hover" data-toggle="popover" data-placement="top"
                 data-content="${languageResource.get("searchResultsDescription")}">
                ${languageResource.get("searchResults")} <span
                        class="hits">&GreaterEqual; ${searchState.getTotalHits()}</span>
            </div>
        </div>
    </div>

    <#if searchState.hasUceMetadataFilters()>
        <div id="search-results-visualization-container">
            <div class="group-box bg-ghost card-shadow w-100">
                <div class="flexed align-items-center justify-content-between clickable"
                     onclick="$(this).closest('.group-box').find('.expanded').fadeToggle(75)">
                    <h6 class="mb-0 w-100">${languageResource.get("searchVisualizationSummaryTitle")}</h6>
                    <a class="rounded-a"><i class="far fa-chart-bar"></i></a>
                </div>

                <div class="expanded display-none mt-3">
                    <#include "*/search/components/searchVisualization.ftl" >
                </div>
            </div>
        </div>
    </#if>

    <div class="row mb-0 mr-0 ml-0 pb-5">

        <div class="col-lg-3 position-relative search-row" data-type="left">
            <div class="side-bar-container">
                <div class="side-bar">

                    <div class="content">
                        <#assign contextState = searchState.getKeywordInContextState()!"">
                        <div class="keyword-in-context-include">
                            <#include "*/search/components/keywordInContext.ftl">
                        </div>

                        <div class="pb-0 taxonomy-tree-include display-none">
                            <hr class="mt-3 mb-3"/>
                            <h6 class="text-center underlined mb-4">${languageResource.get("taxonomy")}</h6>
                            <#include "*/search/components/taxonomyTree.ftl">
                        </div>

                    </div>

                </div>
            </div>
        </div>

        <div class="col-lg-6 search-row" data-type="mid">

            <div class="sort-container pl-3 pr-3 pt-2 pb-2 mb-3">

                <!-- enriched search tokens if they exist -->
                <#if searchState.getEnrichedSearchTokens()?has_content && searchState.getEnrichedSearchTokens()?size gt 0>
                    <div class="mt-2 enriched-search-tokens-list">
                        <label class="query-input display-none">
                            ${searchState.getEnrichedSearchQuery()}
                        </label>
                        <!-- header -->
                        <div class="flexed align-items-baseline mb-2 justify-content-center">
                            <p class="text-center mb-0">Enriched Search Query</p>
                            <a class="rounded-a ml-2 mb-0 light-border" style="height: 25px; width: 25px"
                               onclick="openInExpandedTextView('Enriched Search Query', $(this).closest('.enriched-search-tokens-list').find('.query-input').html())">
                                <i class="fas fa-eye small-font color-prime"></i>
                            </a>
                        </div>
                        <div class="flexed align-items-center justify-content-around overflow-x">
                            <#list searchState.getEnrichedSearchTokens() as token>
                                <#assign width = "">
                                <#if token.getChildren()?has_content && token.getChildren()?size gt 0>
                                    <#assign width = "w-100">
                                </#if>
                                <div class="enriched-token ml-1 mr-1 ${width}" data-type="${token.getType().name()}">
                                    <div class="flexed align-items-center justify-content-between">
                                        <label class="mb-0 text-dark font-italic ml-1 mr-1 text-center w-100 clickable hoverable value"
                                               onclick="$(this).closest('.enriched-token').find('.expanded-content').toggle(75)">
                                            <span>${token.getValue()}</span><span
                                                    class="ml-1 xsmall-font text">(${token.getType().name()})</span>
                                        </label>
                                    </div>
                                    <#if width == "w-100">
                                        <div class="display-none expanded-content">
                                            <hr class="mt-1 mb-2 bg-lightgray"/>
                                            <div class="w-100 children-container">
                                                <label class="mb-0 text">( </label>
                                                <#list token.getChildren() as child>
                                                    <label class="mb-0 text small-font block-text">'${child.getValue()}'
                                                        | </label>
                                                </#list>
                                                <label class="mb-0 text"> ) </label>
                                            </div>
                                        </div>
                                    </#if>
                                </div>
                            </#list>
                        </div>
                        <#if searchState.isEnrichedSearchQueryIsCutoff()>
                            <div class="mb-0 mt-2 alert alert-warning ml-1 mr-1 pt-1 pb-1 pl-2 pr-2 text-center light-border flexed align-items-center justify-content-between">
                                <i class="fas fa-exclamation-circle"></i>
                                <label class="small-font mb-0">${languageResource.get("enrichmentCutoffTitle")}</label>
                                <i class="fas fa-question-circle"
                                   data-trigger="hover" data-toggle="popover" data-placement="top"
                                   data-content="${languageResource.get("enrichmentCutoffDescription")}"></i>
                            </div>
                        </#if>
                    </div>
                    <hr class="mt-3 mb-3 bg-lightgray"/>
                </#if>

                <!-- search layers and such -->
                <div class="flexed align-items-center justify-content-between">
                    <div class="flexed align-items-center">
                        <a class="btn switch-search-layer-result-btn text hoverable selected"
                           data-layer="${searchState.getPrimarySearchLayer()}">
                            <i class="fas fa-search mr-1"></i> ${searchState.getPrimarySearchLayer()}</a>
                        <#if searchState.getFoundDocumentChunkEmbeddings()?exists>
                            <a class="btn switch-search-layer-result-btn text hoverable" data-layer="EMBEDDING">
                                <i class="fab fa-searchengin mr-1"></i> Embedding</a>
                        </#if>
                    </div>

                    <div class="flexed">
                        <div class="flexed align-items-center w-100">
                            <p class="mb-0 mr-1">Published</p>
                            <a class="btn m-0 rounded-a small-font sort-btn" data-orderby="published"
                               data-curorder="ASC">
                                <i class="fas fa-sort-amount-up"></i>
                            </a>
                        </div>
                        <div class="ml-2 flexed align-items-center w-100 justify-content-between">
                            <p class="mb-0 mr-1">${languageResource.get("title")}</p>
                            <a class="btn m-0 rounded-a small-font sort-btn" data-orderby="documenttitle"
                               data-curorder="ASC">
                                <i class="fas fa-sort-amount-up"></i>
                            </a>
                        </div>
                        <div class="ml-2 flexed align-items-center w-100 justify-content-between"
                             data-trigger="hover" data-toggle="popover" data-placement="top"
                             data-content="${languageResource.get("searchRankDescription")}">
                            <p class="mb-0 mr-1">${languageResource.get("relevancy")}</p>
                            <a class="btn m-0 rounded-a small-font sort-btn active-sort-btn"
                               data-orderby="rank" data-curorder="ASC">
                                <i class="fas fa-sort-amount-up"></i>
                            </a>
                        </div>
                    </div>
                </div>
            </div>

            <div>
                <#include "*/search/components/loader.ftl" >
                <div class="document-list-include list" data-layer="${searchState.getPrimarySearchLayer()}">
                    <#include "*/search/components/documentList.ftl" >
                </div>
                <#if searchState.getFoundDocumentChunkEmbeddings()?exists>
                    <div class="embedding-document-list-include list display-none" data-layer="EMBEDDING">
                        <#list searchState.getFoundDocumentChunkEmbeddings() as documentChunkEmbedding>
                            <#assign document = documentChunkEmbedding.getDocument()>
                            <#assign embedding = documentChunkEmbedding.getDocumentChunkEmbedding()>
                            <#assign documentIdx = 999999>
                            <#assign searchId = searchState.getSearchId()>

                            <div class="document-card">
                                <div class="content">
                                    <#include '*/search/components/documentCardContent.ftl' >
                                </div>
                                <div class="mt-0 pl-3 pr-3 pb-3 pt-1">
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

        <div class="col-lg-3 search-row position-relative" data-type="right">
            <div class="side-bar-container">
                <div class="side-bar">
                    <div class="content">

                        <!-- the search tokens we finally took -->
                        <div>
                            <div class="ml-1 mb-1 enriched-search-tokens flexed align-items-center h-100">
                                <i class="fas fa-binoculars mr-2" data-trigger="hover"
                                   data-toggle="popover" data-placement="top"
                                   data-content="${languageResource.get("finalSearchTokens")}"></i>
                                <div class="flexed wrapped mb-0 h-100">
                                    <span class="mr-1 mb-1 small-font text-dark p-1 search-token">
                                        <#if searchState.getSearchQuery()?has_content>
                                            ${searchState.getSearchQuery()}
                                        </#if>
                                    </span>
                                </div>
                            </div>
                        </div>

                        <div class="annotations">
                            <#include "*/search/components/annotations.ftl" >
                        </div>

                        <hr/>

                        <h6 class="text-center mb-3 underlined">Navigation</h6>

                        <div class="navigation-include">
                            <#include "*/search/components/navigation.ftl">
                        </div>

                        <#if corpusVm.getCorpusConfig().getOther().isEnableEmbeddings()>
                            <div id="search-corpus-universe-include">
                                <a class="open-corpus-universe-btn">
                                    <i class="fas fa-external-link-alt"></i>
                                </a>
                                <div id="search-universe-container" class="corpus-universe-container bg-light">
                                </div>
                            </div>
                        </#if>

                    </div>
                </div>

            </div>
        </div>
    </div>

</div>