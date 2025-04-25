<div class="mt-4">
    <div class="row m-0 p-0">
        <div class="col-3">
            <div class="group-box card-shadow bg-light">
                <div class="tree-toggle">
                    <i class="fa-solid fa-chevron-right toggle-icon"></i>
                    <span class="group-label">Modelle</span>
                </div>
                <ul id="myUL">
                    <li><span class="tree-caret">Modelle</span></li>
                    <ul class="tree-nested">
                        <#list modelGroups as group>
                            <li>
                                <span class="tree-caret">${group.name}</span>
                                <ul class="tree-nested">
                                    <#list group.models as model>
                                        <li>
                                            <input type="checkbox" id="${group.name?replace(" ", "_")}_${model.key?replace(" ", "_")}" />
                                            <label for="${group.name?replace(" ", "_")}_${model.key?replace(" ", "_")}">
                                                ${model.name!model.key}
                                            </label>
                                        </li>
                                    </#list>
                                </ul>
                            </li>
                        </#list>
                    </ul>
                </ul>
            </div>
        </div>
        <div class="col-6">
            <div class="grow-text">
                <label for="input"></label><textarea name="input" id="input" rows="10" placeholder="${languageResource.get("input")}" onInput="this.parentNode.dataset.replicatedValue = this.value"></textarea>
            </div>
        </div>
        <div class="col-3">

        </div>
    </div>
</div>
<div class="side-bar">

</div>
<div class="analysis-view">
    <!-- Header -->
    <header class="container-fluid card-shadow bg-lightgray">
        <div class="container flexed align-items-center">
            <div class="flexed align-items-center">
                <h3 class="mb-0 mr-1 color-prime">${languageResource.get("analysis")}</h3>
            </div>
        </div>
    </header>
</div>


