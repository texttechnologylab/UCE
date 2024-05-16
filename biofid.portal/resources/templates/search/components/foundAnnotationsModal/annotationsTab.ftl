<div class="mannotation-list">
    <div class="row m-0 p-0">

        <#assign mannotations = []>
        <#if type == "taxons">
            <#assign mannotations = taxon>
        <#elseif type == "times">
            <#assign mannotations = time>
        <#elseif type == "LOCATION">
            <#assign mannotations = location>
        <#elseif type == "ORGANIZATION">
            <#assign mannotations = organization>
        <#elseif type == "PERSON">
            <#assign mannotations = person>
        <#elseif type == "MISC">
            <#assign mannotations = misc>
        </#if>
        <#list mannotations as annotation>
            <div class="col-lg-3 m-0 p-1">
                <div class="draggable flexed align-items-center justify-content-between border rounded p-2 dcontainer" style="background-color: white"
                     draggable="true" data-id="-">
                    <div class="flexed">
                        <label class="m-0 title">${annotation.getCoveredText()}</label>
                        <label class="ml-1 m-0 text small-font count">(${annotation.getOccurrences()})</label>
                    </div>
                    <div class="h-100">
                        <i class="text fas fa-grip-vertical h-100"></i>
                    </div>
                    <!--<input type="checkbox" checked/>-->
                </div>
            </div>
        </#list>
    </div>
</div>