/**
 * Handles the clicking onto a navbar button
 */
$('body').on('click', 'nav .nav-buttons a', function () {
    // show the correct view
    var id = $(this).data('id');
    console.log(id);
    $('.main-content-container .view').each(function () {
        if ($(this).data('id') === id) {
            $(this).show(150);
        } else {
            $(this).hide();
        }
    })

    // Show the correct button
    $('nav .nav-buttons a').each(function (b) {
        $(this).removeClass('selected-nav-btn');
    });
    $(this).addClass('selected-nav-btn');
})

/**
 * Start a search by pressing Enter
 */
$('body').on('keydown', '.view .search-input', function (event) {
    var id = event.key || event.which || event.keyCode || 0;
    if (id === 'Enter') {
        startNewSearch($(this).val())
    }
})

/**
 * Start a new search by pressing the search btn
 */
$('body').on('click', '.view .search-btn', function (event) {
    startNewSearch($('.view .search-input').val());
})

/**
 * Triggers whenever an open-document element is clicked. This causes to load a new full read vioew of a doc
 */
$('body').on('click', '.open-document', function () {
    var id = $(this).data('id');
    openNewDocumentReadView(id);
})

/**
 * Triggers whenever an open-globe element is clicked. This causes to load a new full read vioew of a doc
 */
$('body').on('click', '.open-globe', function () {
    const id = $(this).data('id');
    const type = $(this).data('type');
    openNewGlobeView(type, id);
})

/**
 * Opens a new globe view
 * @param modelId
 */
function openNewGlobeView(type, id) {
    if (id === undefined || id === '') {
        return;
    }
    console.log('New Globe View for: ' + id);
    window.open("/globe?id=" + id + "&type=" + type, '_blank');
}

/**
 * Opens a new Document reader view
 * @param modelId
 */
function openNewDocumentReadView(id) {
    if (id === undefined || id === '') {
        return;
    }
    console.log('New Document Reader View for: ' + id);
    window.open("/documentReader?id=" + id, '_blank');
}

function activatePopovers() {
    $('[data-toggle="popover"]').popover();
}

$(document).ready(function () {
    console.log('Webpage loaded!');
    activatePopovers();
    // TODO: Just testing
    //startNewSearch('1885');
})