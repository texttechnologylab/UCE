<div class="container">

    <!-- uce corporate data -->
    <div class="mt-5 uce-description">
        <div class="flexed align-items-center justify-content-between">
            <h6 class="color-prime mb-0">${system.getMeta().getName()!"-"}</h6>
            <button class="btn" onclick="$(this).parent().next('.content').toggle(100)">
                <i class="fas fa-info-circle color-secondary large-font"></i>
            </button>
        </div>
        <div class="content display-none">
            <hr class="mt-3 mb-3"/>
            <p class="font-italic small text mb-0">
                ${system.getMeta().getDescription()!languageResource.get("noCorpusDescription")}
            </p>
        </div>
    </div>

    <div class="corpora-list">
        <h5 class="text-center color-prime font-weight-bold">${languageResource.get("corpora")}</h5>
        <div class="row m-0 p-0 ">
            <#list corpora as corpusVm>
                <div class="col-md-6 m-0 p-3">
                    <div class="corpus-card">
                        <!-- header -->
                        <div class="flexed align-items-baseline justify-content-between">
                            <div>
                                <h6 class="open-corpus-inspector-btn border-0 w-100 mb-0 color-prime clickable" data-id="${corpusVm.getCorpus().getId()}">
                                    ${corpusVm.getCorpus().getName()}
                                </h6>
                                <p class="text mb-0 small">${corpusVm.getCorpus().getAuthor()}</p>
                            </div>
                            <a class="btn open-corpus-inspector-btn" data-trigger="hover"
                               data-toggle="popover" data-placement="top" data-id="${corpusVm.getCorpus().getId()}"
                               data-content="${languageResource.get("openCorpus")}">
                                <i class="fas fa-globe"></i>
                            </a>
                        </div>
                        <hr class="mt-1 mb-1 "/>

                        <!-- content -->
                        <p class="corpus-description text font-italic small mb-0">
                            <#if corpusVm.getCorpusConfig().getDescription()?has_content>
                                ${corpusVm.getCorpusConfig().getDescription()}
                            <#else>
                                ${languageResource.get("noCorpusDescription")}
                            </#if>
                        </p>
                    </div>
                </div>
            </#list>
        </div>

        <!-- clal to search -->
        <div class="flexed align-items-center justify-content-center mt-3">
            <a class="clickable text mb-0 text small ml-1" onclick="navigateToView('search')">
                <i class="fas fa-search mr-1"></i> ${languageResource.get("callForSearch")}
            </a>
        </div>
    </div>

</div>