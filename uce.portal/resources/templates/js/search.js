let currentCorpusUniverseHandler = undefined;

$('body').on('click', '#search-viz-update-button', function (e) {
    // TODO more error handling
    const nBins = parseInt($('#search-viz-n-bins').val())
    window.searchVizualization.settings.nBins = nBins

    const selectedFeature = $('#search-viz-selected-feature').val()
    window.searchVizualization.settings.selectedFeature = selectedFeature

    updateSearchVizualization()

    e.preventDefault()
})

function createHistogramData(data, bins) {
    // extract only the values, filter nan
    const values = data.map(d => parseFloat(d.value)).filter(v => !isNaN(v))

    // see https://github.com/texttechnologylab/TextAnnotatorReloaded/blob/main/src/components/BarChart.tsx
    const min = Math.min(...values)
    const max = Math.max(...values)
    const range = max - min

    const bucketSize = range / bins
    const buckets = new Array(bins).fill(0)
    for (const value of values) {
        const bucketIndex = Math.min(
            Math.floor((value - min) / bucketSize),
            bins-1
        )
        buckets[bucketIndex]++
    }

    const labels = buckets.map((_, index) => {
        const start = (min + index * bucketSize).toFixed(3)
        const end = (min + (index + 1) * bucketSize).toFixed(3)
        return start.toString() + " - " + end.toString()
    })

    return [buckets, labels]
}

function updateSearchVizualization() {
    // TODO global state?
    const data = window.searchVizualization.vizData["data"]
    const currentPage = window.searchVizualization.vizData["currentPage"]
    const nBins = window.searchVizualization.settings.nBins
    if(data === undefined) return;

    // set the selected feature to the first one if not set
    const firstFeature = Object.keys(data).sort().shift()
    const selectedFeature = window.searchVizualization.settings.selectedFeature || firstFeature

    // update features options based on current data
    const selectElem = document.getElementById('search-viz-selected-feature')
    while (selectElem.firstChild) {
        selectElem.removeChild(selectElem.lastChild)
    }
    Object.keys(data).sort().forEach(category => {
        const option = document.createElement('option')
        option.value = category
        option.textContent = category
        if (category === selectedFeature) {
            option.selected = true
        }
        selectElem.appendChild(option)
    })

    const chartElem = document.getElementById('search-results-visualization-graph')
    while (chartElem.firstChild) {
        chartElem.removeChild(chartElem.lastChild)
    }

    if (data && Object.keys(data).length > 0 && selectedFeature in data) {
        let [chartData, chartLabels] = createHistogramData(data[selectedFeature], nBins)
        console.log("chart_data", chartData)
        console.log("chart_labels", chartLabels)

        const numDocs = data[selectedFeature].length

        const title = `${languageResource.get("searchVisualizationPlotTitleTemplate")}`
            .replace("{selectedFeature}", selectedFeature)
            .replace("{currentPage}", currentPage.toString())
            .replace("{numDocs}", numDocs.toString())

        window.graphVizHandler.createBasicChart(
            chartElem,
            title,
            {
                "labels": chartLabels,
                "data": chartData,
            },
            'bar',
        )
    }
}

/**
 * Starts a new search with the given input
 */
function startNewSearch(searchInput, reloadCorpus = true) {
    if (searchInput === undefined) {
        return;
    }

    $('.search-menu-div').hide();
    $('.view[data-id="search"] .loader-container').first().fadeIn(150);
    // Get the selected corpus
    const selectElement = document.getElementById("corpus-select");
    const selectedOption = selectElement.options[selectElement.selectedIndex];
    const corpusId = selectedOption.getAttribute("data-id");

    // Get the selected search layers
    const fulltextOrNeLayer = $('.search-menu-div .search-settings-div input[name="searchLayerRadioOptions"]:checked').val();
    const embeddings = $('.search-menu-div .search-settings-div .option input[data-id="EMBEDDINGS"]').is(':checked');
    const kwic = $('.search-menu-div .search-settings-div .option input[data-id="KWIC"]').is(':checked');
    const enrich = $('.search-menu-div .search-settings-div .option input[data-id="ENRICH"]').is(':checked');
    const proMode = $('#proModeSwitch').is(':checked');
    const useLayeredSearch = $('.search-menu-div .search-settings-div .submit-layered-search-input').val() === 'true';
    let layers = {};
    let layeredSearchId = '';
    if (useLayeredSearch === true) {
        layers = window.layeredSearchHandler.buildApplicableLayers([]);
        layeredSearchId = window.layeredSearchHandler.searchId;
    }

    // Get possible uce metadata filters of this selectec corpus
    let metadataFilters = [];
    $('.uce-corpus-search-filter[data-id="' + corpusId + '"]').find('.filter-div').each(function () {
        const key = $(this).find('label').html()
        const valueType = $(this).data('type')
        if (valueType === 'NUMBER') {
            // NUMBER type is a range
            const min = parseFloat($(this).find('input[data-range="min"]').val())
            const max = parseFloat($(this).find('input[data-range="max"]').val())
            metadataFilters.push({
                'key': key,
                'valueType': valueType,
                'min': min,
                'max': max,
                'value': "",  // TODO value must not be null in the backend, change later?
            })
        }
        else {
            metadataFilters.push({
                'key': key,
                'valueType': valueType,
                'value': $(this).find('input').val(),
            })
        }
    })

    // Start a new search TODO: Outsource this into new prototype maybe
    $.ajax({
        url: "/api/search/default",
        type: "POST",
        data: JSON.stringify({
            searchInput: searchInput,
            corpusId: corpusId,
            fulltextOrNeLayer: fulltextOrNeLayer,
            useEmbeddings: embeddings,
            kwic: kwic,
            enrich: enrich,
            uceMetadataFilters: JSON.stringify(metadataFilters),
            proMode: proMode,
            layeredSearchId: layeredSearchId,
            layers: JSON.stringify(layers),
        }),
        contentType: "application/json",
        //dataType: "json",
        success: async function (response) {
            $('.view .search-result-container').html(response);
            activatePopovers();
            if (reloadCorpus) {
                reloadCorpusComponents();
                // Store the search in the local browser for a history.
                addSearchToHistory(searchInput);
            }
            // Load the corpus universe from search
            const searchId = $('.search-state').data('id');
            currentCorpusUniverseHandler = getNewCorpusUniverseHandler;
            await currentCorpusUniverseHandler.createEmptyUniverse('search-universe-container');
            await currentCorpusUniverseHandler.fromSearch(searchId);
        },
        error: function (xhr, status, error) {
            if (xhr.status === 406) {
                showMessageModal("Query Error", xhr.responseText);
            } else {
                $('.view .search-result-container').html(xhr.responseText);
            }
        }
    }).always(function () {
        $('.view[data-id="search"] .loader-container').first().fadeOut(150);
    });
}

/**
 * Adds a new search to the history in the local browser
 */
function addSearchToHistory(searchTerm) {
    if (searchTerm === '' || searchTerm === undefined) return;

    let history = getSearchHistory();
    // If the latest entry in the search history is the same search as now, we
    // dont need to add it. It clouds the history.
    if (history.length > 0 && history[history.length - 1].searchTerm === searchTerm) return;
    history.push({
        'searchTerm': searchTerm,
        'corpusId': selectedCorpus,
        'date': new Date().toLocaleDateString()
    });
    localStorage.setItem('searchHistory', JSON.stringify(history));
    updateSearchHistoryUI();
}

/**
 * Handles the opening of the current corpus universe
 */
$('body').on('click', '.open-corpus-universe-btn', function () {
    if (currentCorpusUniverseHandler === undefined) return;
    currentCorpusUniverseHandler.openUniverseInNewTab(selectedCorpus,)
})

/**
 * Gets the search history from the local storage
 * @returns {*[]}
 */
function getSearchHistory() {
    let historyJson = localStorage.getItem('searchHistory');
    let history = [];
    if (historyJson !== null) {
        history = JSON.parse(historyJson);
    }
    return history;
}

/**
 * Returns the searchHistory object filtered for the corpus
 * @param corpusId
 */
function getSearchHistoryOfCorpus(corpusId) {
    return getSearchHistory().filter(h => h.corpusId === corpusId).reverse();
}

/**
 * Gets the current search history, filters it and places it in the UI
 */
function updateSearchHistoryUI() {
    const history = getSearchHistoryOfCorpus(selectedCorpus);
    const $historyDiv = $('.search-menu-div .search-history-div');
    $historyDiv.html('');
    history.forEach((item) => {
        let html = `
            <#noparse>
            <div class="search-history-entry">
                <p class="text"><i class="fas fa-search mr-1"></i> <span class="content">${item.searchTerm}</span></p>
            </div>
            </#noparse>
        `;
        $historyDiv.append(html);
    });
}

/**
 * Handles the inserting of a search item into the searchbar
 */
$('body').on('click', '.search-history-div .search-history-entry', function () {
    $('.search-input').val($(this).find('.content').text());
})

/**
 * Handles the opening of the search dropdown menu below the searchbar
 */
$('body').on('focus', '.search-input', function () {
    updateSearchHistoryUI();
    $('.search-menu-div').show();
})

/**
 * Removes the search menu when clicking anywhere but the search menu
 */
$('body').on('click', '.search-menu-div .backdrop', function () {
    $('.search-menu-div').hide();
})

/**
 * Handles the opening of the sr builder
 */
$('body').on('click', '.open-sr-builder-btn', function () {
    // Get the selected corpus
    const selectElement = document.getElementById("corpus-select");
    const selectedOption = selectElement.options[selectElement.selectedIndex];
    const corpusId = selectedOption.getAttribute("data-id");
    // Show Loading
    $(this).find('i').removeClass('fa-project-diagram').addClass('rotate fa-spinner');

    $.ajax({
        url: "api/search/semanticRole/builder?corpusId=" + corpusId,
        type: "GET",
        //dataType: "json",
        success: function (response) {
            console.log(response);
            $('.sr-query-builder-include').html(response);
            activatePopovers();
        },
        error: function (xhr, status, error) {
            showMessageModal("Error", "Error opening the SR builder.");
            console.error(xhr.responseText);
        }
    }).always(function () {
        $('.open-sr-builder-btn').find('i').addClass('fa-project-diagram').removeClass('rotate fa-spinner');
    });
})

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
    const $pagination = $('.search-result-container .pagination');
    let curPage = parseInt($pagination.data('cur'));
    let max = parseInt($pagination.data('max'));
    let newPage = curPage - 1;
    if ($(this).data('direction') === "+") newPage += 2;
    if (newPage <= 0 || newPage > max) return;
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
            if (response.status === 500) {
                // Something went wrong, in this case, showcase an error.
                showMessageModal("Error", "There was a problem fetching the right page on the server, operation cancelled.");
                return;
            }
            // Render the new documents
            $('.view .search-result-container .document-list-include').html(response.documentsList);
            $('.view .search-result-container .navigation-include').html(response.navigationView);
            $('.view .search-result-container .keyword-in-context-include').html(response.keywordInContextView);

            if(response.searchVisualization && window.searchVizualization){
                const vizData = response.searchVisualization;
                window.searchVizualization.vizData = vizData;
                updateSearchVizualization();
            }
        },
        error: function (xhr, status, error) {
            console.error(xhr.responseText);
            $('.view .search-result-container .document-list-include').html(xhr.responseText);
        }
    }).always(function () {
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
$('body').on('click', '.sort-container .sort-btn', function () {
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
    }).always(function () {
        $('.search-result-container .loader-container').first().fadeOut(150);
    });

    // Highlight the correct button
    if (curOrder === "ASC") {
        $(this).find('i').removeClass('fa-sort-amount-up').addClass('fa-sort-amount-down');
        $(this).data('curorder', 'DESC');
    } else {
        $(this).find('i').removeClass('fa-sort-amount-down').addClass('fa-sort-amount-up');
        $(this).data('curorder', 'ASC');
    }

    $(this).closest('.sort-container').find('.sort-btn').each(function () {
        $(this).removeClass('active-sort-btn');
    })
    $(this).addClass('active-sort-btn');
})

/**
 * Handles the switching of the search layers
 */
$('body').on('click', '.sort-container .switch-search-layer-result-btn', function () {
    const layer = $(this).data('layer');
    $(`.search-result-container .list`).each(function () {
        $(this).hide();
    })
    $('.sort-container .switch-search-layer-result-btn').each(function () {
        $(this).removeClass('selected');
    })

    // Highlight and show the correct search layer
    $(`.sort-container .switch-search-layer-result-btn[data-layer=` + layer + ']').addClass('selected');
    $(`.search-result-container .list[data-layer=` + layer + ']').show();
})

$('body').on('click', '.document-card .snippets-container .toggle-snippets-btn', function(){
    const $snippets = $(this).closest('.snippets-container').find('.snippet-content');
    $snippets.each(function(){
        if($(this).data('id') !== 0) $(this).toggle();
    });
    if ($(this).parent().find(".snippet-content").length > 1) {
        var style = $(this).parent().find(".snippet-content[data-id='1']")[0].getAttribute("style");;
        const regex =  /display: none;/;
        var found = style.match(regex);
        $(this).text((found ? "${languageResource.get('more')} " : "${languageResource.get('less')} " ));
        $(this).append('<i class="ml-1 fas fa-file-alt" aria-hidden="true"></i>');
    };

})

let currentFocusedDocumentId = -1;
/**
 * Track the currently focused search card
 */
$(window).on('scroll', function () {
    const $container = $('.search-row');
    const $cards = $container.find('.document-card');
    const containerCenter = $(window).scrollTop() + $(window).height() / 2;

    let $closestCard = null;
    let closestDistance = Infinity;

    $cards.each(function () {
        const $card = $(this);
        $card.removeClass('focused-document-card');
        const cardCenter = $card.offset().top + $card.outerHeight() / 2;
        const distance = Math.abs(containerCenter - cardCenter);

        if (distance < closestDistance) {
            closestDistance = distance;
            $closestCard = $card;
        }
    });
    if ($closestCard === undefined || $closestCard == null) return;
    $closestCard.addClass('focused-document-card');

    const documentId = $closestCard.data('id');
    if (documentId === currentFocusedDocumentId) return;

    // If we have a corpus universe view, then switch the focus there as well
    if (currentCorpusUniverseHandler !== undefined) currentCorpusUniverseHandler.focusDocumentNode(documentId);

    // If the keyword in context window exists, then highlight the
    // corresponding items there.
    $contextContainer = $('.search-result-container .keyword-context-card');

    if ($contextContainer != null) {
        const isExpanded = $contextContainer.data('expanded');
        if (isExpanded) return;

        $contextContainer.find('.context-row-container').each(function () {
            const contextDocId = $(this).find('.open-document').data('id');
            if (contextDocId === documentId) {
                $(this).show();
                //$(this).addClass('focused-keyword-context');
                //$contextContainer.prepend($(this));
            } else {
                $(this).hide();
                //$(this).removeClass('focused-keyword-context');
            }
        });
    }

    currentFocusedDocumentId = documentId;
});
