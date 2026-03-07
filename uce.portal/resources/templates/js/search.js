let currentCorpusUniverseHandler = undefined;
let searchRestoreInProgress = false;
let searchViewBootstrapInProgress = false;
let searchVizToggleInProgressUntil = 0;

function encodeLayeredSearchStateForRoute(state) {
    if (!state) return '';
    try {
        const json = JSON.stringify(state);
        const utf8 = encodeURIComponent(json).replace(/%([0-9A-F]{2})/g, (_, p1) =>
            String.fromCharCode(parseInt(p1, 16))
        );
        return btoa(utf8).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '');
    } catch (e) {
        return '';
    }
}

function decodeLayeredSearchStateFromRoute(raw) {
    if (!raw) return null;
    try {
        const b64 = String(raw).replace(/-/g, '+').replace(/_/g, '/');
        const padded = b64 + '==='.slice((b64.length + 3) % 4);
        const utf8 = atob(padded);
        const json = decodeURIComponent(Array.prototype.map.call(utf8, (c) =>
            '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)
        ).join(''));
        const parsed = JSON.parse(json);
        if (!parsed || !Array.isArray(parsed.layers)) return null;
        return parsed;
    } catch (e) {
        return null;
    }
}

function getLayeredSearchStateForRoute() {
    if (!window.layeredSearchHandler) return undefined;
    const submitFromInput = $('.search-menu-div .search-settings-div .submit-layered-search-input').val() === 'true';
    const submitStatus = submitFromInput || !!window.layeredSearchHandler.submitStatus;
    const layers = window.layeredSearchHandler.buildApplicableLayers([]);
    if (!submitStatus || !Array.isArray(layers) || layers.length === 0) return null;
    return {
        submit: true,
        searchId: window.layeredSearchHandler.searchId || '',
        layers: layers
    };
}

function hydrateSearchVizSettingsFromRoute() {
    if (!window.uceUiState || !window.searchVizualization) return;
    if (window.searchVizualization.__routeHydrated) return;
    const routeBins = parseInt(window.uceUiState.get('bins'), 10);
    if (Number.isFinite(routeBins) && routeBins > 0) {
        window.searchVizualization.settings.nBins = routeBins;
    }
    const routeFeature = window.uceUiState.get('feature');
    if (routeFeature) {
        window.searchVizualization.settings.selectedFeature = routeFeature;
    }
    const routeChartType = window.uceUiState.get('chartType');
    if (routeChartType) {
        window.searchVizualization.settings.chartType = routeChartType;
    }
    window.searchVizualization.__routeHydrated = true;
}

function persistSearchVizSettingsToRoute() {
    if (!window.uceUiState || !window.searchVizualization) return;
    window.uceUiState.set('bins', window.searchVizualization.settings.nBins);
    window.uceUiState.set('feature', window.searchVizualization.settings.selectedFeature);
    window.uceUiState.set('chartType', window.searchVizualization.settings.chartType || 'bar');
}

function getSearchVisualizationExpandedStateFromDom() {
    const $expanded = $('#search-results-visualization-container .group-box .expanded').first();
    if ($expanded.length === 0) return null;
    return $expanded.is(':visible');
}

function setSearchVisualizationExpandedState(shouldOpen, animate = false) {
    const $expanded = $('#search-results-visualization-container .group-box .expanded').first();
    if ($expanded.length === 0) return;
    $expanded.stop(true, true);
    if (shouldOpen) {
        $expanded.removeClass('display-none');
        if (animate) $expanded.fadeIn(75);
        else $expanded.show();
    } else {
        if (animate) {
            $expanded.fadeOut(75, function () {
                $(this).addClass('display-none');
            });
        } else {
            $expanded.addClass('display-none').hide();
        }
    }
}

function persistSearchVisualizationExpandedStateToRoute() {
    if (!window.uceUiState) return;
    const $expanded = $('#search-results-visualization-container .group-box .expanded').first();
    if ($expanded.length > 0 && $expanded.is(':animated')) return;
    const isOpen = getSearchVisualizationExpandedStateFromDom();
    if (isOpen === true) window.uceUiState.set('svOpen', 'true');
    else window.uceUiState.remove('svOpen');
}

function applySearchVisualizationExpandedStateFromRoute() {
    if (!window.uceUiState) return;
    const shouldOpen = String(window.uceUiState.get('svOpen') || '').toLowerCase() === 'true';
    setSearchVisualizationExpandedState(shouldOpen, false);
}

function persistSearchRequestToRoute(searchInput, corpusId, proMode, layeredState = undefined) {
    if (!window.uceUiState) return;
    window.uceUiState.set('view', 'search');
    window.uceUiState.set('q', String(searchInput || ''));
    window.uceUiState.set('corpusId', String(corpusId || ''));
    window.uceUiState.set('proMode', proMode ? 'true' : 'false');
    if (layeredState !== undefined) {
        const encoded = encodeLayeredSearchStateForRoute(layeredState);
        if (encoded) window.uceUiState.set('ls', encoded);
        else window.uceUiState.remove('ls');
    }
}

function isSearchResultEmpty() {
    const $container = $('.view[data-id="search"] .search-result-container');
    if ($container.length === 0) return true;
    const hasState = $container.find('.search-state').length > 0;
    const hasDocuments = $container.find('.document-card').length > 0;
    const hasEmbeddingList = $container.find('.embedding-document-list-include .document-card').length > 0;
    return !(hasState || hasDocuments || hasEmbeddingList);
}

function restoreSearchFromRouteIfNeeded() {
    if (!window.uceUiState || searchRestoreInProgress) return;

    const routeView = String(window.uceUiState.get('view') || '');
    const routeSearchId = String(window.uceUiState.get('searchId') || '');
    const routeQuery = String(window.uceUiState.get('q') || '');
    const routeCorpusId = String(window.uceUiState.get('corpusId') || '');
    const routeProMode = String(window.uceUiState.get('proMode') || '').toLowerCase();
    const routeLayeredState = decodeLayeredSearchStateFromRoute(window.uceUiState.get('ls'));
    const shouldRestoreSearch = routeView === 'search' || routeSearchId !== '' || routeQuery !== '';
    if (!shouldRestoreSearch) return;

    const activeSearchId = String($('.search-state').data('id') || '');
    if (activeSearchId) return;
    if (!isSearchResultEmpty()) return;

    if (routeCorpusId) {
        const $select = $('#corpus-select');
        if ($select.length > 0) {
            const $matching = $select.find('option').filter(function () {
                return String($(this).data('id')) === routeCorpusId;
            }).first();
            if ($matching.length > 0) {
                const currentSelectedId = String($select.find('option:selected').data('id') || '');
                if (currentSelectedId !== routeCorpusId) {
                    // Prevent empty auto-search side effects while restoring selected corpus from route.
                    window.__uceSuppressAutoSearchOnCorpusChange = true;
                    $select.prop('selectedIndex', $matching.index());
                    $select.trigger('change');
                    window.__uceSuppressAutoSearchOnCorpusChange = false;
                }
            }
        }
    }

    if (routeProMode === 'true') $('#proModeSwitch').prop('checked', true);
    if (routeProMode === 'false') $('#proModeSwitch').prop('checked', false);
    $('.search-input').val(routeQuery);

    if (routeLayeredState && window.layeredSearchHandler && typeof window.layeredSearchHandler.hydrateFromRouteState === 'function') {
        window.layeredSearchHandler.hydrateFromRouteState(routeLayeredState);
    }

    searchRestoreInProgress = true;
    // Use startNewSearch so all dependent widgets (pagination, universe, viz) are restored consistently.
    startNewSearch(routeQuery, false, {
        layeredState: routeLayeredState
    });
    window.setTimeout(() => {
        searchRestoreInProgress = false;
    }, 1500);
}

function applySearchStateFromRoute() {
    if (!window.uceUiState) return;
    const activeSearchId = String($('.search-state').data('id') || '');
    const routeSearchId = String(window.uceUiState.get('searchId') || '');
    if (!activeSearchId || !routeSearchId || activeSearchId !== routeSearchId) return;

    const routeSortBy = window.uceUiState.get('sortBy');
    const routeSortOrder = String(window.uceUiState.get('sortOrder') || '').toUpperCase();
    if (routeSortBy && (routeSortOrder === 'ASC' || routeSortOrder === 'DESC')) {
        const $btn = $(".sort-container .sort-btn[data-orderby='" + routeSortBy + "']");
        if ($btn.length > 0) {
            const currentForTrigger = routeSortOrder === 'ASC' ? 'DESC' : 'ASC';
            $btn.data('curorder', currentForTrigger);
            $btn.trigger('click');
        }
    }

    const routePage = parseInt(window.uceUiState.get('page'), 10);
    if (Number.isFinite(routePage) && routePage > 1) {
        handleSwitchingOfPage(routePage);
    }
}

function getCurrentSearchQueryForRoute() {
    const inputVal = String($('.search-input').val() || '').trim();
    if (inputVal !== '') return inputVal;
    const tokenVal = String($('.search-result-container .search-token').first().text() || '').trim();
    return tokenVal;
}

function syncRouteFromRenderedSearchState() {
    if (!window.uceUiState) return;

    const selectElement = document.getElementById("corpus-select");
    const selectedOption = selectElement && selectElement.options
        ? selectElement.options[selectElement.selectedIndex]
        : undefined;
    const corpusId = selectedOption ? String(selectedOption.getAttribute("data-id") || '') : '';
    const proMode = $('#proModeSwitch').is(':checked');
    const searchInput = getCurrentSearchQueryForRoute();

    const layeredState = getLayeredSearchStateForRoute();
    persistSearchRequestToRoute(searchInput, corpusId, proMode, layeredState);

    const activeSearchId = String($('.search-state').data('id') || '');
    if (activeSearchId) window.uceUiState.set('searchId', activeSearchId);
    else window.uceUiState.remove('searchId');

    const curPage = parseInt($('.search-result-container .pagination').data('cur'), 10);
    if (Number.isFinite(curPage) && curPage > 1) window.uceUiState.set('page', String(curPage));
    else window.uceUiState.remove('page');

    const $activeSortBtn = $('.sort-container .sort-btn.active-sort-btn').first();
    if ($activeSortBtn.length > 0) {
        const orderBy = String($activeSortBtn.data('orderby') || '').trim();
        const sortOrder = String($activeSortBtn.data('curorder') || '').toUpperCase();
        if (orderBy) window.uceUiState.set('sortBy', orderBy);
        else window.uceUiState.remove('sortBy');
        if (sortOrder === 'ASC' || sortOrder === 'DESC') window.uceUiState.set('sortOrder', sortOrder);
        else window.uceUiState.remove('sortOrder');
    } else {
        window.uceUiState.remove('sortBy');
        window.uceUiState.remove('sortOrder');
    }

    persistSearchVizSettingsToRoute();
    if (Date.now() >= searchVizToggleInProgressUntil) {
        persistSearchVisualizationExpandedStateToRoute();
    }
}

function ensureSearchViewStateOnEnter() {
    if (!window.uceUiState) return;
    const routeView = String(window.uceUiState.get('view') || '');
    if (routeView !== 'search') return;

    const activeSearchId = String($('.search-state').data('id') || '');
    if (activeSearchId) {
        const routeLayeredState = decodeLayeredSearchStateFromRoute(window.uceUiState.get('ls'));
        if (routeLayeredState && window.layeredSearchHandler && typeof window.layeredSearchHandler.hydrateFromRouteState === 'function') {
            window.layeredSearchHandler.hydrateFromRouteState(routeLayeredState);
        }
        applySearchVisualizationExpandedStateFromRoute();
        syncRouteFromRenderedSearchState();
        return;
    }

    if (!isSearchResultEmpty()) {
        const routeLayeredState = decodeLayeredSearchStateFromRoute(window.uceUiState.get('ls'));
        if (routeLayeredState && window.layeredSearchHandler && typeof window.layeredSearchHandler.hydrateFromRouteState === 'function') {
            window.layeredSearchHandler.hydrateFromRouteState(routeLayeredState);
        }
        applySearchVisualizationExpandedStateFromRoute();
        syncRouteFromRenderedSearchState();
        return;
    }

    const routeSearchId = String(window.uceUiState.get('searchId') || '');
    const routeQuery = String(window.uceUiState.get('q') || '');
    if (routeSearchId !== '' || routeQuery !== '') {
        restoreSearchFromRouteIfNeeded();
        return;
    }

    if (searchViewBootstrapInProgress || searchRestoreInProgress) return;

    const selectElement = document.getElementById("corpus-select");
    if (!selectElement || !selectElement.options || selectElement.selectedIndex < 0) return;
    const selectedOption = selectElement.options[selectElement.selectedIndex];
    if (!selectedOption || !selectedOption.getAttribute("data-id")) return;

    searchViewBootstrapInProgress = true;
    startNewSearch(String($('.search-input').val() || ''), false);
    window.setTimeout(() => {
        searchViewBootstrapInProgress = false;
    }, 2000);
}

$('body').on('click', '#search-viz-update-button', function (e) {
    // TODO more error handling
    const nBins = parseInt($('#search-viz-n-bins').val(), 10)
    if (Number.isFinite(nBins) && nBins > 0) {
        window.searchVizualization.settings.nBins = nBins
    }

    const selectedFeature = $('#search-viz-selected-feature').val() || ''
    window.searchVizualization.settings.selectedFeature = selectedFeature

    persistSearchVizSettingsToRoute()
    updateSearchVizualization()

    e.preventDefault()
})

$('body').on('submit', '#search-viz-form', function (e) {
    e.preventDefault();
    $('#search-viz-update-button').trigger('click');
});

$('body').on('change', '#search-viz-n-bins, #search-viz-selected-feature', function () {
    $('#search-viz-update-button').trigger('click');
});

function createHistogramData(data, bins, featureKey = '') {
    function estimateFractionDigits(step) {
        if (!Number.isFinite(step) || step <= 0) return 0;
        const stepString = step.toString();
        if (stepString.includes('e-')) {
            const exp = parseInt(stepString.split('e-')[1], 10);
            return Math.min(6, Math.max(0, exp));
        }
        const dotIdx = stepString.indexOf('.');
        if (dotIdx === -1) return 0;
        return Math.min(6, Math.max(0, stepString.length - dotIdx - 1));
    }

    function formatContinuousBound(value, step) {
        if (!Number.isFinite(value)) return '';
        const rounded = Math.round(value);
        if (Math.abs(value - rounded) < 1e-9) return String(rounded);
        return new Intl.NumberFormat(undefined, {
            minimumFractionDigits: 0,
            maximumFractionDigits: estimateFractionDigits(step),
            useGrouping: false
        }).format(value);
    }

    function buildDiscreteIntegerHistogram(values) {
        const counts = new Map();
        for (const value of values) {
            const key = Math.round(value);
            counts.set(key, (counts.get(key) || 0) + 1);
        }
        const sortedKeys = Array.from(counts.keys()).sort((a, b) => a - b);
        const discreteData = sortedKeys.map((k) => counts.get(k));
        const discreteLabels = sortedKeys.map((k) => String(k));
        return [discreteData, discreteLabels];
    }

    function buildDiscreteIntegerBinnedHistogram(values, binCount) {
        const intValues = values.map((v) => Math.round(v));
        const minInt = Math.min(...intValues);
        const maxInt = Math.max(...intValues);
        const rangeSize = maxInt - minInt + 1;
        const safeBinCount = Math.max(1, Math.min(binCount, rangeSize));
        const step = rangeSize / safeBinCount;
        const buckets = new Array(safeBinCount).fill(0);

        for (const value of intValues) {
            const bucketIndex = Math.min(
                safeBinCount - 1,
                Math.floor((value - minInt) / step)
            );
            buckets[bucketIndex]++;
        }

        const labels = buckets.map((_, index) => {
            const start = Math.floor(minInt + index * step);
            const endExclusive = minInt + (index + 1) * step;
            const end = Math.max(start, Math.ceil(endExclusive) - 1);
            return start === end ? String(start) : (start + ' - ' + end);
        });

        return [buckets, labels];
    }

    // extract only the values, filter nan
    const values = data.map(d => parseFloat(d.value)).filter(v => !isNaN(v))
    if (!values.length) return [[], []];

    const safeBins = Math.max(1, Number.isFinite(bins) ? bins : 10);
    const min = Math.min(...values);
    const max = Math.max(...values);
    const range = max - min;
    const allIntegers = values.every((value) => Math.abs(value - Math.round(value)) < 1e-9);
    const uniqueIntegerCount = allIntegers ? new Set(values.map((v) => Math.round(v))).size : 0;
    const likelyYearFeature = (
        allIntegers &&
        min >= 1000 &&
        max <= 3000 &&
        /(year|jahr|date|datum)/i.test(String(featureKey || ''))
    );

    // For integer-like discrete scales (especially years), treat bins as categories instead of fractional ranges.
    // This keeps semantics clean (e.g. 1891, 1892) and avoids misleading decimal bucket labels.
    const shouldUseDiscreteIntegerBinning = allIntegers && (
        likelyYearFeature ||
        uniqueIntegerCount <= Math.max(safeBins * 4, 24) ||
        range <= Math.max(safeBins * 2, 20)
    );
    if (shouldUseDiscreteIntegerBinning) {
        if (safeBins >= uniqueIntegerCount) {
            return buildDiscreteIntegerHistogram(values);
        }
        return buildDiscreteIntegerBinnedHistogram(values, safeBins);
    }

    if (range === 0) {
        const buckets = new Array(safeBins).fill(0);
        buckets[0] = values.length;
        const label = formatContinuousBound(min, 1) + " - " + formatContinuousBound(max, 1);
        const labels = new Array(safeBins).fill(label);
        return [buckets, labels];
    }

    const bucketSize = range / safeBins
    const buckets = new Array(safeBins).fill(0)
    for (const value of values) {
        const bucketIndex = Math.min(
            Math.floor((value - min) / bucketSize),
            safeBins-1
        )
        buckets[bucketIndex]++
    }

    const labels = buckets.map((_, index) => {
        const start = min + index * bucketSize;
        const end = min + (index + 1) * bucketSize;
        return formatContinuousBound(start, bucketSize) + " - " + formatContinuousBound(end, bucketSize);
    })

    return [buckets, labels]
}

function getSearchVizBinBounds(values, featureKey) {
    const safeValues = (values || []).filter((v) => Number.isFinite(v));
    if (safeValues.length === 0) return { min: 1, max: 1 };

    const nValues = safeValues.length;
    const allIntegers = safeValues.every((value) => Math.abs(value - Math.round(value)) < 1e-9);
    const min = Math.min(...safeValues);
    const max = Math.max(...safeValues);
    const uniqueIntegerCount = allIntegers ? new Set(safeValues.map((v) => Math.round(v))).size : 0;
    const likelyYearFeature = (
        allIntegers &&
        min >= 1000 &&
        max <= 3000 &&
        /(year|jahr|date|datum)/i.test(String(featureKey || ''))
    );
    const shouldUseDiscreteIntegerBinning = allIntegers && (
        likelyYearFeature ||
        uniqueIntegerCount <= Math.max(10 * 4, 24) ||
        (max - min) <= Math.max(10 * 2, 20)
    );

    // Never allow more bins than samples. For discrete integer scales, cap at unique integer values.
    const maxForContinuous = Math.max(1, Math.min(150, nValues));
    const maxForDiscrete = Math.max(1, Math.min(maxForContinuous, uniqueIntegerCount));
    return {
        min: 1,
        max: shouldUseDiscreteIntegerBinning ? maxForDiscrete : maxForContinuous
    };
}

function normalizeSearchVisualizationPayload(rawPayload) {
    if (!rawPayload) return null;
    if (typeof rawPayload === 'string') {
        try {
            return JSON.parse(rawPayload);
        } catch (e) {
            console.error('Failed to parse search visualization payload.', e);
            return null;
        }
    }
    if (typeof rawPayload === 'object') return rawPayload;
    return null;
}

function updateSearchVizualization() {
    if (window.searchVizualization && !window.searchVizualization.__routeHydrated) {
        hydrateSearchVizSettingsFromRoute();
    }
    // Always prefer current UI control values over stale in-memory state.
    const uiBins = parseInt($('#search-viz-n-bins').val(), 10);
    if (Number.isFinite(uiBins) && uiBins > 0) {
        window.searchVizualization.settings.nBins = uiBins;
    }
    const uiFeature = $('#search-viz-selected-feature').val();
    if (uiFeature) {
        window.searchVizualization.settings.selectedFeature = uiFeature;
    }

    // TODO global state?
    const normalizedVizData = normalizeSearchVisualizationPayload(window.searchVizualization.vizData);
    if (!normalizedVizData) return;
    window.searchVizualization.vizData = normalizedVizData;

    const data = normalizedVizData["data"]
    const currentPage = normalizedVizData["currentPage"]
    let nBins = Math.max(1, parseInt(window.searchVizualization.settings.nBins, 10) || 10)
    const chartType = String(window.searchVizualization.settings.chartType || 'bar');
    if(data === undefined) return;

    // set the selected feature to the first one if not set
    const firstFeature = Object.keys(data).sort().shift()
    let selectedFeature = window.searchVizualization.settings.selectedFeature || firstFeature
    if (!(selectedFeature in data)) {
        selectedFeature = firstFeature;
        window.searchVizualization.settings.selectedFeature = selectedFeature;
    }

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

    const numericValuesForFeature = (data[selectedFeature] || [])
        .map((d) => parseFloat(d.value))
        .filter((v) => !isNaN(v));
    const binBounds = getSearchVizBinBounds(numericValuesForFeature, selectedFeature);
    nBins = Math.max(binBounds.min, Math.min(nBins, binBounds.max));
    window.searchVizualization.settings.nBins = nBins;
    const binsInput = $('#search-viz-n-bins');
    binsInput.attr('min', String(binBounds.min));
    binsInput.attr('max', String(binBounds.max));
    binsInput.attr('step', '1');
    binsInput.attr('title', 'Allowed range: ' + binBounds.min + '-' + binBounds.max);

    $('#search-viz-n-bins').val(nBins);
    $('#search-viz-selected-feature').val(selectedFeature);

    const chartElem = document.getElementById('search-results-visualization-graph')
    while (chartElem.firstChild) {
        chartElem.removeChild(chartElem.lastChild)
    }

    if (data && Object.keys(data).length > 0 && selectedFeature in data) {
        let [chartData, chartLabels] = createHistogramData(data[selectedFeature], nBins, selectedFeature)
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
                "labelName": selectedFeature,
            },
            chartType,
        )
    }
    persistSearchVizSettingsToRoute();
    applySearchVisualizationExpandedStateFromRoute();
    persistSearchVisualizationExpandedStateToRoute();
}

$('body').on('click', '.search-visualization-container .change-type', function () {
    if (!window.searchVizualization || !window.searchVizualization.settings) return;
    const type = String($(this).data('type') || '').trim();
    if (!type) return;
    window.searchVizualization.settings.chartType = type;
    persistSearchVizSettingsToRoute();
});

$('body').on('click', '#search-results-visualization-container .group-box > .flexed.clickable', function () {
    const isCurrentlyOpen = getSearchVisualizationExpandedStateFromDom() === true;
    const shouldOpen = !isCurrentlyOpen;
    searchVizToggleInProgressUntil = Date.now() + 250;
    setSearchVisualizationExpandedState(shouldOpen, true);
    if (window.uceUiState) {
        if (shouldOpen) window.uceUiState.set('svOpen', 'true');
        else window.uceUiState.remove('svOpen');
    }
});

/**
 * Starts a new search with the given input
 */
function startNewSearch(searchInput, reloadCorpus = true, options = {}) {
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
    const fulltextOrNeLayer = $('.search-menu-div .search-settings-div input[name="searchLayerRadioOptions"]:checked').val() || 'FULLTEXT';
    const embeddings = $('.search-menu-div .search-settings-div .option input[data-id="EMBEDDINGS"]').is(':checked');
    const kwic = $('.search-menu-div .search-settings-div .option input[data-id="KWIC"]').is(':checked');
    const enrich = $('.search-menu-div .search-settings-div .option input[data-id="ENRICH"]').is(':checked');
    const proMode = $('#proModeSwitch').is(':checked');
    const useLayeredSearch = $('.search-menu-div .search-settings-div .submit-layered-search-input').val() === 'true';
    const forcedLayeredState = options && options.layeredState ? options.layeredState : null;
    let layers = {};
    let layeredSearchId = '';
    if (forcedLayeredState && forcedLayeredState.submit === true && Array.isArray(forcedLayeredState.layers) && forcedLayeredState.layers.length > 0) {
        layers = forcedLayeredState.layers;
        layeredSearchId = forcedLayeredState.searchId
            ? String(forcedLayeredState.searchId)
            : ((window.layeredSearchHandler && window.layeredSearchHandler.searchId)
                ? window.layeredSearchHandler.searchId
                : generateUUID().toString().replaceAll("-", ""));
    } else if (useLayeredSearch === true) {
        layers = window.layeredSearchHandler.buildApplicableLayers([]);
        layeredSearchId = window.layeredSearchHandler.searchId;
    }

    const layeredState = forcedLayeredState
        ? ((forcedLayeredState.submit === true && Array.isArray(forcedLayeredState.layers) && forcedLayeredState.layers.length > 0) ? forcedLayeredState : null)
        : getLayeredSearchStateForRoute();
    persistSearchRequestToRoute(searchInput, corpusId, proMode, layeredState);

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
            refreshPaginationControls();
            if (reloadCorpus) {
                reloadCorpusComponents();
                // Store the search in the local browser for a history.
                addSearchToHistory(searchInput);
            }
            // Load the corpus universe from search
            const searchId = $('.search-state').data('id');
            if (window.uceUiState && searchId) {
                window.uceUiState.set('searchId', String(searchId));
            }
            if (typeof getNewCorpusUniverseHandler !== 'undefined') {
                currentCorpusUniverseHandler = getNewCorpusUniverseHandler;
                await currentCorpusUniverseHandler.createEmptyUniverse('search-universe-container');
                await currentCorpusUniverseHandler.fromSearch(searchId);
            }
            applySearchStateFromRoute();
            applySearchVisualizationExpandedStateFromRoute();
            persistSearchVisualizationExpandedStateToRoute();
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
    if ($(this).hasClass('disabled') || $(this).attr('aria-disabled') === 'true') return;
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
            refreshPaginationControls();
            if (window.uceUiState) window.uceUiState.set('page', page);

            if(response.searchVisualization && window.searchVizualization){
                const vizData = normalizeSearchVisualizationPayload(response.searchVisualization);
                if (!vizData) return;
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

function refreshPaginationControls() {
    const $pagination = $('.search-result-container .pagination');
    if ($pagination.length === 0) return;
    const curPage = parseInt($pagination.data('cur'), 10);
    const max = parseInt($pagination.data('max'), 10);
    const $prev = $pagination.find('.next-page-btn[data-direction="-"]');
    const $next = $pagination.find('.next-page-btn[data-direction="+"]');

    const disablePrev = !Number.isFinite(curPage) || curPage <= 1;
    const disableNext = !Number.isFinite(curPage) || !Number.isFinite(max) || curPage >= max;
    $prev.toggleClass('disabled', disablePrev).attr('aria-disabled', disablePrev ? 'true' : 'false');
    $next.toggleClass('disabled', disableNext).attr('aria-disabled', disableNext ? 'true' : 'false');
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
    const curOrder = String($(this).data('curorder') || 'ASC').toUpperCase();
    const nextOrder = curOrder === "ASC" ? "DESC" : "ASC";
    const searchId = $('.search-state').data('id');
    $('.search-result-container .loader-container').first().fadeIn(150);

    $.ajax({
        url: "/api/search/active/sort?searchId=" + searchId + "&order=" + nextOrder + "&orderBy=" + orderBy,
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
    if (nextOrder === "DESC") {
        $(this).find('i').removeClass('fa-sort-amount-up').addClass('fa-sort-amount-down');
    } else {
        $(this).find('i').removeClass('fa-sort-amount-down').addClass('fa-sort-amount-up');
    }
    $(this).data('curorder', nextOrder);

    $(this).closest('.sort-container').find('.sort-btn').each(function () {
        $(this).removeClass('active-sort-btn');
    })
    $(this).addClass('active-sort-btn');
    if (window.uceUiState) {
        window.uceUiState.set('sortBy', orderBy);
        window.uceUiState.set('sortOrder', nextOrder);
    }
})

/**
 * Handles the switching of the search layers
 */
$('body').on('click', '.sort-container .switch-search-layer-result-btn', function () {
    if ($(this).hasClass('is-static')) return;
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

$(document).ready(function () {
    refreshPaginationControls();
    // Prevent blank search view after reload: restore from route/search context if needed.
    restoreSearchFromRouteIfNeeded();
    ensureSearchViewStateOnEnter();
});

$(window).on('hashchange pageshow', function () {
    restoreSearchFromRouteIfNeeded();
    ensureSearchViewStateOnEnter();
});
