<div class="uce-metadata-container">
    <#if !(uceMetadata?? && uceMetadata?has_content) || uceMetadata?size == 0>
        <p class="text-center mb-0 text">No metadata found for that document.</p>
    <#else>
        <div class="row m-0 p-0">
            <#list uceMetadata as metadata>
                <#if metadata?? && metadata.getValueType()?? && metadata.getValueType().name()?string == 'JSON'>
                    <#if metadata.getJsonValueAsIterable()??>
                        <div class="col-12 mb-3 mr-0 ml-0 mt-0 p-0">
                            <p class="mb-0 text-center color-prime w-100 mb-1" data-trigger="hover"
                               data-toggle="popover" data-placement="top" data-content="${metadata.getComment()!''}">
                                ${metadata.getKey()!''} <i class="mb-0 small-font text">(${metadata.getValueType()?lower_case!''})</i>
                            </p>
                            <div class="w-100">
                                <#assign jsonValueAsIterable = metadata.getJsonValueAsIterable()>
                                <#include "*/document/jsonBeautifier.ftl">
                            </div>
                        </div>
                    <#else>
                        <p>Invalid or missing JSON data.</p>
                    </#if>
                <#else>
                    <div class="col-md-4 m-0 pl-1 pr-1 w-100" data-trigger="hover"
                         data-toggle="popover" data-placement="top" data-content="${metadata.getComment()!''}">
                        <div class="flexed align-items-center justify-content-between uce-metadata-item">
                            <div class="flexed align-items-center">
                                <label class="mb-0 mr-1 color-prime">${metadata.getKey()!''}</label>
                                <i class="mb-0 small-font text">(${metadata.getValueType()?lower_case!''})</i>
                            </div>
                            <#if metadata.getValueType()?has_content && metadata.getValueType() == 'URL'>
                                <a class="ml-2 small-font ellipsis-text" href="${metadata.getValue()!''}"
                                   target="_blank">${metadata.getValue()!''}</a>
                            <#else>
                                <label class="mb-0 ml-2 small-font color-dark">${metadata.getValue()!''}</label>
                            </#if>
                        </div>
                    </div>
                </#if>
            </#list>
        </div>
    </#if>
</div>
