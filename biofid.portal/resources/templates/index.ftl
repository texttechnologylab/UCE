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
    <script src="https://kit.fontawesome.com/b0888ca2eb.js"
            crossorigin="anonymous"></script>
    <style>
        <#include "css/site.css">
        <#include "css/search-redesign.css">
    </style>
    <title>${title}</title>
</head>

<body>

<div class="site-container">

    <nav class="p-3">
        <div class="container-fluid flexed align-items-center justify-content-around">
            <img class="mb-0 logo" src="https://biofid.de/static/images/logo_fid_biodiversity.png">

            <div class="flexed align-items-center">
                <div class="flexed align-items-center nav-buttons">
                    <a class="btn text" data-id="team">Team</a>
                    <a class="btn text selected-nav-btn" data-id="search">Suche</a>
                    <a class="btn text" data-id="contact">Kontakt</a>
                </div>
                <a class="mr-2 text">DE</a>
                <a class="text">GB</a>
            </div>
        </div>
    </nav>

    <div class="mt-5 main-content-container">

        <div class="view container-fluid" data-id="search">

            <div class="flexed align-items-stretch search-header">
                <input type="text" class="search-input form-control large-font" placeholder="Suche..."/>
                <button class="btn btn-primary search-btn">
                    <i class="fas fa-search"></i>
                </button>
            </div>

            <div class="search-result-container">

            </div>

        </div>

        <div class="view display-none" data-id="team">
            Team
        </div>

        <div class="view display-none" data-id="contact">
            Kontakt
        </div>
    </div>

</div>
</body>

<footer>
    <div class="container p-3">
        Footer
    </div>
</footer>

<script src="https://code.jquery.com/jquery-3.3.1.slim.min.js"
        integrity="sha384-q8i/X+965DzO0rT7abK41JStQIAqVgRVzpbzo5smXKp4YfRvH+8abtTE1Pi6jizo"
        crossorigin="anonymous"></script>
<script
        src="https://cdn.jsdelivr.net/npm/popper.js@1.14.7/dist/umd/popper.min.js"
        integrity="sha384-UO2eT0CpHqdSJQ6hJty5KVphtPhzWj9WO1clHTMGa3JDZwrnQq4sF86dIHNDz0W1"
        crossorigin="anonymous"></script>
<script
        src="https://cdn.jsdelivr.net/npm/bootstrap@4.3.1/dist/js/bootstrap.min.js"
        integrity="sha384-JjSmVgyd0p3pXB1rRibZUAYoIIy6OrQ6VrjIEaFf/nJGzIxFDsf4x0xIM+B07jRM"
        crossorigin="anonymous"></script>
<script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>

<script>
    <#include "js/site.js">
    <#include "js/search.js">
</script>

</html>