<!DOCTYPE html>
<html lang="en">
<head>
    <link rel="stylesheet"
          href="https://cdn.jsdelivr.net/npm/bootstrap@4.3.1/dist/css/bootstrap.min.css"
          integrity="sha384-ggOyR0iXCbMQv3Xipma34MD+dH/1fQ784/j6cY/iJTQUOhcWr7x9JvoRxT2MZw1T"
          crossorigin="anonymous">
    <link
            href="https://cdnjs.cloudflare.com/ajax/libs/animate.css/4.1.1/animate.min.css"
            rel="stylesheet">
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"
          integrity="sha256-p4NxAoJBhIIN+hmNHrzRCf9tD/miZyoHS5obTRR9BMY="
          crossorigin=""/>
    <style>
        <#include "*/css/site.css">
    </style>
    <script src="https://kit.fontawesome.com/b0888ca2eb.js"
            crossorigin="anonymous"></script>
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <title>${languageResource.get("imprint")}</title>
</head>

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