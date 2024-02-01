/**
 * Handles the clicking onto a navbar button
 */
$('body').on('click', 'nav .nav-buttons a', function(){

    // show the correct vie
    var id = $(this).data('id');
    console.log(id);
    $('.main-content-container .view').each(function(){
        if($(this).data('id') === id){
            $(this).show(150);
        } else{
            $(this).hide();
        }
    })

    // Show the correct button
    $('nav .nav-buttons a').each(function(b) {
       $(this).removeClass('selected-nav-btn');
    });
    $(this).addClass('selected-nav-btn');
})