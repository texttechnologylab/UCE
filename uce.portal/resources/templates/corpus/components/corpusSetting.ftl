<div class="modal fade" id="addCorpusModal" tabindex="-1" role="dialog" aria-labelledby="addCorpusModalLabel" aria-hidden="true">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="addCorpusModalLabel">${languageResource.get("addNewCorpus")}</h5>
                <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                    <span aria-hidden="true">&times;</span>
                </button>
            </div>
            <div class="modal-body">
                <form id="addCorpusForm">
                    <div class="form-group">
                        <label for="folderPath">${languageResource.get("selectFolder")}</label>
                        <input type="text" class="form-control" id="corpusPath" name="corpusPath" placeholder="${languageResource.get("folderPathPlaceholder")}" required>
                    </div>
                </form>
                <!-- Enhanced instructions -->
                <div class="mt-4" role="alert">
                    <h6>Folder Requirements</h6>
                    <p class="text-muted mb-2">
                        The selected folder must contain the following structure:
                    </p>
                    <pre class="bg-light p-3 rounded border">
your-selected-folder/
├── corpusConfig.json
└── input/
    ├── document1.xmi
    ├── document2.xmi
    └── ...
                    </pre>
                    <p class="text-muted mb-0">
                        <strong>Note:</strong> The <code>input</code> folder should contain one or more <code>.xmi</code> files.
                    </p>
                </div>

            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-dismiss="modal">${languageResource.get("cancel")}</button>
                <button type="submit" class="btn btn-primary" form="addCorpusForm">${languageResource.get("addCorpus")}</button>
            </div>
        </div>
    </div>
</div>

<div class="modal fade" id="manageCorpusModal" tabindex="-1" role="dialog" aria-labelledby="manageCorpusModalLabel" aria-hidden="true">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="manageCorpusModalLabel">${languageResource.get("manageCorpus")}</h5>
                <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                    <span aria-hidden="true">&times;</span>
                </button>
            </div>
            <div class="modal-body">
                <form id="uploadXmiForm" enctype="multipart/form-data">
                    <input type="hidden" id="selectedCorpusId" name="corpusId">
                    <div class="form-group">
                        <label for="xmiFile">${languageResource.get("uploadXmiFile")}</label>
                        <input type="file" class="form-control-file" id="xmiFile" name="file" accept=".xmi" required>
                    </div>
                </form>
                <!-- File upload instructions -->
                <div class="mt-4" role="alert">
                    <h6>Upload Instructions</h6>
                    <p class="text-muted mb-2">
                        Please upload a valid <code>.xmi</code> file associated with an existing corpus.
                    </p>
                    <p class="text-muted mb-0">
                        The uploaded file will be stored in the configured <code>input</code> folder for the selected corpus.
                    </p>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-dismiss="modal">${languageResource.get("cancel")}</button>
                <button type="submit" class="btn btn-primary" form="uploadXmiForm">${languageResource.get("add")}</button>
            </div>
        </div>
    </div>
</div>
