<div>
    <div class="entry">
        <div class="flexed align-items-center w-100 justify-content-between">
            <p class="mb-0"><i class="fas fa-map-marker-alt mr-1"></i> ${languageResource.get("locations")}</p>
            <label class="text mb-0">${searchState.getNamedEntitiesByType("LOCATION", 0, 9999999)?size}</label>
        </div>
    </div>

    <div class="entry">
        <div class="flexed align-items-center w-100 justify-content-between">
            <p class="mb-0"><span><i class="fas fa-user-tag mr-1"></i> ${languageResource.get("people")}</p>
            <label class="text mb-0">${searchState.getNamedEntitiesByType("PERSON", 0, 9999999)?size}</label>
        </div>
    </div>

    <div class="entry">
        <div class="flexed align-items-center w-100 justify-content-between">
            <p class="mb-0"><i class="fas fa-sitemap mr-1"></i> ${languageResource.get("organisations")}</p>
            <label class="text mb-0">${searchState.getNamedEntitiesByType("ORGANIZATION", 0, 9999999)?size}</label>
        </div>
    </div>

    <div class="entry">
        <div class="flexed align-items-center w-100 justify-content-between">
            <p class="mb-0"><i class="fas fa-th mr-1"></i> ${languageResource.get("misc")}</p>
            <label class="text mb-0">${searchState.getNamedEntitiesByType("MISC", 0, 9999999)?size}</label>
        </div>
    </div>

    <hr/>

    <div class="entry">
        <div class="flexed align-items-center w-100 justify-content-between">
            <p class="mb-0"><i class="fas fa-tenge mr-1"></i> ${languageResource.get("taxonomy")}</p>
            <label class="text mb-0">${searchState.getFoundTaxons(0, 9999999)?size}</label>
        </div>
    </div>
    <div class="entry">
        <div class="flexed align-items-center w-100 justify-content-between">
            <p class="mb-0"><i class="fas fa-clock mr-1"></i> ${languageResource.get("times")}</p>
            <label class="text mb-0">${searchState.getFoundTimes(0, 9999999)?size}</label>
        </div>
    </div>
    <div class="entry">
        <div class="flexed align-items-center w-100 justify-content-between">
            <p class="mb-0"><i class="fa-regular fa-bolt"></i> ${languageResource.get("cue")}</p>
            <label class="text mb-0">${searchState.getFoundCue(0, 9999999)?size}</label>
        </div>
    </div>
    <div class="entry">
        <div class="flexed align-items-center w-100 justify-content-between">
            <p class="mb-0"><i class="fa-regular fa-bolt"></i> ${languageResource.get("event")}</p>
            <label class="text mb-0">${searchState.getFoundEvent(0, 9999999)?size}</label>
        </div>
    </div>
    <div class="entry">
        <div class="flexed align-items-center w-100 justify-content-between">
            <p class="mb-0"><i class="fa-regular fa-bolt"></i> ${languageResource.get("focus")}</p>
            <label class="text mb-0">${searchState.getFoundFocus(0, 9999999)?size}</label>
        </div>
    </div>
    <div class="entry">
        <div class="flexed align-items-center w-100 justify-content-between">
            <p class="mb-0"><i class="fa-regular fa-bolt"></i> ${languageResource.get("scope")}</p>
            <label class="text mb-0">${searchState.getFoundScope(0, 9999999)?size}</label>
        </div>
    </div>
    <div class="entry">
        <div class="flexed align-items-center w-100 justify-content-between">
            <p class="mb-0"><i class="fa-regular fa-bolt"></i> ${languageResource.get("xscope")}</p>
            <label class="text mb-0">${searchState.getFoundXscope(0, 9999999)?size}</label>
        </div>
    </div>
</div>
