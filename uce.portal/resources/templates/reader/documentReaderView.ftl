<!DOCTYPE html>
<html lang="en">
<head>
    <link rel="stylesheet" href="css/bootstrap/bootstrap.min.css">
    <link rel="stylesheet" href="css/animate.min.css">
    <link rel="stylesheet" href="leaflet/leaflet.css"/>
    <style>
        <#include "*/css/site.css">
    </style>
    <style>
        <#include "*/css/wiki.css">
    </style>
    <style>
        <#include "*/css/custom-context-menu.css">
    </style>
    <style>
        <#include "*/css/document-reader.css">
    </style>
    <style>
        <#include "*/css/drawflow.css">
    </style>
    <style>
        <#include "*/css/bg-anim.css">
    </style>
    <style>
        <#include "*/css/view-nav.css">
    </style>

    <#assign activeKey = ((activeModeKey!activeMode)!"")?string >
    <#assign activeHandler = (activeModeHandler!"")?string >
    <#assign activeKeyLower = activeKey?lower_case >
    <#assign activeHandlerLower = activeHandler?lower_case >
    <#assign isFeedbackMode = (activeHandler == "document_reader_feedback_view")
        || (activeKey == "document_reader_feedback_view")
        || (activeHandlerLower?contains("feedback"))
        || (activeKeyLower?contains("feedback")) >
    <#assign hasViewModeNav = ((uceConfig.settings.ui.documentReader.showViewModeNav)!true) && renderModes?has_content >

    <script src="js/fontawesome/all.js"></script>
    <script src="leaflet/leaflet.js"></script>
    <script src="js/jquery-3.7.1.min.js"></script>
    <script src="js/popper.js/umd/popper.min.js"></script>
    <script src="js/bootstrap/bootstrap.min.js"></script>
    <script type="importmap">
        {
          "imports": {
            "three": "//js/three/three.module.js",
            "three/addons/": "//js/examples/jsm/"
          }
        }
    </script>

    <script src="js/marked.min.js"></script>
    <script src="js/utils.js"></script>
    <script src="js/visualization/cdns/chartjs-449.js"></script>
    <script src="js/visualization/cdns/echarts-560.js"></script>
    <script src="js/visualization/cdns/d3js-790.js"></script>
    <script src="js/visualization/cdns/drawflow-last.js"></script>
    <script type="module" src="js/md-block.js"></script>
    <script src="js/markdown-it.min.js"></script>

    <script>
        class MarkdownViewer extends HTMLElement {
            connectedCallback() {
                const raw = this.textContent;
                const md = window.markdownit({ html: true });
                const rendered = md.render(raw);
                this.innerHTML = md.render(rendered);
            }
        }
        customElements.define('markdown-viewer', MarkdownViewer);
    </script>

    <title>${document.getDocumentTitle()}</title>
</head>

<body class="no-cursor<#if isFeedbackMode> feedback-mode</#if><#if hasViewModeNav> has-view-nav</#if>">

    <#include "*/messageModal.ftl">
    <#include "*/sessionExpiredModal.ftl">
    <#include "*/auth/userShortProfile.ftl">

    <#if hasViewModeNav>
        <div class="view-mode-nav-shell">
            <button class="view-mode-toggle" type="button" aria-label="Toggle Views">
                <i class="fas fa-sliders-h"></i>
            </button>
            <div class="view-nav-backdrop" aria-hidden="true"></div>
            <nav class="view-mode-nav" aria-label="Views">
                <#list renderModes as mode>
                    <#assign modeKey = mode.key()>
                    <#assign modeName = mode.name()>
                    <#if modeKey == activeKey>
                        <span class="view-mode-link active is-current" aria-current="page">${modeName}</span>
                    <#else>
                        <a class="view-mode-link" href="/documentReader?id=${document.id}&mode=${modeKey?url}">
                            ${modeName}
                        </a>
                    </#if>
                </#list>
            </nav>
        </div>
    </#if>

    <#if (uceConfig.settings.ui.mainPage.showWikiModal)!true>
        <#include "*/wiki/components/wikiPageModal.ftl">
    </#if>

    <div class="corpus-inspector-include display-none"></div>
    <div id="prime-color-container" class="color-prime"></div>

    <div class="site-container reader-shell-container">
        <div class="pages-loader-popup">
            <div class="flexed align-items-center justify-content-center h-100 w-100">
                <p class="mb-0 text">
                    <i class="rotate fas fa-circle-notch mr-1"></i> ${languageResource.get("loadingPages")}
                    <span class="color-prime loaded-pages-count">0</span>/${document.getPages()?size}
                </p>
            </div>
        </div>

        <#if (uceConfig.settings.ui.documentReader.showCustomContextMenu)!true>
            <div class="dot" id="custom-cursor"></div>
            <ul class='custom-menu'>
                <li data-action="open-more"><i class="fab fa-readme mr-2"></i> ${languageResource.get("more")}</li>
                <li data-action="highlight" data-target=""><i class="fas fa-highlighter mr-2"></i> ${languageResource.get("highlight")}</li>
            </ul>
        </#if>

        <div class="container-fluid">
            <div class="reader-shell-row flexed m-0 p-0">
                <div class="reader-main w-100">
                    <div class="reader-view-slot">
                        <#if middlePaneTemplate??>
                            <#attempt>
                                <#include "*/" + middlePaneTemplate>
                            <#recover>
                                <#include "*/reader/modes/defaultMiddlePane.ftl">
                            </#attempt>
                        <#else>
                            <#include "*/reader/modes/defaultMiddlePane.ftl">
                        </#if>

                        <#if hasRightPane?? && hasRightPane && rightPaneTemplate??>
                            <aside class="render-mode-side">
                                <#include "*/" + rightPaneTemplate>
                            </aside>
                        </#if>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <#if (uceConfig.settings.ui.documentReader.showVisualizationTab)!true>
        <script type="module">
            <#include "*/js/graphViz.js">
            <#include "*/js/flowViz.js">
        </script>
    </#if>

    <script>
        <#include "*/js/site.js">
        <#include "*/js/documentReader.js">
        <#if (uceConfig.settings.ui.documentReader.showCustomContextMenu)!true>
            <#include "*/js/customContextMenu.js">
        </#if>
        <#include "*/js/feedbackView.js">
    </script>

    <script>
        (function () {
            const body = document.body;
            const nav = document.querySelector('.view-mode-nav');
            const toggle = document.querySelector('.view-mode-toggle');
            const backdrop = document.querySelector('.view-nav-backdrop');
            const main = document.querySelector('.reader-main');
            if (!nav || !toggle || !main) return;

            const VIEW_NAV_RESIZE_DEBOUNCE_MS = 180;
            const VIEW_NAV_LAYOUT_COOLDOWN_MS = 360;
            const VIEW_NAV_COLLAPSE_WIDTH = 980;
            const VIEW_NAV_EXPAND_WIDTH = 1080;
            const VIEW_NAV_OPEN_STABLE_DELAY_MS = 700;

            let viewNavResizeTimer = null;
            let viewNavPendingModeTimer = null;
            let viewNavPendingMode = null;
            let viewNavAutoLayoutLockedUntil = 0;
            let viewNavTogglePosTimer = null;

            const setOpen = (isOpen) => {
                nav.classList.toggle('is-open', !!isOpen);
                body.classList.toggle('view-nav-open', !!isOpen);
                refreshViewNavTogglePositionSmooth();
            };
            const close = () => setOpen(false);

            function updateViewNavTogglePosition() {
                if (!body.classList.contains('view-nav-drawer-mode')) {
                    body.style.setProperty('--view-nav-toggle-left', '-1px');
                    body.style.setProperty('--view-nav-toggle-top', '84px');
                    body.style.setProperty('--view-nav-toggle-height', '72px');
                    return;
                }

                if (!nav.classList.contains('is-open')) {
                    body.style.setProperty('--view-nav-toggle-left', '-1px');
                    body.style.setProperty('--view-nav-toggle-top', '84px');
                    body.style.setProperty('--view-nav-toggle-height', '72px');
                    return;
                }

                const navRect = nav.getBoundingClientRect();
                if (!navRect || !Number.isFinite(navRect.right)) return;
                const edgeAlignedLeft = Math.max(0, Math.round(navRect.right));
                const toggleHeight = Math.max(40, Math.round(navRect.height));
                const topAligned = Math.round(navRect.top);
                const clampedTop = Math.max(16, Math.min(window.innerHeight - toggleHeight - 16, topAligned));
                body.style.setProperty('--view-nav-toggle-left', edgeAlignedLeft + 'px');
                body.style.setProperty('--view-nav-toggle-top', clampedTop + 'px');
                body.style.setProperty('--view-nav-toggle-height', toggleHeight + 'px');
            }

            function refreshViewNavTogglePositionSmooth() {
                updateViewNavTogglePosition();
                requestAnimationFrame(updateViewNavTogglePosition);
                window.setTimeout(updateViewNavTogglePosition, 90);
                window.setTimeout(updateViewNavTogglePosition, 220);
                if (viewNavTogglePosTimer) window.clearTimeout(viewNavTogglePosTimer);
                viewNavTogglePosTimer = window.setTimeout(updateViewNavTogglePosition, 420);
            }

            function computeShouldUseViewNavDrawer() {
                const currentModeIsDrawer = body.classList.contains('view-nav-drawer-mode');
                const mainRect = main.getBoundingClientRect();
                const navRect = nav.getBoundingClientRect();
                const geometricOverlap = !currentModeIsDrawer && (navRect.right - mainRect.left) > 2;
                const keepDrawerUntilSafe = currentModeIsDrawer && (navRect.right - mainRect.left) > -24;
                const mainWidth = mainRect.width || 0;
                const emergencyNarrow = mainWidth > 0 && mainWidth < VIEW_NAV_COLLAPSE_WIDTH;
                const leaveEmergency = currentModeIsDrawer && mainWidth > 0 && mainWidth < VIEW_NAV_EXPAND_WIDTH;
                return geometricOverlap || keepDrawerUntilSafe || emergencyNarrow || leaveEmergency;
            }

            function updateViewNavMode(options = {}) {
                const force = !!options.force;
                const shouldUseDrawer = computeShouldUseViewNavDrawer();
                const currentModeIsDrawer = body.classList.contains('view-nav-drawer-mode');
                const now = Date.now();

                if (!force && now < viewNavAutoLayoutLockedUntil && shouldUseDrawer !== currentModeIsDrawer) return;

                if (!force && shouldUseDrawer !== currentModeIsDrawer) {
                    if (viewNavPendingMode === shouldUseDrawer) return;
                    if (viewNavPendingModeTimer) window.clearTimeout(viewNavPendingModeTimer);
                    viewNavPendingMode = shouldUseDrawer;

                    const delay = shouldUseDrawer ? 120 : VIEW_NAV_OPEN_STABLE_DELAY_MS;
                    viewNavPendingModeTimer = window.setTimeout(() => {
                        viewNavPendingModeTimer = null;
                        const target = viewNavPendingMode;
                        viewNavPendingMode = null;
                        if (target === computeShouldUseViewNavDrawer()) updateViewNavMode({ force: true });
                    }, delay);
                    return;
                }

                if (viewNavPendingModeTimer) {
                    window.clearTimeout(viewNavPendingModeTimer);
                    viewNavPendingModeTimer = null;
                }
                viewNavPendingMode = null;

                if (shouldUseDrawer !== currentModeIsDrawer) {
                    body.classList.toggle('view-nav-drawer-mode', shouldUseDrawer);
                    close();
                    refreshViewNavTogglePositionSmooth();
                    viewNavAutoLayoutLockedUntil = now + VIEW_NAV_LAYOUT_COOLDOWN_MS;
                    return;
                }

                if (!shouldUseDrawer) close();
                else if (!nav.classList.contains('is-open')) body.classList.remove('view-nav-open');
                refreshViewNavTogglePositionSmooth();
            }

            function scheduleViewNavModeUpdate() {
                if (viewNavResizeTimer) window.clearTimeout(viewNavResizeTimer);
                viewNavResizeTimer = window.setTimeout(() => updateViewNavMode(), VIEW_NAV_RESIZE_DEBOUNCE_MS);
            }

            toggle.addEventListener('click', () => setOpen(!nav.classList.contains('is-open')));
            if (backdrop) backdrop.addEventListener('click', close);
            nav.querySelectorAll('a').forEach(link => link.addEventListener('click', close));
            nav.addEventListener('transitionend', (event) => {
                if (!event || event.propertyName === 'transform') refreshViewNavTogglePositionSmooth();
            });

            window.addEventListener('resize', scheduleViewNavModeUpdate);
            document.querySelectorAll('.view-mode-toggle, .view-nav-backdrop').forEach((el) => {
                el.addEventListener('click', () => {
                    window.setTimeout(() => updateViewNavMode({ force: true }), 50);
                });
            });

            updateViewNavMode({ force: true });
            refreshViewNavTogglePositionSmooth();
        })();
    </script>

</body>
</html>
