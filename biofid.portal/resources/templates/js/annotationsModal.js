
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