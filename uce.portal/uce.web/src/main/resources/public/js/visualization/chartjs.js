class ChartJS {

    constructor(canvas, title='') {
        this.canvas = canvas;
        this.dataDict = undefined;
        this.config = {
            type: 'bar',
            data: {},
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom',
                        align: 'start'
                    },
                    title: title
                        ? { display: true, text: title }
                        : { display: false }
                },
                scales: {
                    x: { grid: { display: false } },
                    y: { grid: { display: false } }
                }
            }
        };
        this.primaryColor = getCustomUCEColors().primaryColor;
        this.chartObj = new Chart(canvas, this.config);
        this.attachEvents();
    }

    /**
     * We need to attach to some menu events, button events and the likes
     */
    attachEvents(){
        const $canvas = $(this.canvas);
        const $container = $canvas.closest('.chart-container');
        const ctx = this;
        $container.find('.btn-expanded .change-type').on('click', function(){
            ctx.setType($(this).data('type'));
        });
    }

    setData(data) {
        this.dataDict = data;
        const chartType = this.config.type;
        const isSegmentedChart = ['pie', 'doughnut', 'polarArea', 'radar'].includes(chartType);

        // Derive the color pallete from our primary color of UCE
        const baseHex = this.primaryColor.startsWith('#') ? this.primaryColor : rgbToHex(this.primaryColor);
        const backgroundColor = isSegmentedChart
            ? generateColorPalette(baseHex, values.length, 0.6)
            : convertToRGBA(baseHex, 0.85);

        this.chartObj.data = {
            labels: this.dataDict.map(row => row.year),
            datasets: [
                {
                    label: 'Acquisitions by year',
                    data: this.dataDict.map(row => row.count),
                    backgroundColor,
                    borderColor: backgroundColor
                }
            ]
        };

        this.chartObj.update();
    }

    setChartJsConfig(config) {
        if (this.chartObj) {
            this.chartObj.destroy();
        }

        // Clone to avoid mutations
        const newConfig = JSON.parse(JSON.stringify(config));

        // There is a problem with cartesian vs radial grids when
        // switching the chart type, hence the special checking
        if (['polarArea', 'radar'].includes(newConfig.type)) {
            delete newConfig.options.scales;
        } else {
            newConfig.options.scales = {
                x: {
                    grid: {
                        display: false
                    }
                },
                y: {
                    grid: {
                        display: true
                    }
                }
            };
        }

        this.config = newConfig;
        this.chartObj = new Chart(this.canvas, this.config);
        this.setData(this.dataDict);
    }

    getChartJsConfig(){
        return this.config;
    }

    setType(type) {
        this.config.type = type;
        this.setChartJsConfig(this.config);
    }

    bar()       { this.setType('bar'); }
    line()      { this.setType('line'); }
    pie()       { this.setType('pie'); }
    doughnut()  { this.setType('doughnut'); }
    polar()     { this.setType('polarArea'); }
    radar()     { this.setType('radar'); }
}

export {ChartJS}