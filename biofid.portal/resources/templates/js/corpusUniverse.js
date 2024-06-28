
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

    return CorpusUniverseHandler;
}());

function getNewCorpusUniverseHandler(){
    return new CorpusUniverseHandler();
}

window.getNewCorpusUniverseHandler = getNewCorpusUniverseHandler();