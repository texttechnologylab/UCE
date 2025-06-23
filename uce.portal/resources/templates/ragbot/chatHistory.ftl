<div class="chat-state h-100 pb-5" data-id="${chatState.getChatId()}"></div>
<#list chatState.getMessages() as message>
    <div class="message" data-type="${message.getRole()?lower_case}">

        <#if message.getContextDocuments()?size gt 0>
            <p class="mb-1 text-center">
                ${languageResource.get("ragBotSearch")}
            </p>
            <div class="cdocuments-list row mr-0 mt-0 ml-0">
                <#list message.getContextDocuments() as document>
                    <div class="col-md-12 p-2 m-0">
                        <div class="cdocument-card hoverable open-document"
                             data-id="${document.getId()?string?replace('.', '')?replace(',', '')}">
                            <p class="mb-1 small-font color-secondary"><i
                                        class="m-0 fas fa-book-open mr-1 color-secondary"></i> ${document.getDocumentTitle()}
                            </p>
                            <hr class="mt-2 mb-2"/>
                            <p class="mb-0 small-font text font-italic mr-2">
                                "${document.getFullTextSnippet(50)}..."
                            </p>
                        </div>
                    </div>
                </#list>
            </div>
        </#if>

        <#if message.getRole().name()?string == "USER">
            <p class="raw mb-0">${message.getMessage()}</p>
        <#else>
            <md-block class="raw mb-0">${message.getMessage()}</md-block>
        </#if>
    </div>
</#list>

