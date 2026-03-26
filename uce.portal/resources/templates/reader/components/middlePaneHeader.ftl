<#macro render title="" published="" language="" wikiId="" wikiCovered="" externalUrl="" subtitle="">
    <div class="reader-middle-header">
        <div class="reader-middle-header-actions">
            <#if ((uceConfig.settings.ui.mainPage.showWikiModal)!true) && wikiId?has_content>
                <a class="header-btn open-wiki-page color-prime"
                   data-wtype="D"
                   data-wid="${wikiId}"
                   data-wcovered="${wikiCovered!title}">
                    <i class="large-font m-0 fab fa-wikipedia-w"></i>
                </a>
            </#if>

            <#if externalUrl?has_content>
                <a class="header-btn open-metadata-url-btn m-0" href="${externalUrl}" target="_blank">
                    <i class="color-prime m-0 large-font fas fa-university"></i>
                </a>
            </#if>
        </div>

        <div class="reader-middle-header-title-wrap">
            <h5>${title}</h5>
            <#if published?has_content>
                <p class="text mb-0">${published}</p>
            </#if>
            <#if subtitle?has_content>
                <p class="text mb-0">${subtitle}</p>
            </#if>
        </div>

        <#if language?has_content>
            <p class="m-0 text reader-middle-header-language">${language?upper_case}</p>
        </#if>
    </div>
</#macro>
