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

        <div class="include">
        </div>

    </div>
</div>