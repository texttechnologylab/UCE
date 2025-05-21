<#if inputText??>
    <div class="group-box card-shadow bg-light">
        <h5 class="mb-0 mr-1 color-prime">Text: Request ${historyID}</h5>
        <div class="analysis-text-view">
            <div class="grow-text">
                <textarea name="analysis-text" id="analysis-text" rows="10" onInput="this.parentNode.dataset.replicatedValue = this.value" readonly>${inputText}
                </textarea>
            </div>
        </div>
        <#if inputClaim?? && inputClaim?has_content>
            <h5 class="mb-0 mr-1 color-prime">Claim</h5>
            <div class="analysis-text-view">
                <div class="grow-text">
                    <textarea name="analysis-text" id="analysis-text" rows="10" onInput="this.parentNode.dataset.replicatedValue = this.value" readonly>${inputClaim}
                    </textarea>
                </div>
            </div>
        </#if>
        <#if inputCoherence?? && inputCoherence?has_content>
            <h5 class="mb-0 mr-1 color-prime">Cohesion</h5>
            <div class="analysis-text-view">
                <div class="grow-text">
                    <textarea name="analysis-text" id="analysis-text" rows="10" onInput="this.parentNode.dataset.replicatedValue = this.value" readonly>${inputCoherence}
                    </textarea>
                </div>
            </div>
        </#if>
        <#if inputStance?? && inputStance?has_content>
            <h5 class="mb-0 mr-1 color-prime">Hypothesis</h5>
            <div class="analysis-text-view">
                <div class="grow-text">
                    <textarea name="analysis-text" id="analysis-text" rows="10" onInput="this.parentNode.dataset.replicatedValue = this.value" readonly>${inputStance}
                    </textarea>
                </div>
            </div>
        </#if>
    </div>
</#if>
