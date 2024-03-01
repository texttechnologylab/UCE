<div class="mt-3">

    <div class="header">
        <div class="flexed w-100 align-items-center justify-content-center">
            <button class="btn selected-btn">
                Suchergebnisse <span class="hits">12.138</span>
            </button>
        </div>
    </div>

    <div class="row mb-0 mr-0 ml-0 pb-5">

        <div class="col-md-3 position-relative">
            <div class="side-bar-container">
                <div class="side-bar">
                    <div class="content pb-0">

                        <h6 class="text-center underlined mb-4">Taxonomie</h6>

                        <div class="taxonomy-tree">
                            <div class="layer" style="background-color: rgba(87, 99, 255, 0.2)">
                                <label>Lebewesen</label>
                                <input type="checkbox" checked/>
                            </div>
                            <div class="layer" style="background-color: rgba(87, 182, 255, 0.2)">
                                <label>Domäne</label>
                                <input type="checkbox" checked/>
                            </div>
                            <div class="layer" style="background-color: rgba(87, 255, 247, 0.2)">
                                <label>Reich</label>
                                <input type="checkbox" checked/>
                            </div>
                            <div class="layer" style="background-color: rgba(87, 255, 166, 0.2)">
                                <label>Stamm</label>
                                <input type="checkbox" checked/>
                            </div>
                            <div class="layer" style="background-color: rgba(93, 255, 87, 0.2)">
                                <label>Klasse</label>
                                <input type="checkbox" checked/>
                            </div>
                            <div class="layer" style="background-color: rgba(177, 255, 87, 0.2)">
                                <label>Ordnung</label>
                                <input type="checkbox" checked/>
                            </div>
                            <div class="layer" style="background-color: rgba(255, 253, 87, 0.2)">
                                <label>Familie</label>
                                <input type="checkbox" checked/>
                            </div>
                            <div class="layer" style="background-color: rgba(255, 172, 87, 0.25)">
                                <label>Gattung</label>
                                <input type="checkbox" checked/>
                            </div>
                            <div class="layer" style="background-color: rgba(227, 142, 83, 0.25)">
                                <label>Art</label>
                                <input type="checkbox" checked/>
                            </div>
                        </div>

                    </div>
                </div>
            </div>
        </div>

        <div class="col-md-6">
            <div>
                <#list documents as document>
                    <div class="flexed justify-content-center">
                        <div class="document-card">
                            <div>
                                <div class="flexed align-items-stretch h-100">

                                    <div class="flexed align-items-center w-auto">
                                    </div>

                                    <div class="content">

                                        <div class="document-header">
                                            <div class="w-100">
                                                <h6 class="open-document mb-0 title"
                                                    data-id="${document.getId()}">${document.getDocumentTitle()} </h6>
                                                <label class="text-secondary small-font">(${document.getGoetheTitleInfo().getPublished()}
                                                    )</label>
                                            </div>

                                            <div class="flexed align-items-center">
                                                <p class="mb-0 text mr-3"> ${document.getLanguage()?upper_case}</p>
                                                <div class="mb-0 flexed align-items-center text">
                                                    <i class="fas fa-file-alt"></i> <label
                                                            class="mb-0 ml-2">${document.getPages(10000, 0)?size}</label>
                                                </div>
                                            </div>
                                        </div>

                                        <div class="snippet-content flexed align-items-center justify-content-between h-100">
                                            <p class="mb-0 small-font text font-italic mr-2">
                                                "${document.getFullTextSnippet(85)}..."
                                            </p>
                                        </div>

                                        <div class="search-hits">
                                            <span><i class="fas fa-map-marker-alt"></i> Schweiz</span>
                                            <span><i class="fas fa-user-tag"></i> Arnold</span>
                                            <span><i class="fas fa-sitemap"></i> Verein</span>
                                            <span><i class="fas fa-th"></i> Bett</span>
                                            <span><i class="fas fa-tenge"></i> Pflanzen</span>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                </#list>
            </div>
        </div>

        <div class="col-md-3 position-relative">
            <div class="side-bar-container">
                <div class="side-bar">
                    <div class="content">

                        <h6 class="text-center underlined">Eigennamen</h6>

                        <div class="entry">
                            <div class="flexed align-items-center w-100 justify-content-between">
                                <p class="mb-0"><i class="fas fa-map-marker-alt mr-1"></i> Orte</p>
                                <label class="text mb-0">134</label>
                            </div>
                        </div>

                        <div class="entry">
                            <div class="flexed align-items-center w-100 justify-content-between">
                                <p class="mb-0"><span><i class="fas fa-user-tag mr-1"></i> Personen</p>
                                <label class="text mb-0">134</label>
                            </div>
                        </div>

                        <div class="entry">
                            <div class="flexed align-items-center w-100 justify-content-between">
                                <p class="mb-0"><i class="fas fa-sitemap mr-1"></i> Organisationen</p>
                                <label class="text mb-0">134</label>
                            </div>
                        </div>

                        <div class="entry">
                            <div class="flexed align-items-center w-100 justify-content-between">
                                <p class="mb-0"><i class="fas fa-th mr-1"></i> Sonstiges</p>
                                <label class="text mb-0">134</label>
                            </div>
                        </div>

                        <hr />

                        <div class="entry">
                            <div class="flexed align-items-center w-100 justify-content-between">
                                <p class="mb-0"><i class="fas fa-tenge mr-1"></i> Taxone</p>
                                <label class="text mb-0">134</label>
                            </div>
                        </div>

                        <hr />

                        <h6 class="text-center mb-3 underlined">Navigation</h6>

                        <div class="pagination">
                            <a class="btn mr-3 rounded-a"><i class="fas fa-chevron-left"></i></a>
                            <a class="btn current-page rounded-a">1</a>
                            <a class="btn rounded-a">2</a>
                            <a class="btn rounded-a">3</a>
                            <a class="btn rounded-a">4</a>
                            <a class="btn ml-3 rounded-a"><i class="fas fa-chevron-right"></i></a>
                        </div>

                        <hr class="mt-4 mb-4"/>

                        <h6 class="text-center mb-3 underlined">Sortierung</h6>
                        <div class="sort">

                            <div class="mb-2 flexed align-items-center w-100 justify-content-between">
                                <p class="mb-0">Titel</p>
                                <a class="btn mb-0 rounded-a small-font">
                                    <i class="fas fa-sort-amount-up"></i>
                                </a>
                            </div>

                            <div class="mb-2 flexed align-items-center w-100 justify-content-between">
                                <p class="mb-0">Seiten</p>
                                <a class="btn mb-0 rounded-a small-font">
                                    <i class="fas fa-sort-amount-up"></i>
                                </a>
                            </div>

                        </div>

                    </div>
                </div>
            </div>
        </div>
    </div>

</div>