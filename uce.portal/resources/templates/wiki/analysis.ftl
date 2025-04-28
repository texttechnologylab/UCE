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
                <h5 class="mb-0 mr-1 color-prime">DUUI-${languageResource.get("models")}</h5>
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
            </div>
            <div id="analysis-result-history">

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
        </div>
        <div class="col-5">
            <div class="group-box card-shadow bg-light">
                <h5 class="mb-0 mr-1 color-prime">${languageResource.get("output")}</h5>
            </div>
            <div id="analysis-result-container">
            </div>
<#--            <div class="group-box card-shadow bg-light">-->
<#--                <h5>Analyse-Resultat</h5>-->
<#--                <div id="analysis-result-container">-->
<#--                    <p><strong>Eingabetext:</strong></p>-->
<#--                    <#if DUUI??>-->
<#--                        <#if DUUI.modelGroups?has_content>-->
<#--                            <div class="border p-2 mb-2 bg-light">$</div>-->
<#--                            <#if DUUI.isTopic>-->
<#--                                <p><strong>Topic</strong></p>-->
<#--                            </#if>-->
<#--                            <#if DUUI.isHateSpeech>-->
<#--                                <p><strong>Hate</strong></p>-->
<#--                            </#if>-->
<#--                            <#if DUUI.isSentiment>-->
<#--                                <p><strong>Sentiment</strong></p>-->
<#--                            </#if>-->
<#--                        <#else>-->
<#--                            <p><strong>Kein Model</strong></p>-->
<#--                        </#if>-->
<#--                    <#else>-->
<#--                        <p><em>Keine Ausgabe</em></p>-->
<#--                    </#if>-->
<#--                </div>-->
<#--            </div>-->
        </div>
    </div>
</div>

