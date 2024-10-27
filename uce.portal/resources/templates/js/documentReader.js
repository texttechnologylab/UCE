var currentFocusedPage = 0;

/**
 * Handles the expanding and depanding of the side bar
 */
$('body').on('click', '.side-bar .expander', function(){
    var expanded = $(this).data('expanded');

    if(expanded){
        $('.side-bar').css('width', '20px');
        $('.side-bar .side-bar-content').fadeOut(150);
        $(this).find('i').css({
            'transform': 'rotate(180deg)',
            'transition': '0.35s'
        });
    } else{
        $(this).find('i').css({
            'transform': 'rotate(0deg)',
            'transition': '0.35s'
        });
        $('.side-bar .side-bar-content').fadeIn(500);
        $('.side-bar').css('width', '500px');
    }
    $(this).data('expanded', !expanded);
})

/**
 * Handles the toggling of the focus function
 */
$('body').on('click', '.side-bar .toggle-focus-btn', function(){
    var $blurrer = $('.blurrer');
    var toggled = $blurrer.data('toggled');

    if(toggled){
        $(this).removeClass('toggled-btn');
        $blurrer.fadeOut(500);
    } else{
        $(this).addClass('toggled-btn');
        $blurrer.fadeIn(500);
    }

    $blurrer.data('toggled', !toggled);
})

/**
 * Keep track of the current page we are focusing right now
 */
$(window).scroll(function() {
    checkScroll();
});

function checkScroll(){
    var scrollPosition = $(this).scrollTop();
    var windowHeight = $(window).height();

    $('.document-content .page').each(function() {
        var offset = $(this).offset().top;
        var sectionHeight = $(this).outerHeight();

        if (scrollPosition >= offset && scrollPosition < offset + sectionHeight - windowHeight/2) {
            var pageNumber = $(this).data('id');
            if(pageNumber !== currentFocusedPage){
                currentFocusedPage = pageNumber;
                handleFocusedPageChanged();
            }
        }
    });
}

/**
 * This is like an event that gets called whenever the user scrolls into a new page view.
 */
function handleFocusedPageChanged(){
    $('.side-bar-content .current-page').html(currentFocusedPage);

    // We have to adjust the href of the metadata page
    const url = $('.open-metadata-url-page-btn').data('href');
    if(url === undefined) return;
    const splited = url.split('/');
    const newId = parseInt(splited[splited.length - 1]) + currentFocusedPage - 1;
    let newUrl = "";
    for(let i = 0; i < splited.length - 1; i++){
        newUrl += splited[i] + "/";
    }
    $('.side-bar-content .open-metadata-url-page-btn').attr('href', newUrl + newId.toString());
}

/**
 * Handle the changing of the font size
 */
$('body').on('change', '.font-size-range', function(){
    const fontSize = $(this).val();
    $('.document-content p').each(function(){
       $(this).css('font-size', fontSize + 'px');
    });
})

$('body').on('mouseenter', '.reader-container .annotation', function(){
})
$('body').on('mouseleave', '.reader-container .annotation', function(){
})

$(document).ready(function () {
    checkScroll();

    // we want to continously lazy load new pages
    lazyLoadPages();

    // Enable popovers
    activatePopovers();
})

/**
 * Handle the custom cursor
 */
document.addEventListener("mousemove", function(event) {
    var dot = document.getElementById("custom-cursor");
    dot.style.left = event.clientX -9 + "px";
    dot.style.top = event.clientY - 9 + "px";
});

/**
 * Handle the lazy loading of more pages
 */
async function lazyLoadPages(){
    const $readerContainer = $('.reader-container');
    const id = $readerContainer.data('id');
    const pagesCount = $readerContainer.data('pagescount');
    let counter = 0;
    for(let i = 10; i <= pagesCount; i += 10){
        const $loadedPagesCount = $('.site-container .loaded-pages-count');
        $loadedPagesCount.html(i);
        if(i >= pagesCount)
            $loadedPagesCount.html(i);
        else{
            await $.ajax({
                url: "/api/document/reader/pagesList?id=" + id + "&skip=" + i,
                type: "GET",
                success: function (response) {
                    // Render the new pages
                    $('.reader-container .document-content').append(response);
                    activatePopovers();
                    counter++;
                    //highlightPotentialSearchTokensOfPage(['Die'], $('.reader-container .document-content .page[data-id="' + counter + '"] .page-content'));
                },
                error: function (xhr, status, error) {
                    console.error(xhr.responseText);
                    $('.reader-container .document-content').append(xhr.responseText);
                }
            }).always(function(){
                $('.site-container .loaded-pages-count').html(i);
            });
        }
    }

    $('.site-container .pages-loader-popup').fadeOut(250);
}

