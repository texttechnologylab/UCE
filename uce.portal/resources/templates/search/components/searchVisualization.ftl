<head>
    <style>
        <#include "*/css/search-visualization.css">
    </style>
</head>

<div class="search-visualization-container">
    <div id="search-results-visualization-graph"></div>

    <form>
        <div class="row mb-0 mx-0 mt-3">
            <div class="col-md-4 mb-0">
                <div class="form-group">
                    <label for="search-viz-n-bins">${languageResource.get("searchVisualizationPlotNBinsLabel")}</label>
                </div>
            </div>
            <div class="col-md-4 mb-0">
                <div class="form-group">
                    <label for="search-viz-selected-feature">${languageResource.get("searchVisualizationPlotFeatureLabel")}</label>
                </div>
            </div>
            <div class="col-md-4 mb-0">
            </div>
        </div>
        <div class="row mx-0">
            <div class="col-md-4 mt-0">
                <div class="form-group">
                    <input
                        type="number"
                        class="form-control"
                        id="search-viz-n-bins"
                        name="search-viz-n-bins"
                        min="1"
                        step="1"
                        placeholder="Number of bins"
                        value="10"
                        required
                    >
                </div>
            </div>
            <div class="col-md-4 mt-0">
                <div class="form-group">
                    <select id="search-viz-selected-feature" name="search-viz-selected-feature" class="form-control" required>
                    </select>
                </div>
            </div>
            <div class="col-md-4 mt-0">
                <div class="form-group">
                    <button type="submit" id="search-viz-update-button" class="form-control p-0 btn btn-primary"><i class="fas fa-sync mr-1"></i> ${languageResource.get("searchVisualizationPlotUpdateButton")}</button>
                </div>
            </div>
        </div>
    </form>
</div>

<script type="text/javascript">
    // TODO can we remove global state?
    // TODO "selected_feature" might not exist
    window.searchVizualization = window.searchVizualization || {
        settings: {
            nBins: 10,
            selectedFeature: "",
        }
    }

    // hydration from search results
    console.log('${searchState.getVisualizationData()}');
    window.searchVizualization.vizData = JSON.parse('${searchState.getVisualizationData()}')
    console.log(window.searchVizualization.vizData);

    updateSearchVizualization()
</script>