/**
 * Handles the correct expanding of the keyword in context window
 */
$('body').on('click', '.keyword-context-card .expand-keyword-context-btn', function () {
    let expanded = $('.keyword-context-card').data('expanded');

    if (expanded) {
        const $left = $('.search-state .search-row[data-type="left"]');
        $left.removeClass('col-md-3');
        $left.addClass('col-md-9');

        const $mid = $('.search-state .search-row[data-type="mid"]');
        $mid.hide(0);
    } else{
        const $left = $('.search-state .search-row[data-type="left"]');
        $left.removeClass('col-md-9');
        $left.addClass('col-md-3');

        const $mid = $('.search-state .search-row[data-type="mid"]');
        $mid.show(0);
    }

    $('.keyword-context-card').data('expanded', !expanded);
});