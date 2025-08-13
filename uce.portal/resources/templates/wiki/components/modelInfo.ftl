<#if model.getModelVersion()??>
    <#assign modelVersionObject = model.getModelVersion()>
    <#assign modelObject = modelVersionObject.getModel()>
    <#assign modelVersion = modelVersionObject.getVersion()!>
    <#assign modelName = modelObject.getName()!>
<#else>
    <#assign modelVersion = "N/A">
    <#assign modelName = "N/A">
</#if>
<div class="row m-0 p-0">
    <div class="col-md-6 p-0 m-0 custom-col">
        <div class="pl-2 pr-2 pb-1 pt-1 flexed align-items-center justify-content-between">
            <label class="mb-0 mr-1">
                Model:
            </label>
            <label class="text mb-0 ml-1">
                ${modelName} (${modelVersion})
            </label>
        </div>
    </div>
</div>