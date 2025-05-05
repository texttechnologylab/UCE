<div class="analysis-view">
    <!-- Header -->
    <header class="container-fluid card-shadow bg-lightgray">
                <h3 class="mb-0 mr-1 color-prime text-center">${languageResource.get("analysis")}</h3>
    </header>
</div>
<div class="mt-4">
    <div class="row m-0 p-0">
        <div class="col-2">
            <div class="group-box card-shadow bg-light">
                <h5 class="mb-0 mr-1 color-prime">NLP ${languageResource.get("models")}</h5>
                <ul class="analysis-treeview">
                    <li>
                        <div class="tree-toggle">
                            <i class="fa-solid fa-chevron-right toggle-icon"></i>
                            <input type="checkbox" id="all-models-checkbox" />
                            <label class="group-label" for="all-models-checkbox">${languageResource.get("models")}</label>
                        </div>
                        <ul class="nested">
                            <#list modelGroups as group>
                                <li>
                                    <div class="tree-toggle">
                                        <i class="fa-solid fa-chevron-right toggle-icon"></i>
                                        <input type="checkbox" class="group-checkbox" id="group_${group_index}" />
                                        <label class="group-label" for="group_${group_index}">${group.name}</label>
                                    </div>
                                    <ul class="nested">
                                        <#list group.models as model>
                                            <li>
                                                <div class="model-item">
                                                    <label for="${group.name?replace(" ", "_")}_${model.key?replace(" ", "_")}">
                                                        <input type="checkbox" class="model-checkbox" id="${group.name?replace(" ", "_")}_${model.key?replace(" ", "_")}"/>
                                                        ${model.name!model.key}
                                                    </label>
                                                </div>
                                            </li>
                                        </#list>
                                    </ul>
                                </li>
                            </#list>
                        </ul>
                    </li>
                </ul>
            </div>
            <div class="group-box card-shadow bg-light">
                <h5 class="mb-0 mr-1 color-prime">${languageResource.get("history")}</h5>
                <div id="analysis-result-history">
                </div>
            </div>
        </div>
        <div class="col-5">
            <div class="group-box card-shadow bg-light">
                <h5 class="mb-0 mr-1 color-prime">${languageResource.get("inputField")}</h5>
                <div class="grow-text">
                    <label for="input"></label><textarea name="input" id="input" rows="10" placeholder="${languageResource.get("input")}" onInput="this.parentNode.dataset.replicatedValue = this.value"></textarea>
                    <br />
                    <button type="button" class="btn-primary" id="upload-btn">${languageResource.get("upload")}</button>
                    <input type="file" id="file-input" accept=".txt" style="display: none;" />
                    <button class="btn btn-success run-pipeline-btn">
                        <i class="fas fa-play"></i> ${languageResource.get("RunPipeline")}
                    </button>
                </div>
            </div>
            <div id="claim-field-wrapper" style="display: none;">
                <div class="group-box card-shadow bg-light">
                    <h5 class="mb-0 mr-1 color-prime">Claim</h5>
                    <div class="grow-text">
                        <textarea name="claim-text" id="claim-text" rows="10" placeholder="Claim" onInput="this.parentNode.dataset.replicatedValue = this.value"></textarea>
                    </div>
                </div>
            </div>
            <div id="text-field-wrapper" style="display: none;">
                <div class="group-box card-shadow bg-light">
                    <h5 class="mb-0 mr-1 color-prime">Coherence Text</h5>
                    <div class="grow-text">
                        <textarea name="coherence-text" id="coherence-text" rows="10" placeholder="Text" onInput="this.parentNode.dataset.replicatedValue = this.value"></textarea>
                    </div>
                </div>
            </div>
            <div id="analysis-InputText-container"></div>
        </div>
        <div class="col-5">
            <div class="group-box card-shadow bg-light">
                <h5 class="mb-0 mr-1 color-prime">${languageResource.get("output")}</h5>
                <div id="analysis-result-container">
                </div>
            </div>
        </div>
    </div>
</div>

