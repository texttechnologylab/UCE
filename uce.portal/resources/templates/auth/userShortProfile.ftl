<div class="user-short-profile">

    <div class="header w-100">
        <div class="flexed align-items-center w-100 pl-3 pr-3 justify-content-between">
            <h5 class="mb-0 color-prime">${languageResource.get("profile")}</h5>
            <a class="rounded-a" onclick="$(this).closest('.user-short-profile').hide(25)"><i class="fas fa-times"></i></a>
        </div>
    </div>

    <#if uceUser?has_content>
        <div class="content p-3">
            <div class="group-box bg-default position-relative mb-0">
                <a class="user-icon">${uceUser.getAbbreviation()}</a>

                <div class="mt-4">
                    <div class="text-center">
                        <p class="mb-1 text-dark font-italic"><i class="fas fa-user mr-1"></i> ${uceUser.getUsername()}</p>
                        <p class="mb-1 text-dark">${uceUser.getName()}</p>
                        <p class="mb-1 text-dark">${uceUser.getEmail()}</p>
                    </div>
                    <a class="btn btn-secondary rounded-0 mt-2 mb-0"
                       href="${commonConf.getKeyCloakConfiguration().getAuthServerUrl()}/realms/uce/protocol/openid-connect/logout?post_logout_redirect_uri=${commonConf.getKeycloakRedirectUrl()}/logout&client_id=uce-web">
                        Logout
                    </a>
                </div>
            </div>
        </div>
    </#if>

</div>