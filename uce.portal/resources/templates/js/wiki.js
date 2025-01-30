let WikiHandler = (function () {

    WikiHandler.prototype.history = [];
    WikiHandler.prototype.currentPage = undefined;
    WikiHandler.prototype.universeHandler = undefined;

    function WikiHandler() {
    }

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
            url: "/api/wiki/annotationPage?wid=" + wikiDto.wid + "&covered=" + encodeURIComponent(wikiDto.coveredText),
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
                alert("There was an unknown error loading your page.")
            }
        }).always(() => {
            $('.wiki-page-modal .page-content .loading-div').fadeOut(100);
        });
    }

    WikiHandler.prototype.handleAnnotationClicked = function ($wikiEl) {
        const wid = $wikiEl.data('wid');
        let coveredText = $wikiEl.data('wcovered');
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
        if(value.includes('www.gbif.org')){
            window.open(value, '_blank').focus();
            return;
        }

        // Check if we have already loaded this rdfnode children before
        const expanded = $container.data('expanded');
        console.log(expanded);
        console.log($container.data('children'));
        if($container.data('children')){
            if(expanded){
                $container.find('.nodes-list-div').first().hide();
            } else{
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
                // TODO: Add a better error toast here
                alert("Request failed, since the server wasn't reachable.")
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
    $('.wiki-metadata-expanded-view .content').html(expandedContent);
    $('.wiki-metadata-expanded-view .title').html(title);
    $('.wiki-metadata-expanded-view').fadeIn(25);
});