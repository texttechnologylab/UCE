<!DOCTYPE html>
<html lang="${languageResource.getDefaultLanguage()}">
<head>
    <link rel="stylesheet"
          href="https://cdn.jsdelivr.net/npm/bootstrap@4.3.1/dist/css/bootstrap.min.css"
          integrity="sha384-ggOyR0iXCbMQv3Xipma34MD+dH/1fQ784/j6cY/iJTQUOhcWr7x9JvoRxT2MZw1T"
          crossorigin="anonymous">
    <link
            href="https://cdnjs.cloudflare.com/ajax/libs/animate.css/4.1.1/animate.min.css"
            rel="stylesheet">
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"
          integrity="sha256-p4NxAoJBhIIN+hmNHrzRCf9tD/miZyoHS5obTRR9BMY="
          crossorigin=""/>
    <style>
        <#include "*/css/site.css">
        <#include "*/css/simple-loader.css">
        <#include "*/css/search-redesign.css">
        <#include "*/css/corpus-universe.css">
        <#include "*/css/wiki.css">
        <#include "*/css/lexicon.css">
        <#include "*/css/layered-search-builder.css">
        <#include "*/css/kwic.css">
        <#include "*/css/drawflow.css">
    </style>
    <script src="https://kit.fontawesome.com/b0888ca2eb.js"
            crossorigin="anonymous"></script>
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"
            integrity="sha256-20nQCchB9co0qIjJZRGuk2/Z9VM+kNiyxNV1lvTlZBo="
            crossorigin=""></script>
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script
            src="https://cdn.jsdelivr.net/npm/popper.js@1.14.7/dist/umd/popper.min.js"
            integrity="sha384-UO2eT0CpHqdSJQ6hJty5KVphtPhzWj9WO1clHTMGa3JDZwrnQq4sF86dIHNDz0W1"
            crossorigin="anonymous"></script>
    <script
            src="https://cdn.jsdelivr.net/npm/bootstrap@4.3.1/dist/js/bootstrap.min.js"
            integrity="sha384-JjSmVgyd0p3pXB1rRibZUAYoIIy6OrQ6VrjIEaFf/nJGzIxFDsf4x0xIM+B07jRM"
            crossorigin="anonymous"></script>
    <!-- For corpus universe three.js -->
    <script type="importmap">
        {
          "imports": {
            "three": "https://unpkg.com/three@v0.161.0/build/three.module.js",
            "three/addons/": "https://unpkg.com/three@v0.161.0/examples/jsm/"
          }
        }
    </script>
    <!--<script src="https://cdn.plot.ly/plotly-latest.min.js"></script>-->
    <script src="js/utils.js"></script>
    <script src="js/visualization/cdns/chartjs-449.js"></script>
    <script src="js/visualization/cdns/d3js-790.js"></script>
    <script src="js/visualization/cdns/drawflow-last.js"></script>

    <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
    <script type="module" src="js/md-block.js"></script>

    <script src="https://cdnjs.cloudflare.com/ajax/libs/gsap/3.4.0/gsap.min.js"></script>
    <script src="https://requirejs.org/docs/release/2.3.5/minified/require.js"></script>
    <!--<script src="https://unpkg.com/@tweenjs/tween.js@^20.0.0/dist/tween.umd.js"></script>-->

    <title>${title}</title>
</head>

<body>
<#include "*/messageModal.ftl">

<!-- The flow chart of the Linkable objects -->
<div id="flow-chart-modal" class="display-none">
    <div class="header">
        <div class="container flexed align-items-center justify-content-between">
            <h5 class="mb-0 text-dark"><i class="xlarge-font fab fa-hubspot mr-2"></i><span>Linkable Space</span></h5>
            <a class="w-rounded-btn" onclick="$(this).closest('#flow-chart-modal').hide()"><i class="fas fa-times"></i></a>
        </div>

    </div>
    <div id="full-flow-container">
    </div>
    <svg style="height: 0;">
        <defs>
            <!-- Right-pointing arrow with white border -->
            <marker id="arrow-right" markerWidth="10" markerHeight="7" refX="5" refY="3.5"
                    orient="auto" markerUnits="strokeWidth">
                <path d="M0,0 L0,7 L10,3.5 z" fill="var(--prime)" />
            </marker>

            <!-- Left-pointing arrow with white border -->
            <marker id="arrow-left" markerWidth="10" markerHeight="7" refX="5" refY="3.5"
                    orient="auto" markerUnits="strokeWidth">
                <path d="M10,0 L10,7 L0,3.5 z" fill="var(--prime)" />
            </marker>
        </defs>
    </svg>

</div>

<div class="site-container">

    <!-- this object must be set on any site we use within UCE -->
    <div id="prime-color-container" class="color-prime"></div>

    <nav class="position-relative">

        <div class="container-fluid flexed align-items-center justify-content-around">
            <div class="flexed h-100 pr-2">
                <button class="btn switch-view-btn selected-nav-btn" data-id="landing">
                    <img class="mb-0 logo" src="${system.getCorporate().getLogo()}">
                </button>
                <p class="mb-0 ml-3 text xsmall-font align-self-center"><b>Version</b> <i>${uceVersion}</i></p>
            </div>

            <div class="flexed align-items-stretch">
                <!-- system alive buttons -->
                <div class="system-status-bar border-right">
                    <p class="mb-3 text-center text">System Status</p>
                    <div class="flexed align-items-center">
                        <i class="fas fa-project-diagram ml-3 mr-3"
                           style="color: ${isSparqlAlive?string("var(--prime)", "darkgray")}"></i>
                        <i class="fas fa-robot ml-3 mr-3"
                           style="color: ${isRagAlive?string("var(--prime)", "darkgray")}"></i>
                        <i class="fas fa-database ml-3 mr-3"
                           style="color: ${isDbAlive?string("var(--prime)", "darkgray")}"></i>
                    </div>
                </div>

                <!-- select the current focused corpus -->
                <div class="corpus-selection-div">
                    <div class="flexed">
                        <a class="btn btn-light rounded-0 open-corpus-inspector-btn" data-trigger="hover"
                           data-toggle="popover" data-placement="left"
                           data-content="${languageResource.get("openCorpus")}">
                            <i class="fas fa-globe large-font mt-1 text-dark mr-1 ml-1"></i>
                        </a>
                        <select class="form-control" id="corpus-select" aria-label="Default select example" data-trigger="hover"
                                data-toggle="popover" data-placement="right"
                                data-content="${languageResource.get("selectCorpus")}">
                            <#list corpora as corpusVm>
                                <option data-id="${corpusVm.getCorpus().getId()}"
                                        data-hasbiofid="${corpusVm.getCorpusConfig().getAnnotations().getTaxon().isBiofidOnthologyAnnotated()?c}"
                                        data-hasembeddings="${corpusVm.getCorpusConfig().getOther().isEnableEmbeddings()?c}"
                                        data-hastopicdist="${corpusVm.getCorpusConfig().getOther().isAvailableOnFrankfurtUniversityCollection()?c}"
                                        data-hasragbot="${corpusVm.getCorpusConfig().getOther().isEnableRAGBot()?c}"
                                        data-hastaxonannotations="${corpusVm.getCorpusConfig().getAnnotations().getTaxon().isAnnotated()?c}"
                                        data-hastimeannotations="${corpusVm.getCorpusConfig().getAnnotations().isTime()?c}"
                                        data-sparqlalive="${isSparqlAlive?c}"
                                        data-hassr="${corpusVm.getCorpusConfig().getAnnotations().isSrLink()?c}">${corpusVm.getCorpus().getName()}</option>
                            </#list>
                        </select>
                    </div>
                </div>

                <!-- right side buttons -->
                <div class="flexed align-items-center nav-container">
                    <div class="flexed align-items-center nav-buttons">
                        <a class="switch-view-btn btn text" data-id="search"><i
                                    class="fas fa-globe-europe color-prime"></i> Portal</a>
                        <a class="switch-view-btn btn text" data-id="lexicon"><i
                                    class="fab fa-wikipedia-w color-prime"></i> ${languageResource.get("lexicon")}</a>
                        <a class="switch-view-btn btn text" data-id="team"><i
                                    class="fas fa-users color-prime"></i> ${languageResource.get("team")}</a>
                    </div>
                    <select class="form-control bg-default rounded-0 color-prime border-right-0 large-font switch-language-select">
                        <option data-lang="en-EN">Englisch</option>
                        <option data-lang="de-DE">Deutsch</option>
                    </select>
                </div>
            </div>
        </div>
    </nav>

    <div class="layered-search-builder-include display-none">
        <div class="layered-search-builder-modal">
            <#include "*/search/components/layeredSearchBuilder.ftl"/>
        </div>
    </div>

    <div class="sr-query-builder-include"></div>

    <div class="corpusUniverse-content-container main-content-container">

        <!-- landing page -->
        <div class="view" data-id="landing">
            <#include "*/landing-page.ftl" />
        </div>

        <!-- searching -->
        <div class="view pt-5 display-none" data-id="search">

            <!-- A small bg animation - nothing more -->
            <div class="bg-anim">
                <ul class="circles">
                    <li></li>
                    <li></li>
                    <li></li>
                    <li></li>
                    <li></li>
                    <li></li>
                    <li></li>
                    <li></li>
                    <li></li>
                    <li></li>
                </ul>
            </div>

            <!-- actual content -->
            <div class="flexed align-items-stretch search-header container p-0">
                <div class="flexed align-items-center h-100 position-relative" style="z-index: 2">
                    <!-- semantic role button -->
                    <button class="btn open-sr-builder-btn" data-trigger="hover" data-toggle="popover"
                            data-placement="top"
                            data-content="${languageResource.get("openSrBuilder")}">
                        <i class="fas fa-project-diagram mr-1 ml-1"></i>
                    </button>
                    <!-- layered search button -->
                    <div class="position-relative">
                        <button class="btn open-layered-search-builder-btn" data-trigger="hover" data-toggle="popover"
                                data-placement="top"
                                data-content="" onclick="$('.layered-search-builder-include').show();">
                            <i class="fas fa-layer-group mr-1 ml-1"></i>
                        </button>
                        <div class="open-layered-search-builder-btn-badge">0</div>
                    </div>

                </div>

                <!-- Search bar and menu -->
                <div class="w-100 position-relative">
                    <div class="w-100 flexed align-items-center">
                        <input type="text" class="search-input form-control large-font w-100 rounded-0"
                               placeholder="${languageResource.get("searchPlaceholder")}"/>
                        <div class="open-documentation-btn pr-2 pl-2">
                            <i class="fas fa-question-circle large-font clickable open-wiki-page color-secondary"
                               data-trigger="hover" data-toggle="popover" data-placement="top" data-html="true"
                               data-content="${languageResource.get("openSearchDocumentation")}"
                               data-wid="DOC-SEARCH" style="text-decoration: none !important;" data-wcovered="-"></i>
                        </div>

                        <div class="custom-control custom-switch search-pro-mode-switch"
                             data-trigger="hover" data-toggle="popover" data-placement="top" data-html="true"
                             data-content="${languageResource.get("searchProModeDescription")}">
                            <input type="checkbox" class="custom-control-input" id="proModeSwitch">
                            <label class="font-weight-bold font-italic custom-control-label flexed align-items-center"
                                   for="proModeSwitch">
                                Pro
                            </label>
                        </div>
                    </div>

                    <div class="search-menu-div">
                        <div class="backdrop"></div>

                        <div style="z-index: 2; position:relative;">
                            <!-- The search history div -->
                            <div class="search-history-div">
                            </div>

                            <!-- these are the UCEMetadata annotations that can act as a filter if they exist -->
                            <div class="uce-search-filters border-top-1 mb-3 mt-3">
                                <#list corpora as corpusVm>
                                    <#if corpusVm.getCorpusConfig().getAnnotations().isUceMetadata()
                                    && corpusVm.getCorpus().getUceMetadataFilters()?has_content
                                    && corpusVm.getCorpus().getUceMetadataFilters()?size gt 0>
                                        <div class="uce-corpus-search-filter display-none"
                                             data-id="${corpusVm.getCorpus().getId()}">
                                            <div class="flexed align-items-center bg-lightgray p-2 justify-content-between card-shadow light-border rounded">
                                                <p class="text-center w-100 mb-0 text-dark">Filters</p>
                                                <i class="fas fa-filter"></i>
                                            </div>
                                            <div class="flexed align-items-center text-secondary w-100">
                                                <!--<i class="fas fa-filter mr-2"></i>-->
                                                <div class="m-0 pl-0 pr-0 pt-2 pb-2 row w-100 border-top-1 list">
                                                    <#list corpusVm.getCorpus().getUceMetadataFilters() as filter>
                                                        <#include "*/search/components/uceMetadataFilter.ftl">
                                                    </#list>
                                                </div>
                                            </div>
                                        </div>
                                    </#if>
                                </#list>
                            </div>

                            <!-- and the search settings that are always on display -->
                            <div class="search-settings-div flexed align-items-center justify-content-around">
                                <!-- The data-ids are corresponding to the SearchLayer enum. Change them with care!! -->
                                <i class="w-auto fab fa-searchengin text-secondary large-font"></i>
                                <!-- hidden input for layered search -->
                                <input type="hidden" class="submit-layered-search-input" value="false"/>

                                <div class="option" data-type="radio">
                                    <div class="form-check form-check-inline" data-trigger="hover"
                                         data-toggle="popover" data-placement="top" data-html="true"
                                         data-content="${languageResource.get("fulltextSearch")}">
                                        <input class="form-check-input" type="radio" checked
                                               name="searchLayerRadioOptions"
                                               id="inlineRadio1" value="FULLTEXT">
                                        <label class="form-check-label color-prime small-font"
                                               for="inlineRadio1">Fulltext</label>
                                    </div>
                                    <div class="form-check form-check-inline" data-trigger="hover"
                                         data-toggle="popover" data-placement="top" data-html="true"
                                         data-content="${languageResource.get("nerSearch")}">
                                        <input class="form-check-input" type="radio" disabled
                                               name="searchLayerRadioOptions"
                                               id="inlineRadio2" value="NAMED_ENTITIES">
                                        <label class="form-check-label color-secondary small-font" for="inlineRadio2">NER</label>
                                    </div>
                                </div>
                                <div class="option" data-trigger="hover"
                                     data-toggle="popover" data-placement="top" data-html="true"
                                     data-content="${languageResource.get("embeddingSearch")}">
                                    <label class="mb-0 w-100 color-gold small-font">Embedding</label>
                                    <input type="checkbox" data-id="EMBEDDINGS"/>
                                </div>

                                <div class="option" data-trigger="hover"
                                     data-toggle="popover" data-placement="top" data-html="true"
                                     data-content="${languageResource.get("kwicWarning")}">
                                    <label class="mb-0 w-100 color-dark small-font">KWIC</label>
                                    <input type="checkbox" data-id="KWIC"/>
                                </div>

                                <div class="option w-auto" data-trigger="hover"
                                     data-toggle="popover" data-placement="top" data-html="true"
                                     data-content="${languageResource.get("enrichOption")}">
                                    <#assign enrichDisabled = 'checked'>
                                    <#if !isSparqlAlive>
                                        <#assign enrichDisabled = 'disabled'>
                                    </#if>
                                    <label class="mb-0 w-100 small-font mr-3">Enrich</label>
                                    <input type="checkbox" data-id="ENRICH" ${enrichDisabled}/>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <button class="btn btn-primary search-btn">
                    <i class="fas fa-search ml-1 mr-1"></i>
                </button>
            </div>

            <div class="position-relative">
                <#include "*/search/components/loader.ftl">
                <div class="search-result-container container-fluid position-relative">
                </div>
            </div>
        </div>

        <!-- Lexicon -->
        <div class="view display-none" data-id="lexicon">
            <#include "*/wiki/lexicon.ftl" />
        </div>

        <!-- team -->
        <div class="view display-none" data-id="team">
            <div class="container" style="margin-top: 5rem">
                <div class="group-box bg-light">
                    <h5 class="color-prime text-center mb-2">
                        ${languageResource.get("team")}
                    </h5>

                    <p class="mb-0 text text-center">
                        ${system.getCorporate().getTeam().getDescription()}
                    </p>

                    <hr class="mt-2 mb-4"/>

                    <div class="row d-flex align-items-stretch m-0 p-0">
                        <#if (system.getCorporate().getTeam())?? && (system.getCorporate().getTeam().getMembers())??>
                            <#list system.getCorporate().getTeam().getMembers() as member>
                                <div class="col-md-6 p-3 m-0 d-flex">
                                    <div class="team-member-card w-100 h-100">
                                        <div class="flexed align-items-center w-100 h-100">
                                            <div class="p-3 w-100">
                                                <div class="flexed align-items-baseline">
                                                    <h6 class="mb-0 color-prime mr-1">${member.getName()!"-"}</h6>
                                                    <label class="mb-0 small-font">(${member.getRole()!"-"})</label>
                                                </div>
                                                <label class="text small-font mb-0">${member.getDescription()!"-"}</label>
                                                <hr class="mt-1 mb-2"/>
                                                <!-- contact -->
                                                <div class="small-font text-left">
                                                    <a href="mailto:${member.getContact().getEmail()!"-"}">
                                                        <i class="fas fa-envelope mr-1"></i> Mail
                                                    </a>
                                                    <br/>
                                                    <a target="_blank" href="${member.getContact().getWebsite()!"-"}">
                                                        <i class="fas fa-globe-europe mr-1"></i> Website
                                                    </a>
                                                    <p class="mb-0">${member.getContact().getAddress()!"-"}</p>
                                                </div>
                                            </div>
                                            <!-- img -->
                                            <div class="flexed align-items-center justify-content-center text-center h-100">
                                                <img src="${member.getImage()!""}"/>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </#list>
                        </#if>
                    </div>
                </div>

            </div>
        </div>
    </div>

    <#include "*/wiki/components/wikiPageModal.ftl">

    <div class="corpus-inspector-include corpus-inspector display-none">
    </div>

    <div class="ragbot-chat-include">
        <#include "*/ragbot/chatwindow.ftl"/>
    </div>
</div>

</body>

<footer class="bg-lightgray pt-5 pb-5">
    <div class="container h-100 text-center flexed align-items-center justify-content-center">
        <div class="row m-0 p-0 w-100 h-100">

            <!-- contacts -->
            <div class="col-sm-3 color-secondary m-0 justify-content-center flexed">
                <div class="group-box bg-light mb-0">
                    <h6 class="text-left color-prime">${languageResource.get("contact")}</h6>
                    <div class="small-font text-left">
                        <p class="mb-0">${system.getCorporate().getContact().getName()}</p>
                        <a href="mailto:${system.getCorporate().getContact().getEmail()}">
                            <i class="fas fa-envelope mr-1"></i> Mail
                        </a>
                        <br/>
                        <a target="_blank" href="${system.getCorporate().getContact().getWebsite()}">
                            <i class="fas fa-globe-europe mr-1"></i> Website
                        </a>
                        <p class="mb-0">${system.getCorporate().getContact().getAddress()}</p>
                    </div>
                </div>
            </div>

            <!-- add more footer here later -->
            <div class="col-sm-6 color-secondary w-100 m-0 flexed justify-content-center">
                <div class="group-box bg-light mb-0">
                    <h6 class="text-dark">Powered by Unified Corpus Explorer</h6>
                    <a href="https://github.com/texttechnologylab/UCE" target="_blank">
                        <img class="w-100" style="max-width: 125px" src="/img/logo.png"/>
                    </a>
                </div>
            </div>

            <!-- TTLab -->
            <div class="col-md-3 color-secondary text-right justify-content-center">
                <div class="group-box bg-light mb-0">
                    <a href="https://www.texttechnologylab.org/" target="_blank">
                        <h6 class="color-prime">Text Technology Lab</h6>
                    </a>
                    <div class="small-font">
                        <p class="mb-0">
                            ${languageResource.get("ttlabFooter")}
                        </p>
                        <img class="w-100 mt-2" style="max-width: 150px" src="/img/ttlab-logo.png"/>
                    </div>
                </div>
            </div>
        </div>
    </div>
</footer>

<#--<script type="module">
    <#include "js/corpusUniverse.js">
</script>-->
<script type="module">
    <#include "js/graphViz.js">
    <#include "js/flowViz.js">
</script>

<script>
    <#include "js/site.js">
    <#include "js/language.js">
    <#include "js/search.js">
    <#include "js/layeredSearch.js">
    <#include "js/keywordInContext.js">
</script>

</html>