<#list documents as document>
    <div class=" m-0 p-3 flexed justify-content-center h-100 w-100 align-items-start">
        <div class="document-card">
            <#assign searchId = "">
            <#include '*/search/components/documentCardContent.ftl' >
        </div>
    </div>
</#list>