let currentFocusedPage = 0;
let searchTokens = "";
let topicColorMap = {};
let currentSelectedTopic = null;
let currentTopicIndex = -1;
let matchingTopics = [];

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

    $('.document-content .annotation, .multi-annotation').each(function () {
        if (highlight) $(this).removeClass('no-highlighting');
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

    const hasTopics = $('.colorable-topic').length > 0;
    if (hasTopics) {
        $('.scrollbar-minimap').show();
        initScrollbarMinimap();
    } else {
        $('.scrollbar-minimap').hide();
    }

    let possibleSearchTokens = $('.reader-container').data('searchtokens');
    if (possibleSearchTokens === undefined || possibleSearchTokens === '') return;
    searchTokens = possibleSearchTokens.split('[TOKEN]');

    // Highlight potential search terms for the first 10 pages
    for (let i = 1; i < 11; i++) searchPotentialSearchTokensInPage(i);

});

function loadDocumentTopics() {
    $('.topics-loading').hide();

    // Extract topics from colorable-topic spans in the document
    const topicFrequency = {};
    $('.colorable-topic').each(function () {
        const topic = $(this).data('topic-value');
        if (topic) {
            topicFrequency[topic] = (topicFrequency[topic] || 0) + 1;
        }
    });

    // Convert to array and sort by frequency
    const topicArray = Object.keys(topicFrequency).map(topic => ({
        label: topic,
        frequency: topicFrequency[topic]
    }));

    topicArray.sort((a, b) => b.frequency - a.frequency);

    // Find max and min for normalization across ALL topics
    const maxFreq = topicArray.length > 0 ? topicArray[0].frequency : 1;
    const minFreq = topicArray.length > 0 ? topicArray[topicArray.length - 1].frequency : 0;
    const freqRange = maxFreq - minFreq;

    // Create color mapping for ALL topics
    topicArray.forEach(function (topic) {
        const normalizedFreq = freqRange > 0 ?
            (topic.frequency - minFreq) / freqRange : 1;
        topicColorMap[topic.label] = window.graphVizHandler.getColorForWeight(normalizedFreq);
    });

    // Take top 10 topics for display
    const topTopics = topicArray.slice(0, 10);

    if (topTopics.length > 0) {
        let html = '';

        // Generate HTML for each top topic
        topTopics.forEach(function (topic) {
            html += '<div class="topic-item">' +
                '<div class="topic-tag" data-topic="' + topic.label + '" data-frequency="' + topic.frequency + '" style="background-color: ' + topicColorMap[topic.label] + '">' +
                '<span>' + topic.label + '</span>' +
                '</div>' +
                '</div>';
        });

        $('.document-topics-list').html(html);
        attachTopicClickHandlers();

        if (typeof updateMinimapMarkers === 'function') {
            setTimeout(updateMinimapMarkers, 500);
        }
    } else {
        $('.document-topics-list').html('<p>No topics found in this document.</p>');
        // Hide the minimap since there are no topics
        $('.scrollbar-minimap').hide();
    }
}

function attachTopicClickHandlers() {
    $('.topic-tag').off('click');

    $('.topic-tag').on('click', function () {
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
            setTimeout(function () {
                scrollToFirstMatchingTopic(topic);
            }, 100);
        }
    });

    // Attach click handlers to the navigation buttons
    $('.next-topic-button').off('click').on('click', function () {
        if ($(this).hasClass('disabled') || !currentSelectedTopic) return;

        currentTopicIndex++;
        updateTopicNavButtonStates();
        scrollToTopicElement($(matchingTopics[currentTopicIndex]));
    });

    $('.prev-topic-button').off('click').on('click', function () {
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
                    updateMinimapMarkers();
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
                if ($el.parent().hasClass('multi-annotation-popup')) {
                    $el = $el.closest('.multi-annotation');
                    if (highlightedAnnos.includes($el.attr('title'))) continue;
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
    let color;

    if (!selectedTopic) {
        return;
    }

    const $selectedTopicTag = $('.topic-tag').filter(function () {
        return $(this).data('topic') === selectedTopic;
    });

    if ($selectedTopicTag.length === 0) {
        color = topicColorMap[selectedTopic];
    }
    else{
        color = $selectedTopicTag.css('background-color');
    }

    let finalColor = color;

    if (color.startsWith('rgb(') && !color.startsWith('rgba(')) {
        finalColor = color.replace('rgb(', 'rgba(').replace(')', ', 0.3)');
    }

    $('.colorable-topic').each(function () {
        const topicValue = $(this).data('topic-value');

        if (topicValue === selectedTopic) {
            $(this).css({
                'background-color': finalColor,
                'border-radius': '3px',
                'padding': '0 2px'
            });
        }
    });
    updateTopicMarkersOnMinimap();
}

function clearTopicColoring() {
    $('.colorable-topic').css({
        'background-color': '',
        'border-radius': '',
        'padding': ''
    });

    $('.minimap-marker.topic-marker').remove();
    $('.minimap-marker.all-topics-marker').show();
}


function scrollToFirstMatchingTopic(topicValue) {
    currentSelectedTopic = topicValue;

    matchingTopics = $('.colorable-topic').filter(function () {
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
    }, 800, function () {
        $topicElement.addClass('animated-topic-scroll');

        setTimeout(function () {
            $topicElement.removeClass('animated-topic-scroll');
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

function initScrollbarMinimap() {
    setTimeout(updateMinimapMarkers, 500);

    $(window).on('scroll', function() {
        updateMinimapScroll();
    });

    $(window).on('resize', function() {
        updateMinimapMarkers();
        updateMinimapScroll();
    });

    $('.scrollbar-minimap').on('click', function(e) {
        const clickPosition = (e.pageY - $(this).offset().top) / $(this).height();
        const dimensions = getMinimapDimensions();
        const documentPosition = minimapToDocumentPosition(clickPosition * dimensions.minimapHeight, dimensions);

        const scrollTo = documentPosition - (dimensions.windowHeight / 2);
        $('html, body').animate({
            scrollTop: Math.max(0, scrollTo)
        }, 300);
    });

    $(document).on('mouseenter', '.minimap-marker', function(e) {
        const $marker = $(this);
        const $preview = $('.minimap-preview');
        const $previewContent = $('.preview-content');
        const dimensions = getMinimapDimensions();

        let previewText = '';
        let previewTitle = '';

        const topicValue = $marker.data('topic');
        if (topicValue && topicValue !== 'multiple') {
            previewTitle = '<strong>Topic: ' + topicValue + '</strong>';

            const markerTop = parseFloat($marker.css('top'));
            const approximateDocumentPosition = minimapToDocumentPosition(markerTop, dimensions);

            const $topicElements = $('.colorable-topic').filter(function() {
                return $(this).data('topic-value') === topicValue;
            });

            let closestElement = null;
            let minDistance = Number.MAX_VALUE;

            $topicElements.each(function() {
                const $element = $(this);
                const elementOffset = $element.offset();

                if (elementOffset) {
                    const distance = Math.abs(elementOffset.top - approximateDocumentPosition);
                    if (distance < minDistance) {
                        minDistance = distance;
                        closestElement = $element;
                    }
                }
            });

            if (closestElement) {
                const $paragraph = closestElement.closest('p');
                if ($paragraph.length) {
                    let contextText = $paragraph.text().trim();
                    if (contextText.length > 200) {
                        contextText = contextText.substring(0, 200) + '...';
                    }
                    previewText = contextText;
                } else {
                    previewText = closestElement.text().trim();
                }

                const coveredText = closestElement.data('wcovered');
                if (coveredText) {
                    previewText =  coveredText;
                }
            }
            if (previewTitle) {
                $previewContent.html(previewTitle + '<br><br>' + previewText);
            } else {
                $previewContent.text(previewText);
            }

            const previewHeight = $preview.outerHeight();
            let previewTop = markerTop - (previewHeight / 2);
            if (previewTop < 0) previewTop = 0;
            if (previewTop + previewHeight > dimensions.minimapHeight) {
                previewTop = dimensions.minimapHeight - previewHeight;
            }

            $preview.css('top', previewTop + 'px').show();
        } else {
            // Get the text content from the corresponding document section
            // const elementId = $marker.data('element-id');
            // const $element = $('#' + elementId);
            //
            // if ($element.length) {
            //     // Get a snippet of text from the element
            //     previewText = $element.text().trim();
            //     // Limit preview text length
            //     if (previewText.length > 200) {
            //         previewText = previewText.substring(0, 200) + '...';
            //     }
            // }
        }
    });

    $(document).on('mouseleave', '.minimap-marker', function() {
        $('.minimap-preview').hide();
    });

    $('.scrollbar-minimap').on('mouseleave', function() {
        $('.minimap-preview').hide();
    });
}

function updateMinimapMarkers() {
    const $minimap = $('.minimap-markers');
    const dimensions = getMinimapDimensions();
    
    $minimap.empty();

    $('.document-content .page').each(function(index) {
        const $page = $(this);

        if (!$page.attr('id')) {
            $page.attr('id', 'page-' + (index + 1));
        }

        const pageId = $page.attr('id');
        const pageTop = $page.offset().top;
        const pageHeight = $page.outerHeight();

        const markerTop = documentToMinimapPosition(pageTop, dimensions);
        const markerHeight = documentToMinimapPosition(pageHeight, dimensions);

        const $marker = createMinimapMarker({
            top: markerTop,
            height: markerHeight,
            color: '#ccc',
            elementId: pageId
        });

        $minimap.append($marker);
    });

    addAllTopicMarkersToMinimap();
    updateTopicMarkersOnMinimap();
}

function addAllTopicMarkersToMinimap() {
    const $minimap = $('.minimap-markers');
    const dimensions = getMinimapDimensions();
    const topicPositions = {};

    $('.colorable-topic').each(function() {
        const $topic = $(this);
        const topicValue = $topic.data('topic-value');

        if (topicValue && topicColorMap[topicValue]) {
            const topicTop = $topic.offset().top;
            const topicHeight = $topic.outerHeight();

            const markerTop = Math.floor(documentToMinimapPosition(topicTop, dimensions));
            const markerHeight = Math.max(2, documentToMinimapPosition(topicHeight, dimensions));

            if (!topicPositions[markerTop]) {
                topicPositions[markerTop] = {
                    topics: {},
                    height: markerHeight
                };
            }

            topicPositions[markerTop].topics[topicValue] = topicColorMap[topicValue];
            topicPositions[markerTop].height = Math.max(topicPositions[markerTop].height, markerHeight);
        }
    });

    Object.keys(topicPositions).forEach(function(position) {
        const pos = parseInt(position);
        const topicData = topicPositions[pos];
        const topicValues = Object.keys(topicData.topics);

        if (topicValues.length > 0) {
            const topicValue = topicValues[0];
            const color = topicData.topics[topicValue];

            const $marker = createMinimapMarker({
                top: pos,
                height: topicData.height,
                color: color,
                topic: topicValues.length === 1 ? topicValue : 'multiple',
                className: 'all-topics-marker'
            });

            $minimap.append($marker);
        }
    });
}

function updateTopicMarkersOnMinimap(selectedTopic=null) {
    const $minimap = $('.minimap-markers');
    const dimensions = getMinimapDimensions();

    const $activeTopic = $('.topic-tag.active-topic');
    if ($activeTopic.length === 0 && selectedTopic === null) return;

    $('.minimap-marker.all-topics-marker').hide();

    const activeTopic = selectedTopic ? selectedTopic : $activeTopic.data('topic');
    const topicColor = topicColorMap[activeTopic];

    $('.colorable-topic').each(function() {
        const $topic = $(this);
        const topicValue = $topic.data('topic-value');

        if (topicValue === activeTopic) {
            const topicTop = $topic.offset().top;
            const topicHeight = $topic.outerHeight();

            const markerTop = documentToMinimapPosition(topicTop, dimensions);
            const markerHeight = Math.max(3, documentToMinimapPosition(topicHeight, dimensions));

            const $marker = createMinimapMarker({
                top: markerTop,
                height: markerHeight,
                color: topicColor,
                className: 'topic-marker'
            });

            $minimap.append($marker);
        }
    });
}

// Helper functions for minimap calculations
function getMinimapDimensions() {
    return {
        documentHeight: $(document).height(),
        windowHeight: $(window).height(),
        minimapHeight: $('.scrollbar-minimap').height()
    };
}

function documentToMinimapPosition(documentPos, dimensions) {
    const { documentHeight, minimapHeight } = dimensions || getMinimapDimensions();
    return (documentPos / documentHeight) * minimapHeight;
}

function minimapToDocumentPosition(minimapPos, dimensions) {
    const { documentHeight, minimapHeight } = dimensions || getMinimapDimensions();
    return (minimapPos / minimapHeight) * documentHeight;
}

function createMinimapMarker(options) {
    const { top, height, color, elementId, topic, className } = options;
    
    const $marker = $('<div></div>')
        .addClass('minimap-marker')
        .addClass(className || '')
        .css({
            'top': top + 'px',
            'height': Math.max(2, height) + 'px',
            'background-color': color || '#ccc'
        });
    
    if (elementId) $marker.attr('data-element-id', elementId);
    if (topic) $marker.attr('data-topic', topic);
    
    return $marker;
}

function updateMinimapScroll() {
    const windowHeight = $(window).height();
    const documentHeight = $(document).height();
    const scrollTop = $(window).scrollTop();
    const minimapHeight = $('.scrollbar-minimap').height();

    const visibleStart = scrollTop / documentHeight;
    const visibleHeight = windowHeight / documentHeight;

    let $visibleArea = $('.minimap-visible-area');
    if ($visibleArea.length === 0) {
        $visibleArea = $('<div class="minimap-visible-area"></div>')
            .css({
                'position': 'absolute',
                'left': '0',
                'width': '100%',
                'background-color': 'rgba(0, 0, 0, 0.1)',
                'border-top': '1px solid rgba(0, 0, 0, 0.2)',
                'border-bottom': '1px solid rgba(0, 0, 0, 0.2)',
                'z-index': 1
            });
        $('.scrollbar-minimap').append($visibleArea);
    }

    $visibleArea.css({
        'top': (visibleStart * minimapHeight) + 'px',
        'height': (visibleHeight * minimapHeight) + 'px'
    });
}

function updateFloatingUIPositions() {
    const sidebar = document.querySelector('.side-bar');
    const minimap = document.querySelector('.scrollbar-minimap');
    const navButtons = document.querySelector('.topic-navigation-buttons');

    if (!sidebar || !minimap) return;

    const sidebarRect = sidebar.getBoundingClientRect();

    const minimapRight = window.innerWidth - sidebarRect.left + 10;
    minimap.style.right = minimapRight + `px`;

    if (navButtons) {
        navButtons.style.right = minimapRight + 40 + `px`;
    }
}

window.addEventListener('resize', updateFloatingUIPositions);
window.addEventListener('DOMContentLoaded', updateFloatingUIPositions);

document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.addEventListener('click', async () => {
        const targetId = btn.getAttribute('data-tab');
        const sideBar = document.querySelector('.side-bar');

        document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');

        document.querySelectorAll('.tab-pane').forEach(pane => {
            pane.classList.toggle('active', pane.id === targetId);
        });

        hideTopicNavButtons();
        clearTopicColoring();

        if (targetId !== 'navigator-tab') {
            $('.scrollbar-minimap').hide();
            sideBar.classList.add('visualization-expanded');
        } else {
            setTimeout(updateFloatingUIPositions,500) ;
            currentSelectedTopic = null;
            sideBar.classList.remove('visualization-expanded');
            $('.scrollbar-minimap').show();
        }
        if (targetId === 'visualization-tab') {
            setTimeout(() => renderTemporalExplorer('vp-1'), 500);
            $('.viz-nav-btn').removeClass('active');
            $('.viz-nav-btn').first().addClass('active');

            $('.viz-panel').removeClass('active');
            $('.viz-panel').first().addClass('active');
        }
    });

});

$(document).on('click', '.viz-nav-btn', function () {
    const target = $(this).data('target');
    clearTopicColoring();
    hideTopicNavButtons();
    $('.scrollbar-minimap').hide();

    // Update active button
    $('.viz-nav-btn').removeClass('active');
    $(this).addClass('active');

    // Update visible panel
    $('.viz-panel').removeClass('active');
    $(target).addClass('active');

    if (target === '#viz-panel-1') {
        setTimeout(() => renderTemporalExplorer('vp-1'), 500);
    }
    if (target === '#viz-panel-2') {

    }
    if (target === '#viz-panel-3') {

    }
    if (target === '#viz-panel-4') {

    }
    if (target === '#viz-panel-5') {
        setTimeout(() => renderSentenceTopicSankey('vp-5'), 500);

    }
});

function renderSentenceTopicSankey(containerId) {
    const container = document.getElementById(containerId);
    if (!container || container.classList.contains('rendered')) return;

    let sentenceTopicData = [];
    const topicFrequency = {};

    $('.colorable-topic').each(function () {
        const topicValue = $(this).data('topic-value');
        const utId = parseInt(this.id.replace('utopic-UT-', ''));

        if (!isNaN(utId) && topicValue) {
            sentenceTopicData.push({
                from: utId.toString(),
                to: topicValue.toString(),
                weight: 1
            });
            topicFrequency[topicValue] = (topicFrequency[topicValue] || 0) + 1;
        }
    });

    if (!sentenceTopicData.length) return;
    container.classList.add('rendered');

    const nodeSet = new Set();
    sentenceTopicData.forEach(d => {
        nodeSet.add(d.from);
        nodeSet.add(d.to);
    });

    const nodes = Array.from(nodeSet).map(id => {
        const isTopic = topicFrequency.hasOwnProperty(id);
        return {
            name: id,
            itemStyle: {
                color: isTopic ? (topicColorMap[id] || '#888') : '#1f77b4'
            }
        };
    });

    const links = sentenceTopicData.map(d => ({
        source: d.from,
        target: d.to,
        value: d.weight
    }));
    window.graphVizHandler.createSankeyChart(containerId, 'Sentence-Topic Sankey Diagram', links, nodes, function (params) {
        if (params.dataType === 'node') {
            const name = params.name;
            clearTopicColoring();
            hideTopicNavButtons();
            if (typeof name === 'string' && topicColorMap.hasOwnProperty(name)) {
                colorUnifiedTopics(name);
                scrollToFirstMatchingTopic(name);
                updateTopicMarkersOnMinimap(name);
                updateFloatingUIPositions();
                $('.scrollbar-minimap').show();
            } else {
                $('.scrollbar-minimap').hide();
                hideTopicNavButtons();
                $('.colorable-topic').each(function () {
                    const topicValue = $(this).data('topic-value');
                    const utId = this.id.replace('utopic-UT-', '');
                    if (utId === name) {
                        $(this).css({
                            'background-color': topicColorMap[topicValue],
                            'border-radius': '3px',
                            'padding': '0 2px'
                        });
                        this.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    }
                });
            }
        } else if (params.dataType === 'edge') {
            clearTopicColoring();
            hideTopicNavButtons();
            $('.scrollbar-minimap').hide();
            console.log('Edge clicked from', params.data.source, 'to', params.data.target);
        }
    });
}

// TODO: MOVE CODE TO GRAPHVIZ
function renderTemporalExplorer(containerId) {
    const rawValue = document.getElementById('vp-1')?.getAttribute('data-document-id');
    const docId = rawValue ? parseInt(rawValue, 10) : null;
    if (!docId) return console.error("Missing or invalid documentId");

    const taxonReq = $.get('/api/document/page/taxon', { documentId: docId });

    Promise.all([taxonReq]).then(([taxon]) => {
        const rawPageIds = new Set();

        taxon.forEach(d => rawPageIds.add(parseInt(d.page_id)));

        const sortedPageIds = Array.from(rawPageIds).sort((a, b) => a - b);

        const pageIdToPageNumber = new Map();
        sortedPageIds.forEach((pid, idx) => {
            pageIdToPageNumber.set(pid, idx + 1);
        });

        // Merge data
        const dataMap = new Map();


        // From taxon
        taxon.forEach(({ page_id, taxon_count }) => {
            const pid = parseInt(page_id);
            const page = pageIdToPageNumber.get(pid);
            if (!dataMap.has(page)) dataMap.set(page, { page, topicSet: new Set(), taxon: 0, ne: 0 });
            dataMap.get(page).taxon = parseInt(taxon_count);
        });

        // Sort and extract
        const sorted = Array.from(dataMap.values()).sort((a, b) => a.page - b.page);
        const pages = sorted.map(row => row.page);
        const taxonCounts = sorted.map(row => row.taxon);

        const chart = echarts.init(document.getElementById(containerId));

        const option = {
            tooltip: {
                trigger: 'axis',
                formatter: function (params) {
                    const values = {};
                    params.forEach(p => {
                        if (p.seriesName.includes('Taxon')) values['Taxon'] = p.data;
                    });

                    let result = `Page `+params[0].axisValue+`<br/>`;
                    for (const [key, val] of Object.entries(values)) {
                        result += key+`:` +val+`<br/>`;
                    }
                    return result;
                }
            },

            legend: {
                data: [
                    'Taxon (Line)',
                ]
            },
            xAxis: {
                type: 'category',
                name: 'Page Number',
                data: pages
            },
            yAxis: {
                type: 'value',
                name: 'Count'
            },
            series: [


                // Taxon
                {
                    name: 'Taxon (Bar)',
                    type: 'bar',
                    data: taxonCounts,
                    itemStyle: {
                        color: '#91CC75',
                        opacity: 0.15
                    },
                    barGap: '-100%',
                    z: 1
                },
                {
                    name: 'Taxon (Line)',
                    type: 'line',
                    data: taxonCounts,
                    symbol: 'circle',
                    symbolSize: 10,
                    lineStyle: { width: 3, color: '#91CC75' },
                    itemStyle: { color: '#91CC75' },
                    z: 2
                },
            ]
        };

        chart.setOption(option);
        chart.on('click', function (params) {
            if (params.componentSubType === 'line' || params.componentSubType === 'bar') {
                const pageNumber = params.name;

                const pageElement = document.querySelector('.page[data-id="' + pageNumber + '"]');
                if (pageElement) {
                    pageElement.scrollIntoView({ behavior: 'smooth', block: 'start' });
                } else {
                    console.error(`Page`+pageNumber+` not found.`);
                }
            }
        });
    }).catch(err => {
        console.error("Error loading temporal data:", err);
    });
}