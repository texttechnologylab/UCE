let currentFocusedPage = 0;
let searchTokens = "";
let currentSelectedTopic = null;
let currentTopicIndex = -1;
let matchingTopics = [];

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

function setupImageZoomOverlay() {
    // Zoom overlay for images
    window.uceDocumentViewerOverlay = document.createElement('div');
    window.uceDocumentViewerOverlay.style.position = 'fixed';
    window.uceDocumentViewerOverlay.style.top = 0;
    window.uceDocumentViewerOverlay.style.left = 0;
    window.uceDocumentViewerOverlay.style.width = '100vw';
    window.uceDocumentViewerOverlay.style.height = '100vh';
    window.uceDocumentViewerOverlay.style.backgroundColor = 'rgba(0,0,0,0.8)';
    window.uceDocumentViewerOverlay.style.display = 'none';
    window.uceDocumentViewerOverlay.style.justifyContent = 'center';
    window.uceDocumentViewerOverlay.style.alignItems = 'center';
    window.uceDocumentViewerOverlay.style.zIndex = 9999;
    window.uceDocumentViewerOverlay.style.cursor = 'zoom-out';
    document.body.appendChild(window.uceDocumentViewerOverlay);

    window.uceDocumentViewerOverlayImg = document.createElement('img');
    window.uceDocumentViewerOverlayImg.style.maxWidth = '95%';
    window.uceDocumentViewerOverlayImg.style.maxHeight = '95%';
    window.uceDocumentViewerOverlay.appendChild(window.uceDocumentViewerOverlayImg);

    window.uceDocumentViewerOverlay.addEventListener('click', () => {
        window.uceDocumentViewerOverlay.style.display = 'none';
    });
}

function imageZoom(img_src) {
    window.uceDocumentViewerOverlayImg.src = img_src;
    window.uceDocumentViewerOverlay.style.display = 'flex';
}

setupImageZoomOverlay();

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

                    // make embedded images usable
                    // NOTE we are doing this in JavaScript as our text cleaning will destroy more "complex" HTML structures
                    // TODO this should be configurable by the image annotation
                    const images = document.querySelectorAll('img.document-reader-embedded-image');
                    images.forEach(img => {
                        // full width, horizontal scrollable container with max height of 500px
                        const wrapper = document.createElement('div');
                        wrapper.style.width = '100%';
                        wrapper.style.maxHeight = '500px';
                        wrapper.style.overflowX = 'auto';
                        wrapper.style.overflowY = 'hidden';
                        wrapper.style.boxSizing = 'border-box';
                        wrapper.style.padding = '8px';
                        wrapper.style.border = '1px solid #ccc';
                        wrapper.style.boxShadow = '0 2px 6px rgba(0, 0, 0, 0.2)';
                        wrapper.style.backgroundColor = '#fff';
                        wrapper.style.display = 'flex';
                        wrapper.style.alignItems = 'flex-start';

                        // also scale the embedded image "responsive" with a maximum of 500px height
                        img.style.maxHeight = '500px';
                        img.style.height = 'auto';
                        img.style.display = 'block';

                        img.parentNode.insertBefore(wrapper, img);
                        wrapper.appendChild(img);

                        // zoomable
                        img.style.cursor = 'zoom-in';
                        img.addEventListener('click', () => {
                            imageZoom(img.src);
                        });
                    });
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
        setTimeout(() => renderTopicEntityChordDiagram('vp-2'), 500);
    }
    if (target === '#viz-panel-3') {
        setTimeout(() => renderSentenceTopicNetwork('vp-3'), 500);
    }
    if (target === '#viz-panel-4') {
        $('.selector-container').hide();
        setTimeout(() => renderTopicSimilarityMatrix('vp-4'), 500);

    }
    if (target === '#viz-panel-5') {
        setTimeout(() => renderSentenceTopicSankey('vp-5'), 500);

    }
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

    const taxonReq = $.get('/api/document/page/taxon', { documentId: docId });
    const topicReq = $.get('/api/document/page/topics', { documentId: docId });
    const entityReq = $.get('/api/document/page/namedEntities', { documentId: docId });
    const lemmaReq = $.get('/api/document/page/lemma', { documentId: docId });
    const geonameReq = $.get('/api/document/page/geoname', { documentId: docId });

    Promise.all([taxonReq, topicReq, entityReq, lemmaReq, geonameReq]).then(([taxon, topics, entities, lemma, geoname]) => {
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
                        Geonames: []
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
