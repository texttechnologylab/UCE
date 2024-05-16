<script>
    <#include "*/js/annotationsModal.js">
</script>

<div id="found-annotations-modal" style="display: none">
    <div class="mcontent">
        <div class="mheader">
            <h5 class="mb-0 text"><i class="color-prime fas fa-x-ray mr-2"></i>Annotations</h5>
            <a class="btn" onclick="$('#found-annotations-modal').fadeOut(150)"><i
                        class="color-prime fas fa-times"></i></a>
        </div>
        <div class="mtabs">
            <btn class="selected-tab ne-LOCATION" data-id="LOCATION">${languageResource.get("locations")}</btn>
            <btn class="ne-PERSON" data-id="PERSON">${languageResource.get("people")}</btn>
            <btn class="ne-ORGANIZATION" data-id="ORGANIZATION">${languageResource.get("organisations")}</btn>
            <btn class="ne-MISC" data-id="MISC">${languageResource.get("misc")}</btn>
            <btn class="ne-taxons" data-id="taxons">${languageResource.get("taxonomy")}</btn>
            <btn data-id="times">${languageResource.get("times")}</btn>
        </div>
        <div class="views drag-container">
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

        <div class="mfooter">

            <div class="flexed w-100 mt-2">
                <div class="w-100">
                    <div class="w-100 flexed align-items-center justify-content-center">
                        <span class="w-100 position-relative verb-input-before"></span>
                        <input placeholder="Verb..." class="form-control w-100 verb-input" type="text"/>
                        <span class="w-100 position-relative verb-input-before"></span>
                    </div>

                    <div class="w-100 flexed align-items-center">

                        <div class="row m-0 p-0 bricks-container">

                            <div class="col-md-4 pl-2 pr-2">
                                <div data-id="arg0" class=" w-100 drop-container">
                                </div>
                            </div>

                            <div class="col-md-4 pl-2 pr-2">
                                <div data-id="arg1" class=" w-100 drop-container">
                                </div>
                            </div>

                            <div class="col-md-4 pl-2 pr-2">
                                <div data-id="argm" class=" w-100 drop-container">
                                </div>
                            </div>

                        </div>
                    </div>
                </div>
                <button class="submit-btn">
                    <i class="fas fa-search"></i>
                </button>
            </div>

        </div>
    </div>
</div>