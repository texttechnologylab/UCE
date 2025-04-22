// Wrapper class for multiple visualization options.
// THIS NEEDS TO IMPORT THE LOCAL CHARTJS LIB IN THE HEADER:
// <!--<script src="js/visualization/cdns/chartjs-449.js"></script>-->
import {ChartJS} from '/js/visualization/chartjs.js';
import {D3JS} from '/js/visualization/d3js.js';

var GraphVizHandler = (function () {

    GraphVizHandler.prototype.activeCharts = {};

    function GraphVizHandler() {
        console.log('Created GraphViz Handler.');
    }

    /**
     * [CHARTJS] -> Creates a pie chart into the given $target (jquery object)
     */
    GraphVizHandler.prototype.createBasicChart = async function (target, title, data, type=null) {
        const chartId = generateUUID();
        let wrapper = document.createElement('div');
        wrapper.classList.add('chart-container');
        wrapper.setAttribute('data-id', chartId);

        const canvas = document.createElement('canvas');
        const canvasContainer = document.createElement('div');
        canvasContainer.classList.add('canvas-container');
        canvasContainer.appendChild(canvas);
        wrapper.appendChild(canvasContainer);

        const menu = `
        <div class="menu">
            <button class="btn m-0 settings-btn" onclick="$(this).next('.btn-expanded').toggle()">
                <i class="fas fa-cog"></i>
            </button>
            <div class="btn-expanded display-none">
                <button class="btn change-type" data-type="pie" data-trigger="hover" data-toggle="popover" data-content="Pie Chart"><i class="fas fa-chart-pie"></i></button>
                <button class="btn change-type" data-type="line" data-trigger="hover" data-toggle="popover" data-content="Line Chart"><i class="fas fa-chart-line"></i></button>
                <button class="btn change-type" data-type="bar" data-trigger="hover" data-toggle="popover" data-content="Bar Chart"><i class="far fa-chart-bar"></i></button>
                <button class="btn change-type" data-type="polarArea" data-trigger="hover" data-toggle="popover" data-content="Polar Area Chart"><i class="fas fa-sun"></i></button>
                <button class="btn change-type" data-type="doughnut" data-trigger="hover" data-toggle="popover" data-content="Doughnut Chart"><i class="far fa-circle"></i></button>
                <button class="btn change-type" data-type="radar" data-trigger="hover" data-toggle="popover" data-content="Radar Chart"><i class="fas fa-wifi"></i></button>
            </div>
        </div>`;
        wrapper.insertAdjacentHTML('beforeend', menu);
        target.appendChild(wrapper);

        const jsChart = new ChartJS(canvas, title);

        jsChart.setData(data);
        if (type){
            jsChart.setType(type);
        }
        this.activeCharts[chartId] = jsChart;
        return jsChart;
    }

    return GraphVizHandler;
}());

function getNewGraphVizHandler() {
    return new GraphVizHandler();
}

window.graphVizHandler = getNewGraphVizHandler();


