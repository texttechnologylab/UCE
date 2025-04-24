<head>
    <style>
        <#include "*/css/search-visualization.css">
    </style>
</head>

<div class="search-visualization-container">
    <script type="text/javascript">
        // TODO can we remove global state?
        // TODO "selected_feature" might not exist
        window.search_vizualization = window.search_vizualization || {
            settings: {
                n_bins: 10,
                selected_feature: "readability_flesch",
                all_features: ["readability_flesch"],
            }
        }

        // hydration from search results
        window.search_vizualization.viz_data = JSON.parse('${searchState.getVisualizationData()}')
        window.search_vizualization.settings.all_features = Object.keys(window.search_vizualization.viz_data["data"])

        update_search_vizualization()

        // TODO move to "search.js" later, does not work at the moment due to the element being generated later
        $('#search_viz_update_button').on('click', function (e) {
            // TODO more error handling
            const n_bins = parseInt($('#search_viz_n_bins').val())
            window.search_vizualization.settings.n_bins = n_bins

            const selected_feature = $('#search_viz_selected_feature').val()
            window.search_vizualization.settings.selected_feature = selected_feature

            update_search_vizualization()

            e.preventDefault()
        })
    </script>

    <div id="search-results-visualization-graph"></div>

    <form>
        <div class="row mt-2 mb-0 mx-0">
            <div class="col-md-4 mb-0">
                <div class="form-group">
                    <label for="search_viz_n_bins">Number of bins:</label>
                </div>
            </div>
            <div class="col-md-4 mb-0">
                <div class="form-group">
                    <label for="search_viz_selected_feature">Feature:</label>
                </div>
            </div>
            <div class="col-md-4 mb-0">
            </div>
        </div>
        <div class="row mt-0 mx-0">
            <div class="col-md-4 mt-0">
                <div class="form-group">
                    <input
                        type="number"
                        class="form-control"
                        id="search_viz_n_bins"
                        name="search_viz_n_bins"
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
                    <select id="search_viz_selected_feature" name="search_viz_selected_feature" class="form-control" required>
                        <option value="readability_flesch" selected>readability_flesch</option>
                    </select>
                </div>
            </div>
            <div class="col-md-4 mt-0">
                <div class="form-group">
                    <button type="submit" id="search_viz_update_button" class="form-control btn btn-primary">Update histogram</button>
                </div>
            </div>
        </div>
    </form>
</div>
