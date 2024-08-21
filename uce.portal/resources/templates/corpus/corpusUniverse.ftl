<!DOCTYPE html>
<html>
<head>
    <link rel="stylesheet"
          href="https://cdn.jsdelivr.net/npm/bootstrap@4.3.1/dist/css/bootstrap.min.css"
          integrity="sha384-ggOyR0iXCbMQv3Xipma34MD+dH/1fQ784/j6cY/iJTQUOhcWr7x9JvoRxT2MZw1T"
          crossorigin="anonymous">
    <link
            href="https://cdnjs.cloudflare.com/ajax/libs/animate.css/4.1.1/animate.min.css"
            rel="stylesheet">
    <script src="https://kit.fontawesome.com/b0888ca2eb.js"
            crossorigin="anonymous"></script>
    <style>
        <#include "*/css/site.css">
        <#include "*/css/corpus-universe.css">
    </style>

    <script type="importmap">
        {
          "imports": {
            "three": "https://unpkg.com/three@v0.161.0/build/three.module.js",
            "three/addons/": "https://unpkg.com/three@v0.161.0/examples/jsm/"
          }
        }
    </script>
    <title>Corpus Universe</title>
</head>

<div id="inputs-parameters" data-corpusid="${corpusId}" data-currentcenter="${currentCenter}"></div>

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



<script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
<script
        src="https://cdn.jsdelivr.net/npm/popper.js@1.14.7/dist/umd/popper.min.js"
        integrity="sha384-UO2eT0CpHqdSJQ6hJty5KVphtPhzWj9WO1clHTMGa3JDZwrnQq4sF86dIHNDz0W1"
        crossorigin="anonymous"></script>
<script
        src="https://cdn.jsdelivr.net/npm/bootstrap@4.3.1/dist/js/bootstrap.min.js"
        integrity="sha384-JjSmVgyd0p3pXB1rRibZUAYoIIy6OrQ6VrjIEaFf/nJGzIxFDsf4x0xIM+B07jRM"
        crossorigin="anonymous"></script>

<script src="http://d3js.org/d3.v3.min.js"></script>

<script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
<script
        src="https://cdn.jsdelivr.net/npm/chartjs-plugin-annotation"></script>
<script
        src="https://cdnjs.cloudflare.com/ajax/libs/chartjs-plugin-datalabels/2.0.0/chartjs-plugin-datalabels.min.js"
        integrity="sha512-R/QOHLpV1Ggq22vfDAWYOaMd5RopHrJNMxi8/lJu8Oihwi4Ho4BRFeiMiCefn9rasajKjnx9/fTQ/xkWnkDACg=="
        crossorigin="anonymous" referrerpolicy="no-referrer"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/gsap/3.4.0/gsap.min.js"></script>
<script src="https://requirejs.org/docs/release/2.3.5/minified/require.js"></script>

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
    $(document).ready(async function(){
        currentCorpusUniverseHandler = getNewCorpusUniverseHandler;

        const params = $('#inputs-parameters');
        const corpusId = params.data('corpusid');
        const currentCenterAsString = params.data('currentcenter');
        if(corpusId === undefined || corpusId === '') {
            console.error('Cant build universe as the corpus id was invalid.');
            return;
        }
        const currentCenter = currentCenterAsString.split(';');

        await currentCorpusUniverseHandler.createEmptyUniverse('universe-container');
        await currentCorpusUniverseHandler.fromCorpus(corpusId, currentCenter);
    });

</script>

</html>