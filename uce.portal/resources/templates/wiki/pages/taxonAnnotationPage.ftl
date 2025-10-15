<div class="wiki-page container">

    <!-- breadcrumbs -->
    <div class="mb-3">
        <#include "*/wiki/components/breadcrumbs.ftl">
    </div>

    <!-- metadata header -->
    <div>
        <#include "*/wiki/components/metadata.ftl">
    </div>

    <!-- by which DUUI Tool -->
    <div class="flexed align-items-center justify-content-between pl-1 w-100">
        <div class="flexed align-items-center mr-1">
            <i class="fas fa-toolbox color-prime"></i> <span class="text ml-2 mr-1">Annotated by</span><label class="mb-0 font-italic">${vm.getAnnotatedBy()}</label>
        </div>
        <div class="flexed align-items-center ml-1">
            <#if vm.getAnnotatedBy()?lower_case == "gnfindertaxon">
                <i class="fas fa-percentage color-prime"></i> <span class="text ml-1 mr-1">OddsLog10: </span><label class="mb-0 font-italic">${vm.getOdds()}</label>
            </#if>
        </div>
    </div>

    <!-- BIOfid specific urls here -->
    <div class="mt-0 mb-2">
        <div>
            <#list vm.getWikiModel().getIdentifierAsList() as identifier>
                <label class="font-italic mb-0 ml-1 text"><i
                            class="color-prime fas fa-fingerprint mr-1"></i> ${identifier}</label>
            </#list>
        </div>
    </div>

    <hr class="mt-2 mb-4"/>

    <!-- list the lemmas that form this taxon -->
    <div class="mt-3 mb-3">
        <#assign neType = "Taxon">
        <#include "*/wiki/components/lemmaTree.ftl">
    </div>

    <!-- the document this is from -->
    <div class="mt-4 mb-0 w-100 p-0 m-0 justify-content-center flexed align-items-start">
        <div class="document-card w-100">
            <#assign document = vm.getDocument()>
            <#assign searchId = "">
            <#assign reduced = true>
            <#include '*/search/components/documentCardContent.ftl' >
        </div>
    </div>

    <!-- linkable space -->
    <div class="mt-2 mb-2">
        <#assign unique = (vm.getWikiModel().getUnique())!"none">
        <#assign height = 500>
        <#if unique != "none">
            <div class="w-100">
                <#include "*/wiki/components/linkableSpace.ftl">
            </div>
        </#if>
    </div>

    <!-- The next rdf nodes from the sparql db if they exist -->
    <div class="mt-1 mb-3">
        <p class="mb-1 w-100 text-center">
            ${languageResource.get("ontology")}
        </p>
        <#assign rdfNodes = vm.getNextRDFNodes()>
        <#include '*/wiki/components/ontology.ftl' >
    </div>

    <!-- alternative names of this taxon -->
    <#if vm.getAlternativeNames()?has_content && vm.getAlternativeNames()?size gt 0>
        <div class="mt-0 mb-4">
            <p class="mb-1 text-center">${languageResource.get("alternativeNames")}</p>
            <div class="taxon-alt-names">
                <#list vm.getAlternativeNames() as name>
                    <label class="mb-0 ml-1 mr-1 small-font">${name}</label>
                </#list>
            </div>
        </div>
    </#if>

    <!-- taxon occurrences -->
    <#if vm.getGbifOccurrences()?has_content && vm.getGbifOccurrences()?size gt 0>
        <hr class="mt-1 mb-2"/>
        <div class="mt-0 mb-3">
            <div class="flexed align-items-center justify-content-between mb-2">
                <p class="mb-0 text-center mr-1">Occurrences</p>
                <a class="ml-1 w-rounded-btn" target="_blank"
                   href="https://www.gbif.org/species/${vm.getGbifOccurrences()[0].getGbifTaxonId()?string?replace('.', '')?replace(',', '')}">
                    <i class="color-prime large-font fas fa-fingerprint"></i>
                </a>
            </div>
            <div id="wiki-taxon-occurrences-map">

            </div>

            <!-- images -->
            <div class="flexed align-items-center img-gallery">
                <#list vm.getGbifOccurrences() as occ>
                    <#if occ.getImageUrl()?has_content>
                        <img src="${occ.getImageUrl()}"/>
                    </#if>
                </#list>
            </div>

        </div>
    <#else>
        <p class="text-center mt-1 mb-3">No occurrences found.</p>
    </#if>

    <!-- kwic view -->
    <div class="mt-4">
        <#include "*/wiki/components/kwic.ftl">
    </div>

    <!-- similar documents as in the sense of where this ne also exists -->
    <#if (vm.getSimilarDocuments()?has_content) && (vm.getSimilarDocuments()?size > 0)>
        <div class="mt-4">
            <#assign similarDocuments = vm.getSimilarDocuments()>
            <h6 class="text-center">${languageResource.get("foundInDocuments")}</h6>
            <div class="similar-documents">
                <div class="row m-0 p-0">
                    <#list similarDocuments as document>
                        <div class="col-md-4 m-0 p-2">
                            <div class="item">
                                <div class="p-2">
                                    <h6 data-wid="${document.getWikiId()}" data-wcovered=""
                                        class="open-wiki-page color-prime mb-0 text-center flexed align-items-center justify-content-center">
                                        ${document.getDocumentTitle()}
                                    </h6>
                                </div>
                                <hr class="mb-1 mt-1"/>
                                <div class="p-1">
                                    <p class="font-italic mb-0 small-font block-text text normal-line-height">
                                        ${document.getFullTextSnippet(30)}...
                                    </p>
                                </div>
                            </div>
                        </div>
                    </#list>
                </div>
            </div>
        </div>
    </#if>
</div>

<script type="module">
    $(document).ready(function () {
        // Upon loading the document annotation page, we want to init a leaflet occurences map.
        const map = window.L.map('wiki-taxon-occurrences-map').setView([0, 0], 100);
        const tiles = window.L.tileLayer('https://{s}.tile.openstreetmap.fr/hot/{z}/{x}/{y}.png', {
            maxZoom: 19,
        }).addTo(map);

        // Foreach occurrence, add a pointer
        <#list vm.getGbifOccurrences() as occ>
        L.marker([${occ.getLatitude()?replace(",", ".")}, ${occ.getLongitude()?replace(",", ".")}])
            .addTo(map)
            .bindPopup("${vm.getWikiModel().getCoveredText()}")
            .openPopup();

        map.setView([${occ.getLatitude()?replace(",", ".")}, ${occ.getLongitude()?replace(",", ".")}], 5);
        </#list>
    })
</script>
