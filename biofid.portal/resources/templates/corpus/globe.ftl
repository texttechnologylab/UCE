<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>${document.getDocumentTitle()}</title>
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
    <script src="https://kit.fontawesome.com/b0888ca2eb.js"
            crossorigin="anonymous"></script>
    <style>
        <#include "*/css/site.css">
        <#include "*/css/globe.css">
    </style>
</head>
<body>

<div class="site-container">

    <div class="nav container-fluid p-3">
        <div class="w-100 container">
            <div class="flexed align-items-center justify-content-between">
                <h5 class="text-dark"><i class="fas fa-globe-europe mr-1"></i> Taxonverteilung
                    von ${document.getDocumentTitle()}</h5>
                <a class="btn open-reader-btn">
                    <i class="color-prime fas fa-book-open m-0"></i>
                </a>
            </div>
        </div>
    </div>

    <div class="globe-container container-fluid">
        <div id="globeViz"></div>
    </div>

    <div class="inspector-container">
        <div class="header p-3">
            <div class="flexed align-items-center w-100 justify-content-between">
                <h6><i class="fas fa-map-marker-alt mr-1"></i> <span class="title"></span></h6>
                <a class="close-btn" onclick="$('.inspector-container').hide(150)"><i class="color-prime fas fa-long-arrow-alt-right"></i></a>
            </div>
        </div>

        <div class="content p-3">
            <p class="text-dark underlined">Occurrences</p>

            <div id="map">

            </div>

            <div class="occurrences">

            </div>
        </div>
    </div>
</div>


</body>

<script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
<script
        src="https://cdn.jsdelivr.net/npm/popper.js@1.14.7/dist/umd/popper.min.js"
        integrity="sha384-UO2eT0CpHqdSJQ6hJty5KVphtPhzWj9WO1clHTMGa3JDZwrnQq4sF86dIHNDz0W1"
        crossorigin="anonymous"></script>
<script
        src="https://cdn.jsdelivr.net/npm/bootstrap@4.3.1/dist/js/bootstrap.min.js"
        integrity="sha384-JjSmVgyd0p3pXB1rRibZUAYoIIy6OrQ6VrjIEaFf/nJGzIxFDsf4x0xIM+B07jRM"
        crossorigin="anonymous"></script>
<!-- Make sure you put this AFTER Leaflet's CSS -->
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"
        integrity="sha256-20nQCchB9co0qIjJZRGuk2/Z9VM+kNiyxNV1lvTlZBo="
        crossorigin=""></script>
<script src="//unpkg.com/d3"></script>
<script src="//unpkg.com/topojson-client"></script>
<script src="//unpkg.com/globe.gl"></script>
<script type="module">

    // We get the data through the freemaker template engine
    const data = ${data};
    const map = L.map('map').setView([0, 0], 100);
    const tiles = L.tileLayer('https://{s}.tile.openstreetmap.fr/hot/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>'
    }).addTo(map);

    <#noparse>

    /**
     * Builds the html for the tooltip on hover
     * @param d
     * @returns {string}
     */
    function getTooplTipHtml(d) {
        return `
            <div class="tooltip-container">
                <div class="title w-100 p-2">
                    <label class="m-0 small-font text-center w-100"><i class="fas fa-map-marker-alt mr-1"></i> ${d.points.filter(p => p.region != null)[0]?.region}</label>
                </div>
                <hr class="mt-0 mb-1"/>
                <ul class="content small-font p-2 text">
                    <li>
                        ${d.points.slice(0, 10).map(point => "<i class='fas fa-tenge mr-1'></i>" + point.name).join('<br/>')}
                        ${d.points.length > 10 ? '<br/>...' : ''}
                    </li>
                </ul>
            </div>
        `;
    }

    /**
     * Builds the html for the full inspector
     * @param d
     */
    function openInspector(d){
        const $inspector = $('.inspector-container');
        const region = d.points.filter(p => p.region != null)[0]?.region;
        $inspector.find('.header .title').html(region);

        // Setup the leaflet map as well
        map.setView([d.points[0].latitude, d.points[0].longitude], 5);
        const marker = L.marker([d.points[0].latitude, d.points[0].longitude])
            .addTo(map);
        if(region != undefined)
            marker.bindPopup(region).openPopup();

        let occurrencesHtml = "";
        // Then all occurrences card
        for(let i = 0; i < d.points.length; i++){
            let curPoint = d.points[i];
            let pointHtml = `
                <div class="occurrence-card">
                    <div class="row m-0 p-0 w-100 h-100">
                        <div class="col-8 m-0 p-3 h-100">
                            <a target="_blank" href="https://www.gbif.org/species/${curPoint.taxonId}"><h6 class="mb-0 color-secondary">${curPoint.name}</h6></a>
                            <p class="mb-0 small-font text">${curPoint.value}</p>
                        </div>
                            <img class="col-4 m-0 p-0 h-100 thumbnail" src="${curPoint.image}"/>
                    </div>
                </div>
            `;
            occurrencesHtml += pointHtml;
        }
        $inspector.find('.content .occurrences').html(occurrencesHtml);
    }

    const weightColor = d3.scaleLinear()
        .domain([0, 60])
        .range(['lightblue', 'darkgreen'])
        .clamp(true);

    const myGlobe = Globe()
        .globeImageUrl('//unpkg.com/three-globe/example/img/earth-water.png')
        .backgroundColor('rgba(255,255,255,0)')
        .hexBinPointLat(d => d.latitude)
        .hexBinPointLng(d => d.longitude)
        .hexBinPointWeight(d => 5)
        .hexAltitude(({sumWeight}) => sumWeight * 0.0025)
        .hexTopColor(d => weightColor(d.sumWeight))
        .hexSideColor(d => weightColor(d.sumWeight))
        .hexLabel(d => getTooplTipHtml(d))
        .onHexClick(d => {
            // Move the camera
            myGlobe.pointOfView({ lat: d.points[0].latitude, lng: d.points[0].longitude, altitude: 1 }, 750);
            openInspector(d);
        })
        .enablePointerInteraction(true)
        .width($('.globe-container').width())
        (document.getElementById('globeViz'));

    myGlobe.hexBinPointsData(data.occurrences);
    </#noparse>

</script>

</html>