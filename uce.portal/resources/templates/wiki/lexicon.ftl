<div class="lexicon-view">

    <!-- Header -->
    <header class="container-fluid card-shadow bg-lightgray">
        <div class="container flexed align-items-end justify-content-between">
            <div class="flexed align-items-end">
                <h3 class="mb-0 mr-1 color-prime">${languageResource.get("lexicon")}</h3>
                <label class="ml-1 mb-0 font-italic text no-text-wrap"><i
                            class="fas fa-pen-nib"></i> ${lexiconEntriesCount}</label>
            </div>
            <div class="flexed align-items-center ml-1 w-100 justify-content-end">
                <input class="form-control rounded-0 w-100 search-lexicon-input" type="text"
                       placeholder="${languageResource.get("search")}"/>
                <button class="btn rounded-0 bg-lightgray search-lexicon-btn"><i class="fas fa-search"></i></button>
            </div>
        </div>
    </header>

    <!-- Content -->
    <div class="mt-4">
        <div>
            <div class="container-fluid position-relative h-100">
                <#if isLexiconCalculating>
                    <div class="full-loader">
                        <div class="simple-loader"><p
                                    class="m-0 text-center w-100 text">${languageResource.get("lexiconIsCalculating")}</p>
                        </div>
                    </div>
                </#if>
                <!-- main content -->
                <div class="row m-0 p-0">

                    <!-- the actual lexicon -->
                    <div class="col-md-9 position-relative w-100 h-100">

                        <div class="container mt-2 mb-1">
                            <!-- alphabet -->
                            <div class="flexed justify-content-around wrapped align-items-center alphabet">
                                <#list alphabetList as char>
                                    <#assign clazz = "">
                                    <#if char?lower_case == "a">
                                        <#assign clazz = "selected-char">
                                    </#if>
                                    <a class="rounded-a color-dark char ${clazz}"
                                       onclick="window.wikiHandler.handleLexiconAlphabetBtnClicked('${char}')">${char}</a>
                                </#list>
                            </div>
                        </div>

                        <div class="lexicon-content-include pl-3 pb-3 pr-3 mt-3">

                        </div>

                        <div class="lexicon-navigation container">
                            <div class="w-100 flexed justify-content-between align-items-center">

                                <div class="flexed pages-count">
                                </div>

                                <div class="flexed">
                                    <!-- Sorting order -->
                                    <div class="sortings mr-1 pr-2 flexed align-items-center border-right border-left pl-1 ml-1">
                                        <label class="mb-0 ml-1 mr-1">${languageResource.get("occurrence")}</label>
                                        <a class="rounded-a border-0" data-id="occurrence" data-dir="ASC" onclick="window.wikiHandler.handleLexiconSortingChanged($(this))">
                                            <i class="fas fa-sort-amount-up-alt"></i>
                                        </a>
                                        <label class="mb-0 ml-2 mr-1">${languageResource.get("alphabet")}</label>
                                        <a class="rounded-a border-0 selected-sort" data-id="alphabet" data-dir="ASC" onclick="window.wikiHandler.handleLexiconSortingChanged($(this))">
                                            <i class="fas fa-sort-amount-up-alt"></i>
                                        </a>
                                    </div>
                                    <!-- siwtch pages -->
                                    <div class="ml-1 flexed align-items-center">
                                        <a class="rounded-a" onclick="window.wikiHandler.fetchPreviousLexiconEntries()"><i
                                                    class="fas fa-chevron-left"></i></a>
                                        <i class="fas fa-compass color-prime xlarge-font ml-2 mr-2"></i>
                                        <a class="rounded-a" onclick="window.wikiHandler.fetchNextLexiconEntries()"><i
                                                    class="fas fa-chevron-right"></i></a>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- filtering options -->
                    <div class="col-md-3 p-0 m-0">
                        <div class="filter-container card-shadow">
                            <div class="p-2">
                                <h5 class="mb-0 text-dark"><i class="fas fa-filter mr-1"></i> Filter</h5>
                            </div>
                            <hr class="mt-1 mb-2"/>
                            <div class="p-3">
                                <!-- Annotations -->
                                <div class="group-box bg-default pt-2">
                                    <h5 class="color-prime mb-1 text-center w-100">${languageResource.get("annotations")}</h5>
                                    <div class="row m-0">
                                        <#list lexiconizableAnnotations as annotation>
                                            <div class="col-md-6 p-1 m-0">
                                                <div class="annotation-filter">
                                                    <label class="mb-0 mr-1">${annotation.getSimpleName()}</label>
                                                    <input class="ml-1" checked type="checkbox"
                                                           onchange="window.wikiHandler.handleLexiconAnnotationFiltersChanged($(this))"/>
                                                </div>
                                            </div>
                                        </#list>
                                    </div>
                                </div>

                            </div>
                        </div>
                    </div>
                </div>

            </div>
        </div>
    </div>
</div>