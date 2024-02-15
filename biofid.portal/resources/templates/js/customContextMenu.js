
// Trigger action when the contexmenu is about to be shown
$(document).bind("contextmenu", function (event) {

    // Check if we want the custom or normal context menu here
    var c = event.target.className.includes('custom-context-menu');
    if(!c) return;

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
$(".custom-menu li").click(function(){
    // This is the triggered action name
    switch($(this).attr("data-action")) {

        // A case for each action. Your actions here
        case "first": alert("first"); break;
        case "second": alert("second"); break;
        case "third": alert("third"); break;
    }

    // Hide it AFTER the action was triggered
    $(".custom-menu").hide(100);
    // Update cursor
    $('.dot').removeClass('expanded-cursor');
    $('body').addClass('no-cursor');
});