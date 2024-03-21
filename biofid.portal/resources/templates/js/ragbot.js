/**
 * Start a new rag chat TODO: Outsource this into new prototype maybe
 */
$('body').on('click', '.chat-window-container .start-new-chat-btn', function () {
    $.ajax({
        url: "/api/rag/new",
        type: "GET",
        success: function (response) {
            $('.chat-window-container .ccontent').html(response);
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
    const userMessage = $('.chat-window-container .chat-user-input').val();
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
        $('.chat-window-container .loader-container').first().fadeOut(150);
    });
})
