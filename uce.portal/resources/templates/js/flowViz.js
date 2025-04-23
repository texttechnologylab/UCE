import {DrawflowJS} from '/js/visualization/drawflowjs.js';

var FlowVizHandler = (function () {

    FlowVizHandler.prototype.activeFlows = {};

    function FlowVizHandler() {
        console.log('Created FlowVizHandler Handler.');
    }

    /**
     */
    FlowVizHandler.prototype.createFlowChart = async function (target, initialNode) {
        const flowId = generateUUID();
        const flow = new DrawflowJS(target);
        flow.init(initialNode);
        this.activeFlows[flowId] = flow;

        activatePopovers();
    }

    FlowVizHandler.prototype.createNewFromLinkableNode = async function (unique) {
        $.ajax({
            url: '/api/wiki/linkable/node',
            type: "POST",
            data: JSON.stringify({
                unique: unique,
            }),
            contentType: "application/json",
            success: function(response) {
                const node = JSON.parse(response);
                const container = document.getElementById('full-flow-container');
                $(container).show();
                container.style.visibility = 'visible';
                window.flowVizHandler.createFlowChart(container, node);
            },
            error: (xhr, status, error) => {
                showMessageModal("Unknown Error", "There was an unknown error loading the linkable node.")
            }
        }).always(() => {
        });
    }

    return FlowVizHandler;
}());

function getNewFlowVizHandler() {
    return new FlowVizHandler();
}

window.flowVizHandler = getNewFlowVizHandler();

/**
 * This triggers whenever a button to
 */
$('body').on('click', '.open-linkable-node', function () {
    const unique = $(this).data('unique');
    window.flowVizHandler.createNewFromLinkableNode(unique);
})

