let LayeredSearchHandler = (function () {

    LayeredSearchHandler.prototype.layers = {};
    LayeredSearchHandler.prototype.searchId = "";
    LayeredSearchHandler.prototype.submitStatus = false;
    LayeredSearchHandler.prototype.locationMap = undefined;

    function LayeredSearchHandler() {
    }

    LayeredSearchHandler.prototype.init = function () {
        //this.addNewLayer(1);
        this.searchId = generateUUID().toString().replaceAll("-", "");
    }

    LayeredSearchHandler.prototype.setLayerIsLoading = function (depth, isLoading) {
        let $layer = $('.layered-search-builder-container .layer-container[data-depth="' + depth + '"]');
        if (isLoading) $layer.find('.apply-layer-btn').addClass('loading-btn');
        else $layer.find('.apply-layer-btn').removeClass('loading-btn');
    }

    LayeredSearchHandler.prototype.updateUIBatch = function () {
        const status = this.submitStatus;
        if (status === true) {
            $('.search-header .open-layered-search-builder-btn-badge').html(Object.keys(this.layers).length);
        } else {
            $('.search-header .open-layered-search-builder-btn-badge').html('0');
        }
    }

    LayeredSearchHandler.prototype.setSubmitStatus = function ($clickedBtn) {
        const status = $clickedBtn.attr('data-submit') === 'true';
        if (status === true && Object.keys(this.layers).length === 0) {
            showMessageModal('No Layers', 'You can only apply if you have added at least one layer.');
            return;
        }
        $('.layered-search-builder-container .submit-div a').each(function () {
            if ($(this).attr('data-submit') !== status.toString()) $(this).removeClass('activated');
            else $(this).addClass('activated');
        })
        $('.search-settings-div .submit-layered-search-input').val(status);
        this.submitStatus = status;
        this.updateUIBatch();
    }

    LayeredSearchHandler.prototype.deleteSlot = function ($slot) {
        const $layer = $slot.closest('.layer-container');
        const depth = $layer.attr('data-depth');
        if (!this.layers[depth]) return;
        let newSlotList = [];
        this.layers[depth].forEach(($s) => {
            if ($s.attr('data-id') === $slot.attr('data-id')) $s.remove();
            else newSlotList.push($s);
        })
        this.layers[depth] = newSlotList;
        if (newSlotList.length === 0) $layer.find('.empty-slot').get(0).style.maxWidth = '100%';
        this.markLayersAsDirty(depth);
    }

    LayeredSearchHandler.prototype.deleteLayer = function (depth) {
        delete this.layers[depth];
        let newLayers = {};
        Object.keys(this.layers).forEach((k) => {
            if (k > depth) newLayers[k - 1] = this.layers[k];
            else newLayers[k] = this.layers[k];
        })
        this.layers = newLayers;
        let _this = this;
        // Delete the UI element
        $('.layered-search-builder-container .layers-container .layer-container').each(function () {
            let dataDepth = $(this).attr('data-depth');
            if (dataDepth === depth) $(this).remove();
            else if (dataDepth > depth) {
                const newDepth = dataDepth - 1;
                $(this).attr('data-depth', newDepth);
                $(this).find('.depth-label').html(newDepth);
                _this.markLayersAsDirty(newDepth);
            }
        });
        this.updateUIBatch();
    }

    LayeredSearchHandler.prototype.addNewLayerAtEnd = function () {
        const depth = Object.keys(this.layers).length + 1;
        this.addNewLayer(depth);
    }

    LayeredSearchHandler.prototype.addNewLayer = function (depth) {
        let $template = $('.layered-search-builder-container .layer-template').clone();
        $template.find('.layer-container').attr('data-depth', depth);
        $template.find('.depth-label').html(depth);
        $('.layered-search-builder-container .layers-container').append($template.html());
        this.layers[depth] = [];
        this.updateUIBatch();
    }

    LayeredSearchHandler.prototype.addNewSlot = function ($btn) {
        const type = $btn.data('type');
        const depth = parseInt($btn.closest('.layer-container').attr('data-depth'));
        const $slot = $btn.closest('.empty-slot');
        $slot.get(0).style.maxWidth = '100px';

        // Clone the template, set it up with ids and whatnot and add it to UI and our layers dictionary
        const $htmlTemplate = $('.layered-search-builder-container .slot-templates .template-' + type).clone();
        const id = generateUUID();
        $htmlTemplate.attr('data-id', id);
        const $layer = $btn.closest('.layer');
        $layer.prepend($htmlTemplate);
        this.layers[depth].push($htmlTemplate);
        $btn.closest('.choose-layer-popup').toggle(50);
        this.markLayersAsDirty(depth, false);

        // If the type is a location, we need to setup the leaflet map
        if (type === "LOCATION") {
            const uceMap = graphVizHandler.createUceMap($layer.find('.slot[data-id="[ID]"] .location-map'.replace('[ID]', id)).get(0));
            uceMap.twoDim();
            // Subscribe to the events
            const $slot = $('.slot[data-id="[ID]"]'.replace('[ID]', id));
            const $slotInput = $slot.find('.slot-value');
            const ctx = this;
            uceMap.on('stateChanged', function (e) {
                if (e.longLat && e.radius) {
                    $slotInput.val('R::lng=' + e.longLat.lng.toFixed(2) + ";lat=" + e.longLat.lat.toFixed(2) + ";r=" + e.radius.toFixed(2));
                    ctx.markLayersAsDirty(depth, false);
                }
            });
            // There is a weird bug with leaflet and its re-sizing. I need to manually hide it here once.
            $slot.find('.location-map').hide();
        }
    }

    LayeredSearchHandler.prototype.buildApplicableLayers = function (applicableDepths) {
        let applicableLayers = [];
        if (applicableDepths.length === 0) applicableDepths = Object.keys(this.layers);

        applicableDepths.forEach((d) => {
            this.setLayerIsLoading(d, true);
            let slots = this.layers[d];

            // Every slot has information about a queryable filter for our database. We pass
            // this information onto our backend and let it apply it, see how it goes.
            let slotDtos = [];
            for (let i = 0; i < slots.length; i++) {
                let $slot = slots[i];
                const value = $slot.find('.slot-value').val();
                const type = $slot.data('type');
                slotDtos.push({type: type, value: value});
            }
            let layer = {
                depth: d,
                count: -1,
                slots: slotDtos
            }
            applicableLayers.push(layer);
        })
        return applicableLayers;
    }

    LayeredSearchHandler.prototype.applyLayerSearch = async function (depth) {
        if (!this.layers[depth]) return;
        let applicableDepths = Object.keys(this.layers).filter(d => d <= depth);
        let applicableLayers = this.buildApplicableLayers(applicableDepths);

        $.ajax({
            url: "/api/search/layered",
            type: "POST",
            data: JSON.stringify({
                searchId: this.searchId,
                layers: JSON.stringify(applicableLayers),
            }),
            contentType: "application/json",
            success: (response) => {
                this.updateLayerResults(response);
            },
            error: function (xhr, status, error) {
                showMessageModal("Searched Layer Error", xhr.responseText);
            }
        }).always(() => {
            applicableDepths.forEach((d) => this.setLayerIsLoading(d, false));
        });
    }

    LayeredSearchHandler.prototype.markLayersAsDirty = function (depth, apply = true) {
        let applicableDepths = Object.keys(this.layers).filter(d => d >= depth);
        applicableDepths.forEach((d) => {
            let $layer = $('.layers-container .layer-container[data-depth="' + d + '"]');
            let $metadata = $layer.find('.layer-metadata-container');
            $metadata.find('.document-hits').html('?');
            $metadata.find('.page-hits').html('?');
            $metadata.find('.apply-layer-btn').removeClass('applied');
        });

        // Automatic recalculation?
        if (apply) {
            this.applyLayerSearch(Math.max(...applicableDepths.map(d => parseInt(d, 10))));
        }
    }

    LayeredSearchHandler.prototype.updateLayerResults = function (layerResults) {
        for (let i = 0; i < layerResults.length; i++) {
            let curLayer = layerResults[i];
            let $uiLayer = $('.layers-container .layer-container[data-depth="' + curLayer.depth + '"]');
            let $metadata = $uiLayer.find('.layer-metadata-container');
            $metadata.find('.document-hits').html(curLayer.documentHits);
            $metadata.find('.page-hits').html(curLayer.pageHits);
            $metadata.find('.apply-layer-btn').addClass('applied');
        }
    }

    return LayeredSearchHandler;
}());

function getNewLayeredSearchHandler() {
    return new LayeredSearchHandler();
}

/**
 * Triggers when a new layer with a certain type is being created.
 */
$('body').on('click', '.layered-search-builder-container .choose-layer-popup a', function () {
    window.layeredSearchHandler.addNewSlot($(this));
})

/**
 * Triggers when we apply a layer filter.
 */
$('body').on('click', '.layered-search-builder-container .apply-layer-btn', async function () {
    const depth = $(this).closest('.layer-container').attr('data-depth');
    window.layeredSearchHandler.applyLayerSearch(parseInt(depth));
})

/**
 * Triggers when we change any slot of a layer.
 */
$('body').on('change', '.layered-search-builder-container .layer .slot .slot-value', async function () {
    const depth = $(this).closest('.layer-container').attr('data-depth');
    window.layeredSearchHandler.markLayersAsDirty(parseInt(depth));
})

/**
 * Triggers when we want to add a new layer.
 */
$('body').on('click', '.layered-search-builder-container .add-new-layer-btn', function () {
    window.layeredSearchHandler.addNewLayerAtEnd();
})

/**
 * Triggers when we want to delete a layer.
 */
$('body').on('click', '.layered-search-builder-container .layer-container .delete-layer-btn', function () {
    window.layeredSearchHandler.deleteLayer($(this).closest('.layer-container').attr('data-depth'));
})

/**
 * Triggers when we want to delete a layer.
 */
$('body').on('click', '.layered-search-builder-container .submit-div a', function () {
    window.layeredSearchHandler.setSubmitStatus($(this));
})

/**
 * Triggers when we want to delete a slot.
 */
$('body').on('click', '.layered-search-builder-container .layer-container .slot .delete-slot-btn', function () {
    window.layeredSearchHandler.deleteSlot($(this).closest('.slot'));
})

$(document).ready(function () {
    window.layeredSearchHandler = getNewLayeredSearchHandler();
    window.layeredSearchHandler.init();
})