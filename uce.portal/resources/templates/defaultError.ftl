<head>
    <link rel="stylesheet"
          href="https://cdn.jsdelivr.net/npm/bootstrap@4.3.1/dist/css/bootstrap.min.css"
          integrity="sha384-ggOyR0iXCbMQv3Xipma34MD+dH/1fQ784/j6cY/iJTQUOhcWr7x9JvoRxT2MZw1T"
          crossorigin="anonymous">
    <link
            href="https://cdnjs.cloudflare.com/ajax/libs/animate.css/4.1.1/animate.min.css"
            rel="stylesheet">
    <style>
        <#include "css/site.css">
    </style>
</head>

<div class="w-100 h-100 text-center p-3 bg-lightgray ">
    <p class="font-weight-bold mb-2 text-danger text-center w-100">${languageResource.get("unexpectedError")}</p>
    <img src="img/logo.png" style="width: 60px"/>
    <p class="text-danger text-center mt-2">
        <#if information??>
            ${information}
        </#if>
    </p>
</div>
