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