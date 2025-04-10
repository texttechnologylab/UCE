<div class="lexicon-view">

    <!-- Header -->
    <header class="container-fluid card-shadow bg-lightgray">
        <div class="container flexed align-items-end justify-content-between">
            <h3 class="mb-0 mr-1 color-prime">${languageResource.get("lexicon")}</h3>
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
            <div class="container filter-bar mt-2 mb-1">
                <!-- alphabet -->
                <div class="flexed justify-content-around wrapped align-items-center">
                    <#list alphabetList as char>
                        <a class="rounded-a color-dark">${char}</a>
                    </#list>
                </div>
            </div>
            <div class="container-fluid position-relative h-100">
                <div class="lexicon-content-include p-3">

                </div>
                <div class="lexicon-navigation container">
                    <div class="w-100 flexed justify-content-between align-items-center">
                        <div class="flexed pages-count">

                        </div>
                        <div class="flexed align-items-center">
                            <a class="rounded-a" onclick="window.wikiHandler.fetchPreviousLexiconEntries()" ><i class="fas fa-chevron-left"></i></a>
                            <i class="fas fa-compass color-prime xlarge-font ml-2 mr-2"></i>
                            <a class="rounded-a" onclick="window.wikiHandler.fetchNextLexiconEntries()"><i class="fas fa-chevron-right"></i></a>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>