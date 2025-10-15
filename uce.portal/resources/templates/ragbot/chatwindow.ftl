<style>
    <#include "*/css/ragbot.css"/>
</style>

<a class="open-chat-window" data-trigger="hover" data-toggle="popover" data-placement="top"
   data-content="Mit Ragbot chatten" onclick="$('.chat-window-container').fadeIn(150)">
    <i class="m-0 fas fa-robot xlarge-font color-prime"></i>
</a>

<div class="chat-window-container display-none">
    <div class="cheader w-100 flexed align-items-center justify-content-between p-4">
        <h5 class="m-0 color-secondary"><i class="fas fa-robot mr-1"></i></h5>
        <h5 class="m-0 text">Chat</h5>
        <a class="btn" onclick="$('.chat-window-container').fadeOut(150)">
            <i class="fas fa-long-arrow-alt-right m-0 color-prime"></i>
        </a>
    </div>

    <#if !uceConfig.authIsEnabled() || uceUser?has_content>
        <div class="ccontent pt-3">
            <div class="alert alert-warning">
                ${languageResource.get("ragBotRessourcesWarning")}
            </div>
            <div class="flexed align-items-center">
                <button disabled class="btn btn-secondary rounded-0 mb-0 border-right-0">Model</button>
                <select class="rounded-0 form-control ragbot-model-select mr-1">
                    <#list uceConfig.getSettings().getRag().getModels() as ragModel>
                        <option data-id="${ragModel.getModel()}" data-streaming="${ragModel.isStreaming()?c}">${ragModel.getDisplayName()}</option>
                    </#list>
                </select>
                <button class="start-new-chat-btn text-center btn btn-primary rounded-0 w-auto no-text-wrap pl-3 pr-3 ml-1">
                    ${languageResource.get("startNewChat")} <i class="ml-1 fas fa-comments"></i>
                </button>
            </div>
        </div>
    <#else>
        <div class="p-3">
            <div class="alert alert-danger">
                <p class="mb-0 text-center"><i class="fas fa-user-lock mr-1"></i> ${languageResource.get("loginRequired")}</p>
            </div>
        </div>
    </#if>

    <#if !uceConfig.authIsEnabled() || uceUser?has_content>
        <div class="cfooter position-relative">
            <div class="w-100 p-3">
                <div class="flexed align-items-center">
                <textarea disabled class="form-control border-right-0 w-100 rounded-0 chat-user-input"
                          placeholder="Chat..."></textarea>
                    <button class="btn btn-primary send-message-btn">
                        <i class="fas fa-comment-alt"></i>
                    </button>
                </div>
            </div>
            <div class="cloader">
                <div class="w-100 flexed align-items-center justify-content-center h-100">
                    <p class="text">${languageResource.get("loading")} <i class="color-prime fas fa-comment-dots"></i>
                    </p>
                </div>
            </div>
        </div>
    </#if>
</div>

<script>
    <#include "*/js/ragbot.js" />
</script>