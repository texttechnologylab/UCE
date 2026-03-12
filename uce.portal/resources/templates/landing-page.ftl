<div class="container">

    <!-- uce corporate data -->
    <div class="mt-5 uce-description">
        <div class="flexed align-items-center justify-content-between">
            <h5 class="color-prime mb-0 clickable"
                onclick="$(this).parent().next('.content').toggle(50)">${uceConfig.getMeta().getName()?trim!"-"}</h5>
            <button class="btn" onclick="$(this).parent().next('.content').toggle(50)">
                <i class="fas fa-info-circle color-prime large-font"></i>
            </button>
        </div>
        <div class="content display-none text block-text">
            <hr class="mt-3 mb-3"/>
            ${uceConfig.getMeta().getDescription()!languageResource.get("noCorpusDescription")}
        </div>
    </div>

    <div class="corpora-list">
        <div class="d-flex align-items-center justify-content-center mb-2">
            <h3 class="text-center font-weight-bold text-dark"><i
                        class="color-prime fas fa-database mr-2"></i> ${languageResource.get("corpora")}</h3>
            <#if uceConfig.settings.enablePathImport?? && uceConfig.settings.enablePathImport>
                <button class="btn btn-sm btn-outline-secondary ml-3" data-toggle="modal"
                        data-target="#importCorpusModal">
                    <i class="fas fa-file-import"></i>
                </button>
            </#if>
            <button class="btn btn-sm btn-outline-secondary ml-3" onclick="openUploadForNewCorpora()" title="Upload new Corpus">
                <i class="fas fa-upload"></i> Create Corpora
            </button>
        </div>


        <div class="row m-0 p-0 ">
            <#if corpora?size == 0>
                <div class="group-box mt-2 bg-ghost">
                    <p class="mb-0 text-center w-100 text">${languageResource.get("noCorpora")}</p>
                </div>
            </#if>
            <#list corpora as corpusVm>
                <div class="col-md-12 m-0 p-3">
                    <div class="corpus-card">
                        <!-- header -->
                        <div class="flexed align-items-center justify-content-between">
                            <div>
                                <h5 class="justify-content-start open-corpus-inspector-btn border-0 w-100 mb-2 color-prime clickable"
                                    data-id="${corpusVm.getCorpus().getId()}">
                                    <i class="fas fa-globe mr-2"></i> ${corpusVm.getCorpus().getName()?trim}
                                </h5>
                                <p class="text mb-0 small"><i
                                            class="fas fa-pen-nib mr-1"></i> ${corpusVm.getCorpus().getAuthor()}</p>
                            </div>
                            <div>
                                <a class="btn open-corpus-inspector-btn mb-1" data-trigger="hover"
                                   data-toggle="popover" data-placement="top" data-id="${corpusVm.getCorpus().getId()}"
                                   data-content="${languageResource.get("openCorpus")}">
                                    <i class="fas fa-globe color-prime"></i>
                                </a>
                                <a class="btn light-border flexed clickable align-items-center pl-1 pr-1 mt-1 justify-content-center"
                                   data-trigger="hover"
                                   onclick="$(this).closest('.corpus-card').find('.expanded-content').toggle(75)">
                                    <i class="fas fa-info-circle color-prime"></i>
                                </a>
                                <a class="btn btn-outline-danger flexed clickable align-items-center pl-1 pr-1 mt-1 justify-content-center"
                                   title="Delete this Corpus"
                                   onclick="deleteCorpus(${corpusVm.getCorpus().getId()})">
                                    <i class="fas fa-trash-alt"></i>
                                </a>
                            </div>

                        </div>

                        <div class="expanded-content">
                            <hr class="mt-3 mb-1 "/>

                            <!-- content -->
                            <div class="corpus-description small mb-0 p-3">
                                <#if corpusVm.getCorpusConfig().getDescription()?has_content>
                                    ${corpusVm.getCorpusConfig().getDescription()}
                                <#else>
                                    ${languageResource.get("noCorpusDescription")}
                                </#if>
                            </div>
                        </div>
                    </div>
                </div>
            </#list>
        </div>

        <!-- clal to search -->
        <div class="flexed align-items-center justify-content-center mt-3 pb-4">
            <a class="clickable text mb-0 text small ml-1" onclick="navigateToView('search')">
                <i class="fas fa-search mr-1"></i> ${languageResource.get("callForSearch")}
            </a>
        </div>
    </div>
</div>

<#--Modal for importing files via a path-->
<div class="modal fade" id="importCorpusModal" tabindex="-1" role="dialog" aria-hidden="true">
    <div class="modal-dialog modal-lg" role="document">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title"><i class="fas fa-file-import mr-2"></i> Import Corpus</h5>
                <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                    <span aria-hidden="true">×</span>
                </button>
            </div>
            <div class="modal-body">
                <form id="importCorpusForm">
                    <div class="form-group">
                        <label for="importPath">Source Path (Server-side)</label>
                        <input type="text" class="form-control" id="importPath" name="path"
                               placeholder="/path/to/corpus/folder" required>
                        <small class="form-text text-muted">The folder must contain a <code>corpusConfig.json</code> and
                            an <code>input</code> folder with UIMA files.</small>
                    </div>
                    <div class="form-row">
                        <div class="form-group col-md-6">
                            <label for="numThreads">Number of Threads</label>
                            <input type="number" class="form-control" id="numThreads" name="numThreads" value="1"
                                   min="1" max="16">
                        </div>
                        <div class="form-group col-md-6">
                            <label for="casView">Name of the CAS view to import from (Optional)</label>
                            <input type="text" class="form-control" id="casView" name="casView"
                                   placeholder="If not set, the default view (initial view) is used">
                        </div>
                    </div>
                </form>
                <div id="importResult" class="mt-3"></div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-primary" onclick="submitCorpusImport()">Import</button>
            </div>
        </div>
    </div>
</div>

<#--Modal for uploading files-->
<div class="modal fade" id="uploadCorpusModal" tabindex="-1" role="dialog" aria-hidden="true">
    <div class="modal-dialog modal-lg" role="document">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="uploadModalTitle"><i class="fas fa-file-import mr-2"></i> Upload Corpus</h5>
                <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                    <span aria-hidden="true">×</span>
                </button>
            </div>
            <div class="modal-body">
                <form id="uploadCorpusForm" enctype="multipart/form-data">

                    <input type="hidden" id="uploadAddToExisting" name="addToExistingCorpus" value="false">

                    <div class="form-group">
                        <label for="uploadFiles">Select XMI Files</label>
                        <div class="custom-file">
                            <input type="file" class="custom-file-input" id="uploadFiles" name="files" multiple accept=".xmi,.xml,.gz,application/xml,text/xml" required>
                            <label class="custom-file-label" for="uploadFiles">Choose files...</label>
                        </div>
                        <small class="form-text text-muted mt-2">Select one or more <code>.xmi/.xml/.gz</code> files.</small>
                    </div>
                    <div class="form-group mt-3" id="configUploadGroup">
                        <label for="uploadConfigFile">Upload your CorpusConfig.json (Optional)</label>
                        <div class="custom-file">
                            <input type="file" class="custom-file-input" id="uploadConfigFile" name="configFile" accept=".json,application/json">
                            <label class="custom-file-label" for="uploadConfigFile"> Choose config file...</label>
                        </div>
                        <small class="form-text text-muted mt-2">Upload a CorpusConfig file </small>
                    </div>

                    <hr/>
                    
                    <div class="form-group">
                        <label for="uploadCorpusName">Corpus Name</label>
                        <input type="text" class="form-control" id="uploadCorpusName" name="name" placeholder="My New Corpus" required>
                    </div>
                    <div class="form-row">
                        <div class="form-group col-md-6">
                            <label for="uploadCorpusLanguage">Language</label>
                            <input type="text" class="form-control" id="uploadCorpusLanguage" name="language" placeholder="de-DE" required>
                        </div>
                        <div class="form-group col-md-6">
                            <label for="uploadCorpusAuthor">Author</label>
                            <input type="text" class="form-control" id="uploadCorpusAuthor" name="author" placeholder="John Doe" required>
                        </div>
                    </div>
                    <div class="form-group">
                        <label for="uploadCorpusDescription">Description</label>
                        <textarea class="form-control" id="uploadCorpusDescription" name="description" rows="2" placeholder="Description..."></textarea>
                    </div>
                    
                    <div class="form-group">
                        <label for="uploadImportHash">Import ID (Optional)</label>
                        <input type="text" class="form-control" id="uploadImportHash" name="importHash" placeholder="Enter custom hash or leave empty for auto-generation">
                        <small class="form-text text-muted">If empty, the import ID will be auto generated </small>
                    </div>

                    <div class="form-row border-top pt-3">
                        <div class="form-group col-md-6">
                            <label for="uploadNumThreads">Threads</label>
                            <input type="number" class="form-control" id="uploadNumThreads" name="numThreads" value="1" min="1" max="16">
                        </div>
                        <div class="form-group col-md-6">
                            <label for="uploadCasView">CAS View (Optional)</label>
                            <input type="text" class="form-control" id="uploadCasView" name="casView" placeholder="Default view">
                        </div>
                    </div>
                    <hr/>
                    <div class="mb-2 clickable" data-toggle="collapse" data-target="#advancedSettings" aria-expanded="false" aria-controls="advancedSettings">
                        <h6 class="mb-0 text-primary"><i class="fas fa-cogs mr-2"></i>Annotation & RAG-Service Flags (Click to expand)</h6>
                    </div>

                    <div class="collapse" id="advancedSettings">

                        <h6 class="small font-weight-bold border-bottom pb-1">Annotations</h6>
                        <div class="form-row mb-2">
                            <div class="col-md-3">
                                <div class="custom-control custom-checkbox"><input type="checkbox" class="custom-control-input" id="annoSentence" name="sentence"><label class="custom-control-label small" for="annoSentence">Sentence</label></div>
                                <div class="custom-control custom-checkbox"><input type="checkbox" class="custom-control-input" id="annoLemma" name="lemma"><label class="custom-control-label small" for="annoLemma">Lemma</label></div>
                                <div class="custom-control custom-checkbox"><input type="checkbox" class="custom-control-input" id="annoNE" name="namedEntity"><label class="custom-control-label small" for="annoNE">Named Entity</label></div>
                                <div class="custom-control custom-checkbox"><input type="checkbox" class="custom-control-input" id="annoSentiment" name="sentiment"><label class="custom-control-label small" for="annoSentiment">Sentiment</label></div>
                                <div class="custom-control custom-checkbox"><input type="checkbox" class="custom-control-input" id="annoEmotion" name="emotion"><label class="custom-control-label small" for="annoEmotion">Emotion</label></div>
                                <div class="custom-control custom-checkbox"><input type="checkbox" class="custom-control-input" id="annoTopic" name="topic"><label class="custom-control-label small" for="annoTopic">Topic</label></div>
                            </div>
                            <div class="col-md-3">
                                <div class="custom-control custom-checkbox"><input type="checkbox" class="custom-control-input" id="annoGeo" name="geoNames"><label class="custom-control-label small" for="annoGeo">GeoNames</label></div>
                                <div class="custom-control custom-checkbox"><input type="checkbox" class="custom-control-input" id="annoWiki" name="wikipediaLink"><label class="custom-control-label small" for="annoWiki">Wikipedia Link</label></div>
                                <div class="custom-control custom-checkbox"><input type="checkbox" class="custom-control-input" id="annoImage" name="image"><label class="custom-control-label small" for="annoImage">Image</label></div>
                                <div class="custom-control custom-checkbox"><input type="checkbox" class="custom-control-input" id="annoAnnotatorMeta" name="annotatorMetadata"><label class="custom-control-label small" for="annoAnnotatorMeta">Annotator Meta</label></div>
                                <div class="custom-control custom-checkbox"><input type="checkbox" class="custom-control-input" id="annoUceMeta" name="uceMetadata"><label class="custom-control-label small" for="annoUceMeta">UCE Metadata</label></div>
                                <div class="custom-control custom-checkbox"><input type="checkbox" class="custom-control-input" id="annoLogical" name="logicalLinks"><label class="custom-control-label small" for="annoLogical">Logical Links</label></div>
                            </div>
                            <div class="col-md-3">
                                <div class="custom-control custom-checkbox"><input type="checkbox" class="custom-control-input" id="annoSrLink" name="srLink"><label class="custom-control-label small" for="annoSrLink">Semantic Roles</label></div>
                                <div class="custom-control custom-checkbox"><input type="checkbox" class="custom-control-input" id="annoUnifiedTopic" name="unifiedTopic"><label class="custom-control-label small" for="annoUnifiedTopic">Unified Topic</label></div>
                                <div class="custom-control custom-checkbox"><input type="checkbox" class="custom-control-input" id="annoOCRPage" name="OCRPage"><label class="custom-control-label small" for="annoOCRPage">OCR Page</label></div>
                                <div class="custom-control custom-checkbox"><input type="checkbox" class="custom-control-input" id="annoOCRPara" name="OCRParagraph"><label class="custom-control-label small" for="annoOCRPara">OCR Paragraph</label></div>
                                <div class="custom-control custom-checkbox"><input type="checkbox" class="custom-control-input" id="annoOCRBlock" name="OCRBlock"><label class="custom-control-label small" for="annoOCRBlock">OCR Block</label></div>
                                <div class="custom-control custom-checkbox"><input type="checkbox" class="custom-control-input" id="annoOCRLine" name="OCRLine"><label class="custom-control-label small" for="annoOCRLine">OCR Line</label></div>
                            </div>
                            <div class="col-md-3">
                                <div class="custom-control custom-checkbox"><input type="checkbox" class="custom-control-input" id="annoNegation" name="completeNegation"><label class="custom-control-label small" for="annoNegation">Complete Negation</label></div>
                                <div class="custom-control custom-checkbox"><input type="checkbox" class="custom-control-input" id="annoCue" name="cue"><label class="custom-control-label small" for="annoCue">Cue</label></div>
                                <div class="custom-control custom-checkbox"><input type="checkbox" class="custom-control-input" id="annoEvent" name="event"><label class="custom-control-label small" for="annoEvent">Event</label></div>
                                <div class="custom-control custom-checkbox"><input type="checkbox" class="custom-control-input" id="annoFocus" name="focus"><label class="custom-control-label small" for="annoFocus">Focus</label></div>
                                <div class="custom-control custom-checkbox"><input type="checkbox" class="custom-control-input" id="annoScope" name="scope"><label class="custom-control-label small" for="annoScope">Scope</label></div>
                                <div class="custom-control custom-checkbox"><input type="checkbox" class="custom-control-input" id="annoTime" name="time"><label class="custom-control-label small" for="annoTime">Time</label></div>
                            </div>
                            <div class="col-md-3">
                                <div class="custom-control custom-checkbox"><input type="checkbox" class="custom-control-input" id="annoXScope" name="xscope"><label class="custom-control-label small" for="annoXScope">X-Scope</label></div>
                            </div>
                        </div>

                        <h6 class="small font-weight-bold border-bottom pb-1 mt-3">Taxon Annotations</h6>
                        <div class="form-row mb-2">
                            <div class="col-md-6">
                                <div class="custom-control custom-checkbox">
                                    <input type="checkbox" class="custom-control-input" id="taxonAnnotated" name="taxonAnnotated">
                                    <label class="custom-control-label small" for="taxonAnnotated">Taxon Annotated</label>
                                </div>
                            </div>
                            <div class="col-md-6">
                                <div class="custom-control custom-checkbox">
                                    <input type="checkbox" class="custom-control-input" id="taxonBiofid" name="biofidOnthologyAnnotated">
                                    <label class="custom-control-label small" for="taxonBiofid">BioFID Ontology</label>
                                </div>
                            </div>
                        </div>

                        <h6 class="small font-weight-bold border-bottom pb-1 mt-3">Other</h6>
                        <div class="form-row">
                            <div class="col-md-6">
                                <div class="custom-control custom-checkbox">
                                    <input type="checkbox" class="custom-control-input" id="otherEmbeddings" name="enableEmbeddings">
                                    <label class="custom-control-label small" for="otherEmbeddings">Enable Embeddings</label>
                                </div>
                                <div class="custom-control custom-checkbox">
                                    <input type="checkbox" class="custom-control-input" id="otherRAG" name="enableRAGBot">
                                    <label class="custom-control-label small" for="otherRAG">Enable RAGBot</label>
                                </div>
                                <div class="custom-control custom-checkbox">
                                    <input type="checkbox" class="custom-control-input" id="otherGoethe" name="availableOnFrankfurtUniversityCollection">
                                    <label class="custom-control-label small" for="otherGoethe">Frankfurt Univ. Collection</label>
                                </div>
                            </div>
                            <div class="col-md-6">
                                <div class="custom-control custom-checkbox">
                                    <input type="checkbox" class="custom-control-input" id="otherKeywords" name="includeKeywordDistribution">
                                    <label class="custom-control-label small" for="otherKeywords">Keyword Distribution</label>
                                </div>
                                <div class="custom-control custom-checkbox">
                                    <input type="checkbox" class="custom-control-input" id="otherS3" name="enableS3Storage">
                                    <label class="custom-control-label small" for="otherS3">Enable S3 Storage</label>
                                </div>
                            </div>
                        </div>
                    </div>
                </form>
                <div id="uploadResult" class="mt-3"></div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-primary" onclick="submitCorpusUpload()">Import</button>
            </div>
        </div>
    </div>
</div>

<script>
    function submitCorpusImport() {
        const form = document.getElementById('importCorpusForm');
        const formData = new FormData(form);
        const resultDiv = document.getElementById('importResult');
        resultDiv.innerHTML = '<div class="spinner-border text-primary" role="status">' +
            '<span class="sr-only">Loading...</span>' +
            '</div> Starting import...';

        fetch('/api/ie/import/path', {
            method: 'POST',
            body: formData
        }).then(response => response.text())
            .then(data => {
                resultDiv.innerHTML = '<div class="alert alert-success">' + data + '</div>';
            })
            .catch(error => {
                resultDiv.innerHTML = '<div class="alert alert-danger"> Error:' + error + '</div>';
            });
    }
    
    $(document).ready(() => {
        $('#uploadCorpusModal').appendTo('body');
    });
    $('body').on('change','.custom-file-input',function(){
        const fileNames = [];
        for (var i = 0; i< this.files.length; i++){
            fileNames.push(this.files[i].name);
        }
        let labelText = fileNames.length > 2 ? fileNames.length + ' files selected' : fileNames.join(', ');
        const maxLength = 80;
        if(labelText.length > maxLength){
            labelText= labelText.substring(0,maxLength) + '...'
        }
        if(labelText == ''){
            labelText = $(this).attr('id') === 'uploadConfigFile' ? 'Choose config file...' : 'Choose files...';
        }
        $(this).next('.custom-file-label').html(labelText);
    })
    $('body').on('change','#uploadConfigFile',function(event) {
        const file = event.target.files[0];
        if (!file) return;
        const reader = new FileReader();
        
        reader.onload = function (e){
            try{
                const config = JSON.parse(e.target.result);
                // UI Reset
                $('#advancedSettings input[type="checkbox"]').prop('checked', false);
                if (!$('#uploadCorpusName').prop('readonly')) $('#uploadCorpusName').val('');
                if (!$('#uploadCorpusAuthor').prop('readonly')) $('#uploadCorpusAuthor').val('');
                if (!$('#uploadCorpusLanguage').prop('readonly')) $('#uploadCorpusLanguage').val('');
                if (!$('#uploadCorpusDescription').prop('readonly')) $('#uploadCorpusDescription').val('');
                
                if (config.addToExistingCorpus === true && $('#uploadAddToExisting').val() === 'false') {
                    $('#uploadResult').html(
                        `<div class="alert alert-warning small mb-0">
                            <i class="fas fa-exclamation-triangle mr-2"></i>
                            <strong>Note:</strong> Your uploaded config has <code>addToExistingCorpus: true</code>. 
                            Since you are creating a new corpus, this will be automatically handled as <strong>false</strong>.
                        </div>`
                    );
                } else {
                    $('#uploadResult').html('');
                }
                
                if (config.name && !$('#uploadCorpusName').prop('readonly')) $('#uploadCorpusName').val(config.name);
                if (config.author && !$('#uploadCorpusAuthor').prop('readonly')) $('#uploadCorpusAuthor').val(config.author);
                if (config.language && !$('#uploadCorpusLanguage').prop('readonly')) $('#uploadCorpusLanguage').val(config.language);
                if (config.description && !$('#uploadCorpusDescription').prop('readonly')) $('#uploadCorpusDescription').val(config.description);

                if (config.annotations) {
                    const ann = config.annotations;
                    if (ann.sentence) $('#annoSentence').prop('checked', true);
                    if (ann.lemma) $('#annoLemma').prop('checked', true);
                    if (ann.namedEntity) $('#annoNE').prop('checked', true);
                    if (ann.sentiment) $('#annoSentiment').prop('checked', true);
                    if (ann.emotion) $('#annoEmotion').prop('checked', true);
                    if (ann.topic) $('#annoTopic').prop('checked', true);
                    if (ann.time) $('#annoTime').prop('checked', true);
                    if (ann.geoNames) $('#annoGeo').prop('checked', true);
                    if (ann.wikipediaLink) $('#annoWiki').prop('checked', true);
                    if (ann.image) $('#annoImage').prop('checked', true);
                    if (ann.annotatorMetadata) $('#annoAnnotatorMeta').prop('checked', true);
                    if (ann.uceMetadata) $('#annoUceMeta').prop('checked', true);
                    if (ann.logicalLinks) $('#annoLogical').prop('checked', true);
                    if (ann.srLink) $('#annoSrLink').prop('checked', true);
                    if (ann.unifiedTopic) $('#annoUnifiedTopic').prop('checked', true);
                    if (ann.OCRPage) $('#annoOCRPage').prop('checked', true);
                    if (ann.OCRParagraph) $('#annoOCRPara').prop('checked', true);
                    if (ann.OCRBlock) $('#annoOCRBlock').prop('checked', true);
                    if (ann.OCRLine) $('#annoOCRLine').prop('checked', true);
                    if (ann.completeNegation) $('#annoNegation').prop('checked', true);
                    if (ann.cue) $('#annoCue').prop('checked', true);
                    if (ann.event) $('#annoEvent').prop('checked', true);
                    if (ann.focus) $('#annoFocus').prop('checked', true);
                    if (ann.scope) $('#annoScope').prop('checked', true);
                    if (ann.xscope) $('#annoXScope').prop('checked', true);

                    if (ann.taxon) {
                        if (ann.taxon.annotated) $('#taxonAnnotated').prop('checked', true);
                        if (ann.taxon.biofidOnthologyAnnotated) $('#taxonBiofid').prop('checked', true);
                    }
                }

                if (config.other) {
                    const oth = config.other;
                    if (oth.enableEmbeddings) $('#otherEmbeddings').prop('checked', true);
                    if (oth.enableRAGBot) $('#otherRAG').prop('checked', true);
                    if (oth.availableOnFrankfurtUniversityCollection) $('#otherGoethe').prop('checked', true);
                    if (oth.includeKeywordDistribution) $('#otherKeywords').prop('checked', true);
                    if (oth.enableS3Storage) $('#otherS3').prop('checked', true);
                }

                if (!$('#advancedSettings').hasClass('show')) {
                    $('#advancedSettings').collapse('show');
                }
            }catch (err) {
                console.error("Error when parsing uploaded corpusConfig file", err);
                alert("This corpusConfig file is invalid");
            }
        };
        reader.readAsText(file);
    })
    
    function openUploadForNewCorpora(){
        const form = document.getElementById('uploadCorpusForm');
        form.reset();
        $('#advancedSettings input[type="checkbox"]').prop('checked',false).prop('disabled',false);
        $('#uploadConfigFile').prop('disabled', false);
        $('#uploadAddToExisting').val('false');
        $('#uploadModalTitle').html('Create new Corpora');
        $('#uploadCorpusName').val('').prop('readonly',false);
        $('#uploadCorpusLanguage').prop('readonly',false);
        $('#uploadCorpusAuthor').prop('readonly',false);
        $('#uploadCorpusDescription').prop('readonly',false);
        $('#uploadFiles').next('.custom-file-label').html('Choose Files...');
        $('#uploadConfigFile').next('.custom-file-label').html('Choose corpusConfig file...');
        $('#uploadResult').html('');
        
        $('#uploadCorpusModal').modal('show');  
        $('#configUploadGroup').show();
        
    }
    function openUploadForExistingCorpora(corpusName,author,language,description,configJsonStr){
        const form = document.getElementById('uploadCorpusForm');
        form.reset();
        $('#advancedSettings input[type="checkbox"]').prop('checked',false).prop('disabled',false);
        if (configJsonStr){
            try{
                const config = typeof configJsonStr === 'string' ? JSON.parse(configJsonStr) : configJsonStr;
                if (config.annotations) {
                    const ann = config.annotations;
                    if (ann.sentence) $('#annoSentence').prop('checked', true).prop('disabled', true);
                    if (ann.lemma) $('#annoLemma').prop('checked', true).prop('disabled', true);
                    if (ann.namedEntity) $('#annoNE').prop('checked', true).prop('disabled', true);
                    if (ann.sentiment) $('#annoSentiment').prop('checked', true).prop('disabled', true);
                    if (ann.emotion) $('#annoEmotion').prop('checked', true).prop('disabled', true);
                    if (ann.topic) $('#annoTopic').prop('checked', true).prop('disabled', true);
                    if (ann.time) $('#annoTime').prop('checked', true).prop('disabled', true);
                    if (ann.geoNames) $('#annoGeo').prop('checked', true).prop('disabled', true);
                    if (ann.wikipediaLink) $('#annoWiki').prop('checked', true).prop('disabled', true);
                    if (ann.image) $('#annoImage').prop('checked', true).prop('disabled', true);
                    if (ann.annotatorMetadata) $('#annoAnnotatorMeta').prop('checked', true).prop('disabled', true);
                    if (ann.uceMetadata) $('#annoUceMeta').prop('checked', true).prop('disabled', true);
                    if (ann.logicalLinks) $('#annoLogical').prop('checked', true).prop('disabled', true);
                    if (ann.srLink) $('#annoSrLink').prop('checked', true).prop('disabled', true);
                    if (ann.unifiedTopic) $('#annoUnifiedTopic').prop('checked', true).prop('disabled', true);
                    if (ann.OCRPage) $('#annoOCRPage').prop('checked', true).prop('disabled', true);
                    if (ann.OCRParagraph) $('#annoOCRPara').prop('checked', true).prop('disabled', true);
                    if (ann.OCRBlock) $('#annoOCRBlock').prop('checked', true).prop('disabled', true);
                    if (ann.OCRLine) $('#annoOCRLine').prop('checked', true).prop('disabled', true);
                    if (ann.completeNegation) $('#annoNegation').prop('checked', true).prop('disabled', true);
                    if (ann.cue) $('#annoCue').prop('checked', true).prop('disabled', true);
                    if (ann.event) $('#annoEvent').prop('checked', true).prop('disabled', true);
                    if (ann.focus) $('#annoFocus').prop('checked', true).prop('disabled', true);
                    if (ann.scope) $('#annoScope').prop('checked', true).prop('disabled', true);
                    if (ann.xscope) $('#annoXScope').prop('checked', true).prop('disabled', true);
                    
                    if (ann.taxon) {
                        if (ann.taxon.annotated) $('#taxonAnnotated').prop('checked', true).prop('disabled', true);
                        if (ann.taxon.biofidOnthologyAnnotated) $('#taxonBiofid').prop('checked', true).prop('disabled', true);
                    }
                    if (config.other) {
                        const oth = config.other;
                        if (oth.enableEmbeddings) $('#otherEmbeddings').prop('checked', true).prop('disabled', true);
                        if (oth.enableRAGBot) $('#otherRAG').prop('checked', true).prop('disabled', true);
                        if (oth.availableOnFrankfurtUniversityCollection) $('#otherGoethe').prop('checked', true).prop('disabled', true);
                        if (oth.includeKeywordDistribution) $('#otherKeywords').prop('checked', true).prop('disabled', true);
                        if (oth.enableS3Storage) $('#otherS3').prop('checked', true).prop('disabled', true);
                        if (oth.availableOnFrankfurtUniversityCollection) $('#otherGoethe').prop('checked', true).prop('disabled', true);
                    }
                }
            }catch(e){
                console.error("Error parsing config string for locking UI Components")
            }
        }
        
        $('#uploadAddToExisting').val('true');
        $('#uploadModalTitle').html('Add Files to "' + corpusName + '"');
        $('#uploadCorpusName').val(corpusName).prop('readonly',true);
        $('#uploadCorpusLanguage').val(language).prop('readonly',true);
        $('#uploadCorpusAuthor').val(author).prop('readonly',true);
        $('#uploadCorpusDescription').val(description)  .prop('readonly',true);
        $('#uploadFiles').next('.custom-file-label').html('Choose Files...');
        $('#uploadConfigFile').prop('disabled', true);
        $('#uploadResult').html('');
        
        $('#uploadCorpusModal').modal('show');
        $('#configUploadGroup').hide();
    }
    function submitCorpusUpload(){
        const form = document.getElementById('uploadCorpusForm');
        const formData = new FormData(form)
        const resultDiv = document.getElementById('uploadResult');
        
        resultDiv.innerHTML = 
            `<div class="spinner-border" text-primary role="status">
                <span class="sr-only"></span>
             </div>
            Importing...
            `;
        
        fetch('/api/ie/import/upload', {
            method: 'POST',
            body: formData
        })
            .then(async response => {
                const msg = await response.text();
                if (response.ok) {
                    resultDiv.innerHTML = '<div class="text-success font-weight-bold"><i class="fas fa-check-circle mr-1"></i> Upload successful! Reloading...</div>';

                    $('#uploadCorpusModal').modal('hide');
                    const idMatch = msg.match(/ID:\s*(.+)/);
                    if(idMatch && idMatch[1]){
                        const importId = idMatch[1].trim();
                        let activeImports = JSON.parse(localStorage.getItem('activeUceImports') || '[]');
                        if (!activeImports.includes(importId)) {
                            activeImports.push(importId);
                            localStorage.setItem('activeUceImports', JSON.stringify(activeImports));
                        }
                        if(typeof startImportProgress === 'function') startImportProgress();
                    }else{
                        console.warn("No import Id extracted: ",msg);
                    }
                } else {
                    throw new Error(msg);
                }
            })
            .catch(err => {
                console.error(err);
                resultDiv.innerHTML = '<div class="text-danger mt-2"><i class="fas fa-exclamation-triangle mr-1"></i> Error: ' + err.message + '</div>';
            });
    }
    
    function deleteCorpus(corpusId){
        if (!confirm("Are you sure you want to delete this corpus?")){
            return;
        }
        fetch('/api/corpus/delete?corpusId=' + corpusId, {
            method: 'DELETE'
        })
            .then(async response => {
                if (response.ok){
                    alert("Corpus successfully deleted");
                    location.reload();
                }else{
                    const msg = await response.text();
                    alert("Error when trying to delete corpus " + msg);
                }
            })
            .catch(e => {
                console.eor(e);
                alert("Unexpected Error " + err.message);
            })
    }
</script>