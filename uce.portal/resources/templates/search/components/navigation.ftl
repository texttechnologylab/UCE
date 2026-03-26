<#assign totalPages = searchState.getTotalPages()>
<#assign currentPage = searchState.getCurrentPage()>
<#assign windowSize = 5>
<#assign halfWindow = 2>
<#assign start = currentPage - halfWindow>
<#assign end = currentPage + halfWindow>

<#if (start < 1)>
    <#assign end = end + (1 - start)>
    <#assign start = 1>
</#if>
<#if (end > totalPages)>
    <#assign start = start - (end - totalPages)>
    <#assign end = totalPages>
</#if>
<#if (start < 1)>
    <#assign start = 1>
</#if>

<div class="pagination" data-max="${totalPages}" data-cur="${currentPage}">
    <a class="btn mr-3 rounded-a next-page-btn <#if currentPage lte 1>disabled</#if>"
       data-direction="-"
       aria-disabled="${(currentPage lte 1)?string('true','false')}">
        <i class="fas fa-chevron-left"></i>
    </a>
    <#if totalPages gt 0>
        <#list start..end as i>
            <#if i == currentPage>
                <a class="btn rounded-a current-page page-btn" data-page="${i}">${i}</a>
            <#else>
                <a class="btn rounded-a page-btn" data-page="${i}">${i}</a>
            </#if>
        </#list>
    </#if>
    <a class="btn ml-3 rounded-a next-page-btn <#if currentPage gte totalPages>disabled</#if>"
       data-direction="+"
       aria-disabled="${(currentPage gte totalPages)?string('true','false')}">
        <i class="fas fa-chevron-right"></i>
    </a>
</div>
