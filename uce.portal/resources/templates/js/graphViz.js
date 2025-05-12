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

    GraphVizHandler.prototype.createWordCloud = async function (target, title, wordData) {

        if (!wordData || !Array.isArray(wordData) || wordData.length === 0) {
            console.error('Invalid data provided to drawTopicWordCloud:', wordData);
            return;
        }

        target.innerHTML = '';

        const cloudContainer = document.createElement('div');
        cloudContainer.className = 'word-cloud-container';

        if (title) {
            const titleElement = document.createElement('h5');
            titleElement.className = 'word-cloud-title text-center';
            titleElement.textContent = title;
            target.insertBefore(titleElement, target.firstChild);
        }

        const maxWeight = Math.max(...wordData.map(item => item.weight));
        const minWeight = Math.min(...wordData.map(item => item.weight));
        const weightRange = maxWeight - minWeight;

        wordData.forEach(item => {
            const word = document.createElement('div');
            word.className = 'word-cloud-item';
            word.textContent = item.term;

            const normalizedWeight = weightRange === 0 ? 1 : (item.weight - minWeight) / weightRange;
            const fontSize = 12 + (normalizedWeight * 24);
            word.style.fontSize = fontSize + "px";
            word.style.color = getColorForWeight(normalizedWeight);

            word.addEventListener('mouseover', () => {
                word.classList.add('hovered');
                const tooltip = document.createElement('div');
                tooltip.className = 'word-tooltip';
                tooltip.textContent = "Weight: " + item.weight.toFixed(4);
                document.body.appendChild(tooltip);

                const rect = word.getBoundingClientRect();
                tooltip.style.top = (rect.top + word.offsetHeight + 50) + "px";
                tooltip.style.left = (rect.left + (word.offsetWidth / 5)) + "px";
                tooltip.style.transform = 'translateX(-50%)';
                word._tooltip = tooltip;
            });

            word.addEventListener('mouseout', () => {
                word.classList.remove('hovered');
                if (word._tooltip) {
                    word._tooltip.remove();
                    word._tooltip = null;
                }
            });

            cloudContainer.appendChild(word);
        });

        target.appendChild(cloudContainer);
    }


    GraphVizHandler.prototype.getColorForWeight = function(weight) {
        return getColorForWeight(weight);
    }

    return GraphVizHandler;
}());

function getNewGraphVizHandler() {
    return new GraphVizHandler();
}

function getColorForWeight(weight) {
    const r = Math.floor(255 * (1 - weight));
    const g = Math.floor(200 * weight);
    const b = 0;
    return "rgba(" + r + ", " + g + ", " + b + ", 0.5)";
}

window.graphVizHandler = getNewGraphVizHandler();


