
$(document).ready(function () {
    // Read the content language the server has sent from the <html> tag
    let contentLanguage = $('html').attr('lang');
    if(contentLanguage.length > 6) {
        contentLanguage = $('html').attr('lang').split(';')[0].split(',')[1];
    }
    console.log("Sent Content Language: " + contentLanguage);

    // Handle the caching of the language in the browser storage and reload the page should the server have sent wrong language
    const storedLanguage = getLanguage();
    if (storedLanguage !== undefined && storedLanguage != null && storedLanguage !== contentLanguage) {
        location.reload();
    } else {
        setLanguage(contentLanguage);
    }

    // Highlight the correct language select
    const languageSelect = document.querySelector('.switch-language-select');
    if(languageSelect !== null){ // Check if languageSelect is not null
        for(var i = 0; i < languageSelect.options.length; i++){
            const curOption = languageSelect.options[i];
            console.log(curOption);
            if(curOption.getAttribute('data-lang') === contentLanguage){
                languageSelect.options[i].selected = true;
                break;
            }
        }
    }
})

function switchLanguage(language){
    setLanguage(language);
    location.reload();
}

function setLanguage(language) {
    document.cookie = "language=" + language;
}

function getLanguage() {
    if(document.cookie.includes("language=")){
        return document.cookie.split("; ")
            .find((row) => row.startsWith("language="))
            ?.split("=")[1];
    }
    return undefined;
}

$('body').on('change', '.switch-language-select', function(){
    const selectElement = $(this).get(0);
    const selectedOption = selectElement.options[selectElement.selectedIndex];
    switchLanguage($(selectedOption).data('lang'));
})
