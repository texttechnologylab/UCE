<style>
    <#include "*/css/ragbot.css"/>
</style>

<a class="open-chat-window" data-trigger="hover" data-toggle="popover" data-placement="top"
   data-content="Mit Ragbot chatten" onclick="$('.chat-window-container').fadeIn(150)">
    <i class="m-0 fas fa-robot xlarge-font color-prime"></i>
</a>

<div class="chat-window-container display-none">
    <div class="cheader w-100 flexed align-items-center justify-content-between p-3">
        <h5 class="m-0 color-secondary"><i class="fas fa-robot mr-1"></i></h5>
        <h5 class="m-0 text">Chat</h5>
        <a class="btn" onclick="$('.chat-window-container').fadeOut(150)">
            <i class="fas fa-long-arrow-alt-right m-0 color-prime"></i>
        </a>
    </div>

    <div class="ccontent">
        <button class="start-new-chat-btn text-center btn btn-primary rounded-0 w-100">Neuen Chat starten</button>
    </div>

    <div class="cfooter p-3">
        <div class="w-100">
            <div class="flexed align-items-center">
                <textarea class="form-control border-right-0 w-100 rounded-0 chat-user-input" placeholder="Chat..."></textarea>
                <button class="btn btn-primary send-message-btn">
                    <i class="fas fa-comment-alt"></i>
                </button>
            </div>
        </div>
    </div>
</div>

<script>
    <#include "*/js/ragbot.js" />
</script>