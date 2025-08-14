<div class="filter-div flex-grow-1 m-0 pt-1 pb-1 pl-1 pr-1 small-font" style="min-width: 90%;" data-type="${filter.getValueType()}">
    <div class="flexed align-items-center justify-content-between group-box bg-lightgray p-2 mb-0">
        <label class="mb-0 mr-1 color-secondary w-50">${filter.getKey()}</label>

        <#if filter.getValueType().name() == "ENUM">
            <input type="hidden" value="{ANY}"/>
            <select class="ml-1 form-control small-font h-auto w-50 pl-0 pr-1 pb-1 pt-1"
                    onchange="$(this).prev('input').val($(this).find('option:selected').html())">
                <option>{ANY}</option>
                <#list filter.getPossibleCategories() as category>
                    <option>${category}</option>
                </#list>
            </select>
        </#if>

        <!-- TODO: Date needs better filering -->
        <#if filter.getValueType().name() == "DATE">
            <input type="number" class="w-50 ml-1 small-font p-1 form-control h-auto" placeholder="{${filter.getValueType().name()}}"/>
        </#if>

        <#if filter.getValueType().name() == "STRING" || filter.getValueType().name() == "URL">
            <input type="text" class="w-50 ml-1 small-font p-1 form-control h-auto" placeholder="{${filter.getValueType().name()}}"/>
        </#if>

        <!-- TODO: NUMBER needs better filtering option -->
        <#if filter.getValueType().name() == "NUMBER" && filter.getMin()?has_content && filter.getMax()?has_content>
            Min:
            <input type="number" data-range="min" class="w-25 ml-1 small-font p-1 form-control h-auto mr-2" placeholder="${filter.getMin()}"/>
            Max:
            <input type="number" data-range="max" class="w-25 ml-1 small-font p-1 form-control h-auto" placeholder="${filter.getMax()}"/>
        </#if>
    </div>
</div>
