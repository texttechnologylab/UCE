<#if historyList??>
    <#list historyList as history>
        <button type="button" class="btn-primary" id="history-${history}">${history}</button>
    </#list>
</#if>