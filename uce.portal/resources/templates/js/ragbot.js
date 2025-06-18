/**
 * Start a new rag chat TODO: Outsource this into new prototype maybe
 */
$('body').on('click', '.chat-window-container .start-new-chat-btn', function () {
    const $select = $(this).prev('.ragbot-model-select');
    const model = $select.get(0).options[$select.get(0).selectedIndex].getAttribute('data-id');

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

/**
 * Handles the sendeing of a new message
 */
$('body').on('click', '.chat-window-container .send-message-btn', function(){
    $('.chat-window-container .cfooter .cloader').first().fadeIn(150);
    const $userInput = $('.chat-window-container .chat-user-input');
    const userMessage = $userInput.val();
    if(userMessage === '' || userMessage == null) return;

    const stateId = $('.chat-window-container .chat-state').data('id');

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
    }).always(function(){
        $('.chat-window-container .cfooter .cloader').first().fadeOut(150);
        $userInput.val('') // Clear the chat input
    });
})
