class UCEMap {

    constructor(container, readonly = false) {
        this._eventHandlers = {};
        this.readonly = readonly;
        this.isTimelineMap = false;
        this.selectedMarkerPayload = undefined;
        this.corpusId = undefined;
        this.currentMarker = undefined;
        this.currentLongLat = undefined; // {lat: 0, lng: 0}
        this.currentCircle = undefined; // _mRadius

        this.$container = $(container);
        this.$container.addClass('uce-map');
        this.$container.append(`
        <div class="full-loader">
            <div class="simple-loader"><p class="p-2 m-0 text-center w-100 color-prime font-italic">Fetching Occurrences</p></div>
        </div>`);
        this.$container.append(`
        <div class="map-ui-container p-2">
            <div class="flexed align-items-center justify-content-end w-100">
                <div class="flexed align-items-center w-100 inputs">
                    <label class="mb-0 small-font mr-1" data-trigger="hover" data-placement="top" data-toggle="popover" data-content="In Meter">
                        Radius
                    </label>
                    <input class="radius-input w-100 form-range" type="range" min="1" value="1000" max="750000"/>
                </div>
            </div>
        </div>`);
        this.$container.append('<div class="map-container h-100"></div>');
        this.$mapContainer = this.$container.find('.map-container');
        this.$uiContainer = this.$container.find('.map-ui-container');
        if (this.readonly) this.$uiContainer.hide();
    }

    /**
     * Creates a 2D Map using leaflet.
     */
    twoDim() {
        this.twoDimMap = L.map(this.$mapContainer.get(0)).setView([51, 10], 3);
        this.tiles = L.tileLayer('https://tile.openstreetmap.de/{z}/{x}/{y}.png', {
            maxZoom: 19,
        }).addTo(this.twoDimMap);

        this.attachEvents();
    }

    /**
     * Creates a special kind of map: a location/timeline map of our linked annotations. The instance takes care of
     * fetching and loading new points according to zoom and position.
     */
    linkedTimelineMap(corpusId) {
        const ctx = this;
        this.isTimelineMap = true;
        this.corpusId = corpusId;
        this.twoDim();

        this.$container.append(`
        <div class="linked-map-navigator">
            <header class="flexed w-100 p-3 border-bottom bg-default">
                <h5 class="mb-0 text-center w-100"><i class="fas fa-compass mr-1"></i> Navigator</h5>
            </header>
            <div class="p-3 mt-2">
                <!-- Timeline options -->
                <div class="group-box bg-default timeline-inputs">
                    <div class="flexed align-items-center justify-content-between mb-2">
                        <label class="w-100 mb-0">Timeline</label>
                        <div class="custom-control custom-switch" style="margin-right: -6px">
                            <input type="checkbox" class="custom-control-input" id="timeline-switch">
                            <label class="mb-0 custom-control-label flexed align-items-center"
                                   for="timeline-switch">
                            </label>
                        </div>
                    </div>
                    <div>
                        <input class="form-control w-100" data-type="from" type="date" value="1700-01-01"/>
                        <label class="w-100 text-center mt-2 mb-2"><i class="fas fa-long-arrow-alt-down"></i></label>
                        <input class="form-control w-100" data-type="to" type="date" value="2000-01-01"/>
                        <div class="w-100 flexed justify-content-center mt-2">
                            <button class="btn btn-prime p-1 mt-2 submit-time-btn"><i class="fas fa-check"></i></button>
                        </div>
                    </div>
                </div>
                <!-- List of annotation links -->
                <div class="group-box bg-default p-0">
                    <label class="text-center w-100 p-2">Occurrences</label>
                    <div class="occurrences-list p-2">
                        <div class="alert alert-info mb-0">
                            <p class="mb-0 text small-font w-100 text-center">None selected.</p>
                        </div>
                    </div>
                    <div class="w=100 p-2">
                        <button class="p-1 btn btn-light w-100 rounded-0 load-more-occurrences">Load more</button>
                    </div>
                </div>
            </div>
            
        </div>
    `);

        this.$linkedMapNavigator = this.$container.find('.linked-map-navigator');
        this.$linkedMapNavigator.find('#timeline-switch').on('change', () => this.fetchAndRenderTimelineNodes());
        this.$linkedMapNavigator.find('.timeline-inputs .submit-time-btn').on('click', () => this.fetchAndRenderTimelineNodes());
        this.$linkedMapNavigator.find('button.load-more-occurrences').on('click', function () {
            if (!ctx.selectedMarkerPayload) return;
            ctx.selectedMarkerPayload.skip += ctx.selectedMarkerPayload.take;
            ctx.renderTimelineOccurrences(ctx.selectedMarkerPayload);
        });

        this.fetchAndRenderTimelineNodes();
    }

    buildPayload() {
        const zoom = this.twoDimMap.getZoom();
        return {
            minLat: -90,
            maxLat: 90,
            minLng: -180,
            maxLng: 180,
            zoom: zoom,
            corpusId: this.corpusId,
            fromDate: this.$linkedMapNavigator.find('#timeline-switch').is(':checked')
                ? this.$linkedMapNavigator.find('.timeline-inputs input[data-type="from"]').val()
                : null,
            toDate: this.$linkedMapNavigator.find('#timeline-switch').is(':checked')
                ? this.$linkedMapNavigator.find('.timeline-inputs input[data-type="to"]').val()
                : null,
        };
    }

    fetchAndRenderTimelineNodes() {
        const payload = this.buildPayload();
        const ctx = this;
        ctx.$container.find('.full-loader').fadeIn(150);

        fetch('/api/corpus/map/linkedOccurrenceClusters', {
            method: 'POST',
            contentType: "application/json",
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload)
        })
            .then(response => response.json())
            .then(data => {
                this.renderTimelineNodes(data);
                ctx.$container.find('.full-loader').fadeOut(125);
            })
            .catch(err => {
                showMessageModal("Error", "There was an error fetching the map nodes.");
                ctx.$container.find('.full-loader').fadeOut(125);
            });
    }

    renderTimelineNodes(data) {
        // Clear previous layers
        if (this.timelineClusters) this.twoDimMap.removeLayer(this.timelineClusters);
        const ctx = this;

        this.timelineClusters = L.markerClusterGroup({
            spiderfyOnMaxZoom: false,
        });
        data.forEach(d => {
            const marker = L.marker([d.latitude, d.longitude]);
            marker.options.count = d.count;
            marker.bindPopup(`Cluster of ${d.count} items`);

            // Attach to the on click events
            marker.on('click', function (e) {
                // When clicked, we simply list all the annotations of that cluster.
                let payload = ctx.buildPayload();
                payload.minLng = d.longitude - 0.0001;
                payload.maxLng = d.longitude + 0.0001;
                payload.minLat = d.latitude - 0.0001;
                payload.maxLat = d.latitude + 0.0001;
                payload['take'] = 25;
                payload['skip'] = 0;
                ctx.selectedMarkerPayload = payload;
                // Clear the list of the previous occurrences
                const $listContainer = ctx.$linkedMapNavigator.find('.occurrences-list');
                $listContainer.html('');
                ctx.renderTimelineOccurrences(payload);
            });

            this.timelineClusters.addLayer(marker);
        });

        this.twoDimMap.addLayer(this.timelineClusters);
    }

    renderTimelineOccurrences(payload) {
        const ctx = this;

        fetch('/api/corpus/map/linkedOccurrences', {
            method: 'POST',
            contentType: "application/json",
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload)
        })
            .then(response => response.json())
            .then(occurrences => {
                const $listContainer = ctx.$linkedMapNavigator.find('.occurrences-list');
                occurrences.forEach(function (marker) {
                    $listContainer.append(`
                        <div class="occurrence-card" data-id="${marker.annotationId}" 
                             data-type="${marker.annotationType}">
                            <div class="flexed align-items-center justify-content-between">
                                <div>
                                    <p class="mb-0 color-prime"><i class="fas fa-search-location mr-1"></i> <span data-type="label">${marker.label}</span></p>
                                    <p class="mb-0 text font-italic small-font">(${marker.annotationType.split('.').pop()})</p>
                                </div>
                                <div class="text-right">
                                    <p class="small-font mb-0 font-italic text"><i class="far fa-clock mr-1"></i>${marker.date ?? "(-)"}</p>
                                    <span class="display-none" data-type="date">${marker.dateCoveredText}</span>
                                    <p class="small-font mb-0 font-italic text"><i class="fas fa-map-marker-alt"></i> <span data-type="location">${marker.locationCoveredText}</span></p>
                                </div>
                            </div>
                        </div>
                    `);
                });

                // Attach all needed events to those occurence cards.
                $listContainer.find('.occurrence-card').on('click', function () {
                    const $card = $(this);
                    fetch('/api/wiki/annotation?id=' + $(this).data('id') + '&class=' + $(this).data('type'), {
                        method: 'GET',
                        headers: {
                            'Content-Type': 'application/json'
                        }
                    })
                        .then(response => {
                            if (!response.ok) {
                                showMessageModal("Error", "There was an error fetching the information of that occurrence.");
                            }
                            return response.json();
                        })
                        .then(data => {
                            const highlights = [
                                $card.find('span[data-type="label"]').html(),
                                $card.find('span[data-type="location"]').html(),
                                $card.find('span[data-type="date"]').html()
                            ]
                            openInExpandedTextView(data.coveredText, data.page.coveredText, highlights, data.wikiId, data.coveredText);
                        })
                        .catch(err => showMessageModal("Error", "There was an error fetching the information of that occurrence."));
                });
            })
            .catch(err => showMessageModal("Error", "There was an error fetching the map markers."));
    }

    /**
     * Adds a list of nodes in the form of {lat, lng, label} to an existing 2D map.
     */
    placeNodes(nodes) {
        if (!Array.isArray(nodes)) return;

        // Optional: Clear previously added node markers
        if (!this.nodeMarkers) this.nodeMarkers = [];
        this.nodeMarkers.forEach(marker => this.twoDimMap.removeLayer(marker));
        this.nodeMarkers = [];

        nodes.forEach(node => {
            if (typeof node.lat === 'number' && typeof node.lng === 'number') {
                const marker = L.marker([node.lat, node.lng]);

                if (node.label) {
                    marker.bindPopup(node.label);
                }

                marker.addTo(this.twoDimMap);
                this.nodeMarkers.push(marker);
            }
        });
    }

    /**
     * Translates on the map to a given point ({lat, lng}) with a given zoom
     */
    translateTo(point, zoom = 13) {
        if (!point || typeof point.lat !== 'number' || typeof point.lng !== 'number') {
            console.warn('Invalid point provided to translateTo');
            return;
        }
        this.twoDimMap.setView([point.lat, point.lng], zoom);
    }

    on(eventName, callback) {
        if (!this._eventHandlers[eventName]) {
            this._eventHandlers[eventName] = [];
        }
        this._eventHandlers[eventName].push(callback);
    }

    off(eventName, callback) {
        if (!this._eventHandlers[eventName]) return;
        this._eventHandlers[eventName] = this._eventHandlers[eventName].filter(cb => cb !== callback);
    }

    emit(eventName, payload) {
        if (!this._eventHandlers[eventName]) return;
        for (const cb of this._eventHandlers[eventName]) {
            cb(payload);
        }
    }

    /**
     * We need to attach to some menu events, button events and the likes
     */
    attachEvents() {
        const ctx = this;

        // Activate the geo search for locations on the map
        if (typeof L.Control.Geocoder !== 'undefined') {
            L.Control.geocoder({
                defaultMarkGeocode: false,
                position: "topleft"
            })
                .on('markgeocode', (e) => {
                    const center = e.geocode.center;
                    ctx.twoDimMap.setView(center, 13);

                    // Update marker and circle if already set
                    if (!ctx.readonly) {
                        if (this.currentMarker) ctx.twoDimMap.removeLayer(ctx.currentMarker);
                        if (this.currentCircle) ctx.twoDimMap.removeLayer(ctx.currentCircle);

                        ctx.currentMarker = L.marker(center).addTo(ctx.twoDimMap);
                        ctx.currentCircle = L.circle(center, {radius: 1000}).addTo(ctx.twoDimMap);
                        ctx.currentLongLat = center;

                        ctx.emit('stateChanged', {longLat: ctx.currentLongLat, radius: ctx.currentCircle._mRadius});
                    }
                })
                .addTo(ctx.twoDimMap);
        }

        // When zooming in an we have adaptive nodes, we want to rerender nodes
        if (this.isTimelineMap)
            this.twoDimMap.on('moveend', () => {
                //ctx.fetchAndRenderAdaptiveNodes();
            });

        // Place a marker. If a marker exists currently, delete the old one.
        if (!this.readonly) {
            this.twoDimMap.on('click', function (e) {
                const latlng = e.latlng;
                ctx.currentLongLat = latlng;

                // Remove existing marker and circle
                if (ctx.currentMarker) ctx.twoDimMap.removeLayer(ctx.currentMarker);
                if (ctx.currentCircle) ctx.twoDimMap.removeLayer(ctx.currentCircle);

                // Add new marker and circle
                ctx.currentMarker = L.marker(latlng).addTo(ctx.twoDimMap);

                const initialRadius = parseInt(ctx.$uiContainer.find('.radius-input').val(), 10) || 1000;
                ctx.currentCircle = L.circle(latlng, {radius: initialRadius}).addTo(ctx.twoDimMap);

                ctx.emit('stateChanged', {longLat: ctx.currentLongLat, radius: ctx.currentCircle._mRadius});
            });

            // Radius changed, we need to update the circle of the marker then
            this.$uiContainer.on('input change', '.radius-input', function (e) {
                const newRadius = parseInt($(this).val(), 10);
                if (ctx.currentCircle) {
                    ctx.currentCircle.setRadius(newRadius);
                }
                ctx.emit('stateChanged', {longLat: ctx.currentLongLat, radius: ctx.currentCircle._mRadius});
            });
        }
    }
}

export {UCEMap}
