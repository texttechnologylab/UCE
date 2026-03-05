<#-- Spec-driven feedback view. Expects middlePaneModel.metadata (map), middlePaneModel.images (list),
     and middlePaneModel.effectivePermission (optional). -->

<style>
    <#include "*/css/feedback.css">
</style>

<#assign md = middlePaneModel.metadata!{} >

<div class="feedback-main">
    <div class="feedback-header">
        <h3>${(middlePaneModel.documentTitle!md.document_title!md.documentTitle!"")?replace("\\s*\\(Erhebung:.*\\)\\s*$", "", "r")}</h1>
        <p class="subtitle">${md.assessment_phase_name!""} • ${md.assessment_name!""}</p>
        <div class="badges">
            <span class="badge">
                <span class="label">Teilnehmer</span>
                <span class="value">${md.participant_count!0}</span>
            </span>
            <span class="badge">
                <span class="label">Dokument</span>
                <span class="value">${middlePaneModel.documentId!""}</span>
            </span>
            <span class="badge">
                <span class="label">User-Hash</span>
                <span class="value">${md.user_hash!""}</span>
            </span>
            <#if middlePaneModel.effectivePermission??>
                <#assign effectivePermission = middlePaneModel.effectivePermission>
                <#include "../permissionBadge.ftl">
            </#if>
        </div>
    </div>

    <#-- Overview cards (same shape as old template, but values are read directly from metadata) -->
    <section class="feedback-overview">
        <#assign overviewCards = [
            {"title":"Seiten gesamt", "value":md.pages_count!0, "min":md.pages_all_min!0, "max":md.pages_all_max!0, "mean":md.pages_all_mean!0, "stdDev":md.pages_all_std!0, "diffPercent":md.pages_percentage_diff!0},
            {"title":"Seiten einzigartig", "value":md.pages_count_unique!0, "min":md.pages_all_min_unique!0, "max":md.pages_all_max_unique!0, "mean":md.pages_all_mean_unique!0, "stdDev":md.pages_all_std_unique!0, "diffPercent":md.pages_percentage_diff_unique!0},
            {"title":"Bearbeitungszeit", "value":md.time_count!0, "min":md.time_all_min!0, "max":md.time_all_max!0, "mean":md.time_all_mean!0, "stdDev":md.time_all_std!0, "diffPercent":md.time_percentage_diff!0}
        ] >
        <#list overviewCards as card>
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

    <#-- Top URLs are expected as md.top_urls = [{rank,url,...}, ...] -->
    <#if md.top_urls?has_content>
    <section class="feedback-urls">
    <h2>Top URLs</h2>
    <table class="top-urls">
    <colgroup>
        <col style="width: 5rem;">
        <col style="width: 6rem;">
        <col>
    </colgroup>
    <thead>
        <tr>
        <th>Rank</th>
        <th>Hits</th>
        <th>URL</th>
        </tr>
    </thead>
    <tbody>
        <#list md.top_urls as row>
        <tr>
            <td>${row.rank!""}</td>
            <td>${row.hits!""}</td>
            <td class="url-cell">
            <a class="url-ellipsis" href="${row.url}" target="_blank" title="${row.url}">${row.url}</a>
            </td>
        </tr>
        </#list>
    </tbody>
    </table>

    </section>
    </#if>


    <#-- Images are passed as middlePaneModel.images = [{htmlImgSrc, mimeType, ...}, ...] -->
    <#if middlePaneModel.images?has_content>
    <section  class="feedback-urls">
        <h2>Auswertung </h2>
        <#list middlePaneModel.images as img>
            <article class="chart-card">
                <div class="chart-viewport">
                    <img src="${img.htmlImgSrc}" alt="Diagramm" class="feedback-chart-img"/>
                </div>
            </article>
        </#list>
    </section>
    </#if>
</div>

