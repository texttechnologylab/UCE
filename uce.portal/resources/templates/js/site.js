var selectedCorpus = -1;
var currentView = undefined;
var reloadTimelineMap = false;

function generateUUID() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
        var r = Math.random() * 16 | 0,
            v = c == 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}

/**
 * Handles the clicking onto a navbar button
 */
$('body').on('click', 'nav .switch-view-btn', function () {
    // show the correct view
    const id = $(this).data('id');
    navigateToView(id);
})

function navigateToView(id) {
    // Close any potential modals:
    $('.corpus-inspector-include').hide(150)

    // Now adjust the main content
    $('.main-content-container .view').each(function () {
        if ($(this).data('id') === id) {
            $(this).show(50);
        } else {
            $(this).hide();
        }
    })

    // Show the correct button
    $('nav .switch-view-btn').each(function (b) {
        if ($(this).data('id') === id) {
            $(this).addClass('selected-nav-btn');
        } else {
            $(this).removeClass('selected-nav-btn');
        }
    });

    // Special handles
    if (id === 'timeline-map') {
        if (reloadTimelineMap) {
            setTimeout(function () {
                const map = window.graphVizHandler.createUceMap(document.getElementById('uce-timeline-map'), true);
                map.linkedTimelineMap(selectedCorpus);
            }, 750);
        }
    }

    currentView = id;
}

/**
 * Popups a modal with a message and a title.
 */
function showMessageModal(title, body) {
    const $modal = $('#messageModal');
    $modal.find('.modal-title').html(title);
    $modal.find('.modal-body').html(body);
    $modal.modal();
}

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
$('body').on('change', '#corpus-select', function () {
    handleCorpusSelectionChanged($(this));
})

/**
 * We need to hide and display different ui elements according to the corpus selected.
 * @param $select
 */
function handleCorpusSelectionChanged($select){
    const selectedOption = $select.get(0).options[$select.get(0).selectedIndex];
    const hasSr = selectedOption.getAttribute("data-hassr");
    const hasBiofidOnthology = selectedOption.getAttribute("data-hasbiofid");
    const sparqlAlive = selectedOption.getAttribute("data-sparqlalive");
    const hasEmbeddings = selectedOption.getAttribute("data-hasembeddings");
    const hasRagBot = selectedOption.getAttribute("data-hasragbot");
    const hasTimeAnnotations = selectedOption.getAttribute("data-hastimeannotations");
    const hasTaxonAnnotations = selectedOption.getAttribute("data-hastaxonannotations");
    const hasGeoNameAnnotations = selectedOption.getAttribute("data-hasgeonameannotations");
    const oldCorpusId = selectedCorpus;
    selectedCorpus = parseInt(selectedOption.getAttribute("data-id"));
    if (oldCorpusId !== selectedCorpus) {
        // We have switched corpora then, start a new empty search.
        startNewSearch("", false);
    }

    if (hasSr === 'true') $('.open-sr-builder-btn').show(50);
    else $('.open-sr-builder-btn').hide(50);

    if (hasEmbeddings === 'true') $('.search-settings-div input[data-id="EMBEDDINGS"]').closest('.option').show();
    else $('.search-settings-div input[data-id="EMBEDDINGS"]').closest('.option').hide();

    if (hasRagBot === 'true') $('.ragbot-chat-include').show();
    else $('.ragbot-chat-include').hide();

    // Change the UCE Metadata according to the corpus
    $('.uce-corpus-search-filter').each(function () {
        if ($(this).data('id') === selectedCorpus) $(this).show();
        else $(this).hide();
    })

    // Update the layered search. That requires annotations and without them, is useless.
    if (hasTaxonAnnotations === 'true') $('.layered-search-builder-container .choose-layer-popup a[data-type="TAXON"]').show();
    else $('.layered-search-builder-container .choose-layer-popup a[data-type="TAXON"]').hide();

    if (hasTimeAnnotations === 'true') $('.layered-search-builder-container .choose-layer-popup a[data-type="TIME"]').show();
    else $('.layered-search-builder-container .choose-layer-popup a[data-type="TIME"]').hide();

    if (hasGeoNameAnnotations === 'true') {
        $('.layered-search-builder-container .choose-layer-popup a[data-type="LOCATION"]').show();
        $('.site-container nav .nav-container .switch-view-btn[data-id="timeline-map"]').show();
        reloadTimelineMap = true;
    } else {
        $('.site-container nav .nav-container .switch-view-btn[data-id="timeline-map"]').hide();
        $('.layered-search-builder-container .choose-layer-popup a[data-type="LOCATION"]').hide();
        $('#uce-timeline-map').html(''); // Clear old map
        if (currentView === 'timeline-map') navigateToView('search');
    }

    if (hasTimeAnnotations === 'false' && hasTaxonAnnotations === 'false' && hasGeoNameAnnotations === 'false') {
        $('.open-layered-search-builder-btn-badge').hide();
        $('.open-layered-search-builder-btn').hide();
    } else {
        $('.open-layered-search-builder-btn-badge').show();
        $('.open-layered-search-builder-btn').show();
    }

    updateSearchHistoryUI();
}

/**
 * Triggers whenever an open-corpus inspector button is clicked.
 */
$('body').on('click', '.open-corpus-inspector-btn', function () {
    // Get the selected corpus
    let corpusId = $(this).data('id');
    if (corpusId === undefined) {
        const selectElement = document.getElementById("corpus-select");
        const selectedOption = selectElement.options[selectElement.selectedIndex];
        corpusId = selectedOption.getAttribute("data-id");
    }

    // If the wiki modal is currently open, close it.
    $('.corpus-inspector-include').show(0);
    $('.wiki-page-modal').addClass('wiki-page-modal-minimized');

    $.ajax({
        url: "/api/corpus/inspector?id=" + corpusId,
        type: "GET",
        success: function (response) {
            // Render the corpus view
            $('.corpus-inspector-include').html(response);

            // After that, we load documentsListView
            loadCorpusDocuments(corpusId, $('.corpus-inspector-include .corpus-documents-list-include'));
        },
        error: function (xhr, status, error) {
            console.error(xhr.responseText);
            $('.corpus-inspector-include').html(xhr.responseText);
        }
    });
})

/**
 * Loads the raw document list to a corpus into a target include.
 * @param corpusId
 * @param $target
 */
function loadCorpusDocuments(corpusId, $target) {
    $.ajax({
        url: "/api/corpus/documentsList?corpusId=" + corpusId + "&page=" + 1,
        type: "GET",
        success: function (response) {
            $target.html(response);
        },
        error: function (xhr, status, error) {
            $target.html(xhr.responseText);
        },
        always: function () {
            $target.find('.simple-loader').fadeOut(150);
        }
    });
}

/**
 * Triggers whenever an open-document element is clicked. This causes to load a new full read view of a doc
 */
$('body').on('click', '.open-document', function () {
    const id = $(this).data('id');
    // If this document is from a search, get it
    const searchId = $(this).data('searchid');
    openNewDocumentReadView(id, searchId);
})

/**
 * Triggers whenever an open-globe element is clicked. This causes to load a new full read view of a doc
 */
$('body').on('click', '.open-globe', function () {
    const id = $(this).data('id');
    const type = $(this).data('type');
    openNewGlobeView(type, id);
})

$('body').on('click', '.open-document-metadata', async function () {
    await $.ajax({
        url: "/api/document/reader/pagesList?id=" + id + "&skip=" + i,
        type: "GET",
        success: function (response) {
            // Render the new pages
            $('.reader-container .document-content').append(response);
            activatePopovers();
            for (let k = i + 1; k < Math.max(i + 10, pagesCount); k++) searchPotentialSearchTokensInPage(k);
        },
        error: function (xhr, status, error) {
            console.error(xhr.responseText);
            $('.reader-container .document-content').append(xhr.responseText);
        }
    }).always(function () {
        $('.site-container .loaded-pages-count').html(i);
    });
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
 */
function openNewDocumentReadView(id, searchId) {
    if (id === undefined || id === '') {
        return;
    }
    window.open("/documentReader?id=" + id + "&searchId=" + searchId, '_blank');
}

function activatePopovers() {
    $('[data-toggle="popover"]').popover();
}

/**
 * We have some UI components that need to be refreshed when the corpus is loaded.
 */
function reloadCorpusComponents() {
    handleCorpusSelectionChanged($('#corpus-select'));
}

$(document).ready(function () {
    console.log('Webpage loaded!');
    activatePopovers();
    reloadCorpusComponents();
    // Init the lexicon
    if (window.wikiHandler) window.wikiHandler.fetchLexiconEntries(0, 24);
})

