let LayeredSearchHandler = (function () {

    LayeredSearchHandler.prototype.layers = {1: []};
    LayeredSearchHandler.prototype.searchId = "";

    function LayeredSearchHandler() {
    }

    LayeredSearchHandler.prototype.init = function () {
        this.addNewLayer(1);
        this.searchId = generateUUID().toString().replaceAll("-", "");
    }

    LayeredSearchHandler.prototype.addNewLayer = function (depth) {
        let $template = $('.layered-search-builder-container .layer-template').clone();
        $template.find('.layer').attr('data-depth', depth);
        $template.find('.depth-label').html(depth);
        $('.layered-search-builder-container .layers-container').append($template.html());
    }

    LayeredSearchHandler.prototype.addNewSlot = function ($btn) {
        const type = $btn.data('type');
        const depth = parseInt($btn.closest('.layer').data('depth'));
        const $slot = $btn.closest('.empty-slot');
        $slot.get(0).style.maxWidth = '100px';

        // If we haven't added a new layer depth, do so, so the user can add new depths
        if (!this.layers[depth + 1]) {
            this.layers[depth + 1] = [];
            this.addNewLayer(depth + 1);
        }

        // Clone the template, set it up with ids and whatnot and add it to UI and our layers dictionary
        const $htmlTemplate = $('.layered-search-builder-container .slot-templates .template-' + type).clone();
        $htmlTemplate.attr('data-id', generateUUID());
        $btn.closest('.layer').prepend($htmlTemplate);
        this.layers[depth].push($htmlTemplate);
        $btn.closest('.choose-layer-popup').toggle(50);
    }

    LayeredSearchHandler.prototype.applyLayerSearch = async function (depth) {
        if (!this.layers[depth]) return;
        let slots = this.layers[depth];

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
            depth: depth,
            slots: slotDtos
        }
        console.log(slotDtos);

        $.ajax({
            url: "/api/search/layered",
            type: "POST",
            data: JSON.stringify({
                searchId: this.searchId,
                layers: JSON.stringify([layer]),
            }),
            contentType: "application/json",
            //dataType: "json",
            success: async function (response) {
            },
            error: function (xhr, status, error) {
                showMessageModal("Searched Layer Error", xhr.responseText);
            }
        }).always(function () {
        });
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
    const depth = $(this).closest('.layer-container').find('.layer').data('depth');
    window.layeredSearchHandler.applyLayerSearch(parseInt(depth));
})

$(document).ready(function () {
    window.layeredSearchHandler = getNewLayeredSearchHandler();
    window.layeredSearchHandler.init();
})