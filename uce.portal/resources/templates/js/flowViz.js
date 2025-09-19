import {DrawflowJS} from '/js/visualization/drawflowjs.js';

var FlowVizHandler = (function () {

    FlowVizHandler.prototype.activeFlows = {};

    function FlowVizHandler() {
        console.log('Created FlowVizHandler Handler.');
    }

    FlowVizHandler.prototype.createFlowChart = async function (target, initialNode) {
        const flowId = generateUUID();
        const flow = new DrawflowJS(target);
        flow.init(initialNode);
        this.activeFlows[flowId] = flow;

        activatePopovers();
    }

    FlowVizHandler.prototype.createNewFromLinkableNode = async function (unique, target) {
        let container = undefined;
        if (target === undefined || target === '') {
            container = document.getElementById('full-flow-container');
            $(container).parent('#flow-chart-modal').show();
            container.innerHTML = ''; // reset the last chart
        } else {
            container = target;
            container.innerHTML = ''; // reset the last chart
            // Add a loader
            $(container).append(`
                <div class="full-loader">
                    <div class="simple-loader"><p class="p-2 m-0 text-center w-100 color-prime font-italic">Loading</p></div>
                </div>`);
        }

        $.ajax({
            url: '/api/wiki/linkable/node',
            type: "POST",
            data: JSON.stringify({
                unique: unique,
            }),
            contentType: "application/json",
            success: function (response) {
                const node = response;
                window.flowVizHandler.createFlowChart(container, node);
                $(container).find('.full-loader').fadeOut(125);
            },
            error: (xhr, status, error) => {
                showMessageModal("Unknown Error", "There was an unknown error loading the linkable node.");
            }
        }).always(() => {
            $(container).find('.full-loader').fadeOut(125);
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
    const target = $(this).data('target');
    window.flowVizHandler.createNewFromLinkableNode(unique, target);
})

