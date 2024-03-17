<div>
    <div class="entry">
        <div class="flexed align-items-center w-100 justify-content-between">
            <p class="mb-0"><i class="fas fa-map-marker-alt mr-1"></i> Orte</p>
            <label class="text mb-0">${searchState.getNamedEntitiesByType("LOCATION")?size}</label>
        </div>
    </div>

    <div class="entry">
        <div class="flexed align-items-center w-100 justify-content-between">
            <p class="mb-0"><span><i class="fas fa-user-tag mr-1"></i> Personen</p>
            <label class="text mb-0">${searchState.getNamedEntitiesByType("PERSON")?size}</label>
        </div>
    </div>

    <div class="entry">
        <div class="flexed align-items-center w-100 justify-content-between">
            <p class="mb-0"><i class="fas fa-sitemap mr-1"></i> Organisationen</p>
            <label class="text mb-0">${searchState.getNamedEntitiesByType("ORGANIZATION")?size}</label>
        </div>
    </div>

    <div class="entry">
        <div class="flexed align-items-center w-100 justify-content-between">
            <p class="mb-0"><i class="fas fa-th mr-1"></i> Sonstiges</p>
            <label class="text mb-0">${searchState.getNamedEntitiesByType("MISC")?size}</label>
        </div>
    </div>

    <hr/>

    <div class="entry">
        <div class="flexed align-items-center w-100 justify-content-between">
            <p class="mb-0"><i class="fas fa-tenge mr-1"></i> Taxone</p>
            <label class="text mb-0">${searchState.getFoundTaxons()?size}</label>
        </div>
    </div>
    <div class="entry">
        <div class="flexed align-items-center w-100 justify-content-between">
            <p class="mb-0"><i class="fas fa-clock mr-1"></i> Zeiten</p>
            <label class="text mb-0">${searchState.getFoundTimes()?size}</label>
        </div>
    </div>
</div>
