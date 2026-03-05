<#if document.getMimeType() == "application/pdf" || document.getMimeType() == "pdf">
    <#include '*/reader/components/viewerPdf.ftl' />
<#elseif document.getMimeType()?starts_with("image/")>
    <#include '*/reader/components/viewerImage.ftl' />
<#else>
    <div class="document-content">
        <#-- Here we lazily load in the pages -->
    </div>
    <#-- Scrollbar Minimap -->
    <div class="scrollbar-minimap">
        <div class="minimap-markers"></div>
        <div class="minimap-preview">
            <div class="preview-content"></div>
        </div>
    </div>
</#if>

