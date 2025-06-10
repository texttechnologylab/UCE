class ECharts {
    constructor(containerId, option) {
        this.containerId = containerId;
        this.option = option;
        const container = document.getElementById(containerId);
        this.chart = echarts.init(container);
        this.render();
    }

    render() {
        this.chart.setOption(this.option);
    }

    getInstance() {
        return this.chart;
    }

    resize() {
        this.chart.resize();
    }

    updateOption(newOption) {
        this.option = newOption;
        this.render();
    }
}

export { ECharts };
