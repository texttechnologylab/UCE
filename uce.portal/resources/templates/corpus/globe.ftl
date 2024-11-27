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
                <a class="btn open-document" data-id="${document.getId()?string?replace('.', '')?replace(',', '')}">
                    <i class="color-secondary fas fa-book-open m-0"></i>
                </a>
            </div>
        </div>
    </div>

    <div class="globe-container container-fluid position-relative">
        <div id="globeViz"></div>

        <a class="open-search-btn" onclick="$('.taxon-list-container').show(0)">
            <i class="fas fa-search m-0 color-secondary"></i>
        </a>
    </div>

    <div class="taxon-list-container display-none">
        <div class="header p-3">
            <div class="flexed align-items-center w-100 justify-content-between">
                <a class="close-btn" onclick="$('.taxon-list-container').hide(0)"><i
                            class="color-secondary fas fa-long-arrow-alt-left"></i></a>
                <h6></h6>
            </div>
        </div>

        <div class="content p-3">
            <div class="flexed align-items-center mb-3">
                <input class="form-control search-occurrence-input" type="text" placeholder="Suchen..."/>
            </div>

            <div class="row m-0 p-0">
                <#list data as occurrence>
                    <div class="col-lg-6 m-0 p-2 search-occurrence" data-id="${occurrence.getValue()?split("|")?first}">
                        <div class="flexed align-items-center">
                            <a class="show-on-globe-btn mr-2"
                               data-lat="${occurrence.getLatitude()?replace(",", ".")}" data-long="${occurrence.getLongitude()?replace(",", ".")}">
                                <i class="fas fa-map-marked-alt m-0"></i>
                            </a>
                            <p class="m-0 text">${occurrence.getValue()?split("|")?first}</p>
                        </div>
                    </div>
                </#list>
            </div>
        </div>
    </div>

    <div class="inspector-container">
        <div class="header p-3">
            <div class="flexed align-items-center w-100 justify-content-between">
                <h6><i class="fas fa-map-marker-alt mr-1"></i> <span class="title"></span></h6>
                <a class="close-btn" onclick="$('.inspector-container').hide(150)"><i
                            class="color-secondary fas fa-long-arrow-alt-right"></i></a>
            </div>
        </div>

        <div class="content p-3">
            <p class="text-dark underlined"><span class="count"></span> Occurrences</p>

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
<script>
    <#include "*/js/site.js">
</script>

<script type="module">

    // We get the data through the freemaker template engine
    const data = ${jsonData};
    const map = L.map('map').setView([0, 0], 100);
    const tiles = L.tileLayer('https://{s}.tile.openstreetmap.fr/hot/{z}/{x}/{y}.png', {
        maxZoom: 19,
    }).addTo(map);
    $('.inspector-container').hide();

    <#noparse>

    /**
     * Handle the small search function
     */
    $('body').on('keydown', '.search-occurrence-input', function(event){
        const id = event.key || event.which || event.keyCode || 0;
        if (id !== 'Enter') return;

        const val = $(this).val().toLowerCase().trim();
        $('.search-occurrence').each(function(){
           if($(this).data('id').toLowerCase().trim().includes(val)){
               $(this).show();
           } else{
               $(this).hide();
           }
        });
    })

    /**
     * Handles the click onto an occurrence card
     */
    $('body').on('click', '.occurrence-card .mark-on-map-btn', function () {
        // In that case, we want to update the leaflet map
        // Setup the leaflet map as well
        const $card = $(this).closest('.occurrence-card');
        const lat = $card.data('lat');
        const long = $card.data('long');
        const name = $card.data('name');
        map.setView([lat, long], 15);
    })

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
    function openInspector(d) {
        const $inspector = $('.inspector-container');
        $inspector.fadeIn(250);
        const region = d.points.filter(p => p.region != null)[0]?.region;
        $inspector.find('.header .title').html(region);
        $inspector.find('.count').html(d.points.length);

        // Setup the leaflet map as well
        map.setView([d.points[0].latitude, d.points[0].longitude], 5);

        let occurrencesHtml = "";
        // Then all occurrences card
        for (let i = 0; i < d.points.length; i++) {
            let curPoint = d.points[i];

            // Show each occ on the map with a marker
            const marker = L.marker([curPoint.latitude, curPoint.longitude])
                .addTo(map);
            marker.bindPopup(curPoint.name).openPopup();

            let pointHtml = `
                <div class="occurrence-card" data-lat="${curPoint.latitude}" data-long="${curPoint.longitude}" data-name="${curPoint.name}">
                    <div class="row m-0 p-0 w-100 h-100">
                        <div class="col-8 m-0 p-3 h-100">
                            <div class="flexed align-items-center mb-2">
                                <a class="mark-on-map-btn mr-2" data><i class="m-0 fas fa-map-marked-alt"></i></a>
                                <a target="_blank" href="https://www.gbif.org/species/${curPoint.taxonId}"><h6 class="mb-0 color-secondary underlined">${curPoint.name}</h6></a>
                            </div>
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

    /**
     * Handles the moving to the globe pos of a occurrence in the search view
     */
    $('body').on('click', '.show-on-globe-btn', function(){
        moveToGlobePos($(this).data('lat'), $(this).data('long'), 0.15);
    })

    function moveToGlobePos(lat, long, altitude) {
        myGlobe.pointOfView({lat: lat, lng: long, altitude: altitude}, 750);
    }

    const weightColor = d3.scaleLinear()
        .domain([0, 60])
        .range(['lightblue', 'darkgreen'])
        .clamp(true);

    const myGlobe = Globe()
        .globeImageUrl('/img/8k_earth_daymap.jpg')
        .backgroundColor('rgba(255,255,255,0)')
        .hexBinPointLat(d => d.latitude)
        .hexBinPointLng(d => d.longitude)
        .hexBinPointWeight(d => 3)
        .hexAltitude(({sumWeight}) => sumWeight * 0.0025)
        .hexTopColor(d => weightColor(d.sumWeight))
        .hexSideColor(d => weightColor(d.sumWeight))
        .hexLabel(d => getTooplTipHtml(d))
        .onHexClick(d => {
            // Move the camera
            moveToGlobePos(d.points[0].latitude, d.points[0].longitude, 0.4);
            openInspector(d);
        })
        .enablePointerInteraction(true)
        .width($('.globe-container').width())
        (document.getElementById('globeViz'));

    myGlobe.hexBinPointsData(data);
    </#noparse>

</script>

</html>