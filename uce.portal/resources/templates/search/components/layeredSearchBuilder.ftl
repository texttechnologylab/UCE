<div class="layered-search-builder-container">

    <div class="header p-3 w-100">
        <div class="flexed align-items-center justify-content-between">
            <h5 class="text mb-0"><i class="fas fa-layer-group mr-2 color-prime"></i> Layered Search Builder</h5>
            <a class="w-rounded-btn" onclick="$(this).closest('.layered-search-builder-include').fadeOut(50)"><i class="fas fa-times"></i></a>
        </div>
    </div>

    <!-- contains the layer templates we later just copy -->
    <div class="slot-templates display-none">

        <div class="template-TAXON slot" data-id="-" data-type="TAXON">
            <div class="flexed mb-2 align-items-center justify-content-between">
                <label class="mb-0 w-100 text ml-1 mr-1"><i class="fas fa-tenge mr-2"></i>Taxon Filter</label>
                <a class="rounded-a bg-light light-border rounded-0 delete-slot-btn"><i class="small-font fas fa-trash-alt"></i></a>
            </div>
            <div class="w-100 flexed align-items-center">
                <input class="form-control rounded-0 w-100 slot-value" placeholder="Lepidoptera..."/>
            </div>
        </div>

        <div class="template-TIME slot" data-id="-" data-type="TIME">
            <div class="flexed mb-2 align-items-center justify-content-between">
                <label class="mb-0 w-100 text ml-1 mr-1"><i class="fas fa-clock mr-2"></i>Time Filter</label>
                <a class="rounded-a bg-light light-border rounded-0 delete-slot-btn"><i class="small-font fas fa-trash-alt"></i></a>
            </div>
            <div class="w-100 flexed align-items-center">
                <input class="form-control rounded-0 w-100 slot-value" placeholder="1890-1920"/>
            </div>
        </div>

        <div class="template-LOCATION slot" data-id="-" data-type="LOCATION">
            <div class="flexed mb-2 align-items-center justify-content-between">
                <label class="mb-0 w-100 text ml-1 mr-1"><i class="fas fa-map-marker-alt mr-2"></i>Location Filter</label>
                <a class="rounded-a bg-light light-border rounded-0 delete-slot-btn"><i class="small-font fas fa-trash-alt"></i></a>
            </div>
            <div class="w-100 flexed align-items-center">
                <input class="form-control rounded-0 w-100 slot-value" placeholder="Wien"/>
                <button class="btn btn-primary rounded-0" onclick="$(this).parent().next().toggle()"><i class="fas fa-map-marked-alt"></i></button>
            </div>
            <!-- leafmap map for location choosing -->
            <div class="location-map">
                <div class="map">

                </div>
            </div>
        </div>

    </div>

    <!-- this is the template for an empty new layer -->
    <div class="layer-template display-none">
        <div class="layer-container position-relative mb-4" data-depth="1">
            <div class="w-100 flexed align-items-center pl-4 mb-1 justify-content-between">
                <h6 class="text w-100 mb-0"><i class="color-prime fas fa-layer-group mr-2"></i> Layer <span
                            class="depth-label"></span></h6>
                <a class="rounded-a delete-layer-btn"><i class="small-font fas fa-trash-alt"></i></a>
            </div>

            <!-- loader -->
            <div class="load hidden">
                <div class="loading-div">
                    <div class="spinner-grow" style="width: 3rem; height: 3rem;" role="status">
                        <span class="sr-only">Loading...</span>
                    </div>
                </div>
            </div>

            <!-- actual layer with the slots -->
            <div class="layer">
                <div class="empty-slot">
                    <!-- add new layer div -->
                    <div class="w-100">
                        <div class="flexed justify-content-center">
                            <div class="position-relative text-center justify-items-center">
                                <button class="btn rounded-a" onclick="$(this).next('.choose-layer-popup').toggle(150)">
                                    <i class="fas fa-plus color-prime"></i>
                                </button>
                                <!-- here we list the possible filters -->
                                <div class="choose-layer-popup display-none">
                                    <div class="flexed align-items-center">
                                        <a class="rounded-a mt-0 mr-1 ml-1" data-type="TAXON"><i
                                                    class="fas fa-tenge"></i></a>
                                        <a class="rounded-a mt-0 ml-1 mr-1" data-type="TIME"><i
                                                    class="fas fa-clock"></i></a>
                                        <a class="rounded-a mt-0 ml-1 mr-1" data-type="LOCATION"><i
                                                    class="fas fa-map-marker-alt"></i></a>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- the status and metadata of this layer -->
            <div class="layer-metadata-container">
                <div class="w-100 flexed align-items-center justify-content-between">
                    <div class="w-100 flexed justify-content-around align-items-center">
                        <label class="mb-0 mr-2 text"><i class="fas fa-book mr-1"></i> Hits: <span
                                    class="document-hits color-prime">?</span></label>
                        <label class="mb-0 mr-2 text"><i class="fas fa-file-alt mr-1"></i> Hits: <span
                                    class="page-hits color-prime">?</span></label>
                    </div>
                    <button class="btn apply-layer-btn"><i class="fas fa-check"></i></button>
                </div>
            </div>
        </div>
    </div>

    <!-- Here we build all the different layers into -->
    <div class="content pl-4 pb-4 pr-4 pt-0">

        <div class="layers-container pt-4">


        </div>

        <div class="w-100 mt-3 flexed justify-content-center pl-3">
            <button class="btn btn-primary add-new-layer-btn">
                <i class="fas fa-plus"></i>
            </button>
        </div>

        <div class="w-100 mt-5 bg-lightgray submit-div">
            <div class="w-100 flexed align-items-center justify-content-between">
                <div class="flexed align-items-center mr-2">
                    <a class="activated" data-submit="false"><i class="text-light xlarge-font fas fa-times"></i></a>
                    <label class="mb-0 ml-3">Turn Off</label>
                </div>
                <div class="flexed align-items-center ml-2">
                    <label class="mb-0 mr-3">Apply for Search</label>
                    <a class="" data-submit="true"><i class="text-light xlarge-font fas fa-check-double"></i></a>
                </div>
            </div>
        </div>

    </div>

</div>