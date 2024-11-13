<div class="kwic-include">
    <header class="pb-2 pt-2 pl-3 pr-3">
        <h6 class="mb-0 text-dark">Keyword-in-Context</h6>
    </header>
    <div class="p-2">
        <#assign contextState = vm.getKwicState()>
        <#assign showHeader = false>
        <#include "*/search/components/keywordInContext.ftl">
    </div>
</div>