var selectedCorpus = -1;

function generateUUID() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        var r = Math.random() * 16 | 0,
            v = c == 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}

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
 * Fires whenever a new corpus is selected. We update some UI components then
 */
$('body').on('change', '#corpus-select', function(){
    const selectedOption = $(this).get(0).options[$(this).get(0).selectedIndex];
    const hasSr = selectedOption.getAttribute("data-hassr");
    const hasBiofidOnthology = selectedOption.getAttribute("data-hasbiofid");
    selectedCorpus = parseInt(selectedOption.getAttribute("data-id"));

    if(hasSr === 'true') $('.open-sr-builder-btn').show(50);
    else $('.open-sr-builder-btn').hide(50);

    if(hasBiofidOnthology === 'true') $('.taxonomy-tree-include').show();
    else $('.taxonomy-tree-include').hide();

    updateSearchHistoryUI();
})

/**
 * Triggers whenever an open-corpus insecptor button is clicked.
 */
$('body').on('click', '.open-corpus-inspector-btn', function () {
    // Get the selected corpus
    const selectElement = document.getElementById("corpus-select");
    const selectedOption = selectElement.options[selectElement.selectedIndex];
    const corpusId = selectedOption.getAttribute("data-id");
    $('.corpus-inspector-include').show(150);

    $.ajax({
        url: "/corpus?id=" + corpusId,
        type: "GET",
        success: function (response) {
            // Render the corpus view
            $('.corpus-inspector-include').html(response);

            // After that, we load in the corpus plot
            $.ajax({
                url: "/api/rag/plotTsne?corpusId=" + corpusId,
                type: "GET",
                success: function (response) {
                    // If the response is empty, then either no embeddings exist or something
                    // went wrong
                    if(response === ""){
                        $('.corpus-inspector-include .corpus-tsne-plot .error-msg').show();
                        $('.corpus-inspector-include .simple-loader').fadeOut(150);
                        return;
                    }
                    $('.corpus-inspector-include .corpus-tsne-plot').html(response);
                    // Bit buggy: The plot doesnt go full height, no matter what
                    // So I adjust it in script here.
                    const plotName = $('.corpus-inspector-include .corpus-tsne-plot .plotly-graph-div').attr("id");
                    Plotly.Plots.resize(plotName);
                },
                error: function (xhr, status, error) {
                    console.error(xhr.responseText);
                    $('.corpus-inspector-include .corpus-tsne-plot').html(xhr.responseText);
                },
                always: function (){
                    $('.corpus-inspector-include .simple-loader').fadeOut(150);
                }
            });
        },
        error: function (xhr, status, error) {
            console.error(xhr.responseText);
            $('.corpus-inspector-include').html(xhr.responseText);
        }
    });
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

/**
 * We have some UI components that need to be refreshed when the corpus is loaded.
 */
function reloadCorpusComponents(){
    $('#corpus-select').change();
}

$(document).ready(function () {
    console.log('Webpage loaded!');
    activatePopovers();
    reloadCorpusComponents();
})
