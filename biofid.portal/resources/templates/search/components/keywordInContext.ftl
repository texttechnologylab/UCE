<div class="keyword-context-card" data-expanded="true">
    <div class="flexed align-items-center justify-content-between">
        <h6 class="mb-0 text-center"><i class="fas fa-bezier-curve mr-1"></i> Keyword-in-Context</h6>
        <button class="btn expand-keyword-context-btn">
            <i class="color-secondary fas fa-expand m-0"></i>
        </button>
    </div>

    <#if contextState?? && contextState != "">
        <div class="context-table-container">
            <#list contextState.getContexts() as context>
                <div>
                    <#list context.getRight() as keywordsRows>
                        <div class="flexed w-100 align-items-center context-row-container">
                            <div class="w-100 context-row row small-font text">
                                <div class="col-left text-right font-italic">
                                    ${keywordsRows.getBeforeString()}
                                </div>

                                <div class="col-middle text-center font-weight-bold">
                                    ${keywordsRows.getKeyword()}
                                </div>

                                <div class="col-right text font-italic">
                                    ${keywordsRows.getAfterString()}
                                </div>
                            </div>
                            <button class="btn ml-2 pl-2 pr-2 pt-1 pb-1 open-document text"
                                    data-id="${keywordsRows.getDocument_id()?string?replace('.', '')}">
                                <i class="fas fa-external-link-alt"></i>
                            </button>
                        </div>
                    </#list>
                </div>
            </#list>
        </div>
    <#else>
        <p class="text text-center mt-3">${languageResource.get("noKWIC")}</p>
    </#if>

</div>