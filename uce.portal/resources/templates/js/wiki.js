
let WikiHandler = (function () {

    WikiHandler.prototype.history = [];
    WikiHandler.prototype.currentPage = undefined;

    function WikiHandler() {}

    WikiHandler.prototype.addPageToHistory = function (wikiDto) {
        if(wikiDto !== undefined) this.history.push(wikiDto);
    }

    WikiHandler.prototype.handleGoBackBtnClicked = function (){
        if(this.history.length === 0) return;
        let lastPage = this.history[this.history.length - 1];
        this.history.pop();
        this.loadPage(lastPage, true);
    }

    WikiHandler.prototype.loadPage = function(wikiDto, calledFromBackBtn = false){
        // If the current open page is the clicked wiki annotation, don't reload it.
        if(window.wikiHandler.currentPage !== undefined && window.wikiHandler.currentPage.hash === wikiDto.hash) return;

        $.ajax({
            url: "/api/wiki/annotationPage?wid=" + wikiDto.wid + "&covered=" + encodeURIComponent(wikiDto.coveredText),
            type: "GET",
            success: function (response) {
                $('.wiki-page-modal .include').html(response);
                activatePopovers();
                // Add the last page to the history.
                if(calledFromBackBtn){
                    window.wikiHandler.currentPage = wikiDto;
                    window.wikiHandler.addPageToHistory(window.wikiHandler.currentPage);
                } else{
                    window.wikiHandler.addPageToHistory(window.wikiHandler.currentPage);
                    window.wikiHandler.currentPage = wikiDto;
                }
            },
            error: function (xhr, status, error) {
                console.error(xhr.responseText);
                // TODO: Make this alert prettier.
                alert("There was an unknown error loading your page.")
            }
        });
    }

    WikiHandler.prototype.handleAnnotationClicked = function ($wikiEl){
        const wid = $wikiEl.data('wid');
        let coveredText = $wikiEl.data('wcovered');
        if(coveredText === undefined || coveredText === ''){
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

function getNewWikiHandler(){
    return new WikiHandler();
}

$(document).ready(function(){
    window.wikiHandler = getNewWikiHandler();
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

