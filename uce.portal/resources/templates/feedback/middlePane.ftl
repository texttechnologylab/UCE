<div class="feedback-main">
<section class="feedback-header">
    <div class="header-top">
        <h1>${middlePaneModel.title?replace("\\s*\\(Erhebung:.*\\)\\s*$", "", "r")}</h1>
        <#if ((uceConfig.settings.ui.mainPage.showWikiModal)!true) && document??>
            <a class="header-btn open-wiki-page color-prime"
               data-wtype="D"
               data-wid="${document.getWikiId()}"
               data-wcovered="${middlePaneModel.title!''}">
                <i class="large-font m-0 fab fa-wikipedia-w"></i>
            </a>
        </#if>
    </div>
    <p class="subtitle">${middlePaneModel.subtitle}</p>
    <div class="badges">
        <#list middlePaneModel.metaBadges as badge>
            <span class="badge">
                <span class="label">${badge.label}</span>
                <span class="value">${badge.value}</span>
            </span>
        </#list>
        <#if middlePaneModel.effectivePermission??>
            <#assign effectivePermission = middlePaneModel.effectivePermission>
            <#include "../permissionBadge.ftl">
        </#if>
    </div>
</section>

<section class="feedback-overview">
    <#list middlePaneModel.overviewCards as card>
        <article class="metric-card">
            <header>${card.title}</header>
            <div class="value">${card.value?string["#,##0.##"]}</div>
            <div class="metric-chips">
                <span>Min ${card.min}</span>
                <span>Ø ${card.mean?string["#,##0.##"]}</span>
                <span>Max ${card.max}</span>
                <span>Std ${card.stdDev?string["#,##0.##"]}</span>
            </div>
            <small class="diff">
                Abweichung: ${card.diffPercent?string["+#,##0.##;-#,##0.##"]} %
            </small>
        </article>
    </#list>
</section>

<#if middlePaneModel.topUrls?has_content>
<section class="feedback-urls">
    <h2>Top URLs</h2>
    <table>
        <thead><tr><th>Rank</th><th>URL</th></tr></thead>
        <tbody>
        <#list middlePaneModel.topUrls as url>
            <tr>
                <td>${url.rank}</td>
                <td><a href="${url.url}" target="_blank">${url.url}</a></td>
            </tr>
        </#list>
        </tbody>
    </table>
</section>
</#if>

<section class="feedback-content">
    <#list middlePaneModel.contentCards as card>
        <#if card.type == "section-header">
            <h3>${card.title}</h3>
        <#elseif card.type == "text">
            <article class="text-card">
                <header>
                    <strong>${card.title}</strong>
                    <small>${card.subtitle!""}</small>
                </header>
                <div class="body">${card.body}</div>
            </article>
        <#elseif card.type == "chart">
            <article class="chart-card">
                <header>
                    <strong>${card.title}</strong>
                    <small>${card.subtitle!""}</small>
                </header>
                <#if card.chartType == "image">
                    <div class="chart-viewport">
                        <img src="${card.labels[0]}" alt="${card.title}" class="feedback-chart-img"/>
                    </div>
                <#else>
                    <pre>${card.series?join(", ")}</pre>
                </#if>
            </article>
        <#elseif card.type == "table">
            <article class="table-card">
                <header>
                    <strong>${card.title}</strong>
                    <small>${card.subtitle!""}</small>
                </header>
                <table>
                    <tbody>
                    <#list card.rows as row>
                        <tr>
                            <#list row.cells as cell>
                                <td>${cell}</td>
                            </#list>
                        </tr>
                    </#list>
                    </tbody>
                </table>
            </article>
        </#if>
    </#list>
</section>
</div>
