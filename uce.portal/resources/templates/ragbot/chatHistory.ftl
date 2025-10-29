<div class="chat-state h-100 pb-5" data-id="${chatState.getChatId()}"></div>
<#list chatState.getMessages() as message>

    <#if message.getRole()?lower_case == "tool">
      <#continue>
    </#if>

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
                                        class="m-0 fas fa-search mr-1 color-secondary"></i> ID: ${document.getId()?string?replace('.', '')?replace(',', '')}
                            </p>
                            <p class="small-font color-secondary"><i
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
            <#if message.hasThinking()>
                <md-block class="raw mb-2" style="color: #c0c0c0 !important;">
                    Thinking:<br/>
                    ${message.getMessageOnlyThinking()}
                </md-block>
            </#if>
            <md-block class="raw mb-0">${message.getMessageWithoutThinking()}</md-block>
        </#if>
    </div>
</#list>

