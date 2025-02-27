<div class="layered-search-builder-container">

    <div class="header p-3 w-100">
        <div class="flexed align-items-center justify-content-between">
            <h5 class="text mb-0"><i class="fas fa-layer-group mr-2 color-prime"></i> Layered Search Builder</h5>
            <a class="w-rounded-btn"><i class="fas fa-times"></i></a>
        </div>
    </div>

    <!-- contains the layer templates we later just copy -->
    <div class="slot-templates display-none">
        <div class="template-TAXON slot" data-id="-" data-type="TAXON">
            <label class="mb-0 w-100 mb-1 text ml-1 mr-1"><i class="fas fa-tenge mr-1"></i>Taxon Filter</label>
            <div class="w-100 flexed align-items-center">
                <input class="form-control rounded-0 w-100 slot-value" />
            </div>
        </div>
    </div>

    <!-- this is the template for an empty new layer -->
    <div class="layer-template display-none">
        <div class="layer-container mb-4">
            <h5 class="text-center text w-100 mb-1"><i class="color-prime fas fa-layer-group mr-2"></i> <span class="depth-label"></span></h5>

            <!-- actual layer with the slots -->
            <div class="layer" data-depth="1">
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
                                        <a class="rounded-a mt-0 ml-1 mr-1" data-type="STRING"><i
                                                    class="fas fa-search-plus"></i></a>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- the status and metadata of this layer -->
            <div class="layer-metadata-container">
                <button class="btn apply-layer-btn"><i class="mr-2 fas fa-database"></i> Apply Layer</button>
            </div>
        </div>
    </div>

    <!-- Here we build all the different layers into -->
    <div class="content pl-4 pb-4 pr-4 pt-0">

        <div class="layers-container pt-4">


        </div>
    </div>

</div>