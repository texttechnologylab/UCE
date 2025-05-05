<#if intputClaim??>
    <div class="group-box card-shadow bg-light">
        <h5 class="mb-0 mr-1 color-prime">Claim</h5>
        <div class="grow-text">
            <textarea name="claim-text" id="claim-text" rows="10" onInput="this.parentNode.dataset.replicatedValue = this.value">${inputClaim}</textarea>
        </div>
    </div>
    <#else>
        <div class="group-box card-shadow bg-light">
            <h5 class="mb-0 mr-1 color-prime">Claim</h5>
            <div class="grow-text">
                <textarea name="claim-text" id="claim-text" rows="10" placeholder="Claim" onInput="this.parentNode.dataset.replicatedValue = this.value"></textarea>
            </div>
        </div>
</#if>