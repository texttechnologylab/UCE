let $currentDraggingObject = undefined;

$(document).ready(function () {
    console.log('D&D activated.')
});

$('body').on('dragstart', '#found-annotations-modal .draggable', function (event) {
    console.log('Dragging started');
    const $container = $(this);
    $container.addClass('dragging');
    $currentDraggingObject = $container;
    $('#found-annotations-modal .drop-container').addClass('highlighted');
});

$('body').on('dragover', '#found-annotations-modal .drop-container', function (event) {
    console.log('Dragging over');
    event.preventDefault();
});

$('body').on('drop', '#found-annotations-modal .drop-container', function (event) {
    console.log('Dragging dropped');
    event.preventDefault();
    if ($currentDraggingObject === undefined) return;
    const uuid = generateUUID();
    let $dropItem = $currentDraggingObject.clone();

    // If the item is being dragged from a drop-container, we delete the old one.
    // This means the user drags it from one field to another
    if ($currentDraggingObject.parent('.drop-container').length)
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

$('body').on('dragend', '#found-annotations-modal .draggable', function (event) {
    handleDragDropEnded();
});

function handleDragDropEnded() {
    console.log('Dragging ended');
    $('#found-annotations-modal .drop-container').removeClass('highlighted');
}

/**
 * Handle the removing of a dropped annotation
 */
$('body').on('click', '#found-annotations-modal .drop-container .remove-btn', function (event) {
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
$('body').on('click', '#found-annotations-modal .mfooter .submit-btn', function () {
    const $container = $('#found-annotations-modal .mfooter .bricks-container');

    // Fetch the data required for the request.
    const arg0 = $container.find('.drop-container[data-id="arg0"] .draggable')
        .map((index, element) => $(element).find('.title').html())
        .get();
    const arg1 = $container.find('.drop-container[data-id="arg1"] .draggable')
        .map((index, element) => $(element).find('.title').html())
        .get();
    const arg2 = $container.find('.drop-container[data-id="arg2"] .draggable')
        .map((index, element) => $(element).find('.title').html())
        .get();
    const argm = $container.find('.drop-container[data-id="argm"] .draggable')
        .map((index, element) => $(element).find('.title').html())
        .get();
    const verb = $('#found-annotations-modal .mfooter .verb-input').val();

    // We enter the built query into the searchbar in correct form
    let query = "SR::";
    if (arg0.length > 0)
        query += "0=" + arg0.join(",") + ";";
    if (arg1.length > 0)
        query += "1=" + arg1.join(",") + ";";
    if (arg2.length > 0)
        query += "2=" + arg2.join(",") + ";";
    if (argm.length > 0)
        query += "m=" + argm.join(",") + ";";
    if (verb !== "")
        query += "v=" + verb + ";";

    $('.search-input').val(query);
    $('#found-annotations-modal').fadeOut(100);
});

/**
 * Handles the search button interactions
 */
$('body').on('click', '#found-annotations-modal .views .search-annotation-btn', function (event) {
    const $searchInput = $('#found-annotations-modal .views .search-annotation-input');
    const $backdrop = $('#found-annotations-modal .views .search-backdrop');
    let expanded = $searchInput.data('expanded');
    if (expanded) {
        // Submit search
        searchAnnotations($searchInput.val())
    } else {
        $searchInput.show(50);
        $searchInput.focus();
        $backdrop.fadeIn(150);
    }
    $searchInput.data('expanded', !expanded);
});

$('body').on('keydown', '#found-annotations-modal .views .search-annotation-input', function (event) {
    if (event.keyCode === 13) {
        searchAnnotations($(this).val());
    }
});

$('body').on('click', '#found-annotations-modal .views .search-backdrop', function (event) {
    const $searchInput = $('#found-annotations-modal .views .search-annotation-input');
    $searchInput.data('expanded', false);
    $searchInput.hide(50);
    $(this).fadeOut(150);
});

/**
 * Searches through the annotations and shows only those that match
 * @param searchTerm
 */
function searchAnnotations(searchTerm) {
    searchTerm = searchTerm.trim().toLowerCase().replace(' ', '');
    const curViewId = $('#found-annotations-modal .mtabs .selected-tab').data('id');
    const $curViewElement = $('#found-annotations-modal .views .mview[data-id="' + curViewId + '"]');
    $curViewElement.find('.mannotation-list .dcontainer').each(function () {
        let content = $(this).find('.title').html().trim().toLowerCase().replace(' ', '');
        if (searchTerm === '' || searchTerm === undefined || content.includes(searchTerm)) {
            $(this).parent().show();
        } else {
            $(this).parent().hide();
        }
    });
}


/**
 * Handles the navigating in the annotations modal
 */
$('body').on('click', '#found-annotations-modal .mtabs btn', function(){
    const id = $(this).data('id');

    $('#found-annotations-modal .views .mview').each(function(){
        if($(this).data('id') === id){
            $(this).show(100);
        } else{
            $(this).hide();
        }
    })

    $('#found-annotations-modal .mtabs btn').each(function(){
        if($(this).data('id') === id){
            $(this).addClass('selected-tab');
        } else{
            $(this).removeClass('selected-tab');
        }
    })

    searchAnnotations('');
})