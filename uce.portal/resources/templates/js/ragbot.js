// NOTE there is only one chat window...
let ragPollingInterval;
let ragStream;

/**
 * Start a new rag chat TODO: Outsource this into new prototype maybe
 */
$('body').on('click', '.chat-window-container .start-new-chat-btn', function () {
    const $select = $(this).prev('.ragbot-model-select');
    const model = $select.get(0).options[$select.get(0).selectedIndex].getAttribute('data-id');
    // NOTE Model streaming is set by the model for now, but we will provide it via JavaScript so in the future we can easily add a per-chat option for the user here
    ragStream = $select.get(0).options[$select.get(0).selectedIndex].getAttribute('data-streaming');

    $.ajax({
        url: "/api/rag/new?model=" + encodeURIComponent(model),
        type: "GET",
        success: function (response) {
            $('.chat-window-container .ccontent').html(response);
            $('.chat-window-container .cfooter .chat-user-input').attr('disabled', false);
            activatePopovers();
        },
        error: function (xhr, status, error) {
            console.error(xhr.responseText);
            $('.chat-window-container .ccontent').html(xhr.responseText);
        }
    }).always(function () {
        $('.chat-window-container .loader-container').first().fadeOut(150);
    });
})

function startPolling(chatId) {
    console.log("Starting RAG polling for chatId:", chatId)
    if (ragPollingInterval) {
        console.log("Warning: Stopping previous RAG polling interval")
        clearInterval(ragPollingInterval)
    }

    ragPollingInterval = setInterval(function () {
        $.ajax({
            url: "/api/rag/messages?chatId=" + encodeURIComponent(chatId),
            type: "GET",
            success: function (response) {
                // replace the full content
                // TODO we should make sure that the messages are received in order in case there is some delay
                $('.chat-window-container .ccontent').html(response["html"])
                activatePopovers()

                $('.chat-window-container .ccontent').animate({
                    scrollTop: $('.chat-window-container .ccontent')[0].scrollHeight
                }, 500);

                if (response["done"]) {
                    console.log("RAG polling done for chatId:", chatId)
                    clearInterval(ragPollingInterval)
                    $('.chat-window-container .cfooter .cloader').first().fadeOut(150);
                }
            },
            error: function (xhr, status, error) {
                // on error we stop polling
                // TODO maybe try again after some time?
                console.error("RAG polling error:", xhr.responseText)
                $('.chat-window-container .ccontent').html(xhr.responseText)
                clearInterval(ragPollingInterval)
            }
        });
    }, 500); // every 1/2 second
}

/**
 * Handles the sendeing of a new message
 */
$('body').on('click', '.chat-window-container .send-message-btn', function(){
    $('.chat-window-container .cfooter .cloader').first().fadeIn(150);
    const $userInput = $('.chat-window-container .chat-user-input');
    const userMessage = $userInput.val();
    if(userMessage === '' || userMessage == null) return;

    const stateId = $('.chat-window-container .chat-state').data('id');

    if (ragStream) {
        // stream the LLM results, this will send the message and then polls for new answers
        $.ajax({
            url: "/api/rag/postUserMessage",
            type: "POST",
            data: JSON.stringify({
                userMessage: userMessage,
                stateId: stateId,
                stream: true
            }),
            contentType: "application/json",
            success: function (response) {
                console.log("received chat id for updating messages", response)
                const chat_id = response["chat_id"]
                startPolling(chat_id)
            },
            error: function (xhr, status, error) {
                console.error(xhr.responseText);
                $('.chat-window-container .ccontent').html(xhr.responseText);
                $('.chat-window-container .cfooter .cloader').first().fadeOut(150);
            }
        }).always(function () {
            $userInput.val('') // Clear the chat input
        });
    }
    else {
        // dont stream results, this will wait for the answer and then display it
        $.ajax({
            url: "/api/rag/postUserMessage",
            type: "POST",
            data: JSON.stringify({
                userMessage: userMessage,
                stateId: stateId
            }),
            contentType: "application/json",
            success: function (response) {
                $('.chat-window-container .ccontent').html(response);
                activatePopovers();
            },
            error: function (xhr, status, error) {
                console.error(xhr.responseText);
                $('.chat-window-container .ccontent').html(xhr.responseText);
            }
        }).always(function () {
            $('.chat-window-container .cfooter .cloader').first().fadeOut(150);
            $userInput.val('') // Clear the chat input
        });
    }
})
