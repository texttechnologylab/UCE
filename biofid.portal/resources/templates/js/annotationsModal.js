
let $currentDraggingObject = undefined;

$(document).ready(function() {
    console.log('D&D activated.')
});

$('body').on('dragstart', '#found-annotations-modal .draggable', function(event) {
    console.log('Dragging started');
    const $container = $(this);
    $container.addClass('dragging');
    $currentDraggingObject = $container;
    $('#found-annotations-modal .drop-container').addClass('highlighted');
});

$('body').on('dragover', '#found-annotations-modal .drop-container', function(event) {
    console.log('Dragging over');
    event.preventDefault();
});

$('body').on('drop','#found-annotations-modal .drop-container' , function(event) {
    console.log('Dragging dropped');
    event.preventDefault();
    if($currentDraggingObject === undefined) return;
    const uuid = generateUUID();
    let $dropItem = $currentDraggingObject.clone();
    // Hide the item in the list. We use that to show it later again.
    $currentDraggingObject.hide();
    $currentDraggingObject.attr('data-id', uuid);

    // Change the drag item container a bit. We want e.g. a delete btn now
    $dropItem.find('i').removeClass('fa-grip-vertical');
    $dropItem.find('i').addClass('fa-trash-alt small-font ml-1 remove-btn');
    $dropItem.attr('draggable', false);

    // Also check which kind of ne we have here and hence apply coloring
    const type = $('#found-annotations-modal .mtabs .selected-tab').data('id');
    $dropItem.find('.title').addClass('ne-' + type);

    // And mark the item with a unique id, as well as the original
    $dropItem.attr('data-id', uuid);

    $(this).append($dropItem);
});

$('body').on('dragend', '#found-annotations-modal .draggable', function(event) {
    console.log('Dragging ended');
    $('#found-annotations-modal .drop-container').removeClass('highlighted');
});

/**
 * Handle the removing of a dropped annotation
 */
$('body').on('click', '#found-annotations-modal .drop-container .remove-btn', function(event) {
    const $container = $(this).closest('.draggable');
    const id = $container.attr('data-id');
    // Show the item in the list again
    $('#found-annotations-modal .mannotation-list .draggable[data-id="{ID}"]'.replace('{ID}', id)).show();
    // Delete this from the drop container
    $container.remove();
});

/**
 * Handles the submission of the semantic role search
 */
$('body').on('click', '#found-annotations-modal .mfooter .submit-btn', function(){
    const $container = $('#found-annotations-modal .mfooter .bricks-container');
    $('.view[data-id="search"] .loader-container').first().fadeIn(150);

    // Fetch the data required for the request.
    const arg0 = $container.find('.drop-container[data-id="arg0"] .draggable')
        .map((index, element) => $(element).find('.title').html())
        .get();
    const arg1 = $container.find('.drop-container[data-id="arg1"] .draggable')
        .map((index, element) => $(element).find('.title').html())
        .get();
    const argm = $container.find('.drop-container[data-id="argm"] .draggable')
        .map((index, element) => $(element).find('.title').html())
        .get();
    const verb = $('#found-annotations-modal .mfooter .verb-input').val();
    const selectElement = document.getElementById("corpus-select");
    const selectedOption = selectElement.options[selectElement.selectedIndex];
    const corpusId = selectedOption.getAttribute("data-id");

    $.ajax({
        url: "/api/search/semanticRole",
        type: "POST",
        data: JSON.stringify({
            verb: verb,
            arg0: arg0,
            arg1: arg1,
            argm: argm,
            corpusId: corpusId
        }),
        contentType: "application/json",
        //dataType: "json",
        success: function (response) {
            $('.view .search-result-container').html(response);
            activatePopovers();
        },
        error: function (xhr, status, error) {
            console.error(xhr.responseText);
            $('.view .search-result-container').html(xhr.responseText);
        }
    }).always(function(){
        $('.view[data-id="search"] .loader-container').first().fadeOut(150);
        $('#found-annotations-modal').fadeOut(50);
    });

});