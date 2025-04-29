<#if historyList??>
    <div class="history-buttons">
        <#list historyList as history>
            <button type="button" class="btn btn-primary mb-2" id="history-${history}">
                Request ${history}
            </button>
        </#list>
    </div>
</#if>