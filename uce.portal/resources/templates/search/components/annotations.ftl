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

    <hr/>

    <div class="entry">
        <div class="flexed align-items-center w-100 justify-content-between">
            <p class="mb-0"><i class="fas fa-exclamation"></i> ${languageResource.get("cues")}</p>
            <label class="text mb-0">${searchState.getFoundCues(0, 9999999)?size}</label>
        </div>
    </div>
    <div class="entry">
        <div class="flexed align-items-center w-100 justify-content-between">
            <p class="mb-0"><i class="fas fa-calendar-check"></i> ${languageResource.get("events")}</p>
            <label class="text mb-0">${searchState.getFoundEvents(0, 9999999)?size}</label>
        </div>
    </div>
    <div class="entry">
        <div class="flexed align-items-center w-100 justify-content-between">
            <p class="mb-0"><i class="fas fa-crosshairs"></i> ${languageResource.get("foci")}</p>
            <label class="text mb-0">${searchState.getFoundFoci(0, 9999999)?size}</label>
        </div>
    </div>
    <div class="entry">
        <div class="flexed align-items-center w-100 justify-content-between">
            <p class="mb-0"><i class="fas fa-circle"></i> ${languageResource.get("scopes")}</p>
            <label class="text mb-0">${searchState.getFoundScopes(0, 9999999)?size}</label>
        </div>
    </div>
    <div class="entry">
        <div class="flexed align-items-center w-100 justify-content-between">
            <p class="mb-0"><i class="fas fa-circle-notch"></i> ${languageResource.get("xscopes")}</p>
            <label class="text mb-0">${searchState.getFoundXScopes(0, 9999999)?size}</label>
        </div>
    </div>
</div>
