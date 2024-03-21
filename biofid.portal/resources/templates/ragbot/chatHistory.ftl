<div class="chat-state h-100 pb-5" data-id="${chatState.getChatId()}"></div>
<#list chatState.getMessages() as message>
    <div class="message" data-type="${message.getRole()?lower_case}">
        <span class="raw">${message.getMessage()}</span>
    </div>
</#list>
