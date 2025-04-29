<#if inputText??>
    <div class="group-box card-shadow bg-light">
        <h5 class="mb-0 mr-1 color-prime">Text: Request ${historyID}</h5>
        <div class="grow-text">
            <textarea name="analysis-text" id="analysis-text" rows="10" onInput="this.parentNode.dataset.replicatedValue = this.value" readonly>${inputText}</textarea>
        </div>
    </div>
</#if>