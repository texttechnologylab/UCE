<div class="mt-0">

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
                <#list searchState.getCurrentDocuments() as document>
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
                                                <label class="text-secondary small-font">(${document.getMetadataTitleInfo().getPublished()}
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

                                        <#assign foundLocations = searchState.getAnnotationsByTypeAndDocumentId("NamedEntities", document.getId(), "LOCATION")>
                                        <#assign foundPersons = searchState.getAnnotationsByTypeAndDocumentId("NamedEntities", document.getId(), "PERSON")>
                                        <#assign foundOrgas = searchState.getAnnotationsByTypeAndDocumentId("NamedEntities", document.getId(), "ORGANIZATION")>
                                        <#assign foundMisc = searchState.getAnnotationsByTypeAndDocumentId("NamedEntities", document.getId(), "MISC")>
                                        <#assign foundTaxons = searchState.getAnnotationsByTypeAndDocumentId("Taxons", document.getId(), "")>
                                        <#assign foundTimes = searchState.getAnnotationsByTypeAndDocumentId("Times", document.getId(), "")>

                                        <div class="flexed align-items-center justify-content-between small text mt-2 text-center">
                                            <span class="w-100 text-center"><i
                                                        class="fas fa-map-marker-alt mr-1"></i>${foundLocations?size}</span>
                                            <span class="w-100 text-center"><i
                                                        class="fas fa-user-tag mr-1"></i>${foundPersons?size}</span>
                                            <span class="w-100 text-center"><i
                                                        class="fas fa-sitemap mr-1"></i>${foundOrgas?size}</span>
                                            <span class="w-100 text-center"><i
                                                        class="fas fa-th mr-1"></i>${foundMisc?size}</span>
                                            <span class="w-100 text-center"><i
                                                        class="fas fa-tenge mr-1"></i>${foundTaxons?size}</span>
                                            <span class="w-100 text-center"><i
                                                        class="fas fa-clock mr-1"></i>${foundTimes?size}</span>
                                            <a class="btn annotation-hit-container-expander" data-expanded="false">
                                                <i class="fas fa-chevron-down"></i>
                                            </a>
                                        </div>

                                        <#function getClassForAnnotation coveredText>
                                            <#assign class = "text"?string>
                                            <#assign coveredTextLowerCase = coveredText?lower_case>
                                            <#list searchState.getSearchTokens() as token>
                                                <#if coveredTextLowerCase?contains(token?lower_case)>
                                                    <#assign class = "color-secondary font-weight-bold">
                                                    <#break>
                                                </#if>
                                            </#list>
                                            <#return class>
                                        </#function>

                                        <div class="annotation-hit-container display-none">
                                            <div class="row m-0">
                                                <div class="search-hits col-2">
                                                    <#list foundLocations as annotation>
                                                        <#assign annotationClass = getClassForAnnotation(annotation.getCoveredText())>
                                                        <span class="test ${annotationClass}">${annotation.getCoveredText()}</span>
                                                    </#list>
                                                </div>
                                                <div class="search-hits col-2">
                                                    <#list foundPersons as annotation>
                                                        <#assign annotationClass = getClassForAnnotation(annotation.getCoveredText())>
                                                        <span class="test ${annotationClass}">${annotation.getCoveredText()}</span>
                                                    </#list>
                                                </div>
                                                <div class="search-hits col-2">
                                                    <#list foundOrgas as annotation>
                                                        <#assign annotationClass = getClassForAnnotation(annotation.getCoveredText())>
                                                        <span class="test ${annotationClass}">${annotation.getCoveredText()}</span>
                                                    </#list>
                                                </div>
                                                <div class="search-hits col-2">
                                                    <#list foundMisc as annotation>
                                                        <#assign annotationClass = getClassForAnnotation(annotation.getCoveredText())>
                                                        <span class="test ${annotationClass}">${annotation.getCoveredText()}</span>
                                                    </#list>
                                                </div>
                                                <div class="search-hits col-2">
                                                    <#list foundTaxons as annotation>
                                                        <#assign annotationClass = getClassForAnnotation(annotation.getCoveredText())>
                                                        <span class="test ${annotationClass}">${annotation.getCoveredText()}</span>
                                                    </#list>
                                                </div>
                                                <div class="search-hits col-2">
                                                    <#list foundTimes as annotation>
                                                        <#assign annotationClass = getClassForAnnotation(annotation.getCoveredText())>
                                                        <span class="test ${annotationClass}">${annotation.getCoveredText()}</span>
                                                    </#list>
                                                </div>
                                            </div>
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
                                <label class="text mb-0">${searchState.getNamedEntitiesByType("LOCATION")?size}</label>
                            </div>
                        </div>

                        <div class="entry">
                            <div class="flexed align-items-center w-100 justify-content-between">
                                <p class="mb-0"><span><i class="fas fa-user-tag mr-1"></i> Personen</p>
                                <label class="text mb-0">${searchState.getNamedEntitiesByType("PERSON")?size}</label>
                            </div>
                        </div>

                        <div class="entry">
                            <div class="flexed align-items-center w-100 justify-content-between">
                                <p class="mb-0"><i class="fas fa-sitemap mr-1"></i> Organisationen</p>
                                <label class="text mb-0">${searchState.getNamedEntitiesByType("ORGANIZATION")?size}</label>
                            </div>
                        </div>

                        <div class="entry">
                            <div class="flexed align-items-center w-100 justify-content-between">
                                <p class="mb-0"><i class="fas fa-th mr-1"></i> Sonstiges</p>
                                <label class="text mb-0">${searchState.getNamedEntitiesByType("MISC")?size}</label>
                            </div>
                        </div>

                        <hr/>

                        <div class="entry">
                            <div class="flexed align-items-center w-100 justify-content-between">
                                <p class="mb-0"><i class="fas fa-tenge mr-1"></i> Taxone</p>
                                <label class="text mb-0">${searchState.getFoundTaxons()?size}</label>
                            </div>
                        </div>
                        <div class="entry">
                            <div class="flexed align-items-center w-100 justify-content-between">
                                <p class="mb-0"><i class="fas fa-clock mr-1"></i> Zeiten</p>
                                <label class="text mb-0">${searchState.getFoundTimes()?size}</label>
                            </div>
                        </div>

                        <hr/>

                        <h6 class="text-center mb-3 underlined">Navigation</h6>

                        <div class="pagination">
                            <a class="btn mr-3 rounded-a"><i class="fas fa-chevron-left"></i></a>
                            <#list 1..(searchState.getTotalPages()) as i>
                                <#if i == searchState.getCurrentPage() + 1>
                                    <a class="btn rounded-a current-page">${i}</a>
                                <#else>
                                    <a class="btn rounded-a">${i}</a>
                                </#if>
                            </#list>
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