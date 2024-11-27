
<div class="lemma-tree">
    <h6 class="w-100 text-center color-prime font-weight-bold">
        ${vm.getCoveredText()}
        <span class="text ml-1 small-font text">(${neType})</span>
    </h6>
    <div class="tree">
        <#list vm.getLemmas() as lemma>
            <div class="mb-0 mr-1 ml-1 position-relative pt-1">
                <label data-wid="${lemma.getWikiId()}" data-wcovered="${lemma.getCoveredText()}"
                       class="mb-0 open-wiki-page add-wiki-logo text">
                    ${lemma.getValue()}
                </label>
            </div>
        </#list>
    </div>
</div>