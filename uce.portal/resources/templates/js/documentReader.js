let currentFocusedPage = 0;
let searchTokens = "";
let currentSelectedTopic = null;
let currentTopicIndex = -1;
let matchingTopics = [];
let selectedTopicModelId = null;
let selectedTopicModelName = null;
let selectedTopicVizType = 'overview';
let selectedEmotionModelId = null;
let selectedEmotionVizType = 'radar';
let defaultTopicColorMap = getDefaultTopicColorMap();
let defaultTopicSettings = {
    topicCount: 10,
    colorMode: 'per-topic', // 'per-topic' or 'gradient'
    gradientStartColor: '#ff0000',
    gradientEndColor: '#00ff00',
    topicColorMap: defaultTopicColorMap
};

const documentId = $('.reader-container').data('id');
const settingsKey = `settings:`+documentId+`:topicSettings`;
const topicColorMapKey = `settings:`+documentId+`:topicColorMap`;
const topicSettings = JSON.parse(localStorage.getItem(settingsKey)) || defaultTopicSettings;
let topicColorMap = topicSettings.topicColorMap;

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

    // Initialize topic settings panel
    initializeTopicSettingsPanel();

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

function sortedTopicArray(){
    const topicFrequency = {};
    $('.colorable-topic').each(function() {
        const topic = $(this).data('topic-value');
        if (topic) {
            topicFrequency[topic] = (topicFrequency[topic] || 0) + 1;
        }
    });

    const topicArray = Object.keys(topicFrequency).map(topic => ({
        label: topic,
        frequency: topicFrequency[topic]
    }));

    topicArray.sort((a, b) => b.frequency - a.frequency);

    return topicArray;
}

async function loadDocumentTopics() {
    $('.topics-loading').hide();

    const topicArray = sortedTopicArray();
    if (topicSettings.colorMode === 'gradient') {

        // Find max and min for normalization across ALL topics
        const maxFreq = topicArray.length > 0 ? topicArray[0].frequency : 1;
        const minFreq = topicArray.length > 0 ? topicArray[topicArray.length - 1].frequency : 0;
        const freqRange = maxFreq - minFreq;

        // Create color mapping for ALL topics
        topicArray.forEach(function (topic) {
            const normalizedFreq = freqRange > 0 ?
                (topic.frequency - minFreq) / freqRange : 1;

            topicColorMap[topic.label] = window.graphVizHandler.getColorForWeight(normalizedFreq, hexToRgb(topicSettings.gradientStartColor), hexToRgb(topicSettings.gradientEndColor));


        });
    }

    // Take top N topics for display based on settings
    const topTopics = topicArray.slice(0, topicSettings.topicCount);

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

function getDefaultTopicColorMap() {
    const topics = new Set();
    $('.colorable-topic').each(function() {
        const topicValue = $(this).data('topic-value');
        if (topicValue) {
            topics.add(topicValue);
        }
    });

    const topicArray = Array.from(topics);
    const topicCount = topicArray.length;

    const defaultTopicColorMap = {};
    topicArray.forEach((topic, index) => {
        const hue = (index * (360 / topicCount)) % 360;
        defaultTopicColorMap[topic] = hslToRgba(hue, 70, 45, 0.6);
    });

    return defaultTopicColorMap;
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

    for (let i = 0; i <= pagesCount; i += 10) {
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

function colorUnifiedTopics(selectedTopic, defaultColor=null) {
    clearTopicColoring();
    let color;

    if (!selectedTopic) {
        return;
    }

    const $selectedTopicTag = $('.topic-tag').filter(function () {
        return $(this).data('topic') === selectedTopic;
    });

    if(defaultColor=== null) {
        if ($selectedTopicTag.length === 0) {
            color = topicColorMap[selectedTopic];
        } else {
            color = $selectedTopicTag.css('background-color');
        }
    }
    else{
        color = defaultColor;
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
                'padding': '0 2px',
                'color': '#ffffff'
            });
        }
    });
    updateTopicMarkersOnMinimap();
}

function clearTopicColoring() {
    $('.colorable-topic').css({
        'background-color': '',
        'border-radius': '',
        'padding': '',
        'color': 'gray'
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

function activateVisualizationPanel(target, $button) {
    clearTopicColoring();
    hideTopicNavButtons();
    $('.scrollbar-minimap').hide();

    $('.viz-nav-btn').removeClass('active');
    $('.viz-nav-parent').removeClass('active');

    if ($button && $button.length) {
        $button.addClass('active');
    }

    $('.viz-panel').removeClass('active');
    $(target).addClass('active');
}

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
            setTimeout(updateFloatingUIPositions, 500);
            currentSelectedTopic = null;
            sideBar.classList.remove('visualization-expanded');
            $('.scrollbar-minimap').show();
        }

        if (targetId === 'visualization-tab') {
            const docId = document.getElementsByClassName('reader-container')[0].getAttribute('data-id');

            loadEmotionModels(docId);
            loadSentimentMenu(docId);
            loadTopicMenu(docId).then(function (topicState) {
                if (topicState.models && topicState.models.length > 0) {
                    activateVisualizationPanel('#viz-panel-3', $('.viz-nav-group[data-category="topic"] .viz-nav-parent'));
                    $('#vp-3').removeClass('rendered');
                    setTimeout(() => renderTopicViz('vp-3'), 500);
                }
            });

            loadOthersMenu(docId).then(function (othersState) {
                if (othersState.hasSemanticDensity) {
                    activateVisualizationPanel('#viz-panel-1', $('.viz-nav-group[data-category="others"] .viz-nav-parent'));
                    $('#vp-1').removeClass('rendered');
                    setTimeout(() => renderTemporalExplorer('vp-1'), 500);
                    return;
                }

                if (othersState.hasTopicEntity) {
                    activateVisualizationPanel('#viz-panel-2', $('.viz-nav-group[data-category="others"] .viz-nav-parent'));
                    $('#vp-2').removeClass('rendered');
                    setTimeout(() => renderTopicEntityChordDiagram('vp-2'), 500);
                    return;
                }
            });
        }
    });
});

$(document).on('click', '.viz-nav-item[data-target]', function (e) {
    e.preventDefault();

    const target = $(this).data('target');
    const $group = $(this).closest('.viz-nav-group');

    activateVisualizationPanel(target, $group.find('.viz-nav-parent'));

    if (target === '#viz-panel-1') {
        $('#vp-1').removeClass('rendered');
        setTimeout(() => renderTemporalExplorer('vp-1'), 500);
    }

    if (target === '#viz-panel-2') {
        $('#vp-2').removeClass('rendered');
        setTimeout(() => renderTopicEntityChordDiagram('vp-2'), 500);
    }
});

$(document).on('click', '.topic-model-item', function (e) {
    e.preventDefault();

    selectedTopicModelId = $(this).data('model-id');
    selectedTopicModelName = $.trim($(this).text());

    $('.topic-model-item').removeClass('active');
    $(this).addClass('active');

    activateVisualizationPanel('#viz-panel-3', $('.viz-nav-group[data-category="topic"] .viz-nav-parent'));
    $('#vp-3').removeClass('rendered');
    renderTopicViz('vp-3');
});
$(document).on('click', '.topic-viz-toggle-btn', function (e) {
    e.preventDefault();

    const nextVizType = $(this).data('viz-type');
    if (!nextVizType || nextVizType === selectedTopicVizType) return;

    selectedTopicVizType = nextVizType;
    $('#vp-3').removeClass('rendered');
    renderTopicViz('vp-3');
});
$(document).on('click', '.emotion-model-item', function (e) {
    e.preventDefault();

    selectedEmotionModelId = $(this).data('model-id');
    $('.emotion-model-item').removeClass('active');
    $(this).addClass('active');

    activateVisualizationPanel('#viz-panel-7', $('.viz-nav-group[data-category="emotion"] .viz-nav-parent'));
    $('#vp-7').removeClass('rendered');
    renderEmotionViz('vp-7');
});
$(document).on('click', '.others-menu-item[data-target]', function (e) {
    e.preventDefault();

    const target = $(this).data('target');
    const $group = $(this).closest('.viz-nav-group');

    activateVisualizationPanel(target, $group.find('.viz-nav-parent'));

    if (target === '#viz-panel-1') {
        $('#vp-1').removeClass('rendered');
        setTimeout(() => renderTemporalExplorer('vp-1'), 500);
    }
    if (target === '#viz-panel-2') {
        $('#vp-2').removeClass('rendered');
        setTimeout(() => renderTopicEntityChordDiagram('vp-2'), 500);
    }
    if (target === '#viz-panel-4') {
        $('#vp-4').removeClass('rendered');
        setTimeout(() => renderTopicSimilarityMatrix('vp-4'), 500);
    }
    if (target === '#viz-panel-5') {
        $('#vp-5').removeClass('rendered');
        setTimeout(() => renderSentenceTopicSankey('vp-5'), 500);
    }
});
$(document).on('click', '.emotion-viz-toggle-btn', function (e) {
    e.preventDefault();

    const nextVizType = $(this).data('viz-type');
    if (!nextVizType || nextVizType === selectedEmotionVizType) return;

    selectedEmotionVizType = nextVizType;
    $('#vp-7').removeClass('rendered');
    renderEmotionViz('vp-7');
});
function renderSentenceTopicNetwork(containerId) {
    const container = document.getElementById(containerId);
    if (!container || container.classList.contains('rendered')) return;

    $('.visualization-spinner').show()
    const documentId = document.getElementsByClassName('reader-container')[0].getAttribute('data-id');


    $.ajax({
        url: `/api/document/unifiedTopicSentenceMap`,
        method: 'GET',
        data: { documentId },
        dataType: 'json',
        success: function (utToSentenceMapList) {
            const utToSentenceMap = new Map();
            const topicToSentences = {};

            utToSentenceMapList.forEach(({ unifiedtopicId, sentenceId }) => {
                const utId = unifiedtopicId.toString();
                const sId = sentenceId.toString();
                utToSentenceMap.set(utId, sId);
                if (!topicToSentences[utId]) topicToSentences[utId] = [];
                topicToSentences[utId].push(sId);
            });

            $.ajax({
                url: `/api/rag/sentenceEmbeddings`,
                method: 'GET',
                data: { documentId },
                dataType: 'json',
                success: function (embeddings) {
                    $('.visualization-spinner').hide()
                    if (!embeddings || !Array.isArray(embeddings) || embeddings.length === 0) {
                        const container = document.getElementById(containerId);
                        if (container) {
                            container.innerHTML = '<div style="color:#888;">' + document.getElementById('viz-content').getAttribute('data-message') + '</div>';
                        }
                        container.classList.add('rendered');
                        return;
                    }
                    const sentenceEmbeddingMap = new Map();
                    embeddings.forEach(({ sentenceId, tsne2d }) => {
                        sentenceEmbeddingMap.set(sentenceId.toString(), tsne2d);
                    });

                    const nodes = [];
                    const nodeSet = new Set();

                    $('.colorable-topic').each(function () {
                        const utId = this.id.replace('utopic-UT-', '');
                        const topicValue = $(this).data('topic-value')?.toString();
                        const text = $(this).data('wcovered').toString();
                        if (!utToSentenceMap.has(utId)) return;
                        const sentenceId = utToSentenceMap.get(utId);
                        if (!sentenceEmbeddingMap.has(sentenceId)) return;

                        if (nodeSet.has(sentenceId)) return;
                        nodeSet.add(sentenceId);

                        const [x, y] = sentenceEmbeddingMap.get(sentenceId);
                        const color = topicColorMap[topicValue] || '#888';

                        nodes.push({
                            id: sentenceId,
                            name: `Sentence `+utId,
                            symbolSize: 5,
                            x, y,
                            itemStyle: { color },
                            label: { show: false },
                            tooltip: {
                                confine: true, // prevent overflow

                                textStyle: {
                                    color: '#333',
                                    overflow: 'truncate',  // enables word wrap
                                    fontSize: 12,

                                },
                                formatter: () => {
                                    return (
                                        "<b>Sentence ID:</b> " + sentenceId + "<br>" +
                                        "<b>Topic:</b> " + topicValue + "<br>" +
                                        "<b>Text:</b> " + text
                                    );
                                }
                            }
                        });
                    });

                    const links = [];

                    const k = 3;
                    const embeddingArray = Array.from(sentenceEmbeddingMap.entries());

                    function euclidean([x1, y1], [x2, y2]) {
                        return Math.sqrt((x1 - x2) ** 2 + (y1 - y2) ** 2);
                    }

                    embeddingArray.forEach(([id1, vec1]) => {
                        if (!nodeSet.has(id1)) return;

                        const neighbors = embeddingArray
                            .filter(([id2]) => id1 !== id2 && nodeSet.has(id2))
                            .map(([id2, vec2]) => ({ id2, dist: euclidean(vec1, vec2) }))
                            .sort((a, b) => a.dist - b.dist)
                            .slice(0, k);

                        neighbors.forEach(({ id2 }) => {
                            links.push({
                                source: id1,
                                target: id2,
                                lineStyle: { color: '#bbb', opacity: 0.5, width: 3 },
                            });
                        });
                    });

                    const nodeDegreeMap = {};
                    links.forEach(link => {
                        nodeDegreeMap[link.source] = (nodeDegreeMap[link.source] || 0) + 1;
                        nodeDegreeMap[link.target] = (nodeDegreeMap[link.target] || 0) + 1;
                    });

                    nodes.forEach(node => {
                        const degree = nodeDegreeMap[node.id] || 0;
                        node.symbolSize = 10 + degree * 2;
                    });

                    window.graphVizHandler.createNetworkGraph(
                        containerId,
                        '',
                        nodes,
                        links,
                    null,
                    function (params) {
                        if (params.dataType === 'node') {
                            const name = params.name.split('Sentence ')[1];
                            $('.scrollbar-minimap').hide();
                            hideTopicNavButtons();
                            clearTopicColoring();
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
                    });

                    container.classList.add('rendered');
                },
                error: function () {
                    console.error('Failed to get sentence embeddings');
                }
            });
        },
        error: function () {
            console.error('Failed to get unified topic to sentence map');
        }
    });
}

function computeTopicSimilarityMatrix(data, type = "cosine") {
    const topicLabels = data.map(t => t.topicLabel);
    const wordProbMaps = data.map(t => t.words);
    const allWords = new Set(data.flatMap(t => Object.keys(t.words)));

    const matrix = [];

    for (let i = 0; i < wordProbMaps.length; i++) {
        for (let j = 0; j < wordProbMaps.length; j++) {
            let score = 0;

            if (type === "cosine") {
                let dot = 0, normI = 0, normJ = 0;
                for (const word of allWords) {
                    const p1 = wordProbMaps[i][word] || 0;
                    const p2 = wordProbMaps[j][word] || 0;
                    dot += p1 * p2;
                    normI += p1 * p1;
                    normJ += p2 * p2;
                }
                score = (normI === 0 || normJ === 0) ? 0 : dot / (Math.sqrt(normI) * Math.sqrt(normJ));

            } else if (type === "count") {
                const wordsI = new Set(Object.keys(wordProbMaps[i]));
                const wordsJ = new Set(Object.keys(wordProbMaps[j]));
                const shared = [...wordsI].filter(w => wordsJ.has(w));
                score = shared.length;
            }

            matrix.push([i, j, score]);
        }
    }

    return {
        labels: topicLabels,
        matrix: matrix
    };
}
function renderTopicViz(containerId) {
    const container = document.getElementById(containerId);
    if (!container) return;

    if (!selectedTopicModelId) {
        container.classList.remove('rendered');
        container.innerHTML = '<div style="color:#888;">Please choose a topic model</div>';
        container.classList.add('rendered');
        return;
    }

    const modelName = selectedTopicModelName || ('Model ' + selectedTopicModelId);

    const overviewBtnClass = selectedTopicVizType === 'overview'
        ? 'btn btn-sm btn-primary topic-viz-toggle-btn'
        : 'btn btn-sm btn-light topic-viz-toggle-btn';

    const timelineBtnClass = selectedTopicVizType === 'timeline'
        ? 'btn btn-sm btn-primary topic-viz-toggle-btn'
        : 'btn btn-sm btn-light topic-viz-toggle-btn';

    const heatmapBtnClass = selectedTopicVizType === 'heatmap'
        ? 'btn btn-sm btn-primary topic-viz-toggle-btn'
        : 'btn btn-sm btn-light topic-viz-toggle-btn';

    container.classList.remove('rendered');
    container.innerHTML = '' +
        '<div class="d-flex align-items-center justify-content-between flex-wrap mb-3">' +
        '<div class="mb-2">' +
        '<div><strong>Topic</strong></div>' +
        '<div class="text-muted small">' + modelName + '</div>' +
        '</div>' +
        '<div class="btn-group btn-group-sm mb-2" role="group" aria-label="Topic visualizations">' +
        '<button type="button" class="' + overviewBtnClass + '" data-viz-type="overview">Overview</button>' +
        '<button type="button" class="' + timelineBtnClass + '" data-viz-type="timeline">Timeline</button>' +
        '<button type="button" class="' + heatmapBtnClass + '" data-viz-type="heatmap">Heatmap</button>' +
        '</div>' +
        '</div>' +
        '<div id="' + containerId + '-body" style="width:100%;flex:1;min-height:380px;"></div>';

    if (selectedTopicVizType === 'timeline') {
        renderTopicTimeline(containerId + '-body');
    } else if (selectedTopicVizType === 'heatmap') {
        renderTopicHeatmap(containerId + '-body');
    } else {
        renderTopicModelOverview(containerId + '-body');
    }
}
function renderTopicSimilarityMatrix(containerId) {
    const container = document.getElementById(containerId);
    if (!container || container.classList.contains('rendered')){
        $('.selector-container').show();
        return;
    }
    $('.visualization-spinner').show()
    const docId = document.getElementsByClassName('reader-container')[0].getAttribute('data-id');

    $.get('/api/document/page/topicWords', { documentId: docId })
        .then(data => {
            $('.visualization-spinner').hide()
            if (!data || !Array.isArray(data) || data.length === 0) {
                const container = document.getElementById(containerId);
                if (container) {
                    container.innerHTML = '<div style="color:#888;">' + document.getElementById('viz-content').getAttribute('data-message') + '</div>';
                }
                container.classList.remove('rendered');
                $('.selector-container').hide();
                return;
            }
            $('.selector-container').show();
            const similarityTypeSelector = document.getElementById('similarityTypeSelector');

            function updateChart() {
                const type = similarityTypeSelector.value;
                const { labels, matrix } = computeTopicSimilarityMatrix(data, type);

                const tooltipFormatter = function (params) {
                    const xLabel = labels[params.data[0]];
                    const yLabel = labels[params.data[1]];
                    const value = type === "count" ? params.data[2] : params.data[2].toFixed(3);
                   return xLabel + " & " + yLabel + "<br>" + (type.charAt(0).toUpperCase() + type.slice(1)) + ": " + value;
                };

                window.graphVizHandler.createHeatMap(
                    containerId,
                    "Topic Similarity (" + type + ")",
                    matrix,
                    labels,
                    "Similarity (" + type + ")",
                    tooltipFormatter
                );

            }

            similarityTypeSelector.addEventListener('change', updateChart);
            updateChart();
            container.classList.add('rendered');
        });
}

function renderTopicEntityChordDiagram(containerId) {
    const container = document.getElementById(containerId);
    if (!container || container.classList.contains('rendered')) return;
    $('.visualization-spinner').show()

    const docId = document.getElementsByClassName('reader-container')[0].getAttribute('data-id');


    $.get('/api/document/page/topicEntityRelation', { documentId: docId })
        .then(data => {
            $('.visualization-spinner').hide()
            if (!data || !Array.isArray(data) || data.length === 0) {
                const container = document.getElementById(containerId);
                if (container) {
                    container.innerHTML = '<div style="color:#888;">' + document.getElementById('viz-content').getAttribute('data-message') + '</div>';
                }
                container.classList.add('rendered');
                return;
            }
            const nodeMap = new Map();
            const nodes = [];
            const linkCounts = new Map();

            let nodeIndex = 0;

            const categories = [
                { name: 'Topic', itemStyle: { color: '#5470C6' } },
                { name: 'Entity', itemStyle: { color: '#91CC75' } }
            ];

            function getCategory(name, isEntity) {
                return isEntity ? 1 : 0;
            }

            // Step 1: build nodes
            data.forEach(item => {
                const topic = item.topicLabel;
                const entityType = item.entityType;

                if (topic && !nodeMap.has(topic)) {
                    nodeMap.set(topic, nodeIndex++);
                    nodes.push({ name: topic, value: 0, category: getCategory(topic, false) });
                }
                if (entityType && !nodeMap.has(entityType)) {
                    nodeMap.set(entityType, nodeIndex++);
                    nodes.push({ name: entityType, value: 0, category: getCategory(entityType, true) });
                }

                // Step 2: count link frequency as weight
                if (topic && entityType) {
                    const linkKey = topic + '___' + entityType;
                    linkCounts.set(linkKey, (linkCounts.get(linkKey) || 0) + 1);
                }
            });

            // Step 3: build links with frequency as value
            const links = [];
            linkCounts.forEach((count, key) => {
                const [sourceName, targetName] = key.split('___');
                if (nodeMap.has(sourceName) && nodeMap.has(targetName)) {
                    links.push({
                        source: nodeMap.get(sourceName),
                        target: nodeMap.get(targetName),
                        value: count
                    });

                    nodes[nodeMap.get(sourceName)].value += count;
                    nodes[nodeMap.get(targetName)].value += count;
                }
            });

            // Step 4: normalize node sizes
            const minSize = 10;
            const maxSize = 50;
            const values = nodes.map(n => n.value);
            const maxVal = Math.max(...values);
            const minVal = Math.min(...values);

            nodes.forEach(n => {
                if (maxVal === minVal) {
                    n.symbolSize = (minSize + maxSize) / 2;
                } else {
                    n.symbolSize = minSize + (n.value - minVal) / (maxVal - minVal) * (maxSize - minSize);
                }
            });

            const tooltipFormatter = function (params) {
                if (params.dataType !== 'node') return '';

                const node = nodes[params.dataIndex];
                const clickedNodeName = node.name;
                const isEntity = node.category === 1;

                const filtered = data.filter(item => {
                    return isEntity ? item.entityType === clickedNodeName : item.topicLabel === clickedNodeName;
                });
                const aggMap = new Map();

                filtered.forEach(item => {
                    // The other side of the relation
                    const key = isEntity ? item.topicLabel : item.entityType;
                    if (key) {
                        aggMap.set(key, (aggMap.get(key) || 0) + 1);
                    }
                });

                const entityColor = '#91CC75'
                const topicColor = '#5470C6';
                const label = (isEntity ? "Topics for Entity: <b>" + clickedNodeName + "</b>" : "Entities for Topic: <b>" + clickedNodeName + "</b>");

                const topN = [...aggMap.entries()]
                    .sort((a, b) => b[1] - a[1])
                    .slice(0, 5);

                return window.graphVizHandler.createMiniBarChart({
                    data: topN,
                    labelPrefix: label,
                    primaryColor: topicColor,
                    secondaryColor: entityColor,
                    usePrimaryForEntity: isEntity,
                    maxBarWidth: 100,
                    fontSize: 10
                });
            };

            window.graphVizHandler.createChordChart(
                containerId,
                '',
                {nodes, links, categories},
                tooltipFormatter,
                function onClick(params) {
                    const pageNumber = params.name;
                    const pageElement = document.querySelector('.page[data-id="' + pageNumber + '"]');
                    if (pageElement) {
                        pageElement.scrollIntoView({ behavior: 'smooth', block: 'start' });
                    } else {
                        console.error(`Page ` + pageNumber + ` not found.`);
                    }
                }
            );
            container.classList.add('rendered');


            // window.addEventListener('resize', () => {
            //     chart.resize();
            // });


        })
        .catch(err => {
            console.error("Error loading topic-entity chord data:", err);
        });
}

function renderSentenceTopicSankey(containerId) {
    const container = document.getElementById(containerId);
    if (!container || container.classList.contains('rendered')) return;

    $('.visualization-spinner').show()

    const $colorableTopics = $('.colorable-topic');
    if ($colorableTopics.length === 0) {
        $('.visualization-spinner').hide()
        const container = document.getElementById(containerId);
        if (container) {
            container.innerHTML = '<div style="color:#888;">' + document.getElementById('viz-content').getAttribute('data-message') + '</div>';
        }
        container.classList.add('rendered');
        return;
    }

    let sentenceTopicData = [];
    const topicFrequency = {};

    $colorableTopics.each(function () {
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
    $('.visualization-spinner').hide()
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

function renderTemporalExplorer(containerId) {

    const container = document.getElementById(containerId);
    if (!container || container.classList.contains('rendered')) return;
    $('.visualization-spinner').show()
    const docId = document.getElementsByClassName('reader-container')[0].getAttribute('data-id');

    const emotionReq = $.get('/api/document/page/emotions', {
        documentId: docId,
        modelId: selectedEmotionModelId
    });
    const taxonReq = $.get('/api/document/page/taxon', { documentId: docId });
    const topicReq = $.get('/api/document/page/topics', { documentId: docId });
    const entityReq = $.get('/api/document/page/namedEntities', { documentId: docId });
    const lemmaReq = $.get('/api/document/page/lemma', { documentId: docId });
    const geonameReq = $.get('/api/document/page/geoname', { documentId: docId });

    Promise.all([taxonReq, topicReq, entityReq, lemmaReq, geonameReq, emotionReq]).then(([taxon, topics, entities, lemma, geoname, emotions]) => {
        $('.visualization-spinner').hide()
        if ((!taxon || taxon.length === 0) && (!topics || topics.length === 0) && (!entities || entities.length === 0) && (!lemma || lemma.length === 0 && !geoname || geoname.length === 0)) {
            const container = document.getElementById(containerId);
            if (container) {
                container.innerHTML = '<div style="color:#888;">' + document.getElementById('viz-content').getAttribute('data-message') + '</div>';
            }
            container.classList.add('rendered');
            return;
        }
        const annotationSources = [
            {
                key: 'Taxon',
                data: taxon,
                pageField: 'pageId',
                valueField: 'taxonValue',
                label: 'Taxon',
                color: '#91CC75',
                transformValue: v => v.split('|')[0]
            },
            {
                key: 'Topics',
                data: topics,
                pageField: 'pageId',
                valueField: 'topicLabel',
                label: 'Topics',
                color: '#75ccc5'
            }
            ,
            {
                key: 'Named Entities',
                data: entities,
                pageField: 'pageId',
                valueField: 'entityType',
                label: 'Named Entities',
                color: '#5470C6',
            },
            {
                key: 'Lemmas',
                data: lemma,
                pageField: 'pageId',
                valueField: 'coarseValue',
                label: 'Lemmas',
                color: '#ff9f7f',
            },
            {
                key: 'Geonames',
                data: geoname,
                pageField: 'pageId',
                valueField: 'geonameValue',
                label: 'Geonames',
                color: '#c680ff',
            },
            {
                key: 'Emotions',
                data: emotions,
                pageField: 'pageId',
                valueField: 'emotionLabel',
                label: 'Emotions',
                color: '#f5c542'
            }
        ];

        // Collect unique sorted page IDs
        const rawPageIds = [];
        annotationSources.forEach(({ data, pageField }) => {
            data.forEach(d => {
                const pid = parseInt(d[pageField]);
                if (!isNaN(pid)) rawPageIds.push(pid);
            });
        });
        const uniqueSortedPageIds = Array.from(new Set(rawPageIds)).sort((a, b) => a - b);

        const pageIdToPageNumber = new Map();
        uniqueSortedPageIds.forEach((pid, idx) => {
            pageIdToPageNumber.set(pid, idx + 1);
        });

        const dataMap = new Map();

        annotationSources.forEach(({ key, data, pageField, valueField, transformValue }) => {
            data.forEach(item => {
                const pid = parseInt(item[pageField]);
                const page = pageIdToPageNumber.get(pid);
                if (!page) return;

                if (!dataMap.has(page)) {
                    dataMap.set(page, {
                        page,
                        Taxon: [],
                        Topics: [],
                        "Named Entities": [],
                        Lemmas: [],
                        Geonames: [],
                        Emotions: []
                    });
                }

                const value = item[valueField];
                if (value) {
                    dataMap.get(page)[key].push(transformValue ? transformValue(value) : value);
                }
            });
        });

        const sorted = Array.from(dataMap.values()).sort((a, b) => a.page - b.page);
        const pages = sorted.map(row => row.page);

        const seriesData = annotationSources
            .map(({ key, label, color }) => {
                const data = sorted.map(row => row[key]?.length || 0);
                const hasNonZero = data.some(count => count > 0);
                return hasNonZero ? { name: label, data, color } : null;
            })
            .filter(d => d !== null);


        const chartConfig = {
            xData: pages,
            seriesData,
            yLabel: 'Count'
        };

        // Tooltip formatter
        const tooltipFormatter = function (params) {
            const page = parseInt(params[0].axisValue);
            const seriesNames = new Set(params.map(p => p.seriesName));

            const record = dataMap.get(page);
            if (!record) return 'Page ' + page + '<br/>No data.';

            let tooltipHtml = '<div><b>Page ' + page + '</b></div>';

            annotationSources.forEach(({ key, label, color }) => {
                if (!seriesNames.has(label)) return;
                const items = record[key];
                if (!items || items.length === 0) return;

                const freq = {};
                items.forEach(item => {
                    freq[item] = (freq[item] || 0) + 1;
                });

                const topN = Object.entries(freq)
                    .sort((a, b) => b[1] - a[1])
                    .slice(0, 5);

                tooltipHtml += window.graphVizHandler.createMiniBarChart({
                    data: topN,
                    labelPrefix: label,
                    labelHighlight: `Page ` + page,
                    primaryColor: color,
                    usePrimaryForEntity: true,
                    maxBarWidth: 100,
                    fontSize: 10
                });
            });

            if (tooltipHtml === '<div><b>Page ' + page + '</b></div>') {
                tooltipHtml += '<div style="color:#888;">No data available</div>';
            }

            return tooltipHtml;
        };

        window.graphVizHandler.createBarLineChart(
            containerId,
            '',
            chartConfig,
            tooltipFormatter,
            function onClick(params) {
                const pageNumber = params.name;
                const pageElement = document.querySelector('.page[data-id="' + pageNumber + '"]');
                if (pageElement) {
                    pageElement.scrollIntoView({ behavior: 'smooth', block: 'start' });
                } else {
                    console.error(`Page ` + pageNumber + ` not found.`);
                }
            }
        );
        container.classList.add('rendered');
    }).catch(err => {
        console.error("Error loading or processing annotation data:", err);
    });
}
function loadEmotionModels(docId) {
    return $.get('/api/document/emotionModels', { documentId: docId })
        .then((models) => {
            const $menu = $('#emotion-model-menu');
            $menu.empty();

            if (!models || models.length === 0) {
                selectedEmotionModelId = null;
                $menu.append('<span class="viz-nav-item viz-disabled">No models found</span>');
                return;
            }

            const hasSelectedModel = models.some(function (m) {
                return String(m.modelId) === String(selectedEmotionModelId);
            });

            if (!hasSelectedModel) {
                selectedEmotionModelId = models[0].modelId;
            }

            models.forEach((m) => {
                const isActive = String(m.modelId) === String(selectedEmotionModelId) ? ' active' : '';
                $menu.append(
                    '<a class="viz-nav-item emotion-model-item viz-nav-model-item' + isActive + '" href="#" data-model-id="' + m.modelId + '">' +
                    (m.modelName ? m.modelName : ('Model ' + m.modelId)) +
                    '</a>'
                );
            });
        })
        .catch(() => {
            selectedEmotionModelId = null;
            $('#emotion-model-menu').html('<span class="viz-nav-item viz-disabled">Failed to load</span>');
        });
}
function loadTopicMenu(docId) {
    const $menu = $('#topic-menu');
    const noDataLabel = $menu.attr('data-label-no-data') || 'No data available';

    $menu.empty();

    const topicModelsReq = $.get('/api/document/topicModels', { documentId: docId })
        .then(function (data) { return data; })
        .catch(function () { return []; });

    return topicModelsReq.then(function (topicModels) {
        const hasModels = Array.isArray(topicModels) && topicModels.length > 0;

        if (hasModels) {
            const selectedStillExists = topicModels.some(function (m) {
                return String(m.modelId) === String(selectedTopicModelId);
            });

            if (!selectedStillExists) {
                selectedTopicModelId = topicModels[0].modelId;
                selectedTopicModelName = topicModels[0].modelName || ('Model ' + topicModels[0].modelId);
            }

            topicModels.forEach(function (m) {
                const isActive = String(m.modelId) === String(selectedTopicModelId) ? ' active' : '';
                const label = m.modelName ? m.modelName : ('Model ' + m.modelId);

                $menu.append(
                    '<a class="viz-nav-item topic-model-item viz-nav-model-item' + isActive + '" href="#" data-model-id="' + m.modelId + '">' +
                    label +
                    '</a>'
                );
            });
        }

        if (!hasModels) {
            $menu.append('<span class="viz-nav-item viz-disabled">No models found</span>');
        }

        return {
            models: topicModels
        };
    });
}
function loadOthersMenu(docId) {
    const $menu = $('#others-menu');
    const noDataLabel = $menu.attr('data-label-no-data') || 'No data available';

    $menu.empty();

    const topicPageReq = $.get('/api/document/page/topics', { documentId: docId })
        .then(function (data) { return data; })
        .catch(function () { return []; });

    const topicEntityReq = $.get('/api/document/page/topicEntityRelation', { documentId: docId })
        .then(function (data) { return data; })
        .catch(function () { return []; });

    return Promise.all([topicPageReq, topicEntityReq]).then(function (results) {
        const topicPageData = results[0] || [];
        const topicEntityData = results[1] || [];

        const hasSemanticDensity = Array.isArray(topicPageData) && topicPageData.length > 0;
        const hasTopicEntity = Array.isArray(topicEntityData) && topicEntityData.length > 0;

        $menu.append(
            '<a class="viz-nav-item others-menu-item" href="#" data-target="#viz-panel-1">' +
            'Semantic Density' +
            '</a>'
        );

        $menu.append(
            '<a class="viz-nav-item others-menu-item" href="#" data-target="#viz-panel-2">' +
            'Topic Entity' +
            '</a>'
        );

        $menu.append(
            '<a class="viz-nav-item others-menu-item" href="#" data-target="#viz-panel-4">' +
            'Topic Landscape' +
            '</a>'
        );

        $menu.append(
            '<a class="viz-nav-item others-menu-item" href="#" data-target="#viz-panel-5">' +
            'Topic Similarity' +
            '</a>'
        );

        $menu.append(
            '<a class="viz-nav-item others-menu-item" href="#" data-target="#viz-panel-5">' +
            'Sentence Topic Flow' +
            '</a>'
        );

        return {
            hasSemanticDensity: hasSemanticDensity,
            hasTopicEntity: hasTopicEntity
        };
    });
}
function loadSentimentMenu(docId) {
    const $menu = $('#sentiment-menu');
    $menu.empty();

    $.get('/api/document/page/sentiments', { documentId: docId })
        .then(function (data) {
            if (Array.isArray(data) && data.length > 0) {
                $menu.append(
                    '<a class="viz-nav-item" href="#" data-target="#viz-panel-6">' +
                    'Sentence Sentiment' +
                    '</a>'
                );
            } else {
                $menu.append('<span class="viz-nav-item viz-disabled">No models found</span>');
            }
        })
        .catch(function () {
            $menu.append('<span class="viz-nav-item viz-disabled">No models found</span>');
        });
}
function renderTopicModelOverview(containerId) {
    const container = document.getElementById(containerId);
    if (!container) return;

    if (!selectedTopicModelId) {
        container.classList.remove('rendered');
        container.innerHTML = '<div style="color:#888;">Please choose a topic model</div>';
        container.classList.add('rendered');
        return;
    }

    container.classList.remove('rendered');
    container.innerHTML = '<div style="width:100%;height:100%;min-height:380px;" id="' + containerId + '-topic-model"></div>';

    const docId = document.getElementsByClassName('reader-container')[0].getAttribute('data-id');

    $('.visualization-spinner').show();

    $.get('/api/document/topicModelOverview', {
        documentId: docId,
        modelId: selectedTopicModelId
    }).then(function (data) {
        $('.visualization-spinner').hide();

        if (!data || !Array.isArray(data) || data.length === 0) {
            container.innerHTML = '<div style="color:#888;">No topic data for this model</div>';
            container.classList.add('rendered');
            return;
        }

        const sortedTopics = data
            .filter(function (item) {
                return item && item.label && String(item.label).trim() !== '';
            })
            .slice(0, 10);

        if (sortedTopics.length === 0) {
            container.innerHTML = '<div style="color:#888;">No topic data for this model</div>';
            container.classList.add('rendered');
            return;
        }

        const labels = sortedTopics.map(function (item) {
            return String(item.label).trim();
        });

        const values = sortedTopics.map(function (item) {
            return item.value || 0;
        });

        const maxValue = Math.max.apply(null, values);

        function formatTopicLabel(label) {
            const text = String(label || '');
            if (text.length <= 28) return text;
            return text.slice(0, 25) + '...';
        }

        const chartDom = document.getElementById(containerId + '-topic-model');
        const chart = echarts.init(chartDom);

        const option = {
            title: {
                text: 'Topic Overview',
                left: 0,
                top: 0
            },
            tooltip: {
                trigger: 'item',
                formatter: function (params) {
                    return '<div><b>' + labels[params.dataIndex] + '</b></div>' +
                        '<div>Occurrences: ' + params.value + '</div>';
                }
            },
            grid: {
                left: '15%',
                right: '12%',
                top: 45,
                bottom: 60,
                containLabel: false
            },
            xAxis: {
                type: 'value',
                minInterval: 1,
                max: maxValue < 5 ? 5 : null,
                splitLine: {
                    show: true
                },
                axisLine: {
                    show: false
                },
                axisTick: {
                    show: false
                },
                name: 'Count',
                nameLocation: 'middle',
                nameGap: 28
            },
            yAxis: {
                type: 'category',
                inverse: true,
                data: labels,
                axisLine: {
                    show: false
                },
                axisTick: {
                    show: false
                },
                axisLabel: {
                    width: 180,
                    overflow: 'truncate',
                    formatter: function (value) {
                        return formatTopicLabel(value);
                    }
                }
            },
            series: [{
                type: 'bar',
                data: values,
                barWidth: 22,
                label: {
                    show: true,
                    position: 'right',
                    formatter: '{c}'
                },
                emphasis: {
                    focus: 'series'
                }
            }]
        };

        chart.setOption(option);

        window.addEventListener('resize', function () {
            chart.resize();
        });

        container.classList.add('rendered');
    }).catch(function () {
        $('.visualization-spinner').hide();
        container.innerHTML = '<div style="color:#888;">Failed to load topic model data</div>';
        container.classList.add('rendered');
    });
}
function loadTopicModelPageCounts() {
    const docId = document.getElementsByClassName('reader-container')[0].getAttribute('data-id');

    return $.get('/api/document/topicModelPageCounts', {
        documentId: docId,
        modelId: selectedTopicModelId
    }).then(function (data) {
        if (!data || !Array.isArray(data) || data.length === 0) {
            return null;
        }

        const rawPageIds = [];
        data.forEach(function (item) {
            const pid = parseInt(item.pageId, 10);
            if (!isNaN(pid)) rawPageIds.push(pid);
        });

        const uniqueSortedPageIds = Array.from(new Set(rawPageIds)).sort(function (a, b) {
            return a - b;
        });

        const pageIdToPageNumber = new Map();
        uniqueSortedPageIds.forEach(function (pid, idx) {
            pageIdToPageNumber.set(pid, idx + 1);
        });

        const pageTopicCounts = new Map();
        const totalTopicCounts = {};

        data.forEach(function (item) {
            const pid = parseInt(item.pageId, 10);
            const pageNumber = pageIdToPageNumber.get(pid);
            const label = item.label ? String(item.label).trim() : '';
            const value = parseInt(item.value, 10) || 0;

            if (!pageNumber || !label) return;

            if (!pageTopicCounts.has(pageNumber)) {
                pageTopicCounts.set(pageNumber, {});
            }

            pageTopicCounts.get(pageNumber)[label] = value;
            totalTopicCounts[label] = (totalTopicCounts[label] || 0) + value;
        });

        const pages = Array.from(pageTopicCounts.keys()).sort(function (a, b) {
            return a - b;
        });

        const topLabels = Object.keys(totalTopicCounts)
            .sort(function (a, b) { return totalTopicCounts[b] - totalTopicCounts[a]; })
            .slice(0, 8);

        return {
            pages: pages,
            labels: topLabels,
            pageTopicCounts: pageTopicCounts
        };
    });
}
function renderTopicTimeline(containerId) {
    const container = document.getElementById(containerId);
    if (!container) return;

    container.classList.remove('rendered');
    container.innerHTML = '<div style="width:100%;height:100%;min-height:520px;" id="' + containerId + '-timeline"></div>';

    $('.visualization-spinner').show();

    loadTopicModelPageCounts()
        .then(function (result) {
            $('.visualization-spinner').hide();

            if (!result || !result.pages.length || !result.labels.length) {
                container.innerHTML = '<div style="color:#888;">No topic timeline data for this model</div>';
                container.classList.add('rendered');
                return;
            }

            const pages = result.pages;
            const labels = result.labels;
            const pageTopicCounts = result.pageTopicCounts;

            const series = labels.map(function (label) {
                return {
                    name: label,
                    type: 'line',
                    smooth: true,
                    symbol: 'circle',
                    symbolSize: 6,
                    data: pages.map(function (page) {
                        const counts = pageTopicCounts.get(page) || {};
                        return counts[label] || 0;
                    })
                };
            });

            const chart = echarts.init(document.getElementById(containerId + '-timeline'));

            chart.setOption({
                title: {
                    text: 'Topic Timeline',
                    left: 0,
                    top: 0
                },
                tooltip: {
                    trigger: 'axis'
                },
                legend: {
                    type: 'scroll',
                    top: 30
                },
                grid: {
                    left: '12%',
                    right: '8%',
                    top: 85,
                    bottom: 50
                },
                xAxis: {
                    type: 'category',
                    name: 'Page',
                    data: pages
                },
                yAxis: {
                    type: 'value',
                    name: 'Count'
                },
                series: series
            });

            container.classList.add('rendered');
        })
        .catch(function () {
            $('.visualization-spinner').hide();
            container.innerHTML = '<div style="color:#888;">Failed to load topic timeline</div>';
            container.classList.add('rendered');
        });
}
function renderTopicHeatmap(containerId) {
    const container = document.getElementById(containerId);
    if (!container) return;

    container.classList.remove('rendered');
    container.innerHTML = '<div style="width:100%;height:100%;min-height:520px;" id="' + containerId + '-heatmap"></div>';

    $('.visualization-spinner').show();

    loadTopicModelPageCounts()
        .then(function (result) {
            $('.visualization-spinner').hide();

            if (!result || !result.pages.length || !result.labels.length) {
                container.innerHTML = '<div style="color:#888;">No topic heatmap data for this model</div>';
                container.classList.add('rendered');
                return;
            }

            const pages = result.pages;
            const labels = result.labels;
            const pageTopicCounts = result.pageTopicCounts;

            const heatmapData = [];
            let maxValue = 0;

            pages.forEach(function (page, pageIndex) {
                const counts = pageTopicCounts.get(page) || {};

                labels.forEach(function (label, labelIndex) {
                    const value = counts[label] || 0;
                    if (value > maxValue) maxValue = value;
                    heatmapData.push([pageIndex, labelIndex, value]);
                });
            });

            const chart = echarts.init(document.getElementById(containerId + '-heatmap'));

            chart.setOption({
                title: {
                    text: 'Topic Heatmap',
                    left: 0,
                    top: 0
                },
                tooltip: {
                    position: 'top',
                    formatter: function (params) {
                        const page = pages[params.value[0]];
                        const label = labels[params.value[1]];
                        const value = params.value[2];
                        return '<div><b>Page ' + page + '</b></div><div>' + label + ': ' + value + '</div>';
                    }
                },
                grid: {
                    left: 120,
                    right: 30,
                    top: 75,
                    bottom: 60
                },
                xAxis: {
                    type: 'category',
                    name: 'Page',
                    data: pages,
                    splitArea: { show: true }
                },
                yAxis: {
                    type: 'category',
                    name: 'Topic',
                    data: labels,
                    splitArea: { show: true }
                },
                visualMap: {
                    min: 0,
                    max: maxValue > 0 ? maxValue : 1,
                    calculable: true,
                    orient: 'horizontal',
                    left: 'center',
                    bottom: 10
                },
                series: [{
                    name: 'Topic Count',
                    type: 'heatmap',
                    data: heatmapData,
                    emphasis: {
                        itemStyle: {
                            shadowBlur: 10,
                            shadowColor: 'rgba(0, 0, 0, 0.35)'
                        }
                    }
                }]
            });

            container.classList.add('rendered');
        })
        .catch(function () {
            $('.visualization-spinner').hide();
            container.innerHTML = '<div style="color:#888;">Failed to load topic heatmap</div>';
            container.classList.add('rendered');
        });
}
function getSelectedEmotionModelName() {
    const $activeModel = $('.emotion-model-item.active');
    if ($activeModel.length > 0) {
        return $.trim($activeModel.text());
    }

    if (selectedEmotionModelId) {
        return 'Model ' + selectedEmotionModelId;
    }

    return 'Emotion model';
}

function renderEmotionViz(containerId) {
    const container = document.getElementById(containerId);
    if (!container) return;

    if (!selectedEmotionModelId) {
        container.classList.remove('rendered');
        container.innerHTML = '<div style="color:#888;">Please choose an emotion model</div>';
        container.classList.add('rendered');
        return;
    }

    const modelName = getSelectedEmotionModelName();

    const radarBtnClass = selectedEmotionVizType === 'radar'
        ? 'btn btn-sm btn-primary emotion-viz-toggle-btn'
        : 'btn btn-sm btn-light emotion-viz-toggle-btn';

    const timelineBtnClass = selectedEmotionVizType === 'timeline'
        ? 'btn btn-sm btn-primary emotion-viz-toggle-btn'
        : 'btn btn-sm btn-light emotion-viz-toggle-btn';

    const heatmapBtnClass = selectedEmotionVizType === 'heatmap'
        ? 'btn btn-sm btn-primary emotion-viz-toggle-btn'
        : 'btn btn-sm btn-light emotion-viz-toggle-btn';

    container.classList.remove('rendered');
    container.innerHTML = '' +
        '<div class="d-flex align-items-center justify-content-between flex-wrap mb-3">' +
        '<div class="mb-2">' +
        '<div><strong>Emotion</strong></div>' +
        '<div class="text-muted small">' + modelName + '</div>' +
        '</div>' +
        '<div class="btn-group btn-group-sm mb-2" role="group" aria-label="Emotion visualizations">' +
        '<button type="button" class="' + radarBtnClass + '" data-viz-type="radar">Radar</button>' +
        '<button type="button" class="' + timelineBtnClass + '" data-viz-type="timeline">Timeline</button>' +
        '<button type="button" class="' + heatmapBtnClass + '" data-viz-type="heatmap">Heatmap</button>' +
        '</div>' +
        '</div>' +
        '<div id="' + containerId + '-body" style="width:100%;height:420px;"></div>';

    if (selectedEmotionVizType === 'timeline') {
        renderEmotionTimeline(containerId + '-body');
    } else if (selectedEmotionVizType === 'heatmap') {
        renderEmotionHeatmap(containerId + '-body');
    } else {
        renderEmotionRadar(containerId + '-body');
    }
}
function renderEmotionRadar(containerId) {
    const container = document.getElementById(containerId);
    if (!container) return;

    container.classList.remove('rendered');
    container.innerHTML = '<div style="width:100%;height:100%;min-height:380px;" id="' + containerId + '-radar"></div>';

    const docId = document.getElementsByClassName('reader-container')[0].getAttribute('data-id');
    const modelId = selectedEmotionModelId || null;

    $('.visualization-spinner').show();

    $.get('/api/document/emotionRadar', { documentId: docId, modelId: modelId })
        .then(data => {
            $('.visualization-spinner').hide();

            if (!data || !Array.isArray(data) || data.length === 0) {
                container.innerHTML = '<div style="color:#888;">No emotion data for this model</div>';
                container.classList.add('rendered');
                return;
            }

            const indicators = data.map(d => ({ name: d.label, max: 1 }));
            const values = data.map(d => d.value);

            const chartDom = document.getElementById(containerId + '-radar');
            const chart = echarts.init(chartDom);

            const option = {
                title: { text: 'Emotion Radar' },
                tooltip: {},
                radar: {
                    indicator: indicators,
                    radius: '65%'
                },
                series: [{
                    type: 'radar',
                    data: [{
                        value: values,
                        name: 'Avg intensity'
                    }]
                }]
            };

            chart.setOption(option);
            container.classList.add('rendered');
        })
        .catch(() => {
            $('.visualization-spinner').hide();
            container.innerHTML = '<div style="color:#888;">Failed to load emotion radar</div>';
            container.classList.add('rendered');
        });
}
function scrollToEmotionPage(pageNumber) {
    const pageElement = document.querySelector('.page[data-id="' + pageNumber + '"]');
    if (pageElement) {
        pageElement.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
}

function loadEmotionPageCounts() {
    const docId = document.getElementsByClassName('reader-container')[0].getAttribute('data-id');
    const modelId = selectedEmotionModelId || null;

    return $.get('/api/document/page/emotions', { documentId: docId, modelId: modelId })
        .then(function (data) {
            if (!data || !Array.isArray(data) || data.length === 0) {
                return null;
            }

            const rawPageIds = [];
            data.forEach(function (item) {
                const pid = parseInt(item.pageId, 10);
                if (!isNaN(pid)) rawPageIds.push(pid);
            });

            const uniqueSortedPageIds = Array.from(new Set(rawPageIds)).sort(function (a, b) {
                return a - b;
            });

            const pageIdToPageNumber = new Map();
            uniqueSortedPageIds.forEach(function (pid, idx) {
                pageIdToPageNumber.set(pid, idx + 1);
            });

            const pageEmotionCounts = new Map();
            const totalEmotionCounts = {};

            data.forEach(function (item) {
                const pid = parseInt(item.pageId, 10);
                const pageNumber = pageIdToPageNumber.get(pid);
                const label = item.emotionLabel ? String(item.emotionLabel).trim() : '';

                if (!pageNumber || !label) return;

                if (!pageEmotionCounts.has(pageNumber)) {
                    pageEmotionCounts.set(pageNumber, {});
                }

                const currentPageMap = pageEmotionCounts.get(pageNumber);
                currentPageMap[label] = (currentPageMap[label] || 0) + 1;
                totalEmotionCounts[label] = (totalEmotionCounts[label] || 0) + 1;
            });

            const pages = Array.from(pageEmotionCounts.keys()).sort(function (a, b) {
                return a - b;
            })
            const topLabels = Object.keys(totalEmotionCounts)
                .sort(function (a, b) {
                    return totalEmotionCounts[b] - totalEmotionCounts[a];
                })
                .slice(0, 6);

            return {
                pages: pages,
                labels: topLabels,
                pageEmotionCounts: pageEmotionCounts,
                totalEmotionCounts: totalEmotionCounts
            };
        });
}

function renderEmotionTimeline(containerId) {
    const container = document.getElementById(containerId);
    if (!container) return;

    container.classList.remove('rendered');
    container.innerHTML = '<div style="width:100%;height:100%;min-height:420px;" id="' + containerId + '-timeline"></div>';

    $('.visualization-spinner').show();

    loadEmotionPageCounts()
        .then(function (result) {
            $('.visualization-spinner').hide();

            if (!result || !result.pages || result.pages.length === 0 || !result.labels || result.labels.length === 0) {
                container.innerHTML = '<div style="color:#888;">No emotion timeline data for this model</div>';
                container.classList.add('rendered');
                return;
            }

            const pages = result.pages;
            const labels = result.labels;
            const pageEmotionCounts = result.pageEmotionCounts;

            const series = labels.map(function (label) {
                return {
                    name: label,
                    type: 'line',
                    smooth: true,
                    symbol: 'circle',
                    symbolSize: 6,
                    data: pages.map(function (page) {
                        const counts = pageEmotionCounts.get(page) || {};
                        return counts[label] || 0;
                    })
                };
            });

            const chartDom = document.getElementById(containerId + '-timeline');
            const chart = echarts.init(chartDom);

            const option = {
                title: { text: 'Emotion Timeline' },
                tooltip: {
                    trigger: 'axis',
                    formatter: function (params) {
                        if (!params || params.length === 0) return '';

                        const page = params[0].axisValue;
                        let html = '<div><b>Page ' + page + '</b></div>';

                        params
                            .slice()
                            .sort(function (a, b) { return b.value - a.value; })
                            .forEach(function (p) {
                                html += '<div>' + p.seriesName + ': ' + p.value + '</div>';
                            });

                        return html;
                    }
                },
                legend: {
                    type: 'scroll',
                    top: 30
                },
                grid: {
                    left: 50,
                    right: 20,
                    top: 80,
                    bottom: 50
                },
                xAxis: {
                    type: 'category',
                    name: 'Page',
                    data: pages
                },
                yAxis: {
                    type: 'value',
                    name: 'Count'
                },
                series: series
            };

            chart.setOption(option);

            chart.on('click', function (params) {
                const pageNumber = parseInt(params.name, 10);
                if (!isNaN(pageNumber)) {
                    scrollToEmotionPage(pageNumber);
                }
            });

            container.classList.add('rendered');
        })
        .catch(function () {
            $('.visualization-spinner').hide();
            container.innerHTML = '<div style="color:#888;">Failed to load emotion timeline</div>';
            container.classList.add('rendered');
        });
}

function renderEmotionHeatmap(containerId) {
    const container = document.getElementById(containerId);
    if (!container) return;

    container.classList.remove('rendered');
    container.innerHTML = '<div style="width:100%;height:100%;min-height:420px;" id="' + containerId + '-heatmap"></div>';

    $('.visualization-spinner').show();

    loadEmotionPageCounts()
        .then(function (result) {
            $('.visualization-spinner').hide();

            if (!result || !result.pages || result.pages.length === 0 || !result.labels || result.labels.length === 0) {
                container.innerHTML = '<div style="color:#888;">No emotion heatmap data for this model</div>';
                container.classList.add('rendered');
                return;
            }

            const pages = result.pages;
            const labels = result.labels;
            const pageEmotionCounts = result.pageEmotionCounts;

            const heatmapData = [];
            let maxValue = 0;

            pages.forEach(function (page, pageIndex) {
                const counts = pageEmotionCounts.get(page) || {};

                labels.forEach(function (label, labelIndex) {
                    const value = counts[label] || 0;
                    if (value > maxValue) maxValue = value;
                    heatmapData.push([pageIndex, labelIndex, value]);
                });
            });

            const chartDom = document.getElementById(containerId + '-heatmap');
            const chart = echarts.init(chartDom);

            const option = {
                title: { text: 'Emotion Heatmap' },
                tooltip: {
                    position: 'top',
                    formatter: function (params) {
                        const page = pages[params.value[0]];
                        const label = labels[params.value[1]];
                        const value = params.value[2];
                        return '<div><b>Page ' + page + '</b></div><div>' + label + ': ' + value + '</div>';
                    }
                },
                grid: {
                    left: 90,
                    right: 30,
                    top: 60,
                    bottom: 60
                },
                xAxis: {
                    type: 'category',
                    name: 'Page',
                    data: pages,
                    splitArea: { show: true }
                },
                yAxis: {
                    type: 'category',
                    name: 'Emotion',
                    data: labels,
                    splitArea: { show: true }
                },
                visualMap: {
                    min: 0,
                    max: maxValue > 0 ? maxValue : 1,
                    calculable: true,
                    orient: 'horizontal',
                    left: 'center',
                    bottom: 10
                },
                series: [{
                    name: 'Emotion Count',
                    type: 'heatmap',
                    data: heatmapData,
                    label: {
                        show: false
                    },
                    emphasis: {
                        itemStyle: {
                            shadowBlur: 10,
                            shadowColor: 'rgba(0, 0, 0, 0.35)'
                        }
                    }
                }]
            };

            chart.setOption(option);

            chart.on('click', function (params) {
                const pageIndex = params.value[0];
                const pageNumber = pages[pageIndex];
                if (pageNumber) {
                    scrollToEmotionPage(pageNumber);
                }
            });

            container.classList.add('rendered');
        })
        .catch(function () {
            $('.visualization-spinner').hide();
            container.innerHTML = '<div style="color:#888;">Failed to load emotion heatmap</div>';
            container.classList.add('rendered');
        });
}
function initializeTopicSettingsPanel() {

    if (topicSettings.colorMode === 'per-topic') {
        $('#per-topic-colors').prop('checked', true);
    } else {
        $('#gradient-range').prop('checked', true);
        $('.color-pickers').show();
    }

    $('#gradient-start-color').val(topicSettings.gradientStartColor);
    $('#gradient-end-color').val(topicSettings.gradientEndColor);

    function populateTopicCountDropdown(topics) {
        const totalTopics = topics.length;
        const $dropdown = $('#topic-count');
        $dropdown.empty();

        for (let i = 1; i <= totalTopics; i++) {
            const $option = $('<option></option>').val(i).text(i);
            if (i === topicSettings.topicCount) {
                $option.prop('selected', true);
            }
            $dropdown.append($option);
        }
    }

    function populateTopicColorGrid(topics) {
        const $grid = $('.key-topic-color-grid');
        const topicCount = parseInt($('#topic-count').val(), 10);
        $grid.empty();

        if (!topics || topics.length === 0) {
            return;
        }
        const requiredTopics = topics.slice(0, topicCount);

        requiredTopics.forEach(function(topic) {
            const $row = $('<div class="topic-setting-per-topic-color-row"></div>');
            const $topicName = $('<div class="topic-setting-per-topic-name"></div>').text(topic.label);
            const $colorPicker = $('<div class="topic-setting-per-topic-color-picker"></div>');
            const $colorInput = $('<input type="color">').val(rgbaToHex(topicColorMap[topic.label] || '#000000'));

            $colorInput.on('change', function() {
                topicColorMap[topic.label] = convertToRGBA($(this).val());
            });

            $colorPicker.append($colorInput);
            $row.append($topicName).append($colorPicker);
            $grid.append($row);
        });
    }


    $('#topic-count').on('change', function() {
        const topicArray = sortedTopicArray();
        if ($('input[name="color-mode"]:checked').val() === 'per-topic') {
            populateTopicColorGrid(topicArray);
            $('.key-topic-color-grid').show();
        }
    });

    $('.key-topics-settings').on('click', function(e) {
        e.stopPropagation();
        $('.key-topic-settings-panel').toggle();
        // set to default position
    $('.key-topic-settings-panel').css('top', '160px');
    $('.key-topic-settings-panel').css('right', '50px');

        const topicArray = sortedTopicArray();
        populateTopicCountDropdown(topicArray);

        if (topicSettings.colorMode === 'per-topic') {
            populateTopicColorGrid(topicArray);
            $('.key-topic-color-grid').show();
        }
    });

    $('input[name="color-mode"]').on('change', function() {
        if ($(this).val() === 'gradient') {
            $('.color-pickers').show();
            $('.key-topic-color-grid').hide();
        } else {
            $('.color-pickers').hide();

            const topicArray = sortedTopicArray();
            populateTopicColorGrid(topicArray);
            $('.key-topic-color-grid').show();
        }
    });

    $('.key-topics-setting-apply-btn').on('click', function() {
        // Update settings
        topicSettings.topicCount = parseInt($('#topic-count').val(), 10) || 10;
        topicSettings.colorMode = $('input[name="color-mode"]:checked').val() || 'per-topic';
        topicSettings.gradientStartColor = $('#gradient-start-color').val();
        topicSettings.gradientEndColor = $('#gradient-end-color').val();
        topicSettings.topicColorMap = topicColorMap;

        $('.key-topic-settings-panel').hide();
        localStorage.setItem(settingsKey, JSON.stringify(topicSettings));

        loadDocumentTopics();
    });

    $('.key-topics-setting-reset-btn').on('click', function() {
        topicSettings.topicCount = defaultTopicSettings.topicCount;
        topicSettings.colorMode = defaultTopicSettings.colorMode;
        topicSettings.gradientStartColor = defaultTopicSettings.gradientStartColor;
        topicSettings.gradientEndColor = defaultTopicSettings.gradientEndColor;
        topicSettings.topicColorMap = defaultTopicSettings.topicColorMap;
        topicColorMap = topicSettings.topicColorMap;
        $('.key-topic-settings-panel').hide();

        localStorage.setItem(settingsKey, JSON.stringify(topicSettings));
        loadDocumentTopics();
    });

    $(document).on('click', function(e) {
        if (!$(e.target).closest('.key-topic-settings-panel').length &&
            !$(e.target).closest('.key-topics-settings').length) {
            $('.key-topic-settings-panel').hide();
        }
    });

    $('.save-topic-setting').on('click', function(e) {
        try {
            const settings = JSON.parse(localStorage.getItem(settingsKey)) || defaultTopicSettings;
            const colorMap = JSON.parse(localStorage.getItem(topicColorMapKey)) || defaultTopicColorMap;
            const documentId = document.getElementsByClassName('reader-container')[0].getAttribute('data-id');

            if (!settings || !colorMap) {
                showMessageModal("Input Error", "No topic settings found in local storage.");
                return;
            }

            const jsonData = JSON.stringify({
                settings: settings,
                colorMap: colorMap,
                documentId: documentId
            }, null, 2);

            const blob = new Blob([jsonData], { type: 'application/json' });
            const url = URL.createObjectURL(blob);

            const a = document.createElement('a');
            a.href = url;
            a.download = 'topic-settings-' + documentId + '-' + new Date().toISOString().replace(/[:.]/g, '-') + '.json';
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            URL.revokeObjectURL(url);
        } catch (err) {
            console.error('Error saving topic settings:', err);
            showMessageModal("Save Error", "An error occurred while trying to save the file.");
        }
    });


    $('.upload-topic-setting').on('click', function(e) {
        const input = document.createElement('input');
        input.type = 'file';
        input.accept = 'application/json';

        input.onchange = function(event) {
            const file = event.target.files[0];
            if (!file) return;

            const reader = new FileReader();
            reader.onload = function(e) {
                try {
                    const data = JSON.parse(e.target.result);
                    const currentDocumentId = document.getElementsByClassName('reader-container')[0].getAttribute('data-id');

                    if (data.documentId !== currentDocumentId) {
                        showMessageModal("Invalid file", "The settings file does not match the current document.");
                        return;
                    }

                    if (data.settings) {
                        localStorage.setItem(settingsKey, JSON.stringify(data.settings));
                    }

                    showMessageModal("Setting Applied", "Settings successfully loaded and applied.");
                    location.reload();

                } catch (err) {
                    console.error('Error reading settings file:', err);
                    showMessageModal("Invalid File", "Could not parse the settings file. Please ensure it is a valid JSON file.");
                }
            };

            reader.readAsText(file);
        };

        input.click();
    });


    document.querySelectorAll('.key-topic-settings-panel').forEach(panel =>
        makeDraggable(panel, 'h4')
    );
}
