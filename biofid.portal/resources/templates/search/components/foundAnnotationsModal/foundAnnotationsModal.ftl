<div id="found-annotations-modal">
    <div class="mcontent">
        <div class="mheader">
            <h5 class="mb-0 text"><i class="color-prime fas fa-x-ray mr-2"></i>Annotations</h5>
            <a class="btn" onclick="$('#found-annotations-modal').fadeOut(150)"><i
                        class="color-prime fas fa-times"></i></a>
        </div>
        <div class="mtabs">
            <btn class="selected-tab" data-id="LOCATION">${languageResource.get("locations")}</btn>
            <btn data-id="PERSON">${languageResource.get("people")}</btn>
            <btn data-id="ORGANIZATION">${languageResource.get("organisations")}</btn>
            <btn data-id="MISC">${languageResource.get("misc")}</btn>
            <btn data-id="taxons">${languageResource.get("taxonomy")}</btn>
            <btn data-id="times">${languageResource.get("times")}</btn>
        </div>
        <div class="views">
            <div data-id="LOCATION" class="mview">
                <#assign type="LOCATION">
                <#include "*/search/components/foundAnnotationsModal/annotationsTab.ftl" >
            </div>
            <div class="display-none mview" data-id="PERSON" >
                <#assign type = "PERSON">
                <#include "*/search/components/foundAnnotationsModal/annotationsTab.ftl" >
            </div>
            <div class="display-none mview" data-id="ORGANIZATION">
                <#assign type = "ORGANIZATION">
                <#include "*/search/components/foundAnnotationsModal/annotationsTab.ftl" >
            </div>
            <div class="display-none mview" data-id="MISC">
                <#assign type = "MISC">
                <#include "*/search/components/foundAnnotationsModal/annotationsTab.ftl" >
            </div>
            <div class="display-none mview" data-id="taxons">
                <#assign type = "taxons">
                <#include "*/search/components/foundAnnotationsModal/annotationsTab.ftl" >
            </div>
            <div class="display-none mview" data-id="times">
                <#assign type = "times">
                <#include "*/search/components/foundAnnotationsModal/annotationsTab.ftl" >
            </div>
        </div>
    </div>
</div>