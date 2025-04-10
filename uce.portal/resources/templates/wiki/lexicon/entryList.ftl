<div class="lexicon-entry-list card-shadow row m-0 p-0">
    <#list entries as entry>
        <div class="lexicon-entry col-md-6" data-type="${entry.getId().getType()}">
            <div class="flexed align-items-center justify-content-between">
                <div class="flexed align-items-center">
                    <div class="">
                        <h5 class="font-weight-bold color-secondary mb-0 mr-1">${entry.getId().getCoveredText()}</h5>
                        <label class="mb-0 mr-2 font-italic text small-font">${entry.getId().getType()}</label>
                    </div>
                </div>
                <div class="flexed align-items-center">
                    <label class="mb-0 mr-2"><i class="text fas fa-pen-nib mr-1"></i> ${entry.getCount()}</label>
                    <a class="w-rounded-btn m-0"><i class="color-prime fas fa-angle-down"></i></a>
                </div>
            </div>
        </div>
    </#list>
</div>