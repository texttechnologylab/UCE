class ECharts {
    constructor(containerId, option) {
        this.containerId = containerId;
        this.option = option;
        const container = document.getElementById(containerId);
        if (!container) {
            this.chart = null;
            return;
        }
        const existingChart = echarts.getInstanceByDom(container);
        if (existingChart) {
            existingChart.dispose();
        }
        this.chart = echarts.init(container);
        this.render();
    }

    render() {
        if (!this.chart) return;
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
