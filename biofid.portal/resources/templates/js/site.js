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

$(document).ready(function () {
    console.log('Webpage loaded!');
    // TODO: Just testing
    startNewSearch('asdas');
})