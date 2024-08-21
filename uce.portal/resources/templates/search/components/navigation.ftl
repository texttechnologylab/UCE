<div class="pagination" data-max="${searchState.getTotalPages()}" data-cur="${searchState.getCurrentPage()}">
    <a class="btn mr-3 rounded-a next-page-btn" data-direction="-"><i class="fas fa-chevron-left"></i></a>
    <#assign start = searchState.getCurrentPage() - 2>
    <#assign end = searchState.getCurrentPage() + 2>
    <#assign totalPages = searchState.getTotalPages()>

    <#if (start < 1)>
        <#assign start = 1>
    </#if>
    <#if (end > totalPages)>
        <#assign end = totalPages>
    </#if>

    <#list start..end as i>
        <#if i == searchState.getCurrentPage()>
            <a class="btn rounded-a current-page page-btn" data-page="${i}">${i}</a>
        <#else>
            <a class="btn rounded-a page-btn" data-page="${i}">${i}</a>
        </#if>
    </#list>
    <a class="btn ml-3 rounded-a next-page-btn" data-direction="+"><i class="fas fa-chevron-right"></i></a>
</div>