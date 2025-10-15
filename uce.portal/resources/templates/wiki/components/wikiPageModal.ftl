<style>
    <#include "*/css/kwic.css">
</style>

<!-- a small reading view for expanded metadata -->
<div class="wiki-metadata-expanded-view display-none">
    <div class="content-reader">

        <header class="pl-4 pr-4 pt-3 pb-3 flexed align-items-center justify-content-between">
            <a class="w-rounded-btn m-0 open-wiki-page" data-wid="-" data-wcovered="-" onclick="$(this).closest('.wiki-metadata-expanded-view').fadeOut(25)">
                <i class="color-prime fab fa-wikipedia-w"></i>
            </a>
            <h5 class="mb-0 text-dark title"></h5>
            <a class="w-rounded-btn m-0" onclick="$(this).closest('.wiki-metadata-expanded-view').fadeOut(25)">
                <i class="fas fa-times"></i>
            </a>
        </header>

        <div class="content">

        </div>
    </div>
</div>

<div class="wiki-page-modal wiki-page-modal-minimized">
    <div class="backdrop" onclick="$('.wiki-page-modal').addClass('wiki-page-modal-minimized')"></div>

    <div class="minimized-content" onclick="$('.wiki-page-modal').removeClass('wiki-page-modal-minimized')">
        <h5 class="color-prime mb-0 mt-1">
            <i class="fab fa-wikipedia-w xlarge-font"></i>
        </h5>
    </div>

    <div class="content bg-default">

        <div class="w-header">
            <div class="container w-100 flexed align-items-center justify-content-between">
                <div class="flexed align-items-center">
                    <a class="w-rounded-btn mr-2 go-back-btn">
                        <i class="fas fa-angle-left"></i>
                    </a>
                    <a class="w-rounded-btn mr-2">
                        <i class="fas fa-home"></i>
                    </a>
                </div>
                <h5 class="color-prime mb-0 mr-2">
                    <i class="fab fa-wikipedia-w xlarge-font"><span class="xlarge-font">iki</span></i>
                </h5>
                <div class="flexed align-items-center">
                    <!-- open full -->
                    <a class="w-rounded-btn mr-2" onclick="$('.wiki-page-modal .content').toggleClass('fullscreen')">
                        <i class="fas fa-expand"></i>
                    </a>
                    <!-- close -->
                    <a class="w-rounded-btn" onclick="$('.wiki-page-modal').addClass('wiki-page-modal-minimized')">
                        <i class="fas fa-times"></i>
                    </a>
                </div>
            </div>
        </div>

        <div class="page-content">

            <div class="loading-div">
                <div class="spinner-grow" style="width: 3rem; height: 3rem;" role="status">
                    <span class="sr-only">Loading...</span>
                </div>
            </div>

            <div class="include">
                <p class="text-center mb-0 w-100 text mt-3">
                    ${languageResource.get("wikiDefault")}
                </p>
            </div>
        </div>

    </div>
</div>

<script>
    <#include "*/js/wiki.js">
</script>