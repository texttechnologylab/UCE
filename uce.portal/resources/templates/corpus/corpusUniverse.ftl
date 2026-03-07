<!DOCTYPE html>
<html>
<head>
    <link rel="stylesheet" href="css/bootstrap/bootstrap.min.css">
    <link rel="stylesheet" href="css/animate.min.css">
    <script src="js/fontawesome/all.js"></script>
    <style>
        <#include "*/css/site.css">
        <#include "*/css/corpus-universe.css">
    </style>

    <script type="importmap">
        {
          "imports": {
            "three": "//js/three/three.module.js",
            "three/addons/": "//js/examples/jsm/"
          }
        }
    </script>
    <title>Corpus Universe</title>
</head>

<div id="inputs-parameters" data-corpusid="${corpusId}" data-currentcenter="${currentCenter}"></div>
<#include "*/sessionExpiredModal.ftl">
<#include "*/auth/userShortProfile.ftl">

<div id="universe-container" class="corpus-universe-container">
</div>

<!-- UI overlay -->
<div class="corpus-universe-container-ui">
    <div class="position-relative w-100 h-100">

        <div class="coordinates">
            <i class="fas fa-map-marker-alt mr-1"></i>
            <span class="x">0</span>,
            <span class="y">0</span>,
            <span class="z">0</span>
        </div>

        <!-- inspector -->
        <div class="inspector-window">

            <div class="header bg-light">
                <div class="flexed align-items-center justify-content-between p-4">
                    <div>
                        <h5 class="title mb-1 mr-2 text-dark"></h5>
                        <p class="mb-0 small-font text-secondary">
                            <i class="fas fa-thumbtack mr-1"></i><span class="position">-170.44, -87.54, -24,57</span>
                            <i class="fas fa-globe ml-1"></i> <span class="type"></span></p>
                    </div>
                    <button class="btn close-btn" onclick="$('.inspector-window').fadeOut(50);">
                        <i class="m-0 fas fa-long-arrow-alt-right"></i>
                    </button>
                </div>
            </div>

            <hr class="mt-0 mb-0"/>

            <!-- The content changes according to the selected object -->
            <div class="content">

            </div>
        </div>

    </div>
</div>

<!-- We take this html, clone it, fill it and then use it as content within js -->
<div class="inspector-window-node-content display-none" data-type="template">

    <div data-type="planet-association" class="content-group flexed align-items-center justify-content-between">
        <p class="mb-0">Belongs to planet:</p>
        <a>Fauna USA</a>
    </div>

    <!-- document data, which we fetch from the backend -->
    <div data-type="document-data" class="content-include">
        <i class="fas fa-spinner rotate mt-2 mb-2 text-center w-100"></i>
    </div>

</div>

<script src="js/jquery-3.7.1.min.js"></script>
<script src="js/popper.js/umd/popper.min.js"></script>
<script src="js/bootstrap/bootstrap.min.js"></script>

<script src="js/d3.v3.min.js"></script>

<script src="js/chart.js"></script>
<script src="js/chartjs-plugin-annotation.js"></script>
<script src="js/chartjs-plugin-datalabels.min.js"></script>
<script src="js/gsap.min.js"></script>
<script src="js/require.js"></script>

<script>
    <#include "*/js/site.js">
</script>
<script type="module">
    <#include "*/js/corpusUniverse.js">
</script>

<script>

    let currentCorpusUniverseHandler = undefined;
    /**
     * Init the universe
     */
    $(document).ready(async function () {
        currentCorpusUniverseHandler = getNewCorpusUniverseHandler;

        const params = $('#inputs-parameters');
        const corpusId = params.data('corpusid');
        const currentCenterAsString = params.data('currentcenter');
        if (corpusId === undefined || corpusId === '') {
            console.error('Cant build universe as the corpus id was invalid.');
            return;
        }
        const currentCenter = currentCenterAsString.split(';');

        await currentCorpusUniverseHandler.createEmptyUniverse('universe-container');
        await currentCorpusUniverseHandler.fromCorpus(corpusId, currentCenter);
    });

</script>

</html>
