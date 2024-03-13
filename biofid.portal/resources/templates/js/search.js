/**
 * Starts a new search with the given input
 */
function startNewSearch(searchInput){
    if(searchInput === undefined || searchInput === ''){
        return;
    }
    console.log('New Search with input: ' + searchInput);

    // Start a new search TODO: Outsource this into new prototype maybe
    $.ajax({
        url: "/api/search/default",
        type: "POST",
        data: JSON.stringify({
           searchInput: searchInput
        }),
        contentType: "application/json",
        //dataType: "json",
        success: function(response) {
            $('.view .search-result-container').html(response);
        },
        error: function(xhr, status, error) {
            console.error(xhr.responseText);
            $('.view .search-result-container').html(xhr.responseText);
        }
    });
}

/**
 * Handles the expanding and de-expanding of the annotation hit container in each document card
 */
$('body').on('click', '.search-result-container .annotation-hit-container-expander', function(){
    const $hitContainer = $(this).parent().next('.annotation-hit-container');
    const expanded = $(this).data('expanded');
    if(expanded){
        $(this).find('i').removeClass('fa-chevron-up').addClass('fa-chevron-down');
        $hitContainer.fadeOut(150);
    } else{
        $(this).find('i').removeClass('fa-chevron-down').addClass('fa-chevron-up');
        $hitContainer.fadeIn(150);
    }

    $(this).data('expanded', !expanded);
})