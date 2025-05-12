class UCEMap {

    constructor(container) {
        this.currentMarker = undefined;
        this.currentLongLat = undefined; // {lat: 0, lng: 0}
        this.currentCircle = undefined;

        this.$container = $(container);
        this.$container.addClass('uce-map');
        this.$container.append(`
        <div class="map-ui-container p-2">
            <div class="w-100 flexed align-items-center">
                <label class="mb-0 small-font mr-1" data-trigger="hover" data-placement="top" data-toggle="popover" data-content="In Meter">
                    Radius
                </label>
                <input class="radius-input w-100" type="range" min="1" value="1000" max="1000000"/>
            </div>
        </div>`);
        this.$container.append('<div class="map-container h-100"></div>');
        this.$mapContainer = this.$container.find('.map-container');
        this.$uiContainer = this.$container.find('.map-ui-container');
        this.twoDimMap();
    }

    /**
     * Creates a 2D Map using leaflet.
     */
    twoDimMap() {
        this.twoDimMap = L.map(this.$mapContainer.get(0)).setView([51, 10], 5);
        this.tiles = L.tileLayer('https://{s}.tile.openstreetmap.fr/hot/{z}/{x}/{y}.png', {
            maxZoom: 19,
        }).addTo(this.twoDimMap);

        this.attachEvents();
    }

    /**
     * We need to attach to some menu events, button events and the likes
     */
    attachEvents() {
        const ctx = this;

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
            ctx.currentCircle = L.circle(latlng, { radius: initialRadius }).addTo(ctx.twoDimMap);
        });

        // Radius changed, we need to update the circle of the marker then
        this.$uiContainer.on('input change', '.radius-input', function (e) {
            const newRadius = parseInt($(this).val(), 10);
            if (ctx.currentCircle) {
                ctx.currentCircle.setRadius(newRadius);
            }
        });
    }
}

export { UCEMap }
