<style>
    <#include "*/css/json-beautfifier.css">
</style>

<script src="https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-mml-chtml.js"></script>

<#macro render metadataList>
    <#if metadataList?? && metadataList?has_content>
        <ul class="json-ul">
            <#list metadataList as item>
                <#assign has_children = item?? && item.getChildren()?? && item.getChildren()?has_content && item.getChildren()?size gt 0>
                <#assign has_value = item?? && item.getValue()?? && item.getValue()?has_content>
                <#assign is_long_string = has_value && item.getValue()?is_string && item.getValue()?length gt 150>
                <li>
                    <#assign layout = "flexed align-items-center justify-content-between">
                    <#if is_long_string>
                        <#assign layout = "">
                    </#if>
                    <#if has_value>
                        <div class="item-container ${layout}">
                            <div class="flexed align-items-center">
                                <i class="fas fa-key xsmall-font mr-2 text"></i>
                                <#assign k_class = "">
                                <label class="key mb-0 mr-2 color-prime">${item.getKey()!''}:</label>
                            </div>
                            <#if has_value>
                                <#if is_long_string>
                                    <hr class="mt-1 mb-0"/>
                                    <div class="p-2">
                                        <md-block class="mb-0 value">${item.getValue()!''}</md-block>
                                    </div>
                                <#else>
                                    <label class="value mb-0">
                                        ${item.getValue()!''} <i class="small-font text">(${item.getValueType()!''})</i>
                                    </label>
                                </#if>
                            </#if>
                        </div>
                    </#if>
                    <#if has_children>
                        <@render item.getChildren() /> <!-- Recursive call to render children -->
                    </#if>
                </li>
            </#list>
        </ul>
    </#if>
</#macro>

<div class="json-display">
    <@render jsonValueAsIterable/>
</div>