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
            <button class="btn btn-sm btn-outline-secondary ml-3" data-toggle="modal" data-target="#importCorpusModal">
                <i class="fas fa-file-import"></i>
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
</script>