<div>
    <#list searchState.getCurrentDocuments() as document>
        <#assign documentIdx = document?index>
        <div class="flexed justify-content-center">
            <div class="document-card" data-id="${document.getId()?string?replace('.', '')?replace(',', '')}">
                <div>
                    <div class="flexed align-items-stretch h-100">

                        <div class="content">

                            <#assign corpusConfig = searchState.getCorpusConfig()>
                            <#assign searchId = searchState.getSearchId()>
                            <#include '*/search/components/documentCardContent.ftl' >

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

                            <!-- TODO: this is VERY scuffed... we check here the search tokens which match the text. -->
                            <#assign matchingTokens = []>
                            <#function getClassForAnnotation coveredText>
                                <#assign class = "text"?string>
                                <#assign coveredTextLowerCase = coveredText?lower_case>
                                <#if (searchState.getSearchTokens()?has_content) && (searchState.getSearchTokens()?size gt 1)>
                                    <#list searchState.getSearchTokens() as token>
                                        <#if coveredTextLowerCase?contains(token?lower_case)>
                                            <#assign class = "color-secondary font-weight-bold">
                                            <#if !(matchingTokens?seq_contains(token))>
                                                <#assign matchingTokens += [token]>
                                            </#if>
                                            <#break>
                                        </#if>
                                    </#list>
                                </#if>
                                <#return class>
                            </#function>

                            <div class="annotation-hit-container display-none">
                                <div class="row m-0">
                                    <div class="search-hits col-2">
                                        <#list foundLocations as annotation>
                                            <#assign annotationClass = getClassForAnnotation(annotation.getCoveredText())>
                                            <span class="${annotationClass} open-wiki-page"
                                                  data-wid="NE-${annotation.getId()?string?replace('.', '')?replace(',', '')}"
                                                  data-wcovered="${annotation.getCoveredText()}">
                                                (${annotation.getOccurrences()}) ${annotation.getCoveredText()}
                                            </span>
                                        </#list>
                                    </div>
                                    <div class="search-hits col-2">
                                        <#list foundPersons as annotation>
                                            <#assign annotationClass = getClassForAnnotation(annotation.getCoveredText())>
                                            <span class="${annotationClass} open-wiki-page"
                                                  data-wid="NE-${annotation.getId()?string?replace('.', '')?replace(',', '')}"
                                                  data-wcovered="${annotation.getCoveredText()}">
                                                (${annotation.getOccurrences()}) ${annotation.getCoveredText()}
                                            </span>
                                        </#list>
                                    </div>
                                    <div class="search-hits col-2">
                                        <#list foundOrgas as annotation>
                                            <#assign annotationClass = getClassForAnnotation(annotation.getCoveredText())>
                                            <span class="${annotationClass} open-wiki-page"
                                                  data-wid="NE-${annotation.getId()?string?replace('.', '')?replace(',', '')}"
                                                  data-wcovered="${annotation.getCoveredText()}">
                                                (${annotation.getOccurrences()}) ${annotation.getCoveredText()}
                                            </span>
                                        </#list>
                                    </div>
                                    <div class="search-hits col-2">
                                        <#list foundMisc as annotation>
                                            <#assign annotationClass = getClassForAnnotation(annotation.getCoveredText())>
                                            <span class="${annotationClass} open-wiki-page"
                                                  data-wid="NE-${annotation.getId()?string?replace('.', '')?replace(',', '')}"
                                                  data-wcovered="${annotation.getCoveredText()}">
                                                (${annotation.getOccurrences()}) ${annotation.getCoveredText()}
                                            </span>
                                        </#list>
                                    </div>
                                    <div class="search-hits col-2">
                                        <#list foundTaxons as annotation>
                                            <#assign annotationClass = getClassForAnnotation(annotation.getCoveredText())>
                                            <span class="${annotationClass} open-wiki-page"
                                                  data-wid="TA-${annotation.getId()?string?replace('.', '')?replace(',', '')}"
                                                  data-wcovered="${annotation.getCoveredText()}">
                                                (${annotation.getOccurrences()}) ${annotation.getCoveredText()}
                                            </span>
                                        </#list>
                                    </div>
                                    <div class="search-hits col-2">
                                        <#list foundTimes as annotation>
                                            <#assign annotationClass = getClassForAnnotation(annotation.getCoveredText())>
                                            <span class="${annotationClass} open-wiki-page"
                                                  data-wid="TI-${annotation.getId()?string?replace('.', '')?replace(',', '')}"
                                                  data-wcovered="${annotation.getCoveredText()}">
                                                (${annotation.getOccurrences()}) ${annotation.getCoveredText()}
                                            </span>
                                        </#list>
                                    </div>
                                </div>
                            </div>

                            <!-- Matched Tokens list -->
                            <div class="matched-tokens-list">
                                <#if matchingTokens?has_content>
                                    <div class="flexed align-items-center h-100">
                                        <i class="fas fa-binoculars color-secondary mr-2"></i>
                                        <#list matchingTokens as token>
                                            <span class="mr-1 matched-token">${token}</span>
                                        </#list>
                                    </div>
                                <#else>
                                    <p class="text small-font mb-0">${languageResource.get("noAnnotationsMatched")}</p>
                                </#if>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

    </#list>
</div>
