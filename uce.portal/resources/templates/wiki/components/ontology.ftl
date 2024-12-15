<#if rdfNodes?has_content && rdfNodes?size gt 0>
    <div class="next-nodes-container">
        <#include '*/wiki/components/rdfNodeList.ftl' >
    </div>
<#else>
    <p class="mb-0 text text-center w-100">
        ${languageResource.get("noOntologies")}
    </p>
</#if>
