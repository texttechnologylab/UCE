<!DOCTYPE html>
<html lang="en">
<head>
    <link rel="stylesheet" href="css/bootstrap/bootstrap.min.css">
    <link rel="stylesheet" href="css/animate.min.css">
    <link rel="stylesheet" href="leaflet/leaflet.css"/>
    <style>
        <#include "*/css/site.css">
    </style>
    <script src="js/fontawesome/all.js"></script>
    <script src="js/jquery-3.7.1.min.js"></script>
    <title>${languageResource.get("imprint")}</title>
</head>

<#include "*/sessionExpiredModal.ftl">
<#include "*/auth/userShortProfile.ftl">

<div class="site-container">
    <div class="container pt-5 pb-5">
        <header class="mb-3">
            <div class="flexed align-items-center">
                <a class="rounded-a" href="/"><i class="fas fa-chevron-left"></i></a>
                <h5 class="mb-0 ml-2 font-weight-bold color-prime">UCE</h5>
            </div>
        </header>
        <div class="imprint bg-ghost group-box card-shadow">
            <#if imprint?has_content>
                ${imprint}
            <#else>
                <p>No imprint was set.</p>
            </#if>
        </div>
    </div>
</div>
