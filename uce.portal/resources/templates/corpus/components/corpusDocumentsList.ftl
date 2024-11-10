<head>
    <style>
        <#include "*/css/corpus-documents-list.css">
    </style>
</head>

<div class="corpus-documents-list">
    <div class="m-0 p-0 documents-include">
        <#include '*/corpus/components/documents.ftl' >
    </div>

    <div class="mt-3 w-100 h-100 flexed align-items-center justify-content-center">
        <button class="btn load-more-documents-btn clickable color-prime" data-page="2"
                data-corpusid="${corpusId}">
            ${languageResource.get("loadMore")}
        </button>
    </div>
</div>

<script>
    /**
     * Reload more documents endlessly if the user wants.
     */
    $('body').on('click', '.corpus-documents-list .load-more-documents-btn', function () {
        const $btn = $(this);
        let page = $btn.attr('data-page');
        const corpusId = $btn.data('corpusid');
        const ogContent = $btn.html();
        $btn.html('${languageResource.get("loading")}');

        $.ajax({
            url: "/api/corpus/documentsList?corpusId=" + corpusId + "&page=" + page,
            type: "GET",
            success: function (response) {
                $('.corpus-documents-list .documents-include').append(response);
                page = parseInt(page) + 1;
                console.log(page);
                $btn.attr('data-page', page);
            },
            error: function (xhr, status, error) {
                console.error(xhr.responseText);
                $('.corpus-documents-list .documents-include').html(xhr.responseText);
            },
            complete: function () {
                $btn.html(ogContent);
            }
        });
    });
</script>

