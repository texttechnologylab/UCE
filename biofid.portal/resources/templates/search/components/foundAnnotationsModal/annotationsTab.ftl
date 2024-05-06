<div class="mannotation-list">
    <div class="row m-0 p-0">

        <#assign mannotations = []>
        <#if type == "taxons">
            <#assign mannotations = searchState.getFoundTaxons(0, 750)>
        <#elseif type == "times">
            <#assign mannotations = searchState.getFoundTimes(0, 750)>
        <#else>
            <#assign mannotations = searchState.getNamedEntitiesByType(type, 0, 750)>
        </#if>
        <#list mannotations as annotation>
            <div class="col-lg-4 m-0 p-1">
                <div class="flexed align-items-center justify-content-between border rounded p-2">
                    <div class="flexed">
                        <label class="m-0 text-dark">${annotation.getCoveredText()}</label>
                        <label class="ml-1 m-0 text small-font">(${annotation.getOccurrences()})</label>
                    </div>
                    <input type="checkbox" checked/>
                </div>
            </div>
        </#list>
    </div>
</div>