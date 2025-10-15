<div class="lexicon-entry-list card-shadow row m-0 p-0">
    <#if entries?size == 0>
        <p class="mt-4 mb-4 text-center w-100">${languageResource.get("noEntriesFound")}</p>
    </#if>
    <#list entries as entry>
        <div class="lexicon-entry col-md-6" data-type="${entry.getId().getType()}" data-covered="${entry.getId().getCoveredText()}"
             onclick="window.wikiHandler.handleOccurrencesNavigationClicked($(this));" data-skip="0">
            <div class="flexed align-items-center justify-content-between">
                <div class="flexed align-items-center">
                    <div class="">
                        <p class="font-weight-bold text-dark mb-0 mr-1">${entry.getId().getCoveredText()}</p>
                        <label class="mb-0 mr-2 font-italic text small-font">${entry.getId().getType()}</label>
                    </div>
                </div>
                <div class="flexed align-items-center">
                    <label class="mb-0 mr-2"><i class="text fas fa-pen-nib mr-1"></i> ${entry.getCount()}</label>
                    <!--<a class="w-rounded-btn m-0">
                        <i class="color-prime fas fa-eye"></i>
                    </a>-->
                </div>
            </div>
        </div>
    </#list>
</div>