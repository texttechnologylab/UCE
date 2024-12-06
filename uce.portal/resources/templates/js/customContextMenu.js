// Trigger action when the contexmenu is about to be shown
$(document).bind("contextmenu", function (event) {

    // Check if we want the custom or normal context menu here
    var c = event.target.className.includes('custom-context-menu');
    if (!c) return;

    // Avoid the real one
    event.preventDefault();

    // Show contextmenu
    $(".custom-menu").finish().toggle(100).
        // In the right position (the mouse)
        css({
            top: event.pageY + "px",
            left: event.pageX + "px"
        });

    // Update cursor
    $('.dot').addClass('expanded-cursor');
    $('body').removeClass('no-cursor');

    // Highlight the correct menu entries
    if (event.target.className.includes('taxon')) {
        $('.custom-menu [data-action="open-more"]').show();
        $('.custom-menu [data-action="open-more"]').attr('href', event.target.href);
    } else {
        $('.custom-menu [data-action="open-more"]').hide();
    }

    // Store the correct data in the menu entries
    $('.custom-menu [data-action="highlight"]').data('target', event.target.title);
});


// If the document is clicked somewhere
$(document).bind("mousedown", function (e) {

    // If the clicked element is not the menu
    if (!$(e.target).parents(".custom-menu").length > 0) {

        // Hide it
        $(".custom-menu").hide(100);
        // Update cursor
        $('.dot').removeClass('expanded-cursor');
        $('body').addClass('no-cursor');
    }
});


// If the menu element is clicked
$(".custom-menu li").click(function () {
    // This is the triggered action name
    switch ($(this).attr("data-action")) {

        // A case for each action. Your actions here
        case "open-more":
            window.open($(this).attr('href'));
            break;
        case "search":
            break;
        case "highlight":
            const toHighlight = $(this).data('target');
            $('.document-content .annotation').each(function () {
               if($(this).attr('title').toLowerCase().includes(toHighlight.toLowerCase())){
                   $(this).addClass('highlighted');
               } else {
                   $(this).removeClass('highlighted');
               }
            });
            break;
    }

    // Hide it AFTER the action was triggered
    $(".custom-menu").hide(100);
    // Update cursor
    $('.dot').removeClass('expanded-cursor');
    $('body').addClass('no-cursor');
});