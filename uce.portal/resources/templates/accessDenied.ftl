<div class="w-100 h-100 text-center p-3 bg-lightgray ">
    <p class="font-weight-bold mb-2 text-danger text-center w-100">${languageResource.get("noAccess")}</p>
    <img src="img/logo.png" style="width: 60px"/>
    <p class="text-danger text-center mt-2">
        <#if information??>
            ${information}
        </#if>
    </p>
</div>
