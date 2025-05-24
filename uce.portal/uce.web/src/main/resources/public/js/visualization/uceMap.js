class UCEMap {

    constructor(container, readonly=false) {
        this._eventHandlers = {};
        this.readonly = readonly;
        this.currentMarker = undefined;
        this.currentLongLat = undefined; // {lat: 0, lng: 0}
        this.currentCircle = undefined; // _mRadius

        this.$container = $(container);
        this.$container.addClass('uce-map');
        this.$container.append(`
        <div class="map-ui-container p-2">
            <div class="flexed align-items-center justify-content-end w-100">
                <div class="flexed align-items-center w-100">
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
        if(this.readonly) this.$uiContainer.hide();
        this.twoDim();
    }

    /**
     * Creates a 2D Map using leaflet.
     */
    twoDim() {
        this.twoDimMap = L.map(this.$mapContainer.get(0)).setView([51, 10], 5);
        this.tiles = L.tileLayer('https://{s}.tile.openstreetmap.fr/hot/{z}/{x}/{y}.png', {
            maxZoom: 19,
        }).addTo(this.twoDimMap);

        this.attachEvents();
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

        if(this.readonly) return;

        // Activate the geo search for locations on the map
        if (typeof L.Control.Geocoder !== 'undefined') {
            L.Control.geocoder({
                defaultMarkGeocode: false
            })
                .on('markgeocode', (e) => {
                    const center = e.geocode.center;

                    // Update marker and circle if already set
                    if (this.currentMarker) ctx.twoDimMap.removeLayer(ctx.currentMarker);
                    if (this.currentCircle) ctx.twoDimMap.removeLayer(ctx.currentCircle);

                    ctx.currentMarker = L.marker(center).addTo(ctx.twoDimMap);
                    ctx.currentCircle = L.circle(center, {radius: 1000}).addTo(ctx.twoDimMap);
                    ctx.twoDimMap.setView(center, 13);
                    ctx.currentLongLat = center;

                    ctx.emit('stateChanged', { longLat: ctx.currentLongLat, radius: ctx.currentCircle._mRadius });
                })
                .addTo(ctx.twoDimMap);
        }

        // Place a marker. If a marker exists currently, delete the old one.
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

            ctx.emit('stateChanged', { longLat: ctx.currentLongLat, radius: ctx.currentCircle._mRadius });
        });

        // Radius changed, we need to update the circle of the marker then
        this.$uiContainer.on('input change', '.radius-input', function (e) {
            const newRadius = parseInt($(this).val(), 10);
            if (ctx.currentCircle) {
                ctx.currentCircle.setRadius(newRadius);
            }
            ctx.emit('stateChanged', { longLat: ctx.currentLongLat, radius: ctx.currentCircle._mRadius });
        });
    }
}

export {UCEMap}
