<div class="container">

    <!--<div class="row m-0 p-0" style="height: 500px">
        <div id="testDummy" class="col-6 m-0 p-2"></div>
        <div id="testDummy2" class="col-6 m-0 p-2"></div>
    </div>-->

    <!-- uce corporate data -->
    <div class="mt-5 uce-description">
        <div class="flexed align-items-center justify-content-between">
            <h5 class="color-prime mb-0">${system.getMeta().getName()?trim!"-"}</h5>
            <button class="btn" onclick="$(this).parent().next('.content').toggle(50)">
                <i class="fas fa-info-circle color-prime large-font"></i>
            </button>
        </div>
        <div class="content display-none">
            <hr class="mt-3 mb-3"/>
            <p class="text block-text mb-0 p-2">
                ${system.getMeta().getDescription()!languageResource.get("noCorpusDescription")}
            </p>
        </div>
    </div>

    <div class="corpora-list">
        <h3 class="text-center font-weight-bold text-dark"><i
                    class="color-prime fas fa-database mr-2"></i> ${languageResource.get("corpora")}</h3>
        <div class="row m-0 p-0 ">
            <#if corpora?size == 0>
                <div class="group-box mt-2 bg-ghost">
                    <p class="mb-0 text-center w-100 text">${languageResource.get("noCorpora")}</p>
                </div>
            </#if>
            <#list corpora as corpusVm>
                <div class="col-md-12 m-0 p-3">
                    <div class="corpus-card">
                        <!-- header -->
                        <div class="flexed align-items-center justify-content-between">
                            <div>
                                <h5 class="open-corpus-inspector-btn border-0 w-100 mb-2 color-prime clickable"
                                    data-id="${corpusVm.getCorpus().getId()}">
                                    ${corpusVm.getCorpus().getName()?trim}
                                </h5>
                                <p class="text mb-0 small">${corpusVm.getCorpus().getAuthor()}</p>
                            </div>
                            <div>
                                <a class="btn open-corpus-inspector-btn mb-1" data-trigger="hover"
                                   data-toggle="popover" data-placement="top" data-id="${corpusVm.getCorpus().getId()}"
                                   data-content="${languageResource.get("openCorpus")}">
                                    <i class="fas fa-globe color-prime"></i>
                                </a>
                                <a class="btn light-border flexed clickable align-items-center pl-1 pr-1 mt-1 justify-content-center"
                                   data-trigger="hover"
                                   onclick="$(this).closest('.corpus-card').find('.expanded-content').toggle(75)">
                                    <i class="fas fa-info-circle color-prime"></i>
                                </a>
                            </div>

                        </div>

                        <div class="expanded-content">
                            <hr class="mt-3 mb-1 "/>

                            <!-- content -->
                            <div class="corpus-description small mb-0 p-3">
                                <#if corpusVm.getCorpusConfig().getDescription()?has_content>
                                    ${corpusVm.getCorpusConfig().getDescription()}
                                <#else>
                                    ${languageResource.get("noCorpusDescription")}
                                </#if>
                            </div>
                        </div>
                    </div>
                </div>
            </#list>
        </div>

        <!-- clal to search -->
        <div class="flexed align-items-center justify-content-center mt-3 pb-4">
            <a class="clickable text mb-0 text small ml-1" onclick="navigateToView('search')">
                <i class="fas fa-search mr-1"></i> ${languageResource.get("callForSearch")}
            </a>
        </div>
    </div>

</div>