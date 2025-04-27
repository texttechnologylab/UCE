let currentFocusedPage = 0;
let searchTokens = "";

/**
 * Handles the expanding and de-expanding of the side bar
 */
$('body').on('click', '.side-bar .expander', function () {
    let expanded = $(this).data('expanded');

    if (expanded) {
        $('.side-bar').css('width', '20px');
        $('.side-bar .side-bar-content').fadeOut(150);
        $(this).find('i').css({
            'transform': 'rotate(180deg)',
            'transition': '0.35s'
        });
    } else {
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
$('body').on('click', '.side-bar .toggle-focus-btn', function () {
    var $blurrer = $('.blurrer');
    var toggled = $blurrer.data('toggled');

    if (toggled) {
        $(this).removeClass('toggled-btn');
        $blurrer.fadeOut(500);
    } else {
        $(this).addClass('toggled-btn');
        $blurrer.fadeIn(500);
    }

    $blurrer.data('toggled', !toggled);
})

/**
 * Handles the toggling of the annotations highlighting.
 */
$('body').on('click', '.side-bar .toggle-highlighting-btn', function () {
    let highlight = $(this).data('highlighted');
    highlight = !highlight;

    $('.document-content .annotation, .multi-annotation').each(function(){
        if(highlight) $(this).removeClass('no-highlighting');
        else $(this).addClass('no-highlighting');
    })

    $(this).data('highlighted', highlight);
})

/**
 * Keep track of the current page we are focusing right now
 */
$(window).scroll(function () {
    checkScroll();
});

function checkScroll() {
    var scrollPosition = $(this).scrollTop();
    var windowHeight = $(window).height();

    $('.document-content .page').each(function () {
        var offset = $(this).offset().top;
        var sectionHeight = $(this).outerHeight();

        if (scrollPosition >= offset && scrollPosition < offset + sectionHeight - windowHeight / 2) {
            var pageNumber = $(this).data('id');
            if (pageNumber !== currentFocusedPage) {
                currentFocusedPage = pageNumber;
                handleFocusedPageChanged();
            }
        }
    });
}

/**
 * This is like an event that gets called whenever the user scrolls into a new page view.
 */
function handleFocusedPageChanged() {
    $('.side-bar-content .current-page').html(currentFocusedPage);

    // We have to adjust the href of the metadata page
    const url = $('.open-metadata-url-page-btn').data('href');
    if (url === undefined) return;
    const splited = url.split('/');
    const newId = parseInt(splited[splited.length - 1]) + currentFocusedPage - 1;
    let newUrl = "";
    for (let i = 0; i < splited.length - 1; i++) {
        newUrl += splited[i] + "/";
    }
    $('.side-bar-content .open-metadata-url-page-btn').attr('href', newUrl + newId.toString());
}

/**
 * Handle the changing of the font size
 */
$('body').on('change', '.font-size-range', function () {
    const fontSize = $(this).val();
    $('.document-content *').each(function () {
        $(this).css('font-size', fontSize + 'px');
    });
})

$('body').on('mouseenter', '.reader-container .annotation', function () {
})
$('body').on('mouseleave', '.reader-container .annotation', function () {
})

/**
 * Jumps to that location of the search occurrence.
 */
$('body').on('click', '.found-searchtokens-list .found-search-token', function () {
    const pageNumber = $(this).data('page');
    const $page = $('.document-content .page[data-id="' + pageNumber + '"] ');

    $([document.documentElement, document.body]).animate({
        scrollTop: $page.offset().top
    }, 1000);
});

$(document).ready(function () {
    checkScroll();

    // we want to continously lazy load new pages
    lazyLoadPages();

    // Enable popovers
    activatePopovers();

    // Load document topics
    loadDocumentTopics();

    let possibleSearchTokens = $('.reader-container').data('searchtokens');
    if (possibleSearchTokens === undefined || possibleSearchTokens === '') return;
    searchTokens = possibleSearchTokens.split('[TOKEN]');

    // Highlight potential search terms for the first 10 pages
    for (let i = 1; i < 11; i++) searchPotentialSearchTokensInPage(i);

});

function loadDocumentTopics() {
    const documentId = $('.document-topics-list').data('document-id');

    if (!documentId) {
        console.error("No document ID found for topics");
        $('.topics-loading').hide();
        $('.document-topics-list').html('<p>No document ID available.</p>');
        return;
    }

    $.ajax({
        url: '/api/document/topics',
        method: 'GET',
        data: {
            documentId: documentId,
            limit: 10
        },
        success: function(data) {
            $('.topics-loading').hide();

            if (data && data.length > 0) {
                let html = '';

                data.sort((a, b) => parseFloat(b.probability) - parseFloat(a.probability));

                const maxProb = parseFloat(data[0].probability);
                const minProb = parseFloat(data[data.length - 1].probability);
                const probRange = maxProb - minProb;

                // Generate HTML for each topic
                data.forEach(function(topic) {
                    const probability = parseFloat(topic.probability);

                    // Normalize probability to 0-1 range
                    const normalizedProb = probRange > 0 ?
                        (probability - minProb) / probRange : 1;
                    const hue = Math.round(normalizedProb * 120);
                    const bgColor = "hsla("+hue+", 70%, 50%, 0.3)";

                    html += '<div class="topic-item">' +
                           '<div class="topic-tag" data-topic="'+topic.label+'" data-probability="'+probability+'" style="background-color: '+bgColor+'">' +
                           '<span>'+topic.label+'</span>' +
                           '</div>' +
                           '</div>';
                });

                $('.document-topics-list').html(html);

                attachTopicClickHandlers();
            } else {
                $('.document-topics-list').html('<p>No topics available for this document.</p>');
            }
        },
        error: function(xhr, status, error) {
            console.error("Error loading topics:", status, error);
            console.error("Response:", xhr.responseText);

            $('.topics-loading').hide();

            $('.document-topics-list').html('<p>Failed to load document topics.</p>');
        },
        complete: function() {
            $('.topics-loading').hide();
        }
    });
}

function attachTopicClickHandlers() {
    $('.topic-tag').off('click');

    $('.topic-tag').on('click', function() {
        const topic = $(this).data('topic');
        const wasActive = $(this).hasClass('active-topic');
        $('.topic-tag').removeClass('active-topic');

        if (wasActive) {
            clearTopicColoring();

            hideTopicNavButtons();
            currentSelectedTopic = null;
        } else {
            $(this).addClass('active-topic');

            colorUnifiedTopics(topic);
            setTimeout(function() {
                scrollToFirstMatchingTopic(topic);
            }, 100);
        }
    });

    // Attach click handlers to the navigation buttons
    $('.next-topic-button').off('click').on('click', function() {
        if ($(this).hasClass('disabled') || !currentSelectedTopic) return;

        currentTopicIndex++;
        updateTopicNavButtonStates();
        scrollToTopicElement($(matchingTopics[currentTopicIndex]));
    });

    $('.prev-topic-button').off('click').on('click', function() {
        if ($(this).hasClass('disabled') || !currentSelectedTopic) return;
        currentTopicIndex--;
        updateTopicNavButtonStates();
        scrollToTopicElement($(matchingTopics[currentTopicIndex]));
    });
}



/**
 * Handle the custom cursor
 * I removed that custom cursor for now.
 */

/*document.addEventListener("mousemove", function (event) {
    var dot = document.getElementById("custom-cursor");
    dot.style.left = event.clientX - 9 + "px";
    dot.style.top = event.clientY - 9 + "px";
});*/

/**
 * Handle the lazy loading of more pages
 */
async function lazyLoadPages() {
    const $readerContainer = $('.reader-container');
    const id = $readerContainer.data('id');
    const pagesCount = $readerContainer.data('pagescount');

    for (let i = 10; i <= pagesCount; i += 10) {
        const $loadedPagesCount = $('.site-container .loaded-pages-count');
        $loadedPagesCount.html(i);

        if (i >= pagesCount) {
            $loadedPagesCount.html(i);
        } else {
            await $.ajax({
                url: "/api/document/reader/pagesList?id=" + id + "&skip=" + i,
                type: "GET",
                success: function (response) {
                    // Render the new pages
                    $('.reader-container .document-content').append(response);
                    activatePopovers();
                    for (let k = i + 1; k < Math.max(i + 10, pagesCount); k++) searchPotentialSearchTokensInPage(k);

                    const $activeTopic = $('.topic-tag.active-topic');
                    if ($activeTopic.length > 0) {
                        colorUnifiedTopics($activeTopic.data('topic'));
                    }
                },
                error: function (xhr, status, error) {
                    console.error(xhr.responseText);
                    $('.reader-container .document-content').append(xhr.responseText);
                }
            }).always(function () {
                $('.site-container .loaded-pages-count').html(i);
            });
        }
    }

    $('.site-container .pages-loader-popup').fadeOut(250);
    $('.search-tokens-box .fa-spinner').fadeOut(250);
}

/**
 * Within a page container, look for possible search tokens.
 */
function searchPotentialSearchTokensInPage(page) {
    let highlightedAnnos = [];
    const $page = $('.document-content .page[data-id="' + page + '"] ');

    $page.find('.annotation').each(function () {
        for (let i = 0; i < searchTokens.length; i++) {
            const toHighlight = searchTokens[i];
            let $el = $(this);

            if ($el.attr('title').toLowerCase().includes(toHighlight.toLowerCase())) {

                // If this annotation is within a multi-annotation, we need to highlight the multi-anno.
                if($el.parent().hasClass('multi-annotation-popup')){
                    $el = $el.closest('.multi-annotation');
                    if(highlightedAnnos.includes($el.attr('title'))) continue;
                    else highlightedAnnos.push($el.attr('title'))
                }

                $el.addClass('highlighted');

                // We cant use \$\{ the syntax as freemarker owns this syntax and hence, throws an error.
                let html = `
                    <div data-page="[page]" class="found-search-token flexed mt-1 align-items-center justify-content-between">
                        <label class="font-italic mb-0 text small-font no-pointer-events">"[value]"</label>
                        <label class="small-font mb-0">
                            <i class="color-prime fas fa-file-alt ml-2 mr-1"></i>
                            <span class="text mb-0">[page]</span>
                        </label>
                    </div>
                `.replace('[value]', toHighlight).replace('[page]', page).replace('[page]', page);

                $('.found-searchtokens-list').append(html);
            }
        }
    });
}

function colorUnifiedTopics(selectedTopic) {
    clearTopicColoring();

    if (!selectedTopic) {
        return;
    }

    const $selectedTopicTag = $('.topic-tag').filter(function() {
        return $(this).data('topic') === selectedTopic;
    });

    if ($selectedTopicTag.length === 0) {
        return;
    }

    const color = $selectedTopicTag.css('background-color');

    let finalColor = color;

    if (color.startsWith('rgb(') && !color.startsWith('rgba(')) {
        finalColor = color.replace('rgb(', 'rgba(').replace(')', ', 0.3)');
    }

    $('.colorable-topic').each(function() {
        const topicValue = $(this).data('topic-value');

        if (topicValue === selectedTopic) {
            $(this).css({
                'background-color': finalColor,
                'border-radius': '3px',
                'padding': '0 2px'
            });
        }
    });
}

function clearTopicColoring() {
    $('.colorable-topic').css({
        'background-color': '',
        'border-radius': '',
        'padding': ''
    });
}


function isElementInViewport($element) {
    if ($element.length === 0) return false;

    const elementTop = $element.offset().top;
    const elementBottom = elementTop + $element.outerHeight();
    const viewportTop = $(window).scrollTop();
    const viewportBottom = viewportTop + $(window).height();

    return (elementTop >= viewportTop && elementTop <= viewportBottom) ||
           (elementBottom >= viewportTop && elementBottom <= viewportBottom) ||
           (elementTop <= viewportTop && elementBottom >= viewportBottom);
}

let currentSelectedTopic = null;

let currentTopicIndex = -1;

let matchingTopics = [];


function scrollToFirstMatchingTopic(topicValue) {
    currentSelectedTopic = topicValue;

    matchingTopics = $('.colorable-topic').filter(function() {
        return $(this).data('topic-value') === topicValue;
    }).toArray();

    if (matchingTopics.length === 0) {
        console.log('No matching unified topics found for: ' + topicValue);
        hideTopicNavButtons();
        return false;
    }

    currentTopicIndex = 0;

    showTopicNavButtons();
    updateTopicNavButtonStates();

    const $firstMatchingTopic = $(matchingTopics[currentTopicIndex]);

    if (isElementInViewport($firstMatchingTopic)) {
        console.log('First matching topic is already visible in viewport');
        return false;
    }
    scrollToTopicElement($firstMatchingTopic);

    return true;
}

function scrollToTopicElement($topicElement) {

    const windowHeight = $(window).height();
    const elementHeight = $topicElement.outerHeight();
    const scrollToPosition = $topicElement.offset().top - (windowHeight / 2) + (elementHeight / 2);

    $('html, body').animate({
        scrollTop: Math.max(0, scrollToPosition)
    }, 800, function() {
        $topicElement.css('transition', 'box-shadow 0.3s ease-in-out');
        $topicElement.css('box-shadow', '0 0 10px 5px rgba(255, 255, 0, 0.5)');

        setTimeout(function() {
            $topicElement.css('box-shadow', '');
            setTimeout(function() {
                $topicElement.css('transition', '');
            }, 300);
        }, 1500);
    });
}

function showTopicNavButtons() {
    $('.topic-navigation-buttons').addClass('visible');
}

function hideTopicNavButtons() {
    $('.topic-navigation-buttons').removeClass('visible');
}

function updateTopicNavButtonStates() {
    if (currentTopicIndex <= 0) {
        $('.prev-topic-button').addClass('disabled');
    } else {
        $('.prev-topic-button').removeClass('disabled');
    }
    if (currentTopicIndex >= matchingTopics.length - 1) {
        $('.next-topic-button').addClass('disabled');
    } else {
        $('.next-topic-button').removeClass('disabled');
    }
}