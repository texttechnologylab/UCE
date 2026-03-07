let WikiHandler = (function () {

    WikiHandler.prototype.history = [];
    WikiHandler.prototype.currentPage = undefined;
    WikiHandler.prototype.universeHandler = undefined;
    WikiHandler.prototype.occurrencesTake = 20;
    WikiHandler.prototype.lexiconState = {
        skip: 0,
        take: 24,
        hasMoreEntries: true,
        lastPageSize: 0,
        knownEmptySkip: null,
        selectedChar: '',
        searchInput: '',
        annotationFilters: [],
        sortColumn: 'occurrence',
        sortDirection: 'DESC',
    }

    function WikiHandler() {
        this.brokenWikiTargets = new Set();
    }

    WikiHandler.prototype.homePage = {
        wid: "DOC-SEARCH",
        coveredText: "-",
        hash: "DOC-SEARCH|-"
    };

    // =================== Lexicon Methods ===================
    WikiHandler.prototype.updateLexiconPage = function () {
        let curPage = this.lexiconState.skip / this.lexiconState.take + 1;
        let start = 1;
        if (curPage <= 3) start = 1;
        else if (curPage > 4) start = curPage - 3;
        const end = this.lexiconState.hasMoreEntries ? curPage + 3 : curPage;
        const btnList = $('.lexicon-view .lexicon-navigation .pages-count');
        btnList.html("");
        for (let i = start; i <= end; i++) {
            const selected = i === curPage ? "cur-page" : "";
            btnList.append(
                "<a class='rounded-a SELECTED' onclick='window.wikiHandler.fetchLexiconPage(PAGE)'>PAGE</a>"
                    .replaceAll("PAGE", i)
                    .replace("SELECTED", selected)
            );
        }
        this.updateLexiconNavigationButtons();
    }

    WikiHandler.prototype.updateLexiconNavigationButtons = function () {
        const curPage = this.lexiconState.skip / this.lexiconState.take + 1;
        const canGoPrevious = curPage > 1;
        const canGoNext = this.lexiconState.hasMoreEntries;

        const $prev = $('.lexicon-view .lexicon-navigation .lexicon-prev-page-btn');
        const $next = $('.lexicon-view .lexicon-navigation .lexicon-next-page-btn');

        $prev.toggleClass('ui-action-disabled', !canGoPrevious);
        $prev.attr('aria-disabled', canGoPrevious ? 'false' : 'true');
        $next.toggleClass('ui-action-disabled', !canGoNext);
        $next.attr('aria-disabled', canGoNext ? 'false' : 'true');
    }

    WikiHandler.prototype.handleLexiconSearchInputChanged = function ($source) {
        this.lexiconState.searchInput = $source.val();
        this.lexiconState.skip = 0;
        this.lexiconState.knownEmptySkip = null;
        this.fetchLexiconEntries(this.lexiconState.skip, this.lexiconState.take);
    }

    WikiHandler.prototype.handleLexiconSortingChanged = function ($source) {
        this.lexiconState.sortColumn = $source.data('id');
        let direction = $source.data('dir');
        if (direction === 'ASC') {
            this.lexiconState.sortDirection = 'DESC';
        } else if (direction === 'DESC') {
            this.lexiconState.sortDirection = 'ASC';
        }
        $('.lexicon-view .sortings a').each(function () {
            if ($(this).data('id') === $source.data('id')) $(this).addClass('selected-sort');
            else $(this).removeClass('selected-sort');
        });

        $source.toggleClass('turn-180');
        $source.data('dir', this.lexiconState.sortDirection);
        this.lexiconState.knownEmptySkip = null;
        this.fetchLexiconEntries(this.lexiconState.skip, this.lexiconState.take);
    }

    WikiHandler.prototype.handleLexiconAnnotationFiltersChanged = function ($filter) {
        let activeFilters = [];
        $('.lexicon-view .filter-container .annotation-filter').each(function () {
            const checked = $(this).find('input').prop('checked');
            if (checked) activeFilters.push($(this).find('label').html());
        });
        this.lexiconState.annotationFilters = activeFilters;
        this.lexiconState.skip = 0;
        this.lexiconState.knownEmptySkip = null;
        this.fetchLexiconEntries(this.lexiconState.skip, this.lexiconState.take);
    }

    WikiHandler.prototype.handleLexiconAlphabetBtnClicked = function (selectedChar) {
        // If the user clicks onto the currently highlighted char, we delete the alphabet filter
        if (selectedChar === this.lexiconState.selectedChar) {
            this.lexiconState.selectedChar = '';
            $('.lexicon-view .alphabet .selected-char').removeClass('selected-char');
        } else {
            $('.lexicon-view .alphabet .char').each(function () {
                if ($(this).html() === selectedChar) $(this).addClass('selected-char');
                else $(this).removeClass('selected-char');
            });
            this.lexiconState.selectedChar = selectedChar;
        }

        // In any case, we reset the list to page 1.
        this.lexiconState.skip = 0;
        this.lexiconState.knownEmptySkip = null;
        this.fetchLexiconEntries(this.lexiconState.skip, this.lexiconState.take);
    }

    WikiHandler.prototype.getLexiconAlphabet = function () {
        let alphabet = [];
        $('.lexicon-view .alphabet .char').each(function () {
            if ($(this).hasClass('selected-char')) alphabet.push($(this).html());
        });
        return alphabet;
    }

    WikiHandler.prototype.fetchLexiconPage = function (pageNum) {
        if (pageNum < 1) return;
        const curPage = this.lexiconState.skip / this.lexiconState.take + 1;
        if (!this.lexiconState.hasMoreEntries && pageNum > curPage) return;
        this.lexiconState.skip = this.lexiconState.take * (pageNum - 1);
        this.fetchLexiconEntries(this.lexiconState.skip, this.lexiconState.take);
    }

    WikiHandler.prototype.fetchPreviousLexiconEntries = function () {
        if (this.lexiconState.skip < this.lexiconState.take) return;
        this.lexiconState.skip -= this.lexiconState.take;
        this.fetchLexiconEntries(this.lexiconState.skip, this.lexiconState.take);
    }

    WikiHandler.prototype.fetchNextLexiconEntries = function () {
        if (!this.lexiconState.hasMoreEntries) return;
        this.lexiconState.skip += this.lexiconState.take;
        this.fetchLexiconEntries(this.lexiconState.skip, this.lexiconState.take);
    }

    WikiHandler.prototype.fetchLexiconEntries = function (skip, take, allowRetryOnEmpty = true) {
        const alphabet = this.getLexiconAlphabet();
        $.ajax({
            url: '/api/wiki/lexicon/entries',
            type: "POST",
            dataType: "json",
            data: JSON.stringify({
                skip: skip,
                take: take,
                searchInput: this.lexiconState.searchInput,
                sortColumn: this.lexiconState.sortColumn,
                sortDirection: this.lexiconState.sortDirection,
                alphabet: this.lexiconState.selectedChar === '' ? undefined : [this.lexiconState.selectedChar],
                annotationFilters: this.lexiconState.annotationFilters
            }),
            contentType: "application/json",
            success: (response) => {
                activatePopovers();
                console.log(response);
                const entries = Array.isArray(response && response.entries) ? response.entries : [];
                this.lexiconState.lastPageSize = entries.length;
                const knownEmptySkip = this.lexiconState.knownEmptySkip;
                this.lexiconState.hasMoreEntries = entries.length >= take &&
                    (knownEmptySkip === null || (skip + take) < knownEmptySkip);

                if (entries.length === 0 && skip > 0 && allowRetryOnEmpty) {
                    this.lexiconState.knownEmptySkip = skip;
                    this.lexiconState.hasMoreEntries = false;
                    this.lexiconState.skip = Math.max(0, skip - take);
                    this.fetchLexiconEntries(this.lexiconState.skip, this.lexiconState.take, false);
                    return;
                }

                if(response.rendered) $('.lexicon-content-include').html(response.rendered);
                else $('.lexicon-content-include').html(response);
                this.updateLexiconPage();
                this.setLexiconLoadMoreState(false, "Choose a lexicon entry first.");
            },
            error: (xhr, status, error) => {
                showMessageModal("Unknown Error", "There was an unknown error loading the lexicon entries.")
            }
        }).always(() => {
        });
    }

    WikiHandler.prototype.setLexiconLoadMoreState = function (enabled, reason = "") {
        const $btn = $('.lexicon-view .lexicon-entry-inspector .lexicon-load-more-btn');
        if ($btn.length === 0) return;
        const disabled = !enabled;
        $btn.toggleClass('ui-action-disabled', disabled);
        $btn.attr('aria-disabled', disabled ? 'true' : 'false');
        if (disabled && reason) {
            $btn.attr('data-disabled-reason', reason);
            $btn.attr('title', reason);
        } else {
            $btn.removeAttr('data-disabled-reason');
            $btn.removeAttr('title');
        }
    }

    WikiHandler.prototype.handleLoadMoreOccurrences = function () {
        const $target = $('.lexicon-view .lexicon-entry-inspector .occurrences-list');
        const covered = String($target.data('covered') || '').trim();
        const type = String($target.data('type') || '').trim();
        if (!covered || !type) {
            this.setLexiconLoadMoreState(false, "Choose a lexicon entry first.");
            return;
        }
        let skip = $target.data('skip') + this.occurrencesTake;
        $target.data('skip', skip);
        this.fetchLexiconEntryOccurrences(covered, type,
            skip, $target);
    }

    WikiHandler.prototype.handleOccurrencesNavigationClicked = function ($source) {
        const $lexiconEntry = $source.closest('.lexicon-entry');
        const type = $lexiconEntry.data('type');
        const covered = $lexiconEntry.data('covered');
        if (!String(type || '').trim() || !String(covered || '').trim()) {
            this.setLexiconLoadMoreState(false, "Choose a valid lexicon entry first.");
            return;
        }
        const $target = $('.lexicon-view .lexicon-entry-inspector .occurrences-list');
        // clean the inspector list
        $target.html('');
        $target.data('skip', 0);
        $target.data('covered', covered);
        $target.data('type', type);
        this.setLexiconLoadMoreState(true);
        this.fetchLexiconEntryOccurrences(covered, type, 0, $target);
    }

    WikiHandler.prototype.fetchLexiconEntryOccurrences = function (coveredText, type, skip, $target) {
        console.log(coveredText);
        $.ajax({
            url: "/api/wiki/lexicon/occurrences",
            type: "POST",
            contentType: "application/json",
            data: JSON.stringify({
                coveredText: String(coveredText),
                type: type,
                skip: skip,
                take: this.occurrencesTake
            }),
            success: (response) => {
                activatePopovers();
                $target.append(response);
            },
            error: (xhr, status, error) => {
                showMessageModal("Unknown Error", "There was an unknown error loading your occurrences.")
            }
        }).always(() => {
        });
    };

    // =================== Lexicon Methods End ===================

    WikiHandler.prototype.addPageToHistory = function (wikiDto) {
        if (wikiDto !== undefined) this.history.push(wikiDto);
    }

    WikiHandler.prototype.handleGoBackBtnClicked = function () {
        if (this.history.length === 0) return;
        let lastPage = this.history.pop();
        this.loadPage(lastPage, true);
    }

    WikiHandler.prototype.handleHomeBtnClicked = function () {
        this.loadPage(this.homePage);
    }

    WikiHandler.prototype.normalizeCoveredText = function ($wikiEl) {
        const rawCovered = $wikiEl.data('wcovered');
        if (rawCovered !== undefined && rawCovered !== null) {
            const normalized = String(rawCovered).trim();
            if (normalized !== '' && normalized.toLowerCase() !== 'undefined' && normalized.toLowerCase() !== 'null') {
                return normalized;
            }
        }

        const text = String($wikiEl.text() || '').trim();
        if (text !== '') return text;
        return '-';
    }

    WikiHandler.prototype.normalizeWikiId = function (wid, $wikiEl = undefined) {
        if (wid === undefined || wid === null) return '';
        let normalized = String(wid).trim();
        if (normalized === '' || normalized === '-' || normalized.toLowerCase() === 'undefined' || normalized.toLowerCase() === 'null') {
            return '';
        }

        // Some legacy ids use underscores instead of hyphens.
        normalized = normalized.replace(/^([A-Za-z_]+)_/, '$1-');

        const inBreadcrumbs = !!($wikiEl && $wikiEl.closest('.breadcrumbs').length > 0);
        const inCorpusInspector = !!($wikiEl && $wikiEl.closest('.corpus-inspector').length > 0);
        const isCorpusBreadcrumb = !!($wikiEl && $wikiEl.find('.fa-globe').length > 0);
        const isDocumentBreadcrumb = !!($wikiEl && $wikiEl.find('.fa-book').length > 0);

        if (normalized.includes('-')) return normalized;

        const elementTypeHint = $wikiEl ? String($wikiEl.data('wtype') || '').trim().toUpperCase() : '';

        // If we only received a numeric id, derive its type from explicit data hint first.
        if (/^\d+$/.test(normalized) && elementTypeHint) {
            return elementTypeHint + "-" + normalized;
        }

        // Corpus inspector links often point to corpus wiki pages; force numeric ids into corpus ids.
        if (/^\d+$/.test(normalized) && inCorpusInspector) {
            return "C-" + normalized;
        }

        // Legacy/faulty ids can come without separator (e.g. "C123"), normalize to "C-123".
        const match = normalized.match(/^([A-Za-z_]+)(\d+)$/);
        if (match) return match[1] + "-" + match[2];

        // Last-resort inference for breadcrumb links if data-wtype is missing.
        if (/^\d+$/.test(normalized) && $wikiEl && $wikiEl.closest('.breadcrumbs').length > 0) {
            if ($wikiEl.find('.fa-book').length > 0) return "D-" + normalized;
            if ($wikiEl.find('.fa-globe').length > 0) return "C-" + normalized;
        }

        // Breadcrumb corpus links can come from stale cached markup; use context-derived id only as final fallback.
        if (inBreadcrumbs && isCorpusBreadcrumb) {
            const corpusIdFromPage = Number($wikiEl.closest('.wiki-page').data('corpusid'));
            if (Number.isFinite(corpusIdFromPage) && corpusIdFromPage > 0) {
                return "C-" + corpusIdFromPage;
            }
        }

        // Additional last resort: parse document wiki id from the current page text if needed.
        if (inBreadcrumbs && isDocumentBreadcrumb) {
            const pageText = String($wikiEl.closest('.wiki-page').text() || '');
            const docMatch = pageText.match(/\bD-\d+\b/);
            if (docMatch) return docMatch[0];
        }

        return normalized;
    }

    WikiHandler.prototype.buildWikiDtoFromElement = function ($wikiEl) {
        const wid = this.normalizeWikiId($wikiEl.data('wid'), $wikiEl);
        const coveredText = this.normalizeCoveredText($wikiEl);
        if (!wid) return undefined;
        return {
            wid: wid,
            coveredText: coveredText,
            hash: wid + "|" + coveredText
        };
    }

    WikiHandler.prototype.getWikiTargetKey = function (wikiDto) {
        if (!wikiDto || !wikiDto.wid) return '';
        const coveredText = String(wikiDto.coveredText || '').trim() || '-';
        return String(wikiDto.wid).trim() + "|" + coveredText;
    }

    WikiHandler.prototype.isErrorTemplateResponse = function (response) {
        if (typeof response !== 'string') return false;
        const $root = $('<div>').html(response);
        const hasErrorBanner = $root.find('.text-danger').length > 0;
        const hasDefaultLogo = $root.find('img[src*=\"img/logo.png\"]').length > 0;
        return hasErrorBanner && hasDefaultLogo;
    }

    WikiHandler.prototype.markWikiTargetAsBroken = function (wikiDto) {
        const key = this.getWikiTargetKey(wikiDto);
        if (!key) return;
        this.brokenWikiTargets.add(key);
        this.syncCurrentPageLinks();
    }

    WikiHandler.prototype.syncCurrentPageLinks = function () {
        const current = this.currentPage;
        const isModalOpen = $('.wiki-page-modal').length > 0 && !$('.wiki-page-modal').hasClass('wiki-page-modal-minimized');
        $('.open-wiki-page').each((_, node) => {
            const $el = $(node);
            const dto = this.buildWikiDtoFromElement($el);
            const hasConfiguredWid = String($el.data('wid') || '').trim() !== '';
            const isInvalidTarget = hasConfiguredWid && !dto;
            const isCurrent = !!(current && dto && dto.hash === current.hash);
            const isBroken = !!(dto && this.brokenWikiTargets.has(this.getWikiTargetKey(dto)));
            const shouldDisable = (isCurrent && isModalOpen) || isBroken || isInvalidTarget;
            const disabledReason = isBroken
                ? 'This link target is currently unavailable.'
                : ((isCurrent && isModalOpen)
                    ? 'You are already on this page.'
                    : (isInvalidTarget ? 'This link has an invalid wiki target.' : ''));
            $el.toggleClass('wiki-link-current', isCurrent);
            $el.toggleClass('wiki-link-broken', isBroken);
            $el.toggleClass('ui-action-disabled', shouldDisable);
            if (isCurrent) {
                $el.attr('aria-current', 'page');
            } else {
                $el.removeAttr('aria-current');
            }
            if (shouldDisable) {
                $el.attr('aria-disabled', 'true');
                if (disabledReason) {
                    $el.attr('title', disabledReason);
                    $el.attr('data-disabled-reason', disabledReason);
                } else {
                    $el.removeAttr('title');
                    $el.removeAttr('data-disabled-reason');
                }
            } else {
                $el.removeAttr('aria-disabled');
                $el.removeAttr('data-disabled-reason');
                if ($el.hasClass('open-wiki-page')) {
                    $el.removeAttr('title');
                }
            }
        });
    }

    WikiHandler.prototype.loadPage = function (wikiDto, calledFromBackBtn = false) {
        if (!wikiDto || !wikiDto.wid) return;

        const safeCoveredText = String(wikiDto.coveredText || '').trim() || '-';
        const safeHash = wikiDto.hash || (wikiDto.wid + "|" + safeCoveredText);
        const normalizedWikiDto = {
            wid: wikiDto.wid,
            coveredText: safeCoveredText,
            hash: safeHash
        };

        // If the current open page is the clicked wiki annotation, don't reload it.
        if (window.wikiHandler.currentPage !== undefined && window.wikiHandler.currentPage.hash === normalizedWikiDto.hash) return;
        const $loading = $('.wiki-page-modal .page-content .loading-div');
        $loading.stop(true, true).addClass('is-active');

        $.ajax({
            url: "/api/wiki/page?wid=" + encodeURIComponent(normalizedWikiDto.wid) + "&covered=" + encodeURIComponent(normalizedWikiDto.coveredText),
            type: "GET",
            success: (response) => {
                if (this.isErrorTemplateResponse(response)) {
                    this.markWikiTargetAsBroken(normalizedWikiDto);
                    showMessageModal("Unavailable link", "This wiki target is currently unavailable and has been disabled.");
                    return;
                }
                if (typeof dismissAllPopovers === 'function') {
                    dismissAllPopovers({ dispose: true, removeOrphans: true });
                }
                $('.wiki-page-modal .page-content .include').html(response);
                activatePopovers();

                // Only add to history if it's not called from the back button
                if (!calledFromBackBtn) {
                    if (this.currentPage) {
                        this.addPageToHistory(this.currentPage);
                    }
                    this.currentPage = normalizedWikiDto;
                } else {
                    // Update current page without adding to history
                    this.currentPage = normalizedWikiDto;
                }
                this.syncCurrentPageLinks();
            },
            error: (xhr, status, error) => {
                this.markWikiTargetAsBroken(normalizedWikiDto);
                console.error(xhr.responseText);
                showMessageModal("Unknown Error", "There was an unknown error loading your page.")
            }
        }).always(() => {
            $loading.stop(true, true).removeClass('is-active');
        });
    }

    WikiHandler.prototype.handleAnnotationClicked = function ($wikiEl) {
        ensureSingleWikiOverlays();
        const wikiDto = this.buildWikiDtoFromElement($wikiEl);
        if (!wikiDto) return;
        // Show the modal
        $('.wiki-page-modal').removeClass('wiki-page-modal-minimized');

        this.loadPage(wikiDto);
    }

    WikiHandler.prototype.addUniverseToDocumentWikiPage = async function (corpusId, currentCenter) {
        this.universeHandler = getNewCorpusUniverseHandler;
        await this.universeHandler.createEmptyUniverse('wiki-universe-container');
        await this.universeHandler.fromCorpus(corpusId, currentCenter);
    }

    WikiHandler.prototype.handleRdfNodeClicked = function ($el) {
        const $container = $el.closest('.node-div');
        const value = $el.data('value');

        // Maybe the value is a gbif link. Open it then.
        if (value.includes('www.gbif.org')) {
            window.open(value, '_blank').focus();
            return;
        }

        // Check if we have already loaded this rdfnode children before
        const expanded = $container.data('expanded');
        if ($container.data('children')) {
            if (expanded) {
                $container.find('.nodes-list-div').first().hide();
            } else {
                $container.find('.nodes-list-div').first().show();
            }
            $container.data('expanded', !expanded);
            return;
        }

        // If an rdf node was clicked the first time, then we query the ontology based on that premis
        const tripletType = $el.data('triplettype');
        const ogHtml = $el.html();

        $el.html('Fetching...');
        $.ajax({
            url: "/api/wiki/queryOntology",
            type: "POST",
            data: JSON.stringify({
                tripletType: tripletType,
                value: value
            }),
            contentType: "application/json",
            success: async function (response) {
                $container.append(response);
                $container.data('expanded', true);
                $container.data('children', true);
            },
            error: function (xhr, status, error) {
                showMessageModal("Bad Request", "Request failed, since the server wasn't reachable.");
                console.error(xhr.responseText);
            }
        }).always(function () {
            $el.html(ogHtml);
        });
    }

    return WikiHandler;
}());

function getNewWikiHandler() {
    return new WikiHandler();
}

function ensureSingleWikiOverlays() {
    const $modals = $('.wiki-page-modal');
    if ($modals.length > 1) {
        $modals.slice(0, -1).remove();
    }
    const $expanded = $('.wiki-metadata-expanded-view');
    if ($expanded.length > 1) {
        $expanded.slice(0, -1).remove();
    }
}

function closeExpandedTextView() {
    ensureSingleWikiOverlays();
    const $overlay = $('.wiki-metadata-expanded-view');
    $overlay.removeClass('is-active').attr('aria-hidden', 'true');
}

$(document).ready(function () {
    ensureSingleWikiOverlays();
    if (!window.wikiHandler) {
        window.wikiHandler = getNewWikiHandler();
    }
    $('.wiki-page-modal .page-content .loading-div').removeClass('is-active');
    closeExpandedTextView();
    window.wikiHandler.syncCurrentPageLinks();
    bindWikiDomHandlers();
    console.log('Created Wiki Handler');
});

function bindWikiDomHandlers() {
    if (window.__wikiDomHandlersBound) return;
    window.__wikiDomHandlersBound = true;

    /**
     * Triggers whenever someone clicks onto an annotation that has a wiki page.
     */
    $('body').on('click.wiki', '.open-wiki-page', function (event) {
        event.preventDefault();
        event.stopPropagation();
        const isModalOpen = $('.wiki-page-modal').length > 0 && !$('.wiki-page-modal').hasClass('wiki-page-modal-minimized');
        if ($(this).hasClass('ui-action-disabled') || $(this).attr('aria-disabled') === 'true') {
            const reason = $(this).attr('data-disabled-reason');
            if (reason) showMessageModal("Unavailable action", reason);
            return;
        }
        if ($(this).hasClass('wiki-link-current') && isModalOpen) return;
        if ($(this).hasClass('wiki-link-broken')) {
            showMessageModal("Unavailable link", "This wiki target is currently unavailable.");
            return;
        }
        if ($(this).closest('.wiki-metadata-expanded-view').length > 0) {
            closeExpandedTextView();
        }
        window.wikiHandler.handleAnnotationClicked($(this));
    });

    /**
     * Keep wiki trigger links usable after closing/minimizing modal.
     */
    $('body').on('click.wiki', '.wiki-page-modal .backdrop, .wiki-page-modal .close-wiki-modal-btn, .wiki-page-modal .minimized-content', function () {
        closeExpandedTextView();
        setTimeout(function () {
            if (window.wikiHandler && typeof window.wikiHandler.syncCurrentPageLinks === 'function') {
                window.wikiHandler.syncCurrentPageLinks();
            }
        }, 0);
    });

    /**
     * Triggers whenever someone wants to go a wiki page back.
     */
    $('body').on('click.wiki', '.wiki-page-modal .go-back-btn', function () {
        window.wikiHandler.handleGoBackBtnClicked();
    });

    /**
     * Triggers whenever someone wants to navigate to the wiki home page.
     */
    $('body').on('click.wiki', '.wiki-page-modal .wiki-home-btn', function () {
        window.wikiHandler.handleHomeBtnClicked();
    });

    /**
     * Triggers when the user presses on a clickable rdf node
     */
    $('body').on('click.wiki', '.clickable-rdf-node', function () {
        window.wikiHandler.handleRdfNodeClicked($(this));
    });

    /**
     * Triggers when the user wants to expand a long metadata string
     */
    $('body').on('click.wiki', '.expand-metadata-string-btn', function () {
        const expandedContent = $(this).closest('.item-container').find('md-block').html();
        const title = $(this).closest('.item-container').find('label,.key').html();
        openInExpandedTextView(title, expandedContent);
    });

    $('body').on('click.wiki', '.wiki-metadata-expanded-view .close-expanded-view-btn', function (event) {
        event.preventDefault();
        closeExpandedTextView();
    });

    $('body').on('click.wiki', '.wiki-metadata-expanded-view', function (event) {
        if (event.target === this) {
            closeExpandedTextView();
        }
    });
}

/**
 * Opens something in a large text window, give title, content and a highlight array
 */
function openInExpandedTextView(title,
                                content,
                                highlightedWords = [],
                                wikiId = undefined,
                                wikiCoveredText = undefined) {
    ensureSingleWikiOverlays();
    if (highlightedWords && highlightedWords.length > 0) {
        highlightedWords.forEach(function (word) {
            if (word !== '') content = content.replaceAll(word, "<b>" + word + "</b>");
        });
    }
    $('.wiki-metadata-expanded-view .content').html(content);
    $('.wiki-metadata-expanded-view .title').html(title);

    const $wikiButton = $('.wiki-metadata-expanded-view a.open-wiki-page');
    if (wikiId) {
        $wikiButton.show();
        $wikiButton.data('wid', wikiId);
        if (wikiCoveredText) $wikiButton.data('wcovered', wikiCoveredText);
    } else {
        $wikiButton.hide();
    }
    $('.wiki-metadata-expanded-view').addClass('is-active').attr('aria-hidden', 'false');
}

bindWikiDomHandlers();

/**
 * retrieve and display the list of words for a selected topic
 */

function showWords() {
    const select = document.getElementById("topicSelect");
    let wordsContainer = document.getElementById("wordsContainer");
    let wordsList = document.getElementById("wordsList");

    let selectedOption = select.options[select.selectedIndex];
    let wordsData = selectedOption.getAttribute("data-words");

    if (wordsData) {
        wordsContainer.style.display = "block";
        wordsList.innerHTML = "";

        let wordsArray = wordsData.split(", ");
        wordsArray.forEach(function (word) {
            if (word.trim() !== "") {
                let li = document.createElement("li");
                li.textContent = word;
                wordsList.appendChild(li);
            }
        });
    } else {
        wordsContainer.style.display = "none";
    }
}
