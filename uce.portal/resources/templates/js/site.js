var selectedCorpus = -1;
var currentView = undefined;
var reloadTimelineMap = false;

function getUiStateParams() {
    const rawHash = window.location.hash && window.location.hash.startsWith('#')
        ? window.location.hash.substring(1)
        : '';
    return new URLSearchParams(rawHash);
}

function updateUiState(mutator) {
    const params = getUiStateParams();
    mutator(params);
    const serialized = params.toString();
    const nextUrl = window.location.pathname + window.location.search + (serialized ? ('#' + serialized) : '');
    history.replaceState(null, '', nextUrl);
}

function stripLegacyLexiconQueryParams() {
    const searchParams = new URLSearchParams(window.location.search || '');
    const keys = ['lex_q', 'lex_char', 'lex_filters', 'lex_sort', 'lex_dir', 'lex_page'];
    let changed = false;
    keys.forEach((key) => {
        if (searchParams.has(key)) {
            searchParams.delete(key);
            changed = true;
        }
    });
    if (!changed) return;
    const cleanSearch = searchParams.toString();
    const nextUrl = window.location.pathname + (cleanSearch ? ('?' + cleanSearch) : '') + window.location.hash;
    history.replaceState(null, '', nextUrl);
}

window.uceUiState = {
    get: function (key) {
        const value = getUiStateParams().get(key);
        return value === null ? undefined : value;
    },
    set: function (key, value) {
        updateUiState((params) => {
            if (value === undefined || value === null || value === '') params.delete(key);
            else params.set(key, String(value));
        });
    },
    remove: function (key) {
        updateUiState((params) => params.delete(key));
    }
};

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

function navigateToView(id, options = {}) {
    const preserveInspectorRoute = !!options.preserveInspectorRoute;
    // Close any potential modals:
    closeCorpusInspector(!preserveInspectorRoute);

    // Now adjust the main content
    let foundView = false;
    $('.main-content-container .view').each(function () {
        if ($(this).data('id') === id) {
            $(this).show(50);
            foundView = true;
        } else {
            $(this).hide();
        }
    })
    if (!foundView) return;

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
    stripLegacyLexiconQueryParams();
    if (window.uceUiState) {
        window.uceUiState.set('view', id);
        if (id !== 'search') {
            [
                'q',
                'searchId',
                'page',
                'sortBy',
                'sortOrder',
                'bins',
                'feature',
                'chartType',
                'svOpen',
                'ls',
                'proMode',
                'corpusId'
            ].forEach((key) => window.uceUiState.remove(key));
        }
        if (id !== 'lexicon') {
            [
                'lex_q',
                'lex_char',
                'lex_filters',
                'lex_sort',
                'lex_dir',
                'lex_page'
            ].forEach((key) => window.uceUiState.remove(key));
        }
    }
    if (id === 'search' && typeof ensureSearchViewStateOnEnter === 'function') {
        window.setTimeout(() => ensureSearchViewStateOnEnter(), 0);
    }
}

function setCorpusInspectorRouteState(corpusId) {
    if (!window.uceUiState) return;
    if (corpusId === undefined || corpusId === null || String(corpusId).trim() === '') {
        window.uceUiState.remove('ci');
        window.uceUiState.remove('ciCorpusId');
        return;
    }
    window.uceUiState.set('ci', 'true');
    window.uceUiState.set('ciCorpusId', String(corpusId));
}

function closeCorpusInspector(clearRouteState = true) {
    $('.corpus-inspector-include').hide(150);
    $('.corpus-inspector-include').removeAttr('data-active-corpus-id');
    if (clearRouteState) {
        setCorpusInspectorRouteState(undefined);
    }
}

function openCorpusInspector(corpusId) {
    if (corpusId === undefined || corpusId === null || String(corpusId).trim() === '') return;

    $('.corpus-inspector-include').show(0);
    $('.corpus-inspector-include').attr('data-active-corpus-id', String(corpusId));
    $('.wiki-page-modal').addClass('wiki-page-modal-minimized');
    setCorpusInspectorRouteState(corpusId);

    $.ajax({
        url: "/api/corpus/inspector?id=" + corpusId,
        type: "GET",
        success: function (response) {
            // Render the corpus view
            $('.corpus-inspector-include').html(response);
            if (window.wikiHandler && typeof window.wikiHandler.syncCurrentPageLinks === 'function') {
                window.wikiHandler.syncCurrentPageLinks();
            }

            // After that, we load documentsListView
            loadCorpusDocuments(corpusId, $('.corpus-inspector-include .corpus-documents-list-include'));
        },
        error: function (xhr, status, error) {
            console.error(xhr.responseText);
            $('.corpus-inspector-include').html(xhr.responseText);
        }
    });
}

function restoreCorpusInspectorFromRoute() {
    if (!window.uceUiState) return;
    const routeCi = String(window.uceUiState.get('ci') || '').toLowerCase();
    const routeCiCorpusId = String(window.uceUiState.get('ciCorpusId') || '');
    const shouldOpen = routeCi === 'true' && routeCiCorpusId !== '';
    const isOpen = $('.corpus-inspector-include:visible').length > 0;

    if (!shouldOpen && isOpen) {
        $('.corpus-inspector-include').hide(150);
        return;
    }
    if (!shouldOpen) return;

    const currentInspectorCorpus = String($('.corpus-inspector-include').attr('data-active-corpus-id') || '');
    if (isOpen && currentInspectorCorpus === routeCiCorpusId) return;
    openCorpusInspector(routeCiCorpusId);
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
    const selectEl = $(this).get(0);
    if (!selectEl || !selectEl.options || selectEl.selectedIndex < 0) return;
    const selectedOption = selectEl.options[selectEl.selectedIndex];
    if (!selectedOption) return;
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
    // Do not auto-run a search during first-time initialization.
    if (!window.__uceSuppressAutoSearchOnCorpusChange && oldCorpusId !== -1 && oldCorpusId !== selectedCorpus) {
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
})

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

    openCorpusInspector(corpusId);
})

$('body').on('click', '.close-corpus-inspector-btn', function () {
    closeCorpusInspector();
});

/**
 * Generic disabled-action guard for links/buttons.
 * Any interactive element can opt in by using class `ui-action-disabled`
 * or `aria-disabled="true"` (optionally with `data-disabled-reason`).
 */
$('body').on('click', 'a.ui-action-disabled, button.ui-action-disabled, [role="button"].ui-action-disabled, a[aria-disabled="true"][data-disabled-reason], button[aria-disabled="true"][data-disabled-reason], [role="button"][aria-disabled="true"][data-disabled-reason]', function (event) {
    event.preventDefault();
    event.stopPropagation();

    const reason = $(this).attr('data-disabled-reason');
    if (reason) {
        showMessageModal("Unavailable action", reason);
    }
});

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
    const params = new URLSearchParams();
    params.set("id", String(id));

    if (searchId !== undefined && searchId !== null) {
        const raw = String(searchId).trim();
        if (raw !== "" && raw.toLowerCase() !== "undefined" && raw.toLowerCase() !== "null") {
            params.set("searchId", raw);
        }
    }

    window.open("/documentReader?" + params.toString(), '_blank');
}

function dismissAllPopovers(options = {}) {
    const opts = Object.assign({ dispose: false, removeOrphans: true }, options);
    const $targets = $('[data-toggle="popover"]');
    $targets.each(function () {
        const $el = $(this);
        const hasPopover = !!$el.data('bs.popover');
        if (!hasPopover) return;
        try {
            $el.popover('hide');
            if (opts.dispose) {
                $el.popover('dispose');
            }
        } catch (e) {
            // Ignore stale plugin instances; we'll still remove dangling DOM below.
        }
    });

    if (opts.removeOrphans) {
        $('.popover').remove();
    }
}

function activatePopovers() {
    dismissAllPopovers({ dispose: true, removeOrphans: true });
    $('[data-toggle="popover"]').popover({
        container: 'body'
    });
}

(function installPopoverPersistenceGuards() {
    if (window.__ucePopoverPersistenceGuardsInstalled) return;
    window.__ucePopoverPersistenceGuardsInstalled = true;

    $('body').on('mouseleave', '.breadcrumbs [data-toggle="popover"]', function () {
        try {
            $(this).popover('hide');
        } finally {
            // In case hide event is missed due to DOM updates.
            window.setTimeout(function () {
                $('.popover').remove();
            }, 120);
        }
    });

    $('body').on('click', '.breadcrumbs [data-toggle="popover"]', function () {
        dismissAllPopovers({ dispose: false, removeOrphans: true });
    });

    $(document).on('scroll touchmove', function () {
        dismissAllPopovers({ dispose: false, removeOrphans: false });
    });
})();

/**
 * We have some UI components that need to be refreshed when the corpus is loaded.
 */
function reloadCorpusComponents() {
    $('#corpus-select').change();
}

function sanitizeLegacyVizQueryParams() {
    const params = new URLSearchParams(window.location.search || '');
    let changed = false;
    ['search-viz-n-bins', 'search-viz-selected-feature'].forEach((key) => {
        if (params.has(key)) {
            params.delete(key);
            changed = true;
        }
    });
    if (!changed) return;
    const nextSearch = params.toString();
    const nextUrl = window.location.pathname + (nextSearch ? ('?' + nextSearch) : '') + window.location.hash;
    history.replaceState(null, '', nextUrl);
}

$(document).ready(function () {
    console.log('Webpage loaded!');
    sanitizeLegacyVizQueryParams();
    activatePopovers();
    reloadCorpusComponents();
    const initialView = window.uceUiState ? window.uceUiState.get('view') : undefined;
    if (initialView) {
        navigateToView(initialView, { preserveInspectorRoute: true });
    }
    restoreCorpusInspectorFromRoute();
    // Init the lexicon
    if (window.wikiHandler) window.wikiHandler.initializeLexicon();
})

$(window).on('hashchange', function () {
    if (!window.uceUiState) return;
    const routeView = window.uceUiState.get('view');
    if (routeView && routeView !== currentView) {
        navigateToView(routeView);
    }
    restoreCorpusInspectorFromRoute();
});

(function installSessionExpiredHandler() {
    if (window.__uceSessionExpiredHandlerInstalled) return;
    window.__uceSessionExpiredHandlerInstalled = true;

    const modalSelector = '#sessionExpiredModal';
    const countdownSelector = '#sessionExpiredCountdown';
    const reloginBtnSelector = '#sessionExpiredReloginBtn';
    const homeBtnSelector = '#sessionExpiredHomeBtn';

    function safeReturnTo() {
        return window.location.pathname + window.location.search + window.location.hash;
    }

    function getCountdownSeconds() {
        const el = document.querySelector(modalSelector);
        const raw = el ? el.getAttribute('data-countdown-seconds') : null;
        const parsed = parseInt(raw || '30', 10);
        return Number.isFinite(parsed) && parsed > 0 ? parsed : 30;
    }

    function getLoginBaseUrl() {
        // keep this in sync with the login icon href in index.ftl
        return "${uceConfig.getSettings().getAuthentication().getPublicUrl()}/realms/uce/protocol/openid-connect/auth" +
            "?client_id=uce-web&response_type=code&scope=openid" +
            "&redirect_uri=${uceConfig.getSettings().getAuthentication().getRedirectUrl()}/login";
    }

    let modalOpen = false;
    let timerId = null;
    let remainingSeconds = 0;
    let lastAuthPingAt = 0;

    function isLoggedInUiState() {
        const el = document.querySelector('a.user-profile-btn');
        if (!el) return false;
        const href = el.getAttribute('href') || '';
        // Logged-in: href="#" and onclick opens profile; logged-out: href points to Keycloak auth endpoint.
        if (href === '#') return true;
        if (el.getAttribute('onclick')) return true;
        return false;
    }

    function hadSessionBefore() {
        return sessionStorage.getItem('uce:hadSession') === '1';
    }

    function markHadSession() {
        sessionStorage.setItem('uce:hadSession', '1');
    }

    function cleanup() {
        if (timerId !== null) {
            window.clearInterval(timerId);
            timerId = null;
        }
        modalOpen = false;
    }

    function startAuthPingOnUserAttention() {
        const authEnabled = ${uceConfig.authIsEnabled()?c};
        if (!authEnabled) return;
        if (!window.fetch) return;

        function triggerAuthPing() {
            if (modalOpen) return;
            // Avoid popping "session expired" for users who were never logged in.
            if (!hadSessionBefore() && !isLoggedInUiState()) return;
            const now = Date.now();
            if (now - lastAuthPingAt < 5000) return;
            lastAuthPingAt = now;
            window.fetch('/api/auth/ping', {cache: 'no-store'})
                .then(function (res) {
                    if (res && res.status === 204) {
                        markHadSession();
                    }
                })
                .catch(function () {});
        }

        window.addEventListener('focus', function () {
            triggerAuthPing();
        });
        window.addEventListener('pageshow', function () {
            triggerAuthPing();
        });
        document.addEventListener('visibilitychange', function () {
            if (!document.hidden) {
                triggerAuthPing();
            }
        });
    }

    function hardLogoutToHome() {
        cleanup();
        window.location.assign('/logout');
    }

    function relogin() {
        cleanup();
        const returnTo = sessionStorage.getItem('uce:returnTo') || '/';
        const url = getLoginBaseUrl() + "&state=" + encodeURIComponent(returnTo);
        window.location.assign(url);
    }

    function openModalOnce() {
        if (modalOpen) return;
        // Avoid showing this modal to users who never had a session.
        if (!hadSessionBefore() && !isLoggedInUiState()) return;
        modalOpen = true;

        markHadSession();
        sessionStorage.setItem('uce:returnTo', safeReturnTo());
        remainingSeconds = getCountdownSeconds();

        const $modal = $(modalSelector);
        $modal.find(countdownSelector).text(String(remainingSeconds));
        $modal.modal({backdrop: 'static', keyboard: false});

        timerId = window.setInterval(function () {
            remainingSeconds -= 1;
            $(countdownSelector).text(String(Math.max(0, remainingSeconds)));
            if (remainingSeconds <= 0) {
                hardLogoutToHome();
            }
        }, 1000);
    }

    // Buttons (event delegation: safe even if modal is included once at page load)
    $('body').on('click', reloginBtnSelector, function () {
        relogin();
    });
    $('body').on('click', homeBtnSelector, function () {
        hardLogoutToHome();
    });
    $(document).on('hidden.bs.modal', modalSelector, function () {
        cleanup();
    });

    // Intercept fetch() centrally (covers code using fetch directly)
    if (window.fetch) {
        const originalFetch = window.fetch.bind(window);
        window.fetch = function (input, init) {
            return originalFetch(input, init).then(function (res) {
                if (res && res.status === 401) {
                    openModalOnce();
                }
                return res;
            }).catch(function (err) {
                throw err;
            });
        };
    }

    // Intercept jQuery ajax globally (covers $.ajax usage)
    $(document).ajaxError(function (event, xhr) {
        if (xhr && xhr.status === 401) {
            openModalOnce();
        }
    });

    // Detect session loss when the user returns to the tab/window (avoids keeping the session alive).
    startAuthPingOnUserAttention();
})();
