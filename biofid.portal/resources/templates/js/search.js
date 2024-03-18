/**
 * Starts a new search with the given input
 */
function startNewSearch(searchInput) {
    if (searchInput === undefined || searchInput === '') {
        return;
    }
    console.log('New Search with input: ' + searchInput);
    $('.view[data-id="search"] .loader-container').first().fadeIn(150);
    // Get the selected corpus
    const selectElement = document.getElementById("corpus-select");
    const selectedOption = selectElement.options[selectElement.selectedIndex];
    const corpusId = selectedOption.getAttribute("data-id");

    // Start a new search TODO: Outsource this into new prototype maybe
    $.ajax({
        url: "/api/search/default",
        type: "POST",
        data: JSON.stringify({
            searchInput: searchInput,
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
    });
}

/**
 * Handles the loading of the next pages
 */
$('body').on('click', '.search-result-container .page-btn', function () {
    // We dont reload the documents we currently show.
    if ($(this).hasClass('current-page')) return;
    const page = $(this).data('page');
    handleSwitchingOfPage(page);
    $(this).addClass('current-page');
})

$('body').on('click', '.search-result-container .next-page-btn', function () {
    let curPage = parseInt($('.search-result-container .pagination').data('cur'));
    let max = parseInt($('.search-result-container .pagination').data('max'));
    let newPage = curPage - 1;
    if($(this).data('direction') === "+") newPage += 2;
    if(newPage <= 0 || newPage > max) return;
    handleSwitchingOfPage(newPage);
})

async function handleSwitchingOfPage(page) {
    const searchId = $('.search-state').data('id');
    $('.search-result-container .loader-container').first().fadeIn(150);

    $.ajax({
        url: "/api/search/active/page?searchId=" + searchId + "&page=" + page,
        type: "GET",
        dataType: "json",
        success: function (response) {
            // Render the new documents
            $('.view .search-result-container .document-list-include').html(response.documentsList);
            $('.view .search-result-container .navigation-include').html(response.navigationView);
        },
        error: function (xhr, status, error) {
            console.error(xhr.responseText);
            $('.view .search-result-container .document-list-include').html(xhr.responseText);
        }
    }).always(function(){
        $('.search-result-container .loader-container').first().fadeOut(150);
    });
}

/**
 * Handles the expanding and de-expanding of the annotation hit container in each document card
 */
$('body').on('click', '.search-result-container .annotation-hit-container-expander', function () {
    const $hitContainer = $(this).parent().next('.annotation-hit-container');
    const expanded = $(this).data('expanded');
    if (expanded) {
        $(this).find('i').removeClass('fa-chevron-up').addClass('fa-chevron-down');
        $hitContainer.fadeOut(150);
    } else {
        $(this).find('i').removeClass('fa-chevron-down').addClass('fa-chevron-up');
        $hitContainer.fadeIn(150);
    }

    $(this).data('expanded', !expanded);
})

/**
 * Handles the sorting of the documents through their sort buttons
 */
$('body').on('click', '.sort-container .sort-btn', function(){
    const orderBy = $(this).data('orderby');
    const curOrder = $(this).data('curorder');
    const searchId = $('.search-state').data('id');
    $('.search-result-container .loader-container').first().fadeIn(150);

    $.ajax({
        url: "/api/search/active/sort?searchId=" + searchId + "&order=" + curOrder + "&orderBy=" + orderBy,
        type: "GET",
        success: function (response) {
            // Render the new documents
            $('.view .search-result-container .document-list-include').html(response);
        },
        error: function (xhr, status, error) {
            console.error(xhr.responseText);
            $('.view .search-result-container .document-list-include').html(xhr.responseText);
        }
    }).always(function (){
        $('.search-result-container .loader-container').first().fadeOut(150);
    });

    // Highlight the correct button
    if(curOrder === "ASC"){
        $(this).find('i').removeClass('fa-sort-amount-up').addClass('fa-sort-amount-down');
        $(this).data('curorder', 'DESC');
    } else{
        $(this).find('i').removeClass('fa-sort-amount-down').addClass('fa-sort-amount-up');
        $(this).data('curorder', 'ASC');
    }

    $(this).closest('.sort-container').find('.sort-btn').each(function(){
        $(this).removeClass('active-sort-btn');
    })
    $(this).addClass('active-sort-btn');
})