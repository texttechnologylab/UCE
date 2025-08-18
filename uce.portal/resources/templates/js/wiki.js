let WikiHandler = (function () {

    WikiHandler.prototype.history = [];
    WikiHandler.prototype.currentPage = undefined;
    WikiHandler.prototype.universeHandler = undefined;
    WikiHandler.prototype.occurrencesTake = 20;
    WikiHandler.prototype.lexiconState = {
        skip: 0,
        take: 24,
        selectedChar: '',
        searchInput: '',
        annotationFilters: [],
        sortColumn: 'occurrence',
        sortDirection: 'DESC',
    }

    function WikiHandler() {
    }

    // =================== Lexicon Methods ===================
    WikiHandler.prototype.updateLexiconPage = function () {
        let curPage = this.lexiconState.skip / this.lexiconState.take + 1;
        let start = 1;
        if (curPage <= 3) start = 1;
        else if (curPage > 4) start = curPage - 3;
        const btnList = $('.lexicon-view .lexicon-navigation .pages-count');
        btnList.html("");
        for (let i = start; i < curPage + 4; i++) {
            const selected = i === curPage ? "cur-page" : "";
            btnList.append(
                "<a class='rounded-a SELECTED' onclick='window.wikiHandler.fetchLexiconPage(PAGE)'>PAGE</a>"
                    .replaceAll("PAGE", i)
                    .replace("SELECTED", selected)
            );
        }
    }

    WikiHandler.prototype.handleLexiconSearchInputChanged = function ($source) {
        this.lexiconState.searchInput = $source.val();
        this.lexiconState.skip = 0;
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
        this.lexiconState.skip = this.lexiconState.take * (pageNum - 1);
        this.fetchLexiconEntries(this.lexiconState.skip, this.lexiconState.take);
    }

    WikiHandler.prototype.fetchPreviousLexiconEntries = function () {
        if (this.lexiconState.skip < this.lexiconState.take) return;
        this.lexiconState.skip -= this.lexiconState.take;
        this.fetchLexiconEntries(this.lexiconState.skip, this.lexiconState.take);
    }

    WikiHandler.prototype.fetchNextLexiconEntries = function () {
        this.lexiconState.skip += this.lexiconState.take;
        this.fetchLexiconEntries(this.lexiconState.skip, this.lexiconState.take);
    }

    WikiHandler.prototype.fetchLexiconEntries = function (skip, take) {
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
                if(response.rendered) $('.lexicon-content-include').html(response.rendered);
                else $('.lexicon-content-include').html(response);
                this.updateLexiconPage();
            },
            error: (xhr, status, error) => {
                showMessageModal("Unknown Error", "There was an unknown error loading the lexicon entries.")
            }
        }).always(() => {
        });
    }

    WikiHandler.prototype.handleLoadMoreOccurrences = function () {
        const $target = $('.lexicon-view .lexicon-entry-inspector .occurrences-list');
        let skip = $target.data('skip') + this.occurrencesTake;
        $target.data('skip', skip);
        this.fetchLexiconEntryOccurrences($target.data('covered'), $target.data('type'),
            skip, $target);
    }

    WikiHandler.prototype.handleOccurrencesNavigationClicked = function ($source) {
        const $lexiconEntry = $source.closest('.lexicon-entry');
        const type = $lexiconEntry.data('type');
        const covered = $lexiconEntry.data('covered');
        const $target = $('.lexicon-view .lexicon-entry-inspector .occurrences-list');
        // clean the inspector list
        $target.html('');
        $target.data('skip', 0);
        $target.data('covered', covered);
        $target.data('type', type);
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

    WikiHandler.prototype.loadPage = function (wikiDto, calledFromBackBtn = false) {
        // If the current open page is the clicked wiki annotation, don't reload it.
        if (window.wikiHandler.currentPage !== undefined && window.wikiHandler.currentPage.hash === wikiDto.hash) return;
        $('.wiki-page-modal .page-content .loading-div').fadeIn(100);

        $.ajax({
            url: "/api/wiki/page?wid=" + wikiDto.wid + "&covered=" + encodeURIComponent(wikiDto.coveredText),
            type: "GET",
            success: (response) => {
                $('.wiki-page-modal .page-content .include').html(response);
                activatePopovers();

                // Only add to history if it's not called from the back button
                if (!calledFromBackBtn) {
                    if (this.currentPage) {
                        this.addPageToHistory(this.currentPage);
                    }
                    this.currentPage = wikiDto;
                } else {
                    // Update current page without adding to history
                    this.currentPage = wikiDto;
                }
            },
            error: (xhr, status, error) => {
                console.error(xhr.responseText);
                showMessageModal("Unknown Error", "There was an unknown error loading your page.")
            }
        }).always(() => {
            $('.wiki-page-modal .page-content .loading-div').fadeOut(100);
        });
    }

    WikiHandler.prototype.handleAnnotationClicked = function ($wikiEl) {
        const wid = $wikiEl.data('wid');
        let coveredText = String($wikiEl.data('wcovered'));
        if (coveredText === undefined || coveredText === '') {
            coveredText = $wikiEl.html();
        }
        // Show the modal
        $('.wiki-page-modal').removeClass('wiki-page-modal-minimized');
        const wikiDto = {
            wid: wid,
            coveredText: coveredText,
            hash: wid + coveredText
        }

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

$(document).ready(function () {
    window.wikiHandler = getNewWikiHandler();
    $('.wiki-page-modal .page-content .loading-div').fadeOut();
    console.log('Created Wiki Handler');
});

/**
 * Triggers whenever someone clicks onto an annotation that has a wiki page.
 */
$('body').on('click', '.open-wiki-page', function () {
    window.wikiHandler.handleAnnotationClicked($(this));
});

/**
 * Triggers whenever someone wants to go a wiki page back.
 */
$('body').on('click', '.wiki-page-modal .go-back-btn', function () {
    window.wikiHandler.handleGoBackBtnClicked();
});

/**
 * Triggers when the user presses on a clickable rdf node
 */
$('body').on('click', '.clickable-rdf-node', function () {
    window.wikiHandler.handleRdfNodeClicked($(this));
});

/**
 * Triggers when the user wants to expand a long metadata string
 */
$('body').on('click', '.expand-metadata-string-btn', function () {
    const expandedContent = $(this).closest('.item-container').find('md-block').html();
    const title = $(this).closest('.item-container').find('label,.key').html();
    openInExpandedTextView(title, expandedContent);
});

/**
 * Opens something in a large text window, give title, content and a highlight array
 */
function openInExpandedTextView(title,
                                content,
                                highlightedWords = [],
                                wikiId = undefined,
                                wikiCoveredText = undefined) {
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
    $('.wiki-metadata-expanded-view').fadeIn(25);
}

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