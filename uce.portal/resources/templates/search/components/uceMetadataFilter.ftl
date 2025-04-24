<div class="filter-div flex-grow-1 col-md-auto m-0 pt-1 pb-1 pl-2 pr-2 small-font" data-type="${filter.getValueType()}">
    <div class="flexed align-items-center justify-content-between">
        <label class="mb-0 mr-1 color-secondary">${filter.getKey()}</label>

        <#if filter.getValueType().name() == "ENUM">
            <input type="hidden" value="{ANY}"/>
            <select class="ml-1 form-control small-font h-auto w-100 pl-0 pr-1 pb-1 pt-1"
                    onchange="$(this).prev('input').val($(this).find('option:selected').html())">
                <option>{ANY}</option>
                <#list filter.getPossibleCategories() as category>
                    <option>${category}</option>
                </#list>
            </select>
        </#if>

        <!-- TODO: Date needs better filering -->
        <#if filter.getValueType().name() == "DATE">
            <input type="number" class="w-100 ml-1 small-font p-1 form-control h-auto" placeholder="{${filter.getValueType().name()}}"/>
        </#if>

        <#if filter.getValueType().name() == "STRING" || filter.getValueType().name() == "URL">
            <input type="text" class="w-100 ml-1 small-font p-1 form-control h-auto" placeholder="{${filter.getValueType().name()}}"/>
        </#if>

        <!-- TODO: NUMBER needs better filtering option -->
        <#if filter.getValueType().name() == "NUMBER">
            <input type="number" class="w-100 ml-1 small-font p-1 form-control h-auto" placeholder="{${filter.getValueType().name()}}"/>
        </#if>
    </div>
</div>
