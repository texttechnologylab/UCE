import { World } from '/js/corpusUniverse/world/world.js';

var CorpusUniverseHandler = (function () {

    CorpusUniverseHandler.prototype.world = undefined;

    function CorpusUniverseHandler() {}

    /**
     * Inits a new universe
     * @param targetId
     * @returns {Promise<void>}
     */
    CorpusUniverseHandler.prototype.createEmptyUniverse = async function (targetId) {
        const universeId = generateUUID();
        const container = document.querySelector('#' + targetId);

        this.world = new World(container, universeId);
        await this.world.init();

        this.world.start();
        container.setAttribute('data-id', universeId);
        console.log('Started new Universe with id: ' + this.world.getUniverseId());
    }

    /**
     * Within the world network, we focus a specific node determined by the document id
     * @param documentId
     */
    CorpusUniverseHandler.prototype.focusDocumentNode = function(documentId){
        this.world.focusNodeByDocumentId(documentId);
    }

    /**
     * Opens a new, full-screen, seperated view for the universe
     */
    CorpusUniverseHandler.prototype.openUniverseInNewTab = function(corpusId){
        const currentCenter = this.world.getCurrentCenter();
        let url = window.location.origin;
        url += "/api/corpusUniverse/new?corpusId=" + corpusId;
        url += "&currentCenter=" + currentCenter.x + ';'
            + currentCenter.y + ';'
            + currentCenter.z;
        window.open(url, '_blank');
    }

    /**
     * Renders a corpus in an empty universe
     */
    CorpusUniverseHandler.prototype.fromCorpus = async function(corpusId, currentCenter) {
        console.log('New universe from corpus with id ' + corpusId + ' with center ' + currentCenter);

        let networkDto = undefined;
        // Wrap AJAX call in a Promise to use await properly
        let result = await new Promise((resolve, reject) => {
            $.ajax({
                url: "/api/corpusUniverse/fromCorpus",
                type: "POST",
                data: JSON.stringify({
                    corpusId: corpusId,
                    level: 'DOCUMENTS',
                    currentCenter: currentCenter
                }),
                dataType: "json",
                success: function (response) {
                    if(response.status === 500){
                        showMessageModal("Error", "Problem building corpus universe - aborting operation.")
                        return;
                    }
                    networkDto = response;
                    resolve(response);
                },
                error: function (xhr, status, error) {
                    console.error(xhr.responseText);
                    reject(error);
                }
            });
        });

        if(!result || networkDto === undefined) return;

        console.log(networkDto);
        this.world.setNetworkFromDto(networkDto);
        this.world.redrawNetwork();
    }

    /**
     * Fills the universe with information by a search state
     * @param corpusId
     * @param range
     * @returns {Promise<void>}
     */
    CorpusUniverseHandler.prototype.fromSearch = async function(searchId){
        let networkDto = undefined;
        // Wrap AJAX call in a Promise to use await properly
        let result = await new Promise((resolve, reject) => {
            $.ajax({
                url: "/api/corpusUniverse/fromSearch",
                type: "POST",
                data: JSON.stringify({
                    searchId: searchId,
                    level: 'DOCUMENTS'
                }),
                dataType: "json",
                success: function (response) {
                    if(response.status === 500){
                        showMessageModal("Error", "Problem building corpus universe - aborting operation.")
                        return;
                    }
                    networkDto = response;
                    resolve(response);
                },
                error: function (xhr, status, error) {
                    console.error(xhr.responseText);
                    reject(error);
                }
            });
        });

        if(!result || networkDto === undefined) return;

        console.log(networkDto);
        // If its from a search, then the universe should be reduced
        this.world.setIsReducedView(true);
        this.world.setNetworkFromDto(networkDto);
        this.world.redrawNetwork();
    }

    return CorpusUniverseHandler;
}());

function getNewCorpusUniverseHandler(){
    return new CorpusUniverseHandler();
}

window.getNewCorpusUniverseHandler = getNewCorpusUniverseHandler();
