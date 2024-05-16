
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

    // If the item is being dragged from a drop-container, we delete the old one.
    // This means the user drags it from one field to another
    if($currentDraggingObject.parent('.drop-container').length)
        $currentDraggingObject.remove();
    else
        $currentDraggingObject.attr('data-id', uuid);

    // Change the drag item container a bit. We want e.g. a delete btn now
    $dropItem.find('i').removeClass('fa-grip-vertical');
    $dropItem.find('i').addClass('fa-trash-alt small-font ml-1 remove-btn');
    //$dropItem.attr('draggable', false);

    // Also check which kind of ne we have here and hence apply coloring
    const type = $('#found-annotations-modal .mtabs .selected-tab').data('id');
    $dropItem.find('.title').addClass('ne-' + type);

    // And mark the item with a unique id, as well as the original
    $dropItem.attr('data-id', uuid);

    $(this).append($dropItem);
    handleDragDropEnded();
});

$('body').on('dragend', '#found-annotations-modal .draggable', function(event) {
    handleDragDropEnded();
});

function handleDragDropEnded(){
    console.log('Dragging ended');
    $('#found-annotations-modal .drop-container').removeClass('highlighted');
}

/**
 * Handle the removing of a dropped annotation
 */
$('body').on('click', '#found-annotations-modal .drop-container .remove-btn', function(event) {
    const $container = $(this).closest('.draggable');
    const id = $container.attr('data-id');
    // Show the item in the list again, UPDATE: Don't do that anymore.
    //$('#found-annotations-modal .mannotation-list .draggable[data-id="{ID}"]'.replace('{ID}', id)).show();
    // Delete this from the drop container
    $container.remove();
});

/**
 * Handles the submission of the semantic role search
 */
$('body').on('click', '#found-annotations-modal .mfooter .submit-btn', function(){
    const $container = $('#found-annotations-modal .mfooter .bricks-container');

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

    // We enter the built query into the searchbar in correct form
    let query = "SR::";
    if(arg0.length > 0)
        query += "0=" + arg0.join(",") + ";";
    if(arg1.length > 0)
        query += "1=" + arg1.join(",") + ";";
    // query += "2=" + arg0.join(",") + ";";
    if(argm.length > 0)
        query += "m=" + argm.join(",") + ";";
    if(verb !== "")
        query += "v=" + verb + ";";

    console.log(query);
    $('.search-input').val(query);
    $('#found-annotations-modal').fadeOut(100);
});