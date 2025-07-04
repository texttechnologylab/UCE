<div class="analysis-view">
    <!-- Header -->
    <header class="container-fluid card-shadow bg-lightgray">
                <h3 class="mb-0 mr-1 color-prime text-center">${languageResource.get("analysis")}</h3>
    </header>
</div>
<div class="mt-4">
    <div class="row m-0 p-0">
        <div class="col-3">
            <div class="resizable-container">
                <div class="analysis-sidebar-toggle">
                    <button class="analysis-toggle-btn" onclick="toggleSidebar('nlp-tools')" title="NLP-Tools">
                        <i class="fa fa-language" aria-hidden="true"></i>
                    </button>
                    <button class="analysis-toggle-btn" onclick="toggleSidebar('history')" title="History">
                        <i class="fa fa-folder-plus"></i>
                    </button>
                </div>
                <div class="analysis-main-content" id="analysis-main-content">
                    <div id="nlp-tools" class="analysis-sidebar-section draggable-section" draggable="true">
                        <div class="group-box card-shadow bg-light">
                            <h5 class="mb-0 mr-1 color-prime">NLP ${languageResource.get("models")}</h5>
                            <div class="treeview-wrapper">
                                <ul class="analysis-treeview">
                                    <li>
                                        <div class="tree-toggle">
                                            <i class="toggle-icon"></i>
                                            <input type="checkbox" id="all-analysis-models-checkbox" />
                                            <label class="group-label" for="all-analysis-models-checkbox">${languageResource.get("models")}</label>
                                        </div>
                                        <ul class="nested">
                                            <#list modelGroups as group>
                                                <li>
                                                    <div class="tree-toggle">
                                                        <i class="toggle-icon"></i>
                                                        <input type="checkbox" class="nlp-group-checkbox analysis-group-checkbox" id="group_${group_index}" />
                                                        <label class="analysis-group-label" for="group_${group_index}">${group.name} (${group.models?size})</label>
                                                    </div>
                                                    <ol class="nested">
                                                        <#list group.models as model>
                                                            <li>
                                                                <div class="model-item">
                                                                    <label for="${group.name?replace(" ", "_")}_${model.key?replace(" ", "_")}">
                                                                        <input type="checkbox" class="nlp-model-checkbox analysis-model-checkbox" id="${group.name?replace(" ", "_")}_${model.key?replace(" ", "_")}" />
                                                                        ${model.name!model.key}
                                                                    </label>
                                                                </div>
                                                            </li>
                                                        </#list>
                                                    </ol>
                                                </li>
                                            </#list>

                                            <li>
                                                <div class="tree-toggle">
                                                    <i class="toggle-icon"></i>
                                                    <input type="checkbox" class="ttlab-group-checkbox analysis-group-checkbox" id="group_ttlab_scorer" />
                                                    <label class="analysis-group-label" for="group_ttlab_scorer">TTLAB Scorer (${ttlabScorer?size})</label>
                                                </div>
                                                <ul class="nested">
                                                    <#list ttlabScorer?keys as models>
                                                        <#assign submodels = ttlabScorer[models]>
                                                        <li>
                                                            <div class="tree-toggle">
                                                                <i class="toggle-icon"></i>
                                                                <input type="checkbox" class="ttlab-subgroup-checkbox analysis-group-checkbox" id="group_${models?index}" />
                                                                <label class="analysis-group-label" for="group_${models?index}">${models} (${submodels?size})</label>
                                                            </div>
                                                            <ul class="nested">
                                                                <#list submodels?keys as properties>
                                                                    <#assign property = submodels[properties]>
                                                                    <li>
                                                                        <div class="tree-toggle">
                                                                            <i class="toggle-icon"></i>
                                                                            <input type="checkbox" class="ttlab-subgroup-checkbox analysis-group-checkbox" id="subgroup_${properties?index}" />
                                                                            <label class="analysis-group-label" for="subgroup_${properties?index}">${properties} (${property?size})</label>
                                                                        </div>
                                                                        <ol class="nested">
                                                                            <#list property?keys as name>
                                                                                <#assign keyname = property[name]>
                                                                                <li>
                                                                                    <div class="model-item">
                                                                                        <label for="${keyname}">
                                                                                            <input type="checkbox" class="ttlab-model-checkbox analysis-model-checkbox" id="ttlabscorer##${keyname}" />
                                                                                            ${name}
                                                                                        </label>
                                                                                    </div>
                                                                                </li>
                                                                            </#list>
                                                                        </ol>
                                                                    </li>
                                                                </#list>
                                                            </ul>
                                                        </li>
                                                    </#list>
                                                </ul>
                                            </li>
                                            <!-- Ende TTLAB Scorer -->
                                        </ul>
                                    </li>
                                </ul>
                            </div>
                        </div>
                    </div>
                    <div id="history" class="analysis-sidebar-section draggable-section" draggable="true">
                        <div class="group-box card-shadow bg-light">
                            <h5 class="mb-0 mr-1 color-prime">${languageResource.get("history")}</h5>
                            <div id="analysis-result-history">
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <div class="col-4">
            <div class="group-box card-shadow bg-light">
                <h5 class="mb-0 mr-1 color-prime">${languageResource.get("inputField")}</h5>
                <div class="analysis-text-view">
                    <div class="grow-text">
                        <label for="analysis-input"></label><textarea name="analysis-input" id="analysis-input" rows="10" placeholder="${languageResource.get("input")}" onInput="this.parentNode.dataset.replicatedValue = this.value"></textarea>
                        <br />
                        <button type="button" class="btn-primary" id="analysis-upload-btn">${languageResource.get("upload")}</button>
                        <input type="file" id="file-input" accept=".txt" style="display: none;" />
                        <button class="btn btn-success run-pipeline-btn">
                            <i class="fas fa-play"></i> ${languageResource.get("RunPipeline")}
                        </button>
                    </div>
                </div>
            </div>
            <div id="claim-field-wrapper" style="display: none;">
                <div class="group-box card-shadow bg-light">
                    <h5 class="mb-0 mr-1 color-prime">Claim</h5>
                    <div class="analysis-text-view">
                        <div class="grow-text">
                            <textarea name="claim-text" id="claim-text" rows="10" placeholder="Claim" onInput="this.parentNode.dataset.replicatedValue = this.value"></textarea>
                        </div>
                    </div>
                </div>
            </div>
            <div id="text-field-wrapper" style="display: none;">
                <div class="group-box card-shadow bg-light">
                    <h5 class="mb-0 mr-1 color-prime">Cohesion Text</h5>
                    <div class="analysis-text-view">
                        <div class="grow-text">
                            <textarea name="coherence-text" id="coherence-text" rows="10" placeholder="Text" onInput="this.parentNode.dataset.replicatedValue = this.value"></textarea>
                        </div>
                    </div>
                </div>
            </div>
            <div id="stance-field-wrapper" style="display: none;">
                <div class="group-box card-shadow bg-light">
                    <h5 class="mb-0 mr-1 color-prime">Hypothesis</h5>
                    <div class="analysis-text-view">
                        <div class="grow-text">
                            <textarea name="stance-text" id="stance-text" rows="10" placeholder="Text" onInput="this.parentNode.dataset.replicatedValue = this.value"></textarea>
                        </div>
                    </div>
                </div>
            </div>
            <div id="llm-field-wrapper" style="display: none;">
                <div class="group-box card-shadow bg-light">
                    <h5 class="mb-0 mr-1 color-prime">System Prompt</h5>
                    <div class="analysis-text-view">
                        <div class="grow-text">
                            <textarea name="llm-text" id="llm-text" rows="10" placeholder="Text" onInput="this.parentNode.dataset.replicatedValue = this.value"></textarea>
                        </div>
                    </div>
                </div>
            </div>
            <div id="analysis-InputText-container"></div>
        </div>
        <div class="col-5">
            <div>
<#--                <h5 class="mb-0 mr-1 color-prime">${languageResource.get("output")}</h5>-->
                <div id="analysis-result-container">
                </div>
            </div>
        </div>
    </div>
</div>

