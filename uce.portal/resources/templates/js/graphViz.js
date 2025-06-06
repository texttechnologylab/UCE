// Wrapper class for multiple visualization options.
// THIS NEEDS TO IMPORT THE LOCAL CHARTJS LIB IN THE HEADER:
// <!--<script src="js/visualization/cdns/chartjs-449.js"></script>-->
import {ChartJS} from '/js/visualization/chartjs.js';
import {UCEMap} from '/js/visualization/uceMap.js';
import {D3JS} from '/js/visualization/d3js.js';
import {ECharts} from '/js/visualization/echarts.js';

var GraphVizHandler = (function () {

    GraphVizHandler.prototype.activeCharts = {};

    function GraphVizHandler() {
        console.log('Created GraphViz Handler.');
    }

    GraphVizHandler.prototype.createUceMap = function (target, readonly = false) {
        const chartId = generateUUID();
        const uceMap = new UCEMap(target, readonly);

        this.activeCharts[chartId] = uceMap;
        activatePopovers();
        return uceMap;
    }

    /**
     * [CHARTJS] -> Creates a pie chart into the given $target (jquery object)
     */
    GraphVizHandler.prototype.createBasicChart = async function (target, title, data, type = null) {
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
        if (type) {
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
            word.style.cursor = 'default';
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


    GraphVizHandler.prototype.getColorForWeight = function (weight) {
        return getColorForWeight(weight);
    }

    GraphVizHandler.prototype.createSankeyChart = async function (target, title, linksData, nodesData,onClick = null) {
        const chartId = generateUUID();

        const option = {
            title: {
                text: title,
                top: 'bottom',
                left: 'right'
            },
            tooltip: {
                trigger: 'item',
                triggerOn: 'mousemove',
                formatter: function (params) {
                    if (params.dataType === 'edge') {
                        return params.data.source + ' → ' + params.data.target + ': <b>' + params.data.value + '</b>';
                    } else {
                        return params.name;
                    }
                }
            },
            series: {
                type: 'sankey',
                layout: 'none',
                data: nodesData,
                links: linksData,
                emphasis: {
                    focus: 'adjacency',
                    label: {
                        show: true,
                        color: '#000'
                    }
                },
                lineStyle: {
                    color: 'gradient',
                    curveness: 0.5
                },
                label: {
                    //color: '#000'
                    show: false
                }
            }
        };

        const echart = new ECharts(target, option); // Pass full option
        this.activeCharts[chartId] = echart;

        echart.getInstance().on('click', function (params) {
            if (onClick && typeof onClick === 'function') {
                onClick(params);
            }

            if (params.dataType === 'node') {
                const clickedNode = params.name;

                const connectedNodes = new Set([clickedNode]);
                const connectedLinks = new Set();

                linksData.forEach(link => {
                    if (link.source === clickedNode || link.target === clickedNode) {
                        connectedNodes.add(link.source);
                        connectedNodes.add(link.target);
                        connectedLinks.add(link.source + '->' + link.target);
                    }
                });

                const updatedNodes = nodesData.map(node => ({
                    ...node,
                    label: {
                        show: connectedNodes.has(node.name)
                    },
                    itemStyle: {
                        ...(node.itemStyle || {}),
                        opacity: connectedNodes.has(node.name) ? 1 : 0.2
                    }
                }));

                const updatedLinks = linksData.map(link => ({
                    ...link,
                    lineStyle: {
                        ...(link.lineStyle || {}),
                        opacity: connectedLinks.has(link.source + '->' + link.target) ? 1 : 0.1
                    }
                }));

                echart.getInstance().setOption({
                    series: [{
                        data: updatedNodes,
                        links: updatedLinks
                    }]
                });

            }
            else {
                const resetNodes = nodesData.map(node => ({
                    ...node,
                    label: { show: false },
                    // itemStyle: {
                    //     ...(node.itemStyle || {}),
                    //     opacity: 0.5
                    // }
                }));

                const resetLinks = linksData.map(link => ({
                    ...link,
                    lineStyle: {
                        color: 'gradient',
                        curveness: 0.5
                    },
                }));

                echart.getInstance().setOption({
                    series: [{
                        data: resetNodes,
                        links: resetLinks
                    }]
                });
            }
        });
        return echart;
    };

    GraphVizHandler.prototype.createMiniBarChart = function ({
                                                                 data = [],                   // Array of [label, value]
                                                                 title = '',                  // Chart title
                                                                 labelPrefix = '',            // Optional prefix for title like "Topics for"
                                                                 labelHighlight = '',         // The highlighted part (e.g., entity/topic name)
                                                                 primaryColor = '#5470C6',    // Default bar color
                                                                 secondaryColor = '#91CC75',  // Alternate bar color
                                                                 usePrimaryForEntity = false, // Toggle color logic
                                                                 barHeight = 10,              // Height of bars in px
                                                                 maxBarWidth = 100,           // Max bar width in px
                                                                 minLabelWidth = 70,          // Width of label column in px
                                                                 fontSize = 10                // Font size for values
                                                             } = {}) {

        const maxVal = Math.max(...data.map(([_, v]) => v)) || 1;

        const barsHtml = data.map(([label, value]) => {
            const width = Math.round((value / maxVal) * maxBarWidth);
            return (
                '<div style="margin:2px 0; display:flex; align-items:center;">' +
                '<span style="display:inline-block;min-width:' + minLabelWidth + 'px;vertical-align:middle;">' + label + '</span>' +
                '<span style="display:inline-block;height:' + barHeight + 'px;width:' + width + 'px;margin-left:5px;background:' + (usePrimaryForEntity ? primaryColor : secondaryColor) + ';vertical-align:middle;"></span>' +
                '<span style="font-size:' + fontSize + 'px;margin-left:5px;vertical-align:middle;">' + value + '</span>' +
                '</div>'
            );
        }).join('');

        const fullTitle = '<i>' + labelPrefix + '</i>';

        return (
            '<div style="min-width:200px;">' +
            '<div style="margin-bottom:6px;">' + (title || fullTitle) + '</div>' +
            (barsHtml || '<div style="color:#888;">No associations</div>') +
            '</div>'
        );

    }

    GraphVizHandler.prototype.createBarLineChart = async function (
        target,
        title,
        config,
        tooltipFormatter,
        onClick = null
    ) {
        const chartId = generateUUID();

        const {
            xData,
            seriesData,
            yLabel = 'Count'
        } = config;

        const option = {
            tooltip: {
                trigger: 'axis',
                enterable: true,
                backgroundColor: '#fff',
                borderColor: '#ccc',
                borderWidth: 1,
                textStyle: {
                    color: '#000',
                    fontSize: 12
                },
                formatter: tooltipFormatter
            },

            title: {
                text: title,
                left: 'center'
            },

            legend: {
                data: seriesData.map(s => s.name),
                top: 'bottom'
            },

            xAxis: {
                type: 'category',
                name: 'X',
                data: xData
            },

            yAxis: {
                type: 'value',
                name: yLabel
            },

            series: []
        };

        seriesData.forEach(s => {
            option.series.push({
                name: s.name + ' (Bar)',
                type: 'bar',
                data: s.data,
                itemStyle: {
                    color: s.color,
                    opacity: 0.15
                },
                barGap: '-100%',
                z: 1
            });

            option.series.push({
                name: s.name + ' (Line)',
                type: 'line',
                data: s.data,
                symbol: 'circle',
                symbolSize: 10,
                lineStyle: { width: 3, color: s.color },
                itemStyle: { color: s.color },
                z: 2
            });
        });

        const echart = new ECharts(target, option);
        this.activeCharts[chartId] = echart;

        if (onClick && typeof onClick === 'function') {
            echart.getInstance().on('click', onClick);
        }

        return echart;
    };

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


