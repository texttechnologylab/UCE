<style>
    <#include "*/css/loader.css"/>
</style>

<div class="loader-container display-none">
    <div style="position:fixed; top:50%; left:50%; transform: translate(-50%, -50%)">
        <div class="loader book">
            <figure class="page"></figure>
            <figure class="page"></figure>
            <figure class="page"></figure>
        </div>
        <h1 class="text color-prime"><i class="fas fa-circle-notch rotate"></i> ${languageResource.get("searchPlaceholder")?replace("...", "")}</h1>
    </div>
</div>