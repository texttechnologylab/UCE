
<div class="modal fade" id="sessionExpiredModal" tabindex="-1" role="dialog" aria-hidden="true"
     data-countdown-seconds="30">
    <div class="modal-dialog" role="document">
        <div class="modal-content rounded-0 card-shadow border-0">
            <div class="modal-header">
                <h5 class="modal-title color-prime">
                    <#if languageResource??>
                        ${languageResource.get("sessionExpiredTitle")!"Session expired"}
                    <#else>
                        Session expired
                    </#if>
                </h5>
            </div>
            <div class="modal-body text">
                <p class="mb-2">
                    <#if languageResource??>
                        ${languageResource.get("sessionExpiredBody")!"Your login session ended. Please log in again to continue."}
                    <#else>
                        Your login session ended. Please log in again to continue.
                    </#if>
                </p>
                <p class="mb-0">
                    <#if languageResource??>
                        ${languageResource.get("sessionExpiredCountdownPrefix")!"Redirecting to home in"} <span id="sessionExpiredCountdown">30</span>s.
                    <#else>
                        Redirecting to home in <span id="sessionExpiredCountdown">30</span>s.
                    </#if>
                </p>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-primary rounded-0" id="sessionExpiredReloginBtn">
                    <#if languageResource??>
                        ${languageResource.get("sessionExpiredReloginBtn")!"Log back in"}
                    <#else>
                        Log back in
                    </#if>
                </button>
                <button type="button" class="btn btn-outline-secondary rounded-0" id="sessionExpiredHomeBtn">
                    <#if languageResource??>
                        ${languageResource.get("sessionExpiredHomeBtn")!"Go to home now"}
                    <#else>
                        Go to home now
                    </#if>
                </button>
            </div>
        </div>
    </div>
</div>
