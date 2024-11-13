let WikiHandler = (function () {

    WikiHandler.prototype.history = [];
    WikiHandler.prototype.currentPage = undefined;

    function WikiHandler() {
    }

    WikiHandler.prototype.addPageToHistory = function (wikiDto) {
        if (wikiDto !== undefined) this.history.push(wikiDto);
        console.log(this.history);
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

    return WikiHandler;
}());

function getNewWikiHandler() {
    return new WikiHandler();
}

$(document).ready(function () {
    window.wikiHandler = getNewWikiHandler();
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

