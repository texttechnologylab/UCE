<style>
    <#include "*/css/corpus-inspector.css">
</style>
<div class="row m-0 p-0 w-100">

    <!-- corpus statistics -->
    <div class="col-md-4 p-0 m-0">
        <div class="cheader w-100 flexed align-items-center justify-content-between p-4">
            <a class="btn" onclick="$('.corpus-inspector-include').hide(150)">
                <i class="fas fa-long-arrow-alt-left m-0 color-prime"></i>
            </a>
            <div class="text-center">
                <h5 class="mb-1 color-prime">${corpus.getName()}</h5>
                <hr class="mt-0 mb-1 text"/>
                <p class="text mb-0 font-italic">${languageResource.get("corpusInspector")}</p>
            </div>
            <h4 class="m-0 color-secondary"><i class="fas fa-atlas mr-1"></i></h4>
        </div>

        <div class="ccontent">

            <!-- Meta -->
            <h6 class="large-font mt-2 mb-2 text-center color-prime">Meta</h6>
            <div class="row m-0 p-0 border rounded">
                <div class="col-lg-6 entry">
                    <label class="mb-0 pr-3 pl-3 text">Name</label>
                    <input readonly type="text" class="form-control rounded-0" value="${corpus.getName()}"/>
                </div>

                <div class="col-lg-6 entry">
                    <label class="mb-0 pr-3 pl-3 text">${languageResource.get("source")}</label>
                    <input readonly type="text" class="form-control rounded-0" value="${corpus.getAuthor()}"/>
                </div>

                <div class="col-lg-6 entry">
                    <label class="mb-0 pr-3 pl-3 text">${languageResource.get("language")}</label>
                    <input readonly type="text" class="form-control rounded-0" value="${corpus.getLanguage()}"/>
                </div>

                <div class="col-lg-6 entry">
                    <label class="mb-0 pr-3 pl-3 text">${languageResource.get("imported")}</label>
                    <input readonly type="text" class="form-control rounded-0" value="${corpus.getCreated()}"/>
                </div>

                <div class="col-lg-6 entry">
                    <label class="mb-0 pr-3 pl-3 text">${languageResource.get("documents")}</label>
                    <input readonly type="text" class="form-control rounded-0" value="${documentsCount}"/>
                </div>
            </div>

            <!-- Annotations -->
            <h6 class="large-font mb-2 mt-4 text-center color-prime">Annotations</h6>
            <div class="border rounded">
                <div class="row m-0 pl-0 pr-0">

                    <div class="col-lg-6 mt-3 mb-3">
                        <#assign isChecked = "" />
                        <#if corpusConfig.getAnnotations().isOCRPage() == true>
                            <#assign isChecked = "checked"/>
                        </#if>
                        <div class="flexed align-items-center justify-content-between border p-2 rounded wrapped">
                            <label class="mb-0 pr-1 text">OCRPages</label>
                            <input type="checkbox" class="rounded-0 bg-prime" disabled ${isChecked}/>
                        </div>
                    </div>

                    <div class="col-lg-6 mt-3 mb-3">
                        <#assign isChecked = "" />
                        <#if corpusConfig.getAnnotations().isOCRParagraph() == true>
                            <#assign isChecked = "checked"/>
                        </#if>
                        <div class="flexed align-items-center justify-content-between border p-2 rounded wrapped">
                            <label class="mb-0 pr-1 text">OCRParagraphs</label>
                            <input type="checkbox" class="rounded-0 bg-prime" disabled ${isChecked}/>
                        </div>
                    </div>

                    <div class="col-lg-6 mt-3 mb-3">
                        <#assign isChecked = "" />
                        <#if corpusConfig.getAnnotations().isOCRBlock() == true>
                            <#assign isChecked = "checked"/>
                        </#if>
                        <div class="flexed align-items-center justify-content-between border p-2 rounded wrapped">
                            <label class="mb-0 pr-1 text">OCRBlocks</label>
                            <input type="checkbox" class="rounded-0 bg-prime" disabled ${isChecked}/>
                        </div>
                    </div>

                    <div class="col-lg-6 mt-3 mb-3">
                        <#assign isChecked = "" />
                        <#if corpusConfig.getAnnotations().isOCRLine() == true>
                            <#assign isChecked = "checked"/>
                        </#if>
                        <div class="flexed align-items-center justify-content-between border p-2 rounded wrapped">
                            <label class="mb-0 pr-1 text">OCRLines</label>
                            <input type="checkbox" class="rounded-0 bg-prime" disabled ${isChecked}/>
                        </div>
                    </div>

                    <div class="col-lg-6 mt-3 mb-3">
                        <#assign isChecked = "" />
                        <#if corpusConfig.getAnnotations().getTaxon().isAnnotated() == true>
                            <#assign isChecked = "checked"/>
                        </#if>
                        <div class="flexed align-items-center justify-content-between border p-2 rounded wrapped">
                            <label class="mb-0 pr-1 text">Taxone</label>
                            <input type="checkbox" class="rounded-0 bg-prime" disabled ${isChecked}/>
                        </div>
                    </div>

                    <div class="col-lg-6 mt-3 mb-3">
                        <#assign isChecked = "" />
                        <#if corpusConfig.getAnnotations().getTaxon().isBiofidOnthologyAnnotated() == true>
                            <#assign isChecked = "checked"/>
                        </#if>
                        <div class="flexed align-items-center justify-content-between border p-2 rounded wrapped">
                            <label class="mb-0 pr-1 text">BIOfid Ont.</label>
                            <input type="checkbox" class="rounded-0 bg-prime" disabled ${isChecked}/>
                        </div>
                    </div>

                    <div class="col-lg-6 mt-3 mb-3">
                        <#assign isChecked = "" />
                        <#if corpusConfig.getAnnotations().isSrLink() == true>
                            <#assign isChecked = "checked"/>
                        </#if>
                        <div class="flexed align-items-center justify-content-between border p-2 rounded wrapped">
                            <label class="mb-0 pr-1 text">SRLinks</label>
                            <input type="checkbox" class="rounded-0 bg-prime" disabled ${isChecked}/>
                        </div>
                    </div>

                    <div class="col-lg-6 mt-3 mb-3">
                        <#assign isChecked = "" />
                        <#if corpusConfig.getAnnotations().isNamedEntity() == true>
                            <#assign isChecked = "checked"/>
                        </#if>
                        <div class="flexed align-items-center justify-content-between border p-2 rounded wrapped">
                            <label class="mb-0 pr-1 text">Named-Entities</label>
                            <input type="checkbox" class="rounded-0 bg-prime" disabled ${isChecked}/>
                        </div>
                    </div>

                    <div class="col-lg-6 mt-3 mb-3">
                        <#assign isChecked = "" />
                        <#if corpusConfig.getAnnotations().isSentence() == true>
                            <#assign isChecked = "checked"/>
                        </#if>
                        <div class="flexed align-items-center justify-content-between border p-2 rounded wrapped">
                            <label class="mb-0 pr-1 text">Sentences</label>
                            <input type="checkbox" class="rounded-0 bg-prime" disabled ${isChecked}/>
                        </div>
                    </div>

                    <div class="col-lg-6 mt-3 mb-3">
                        <#assign isChecked = "" />
                        <#if corpusConfig.getAnnotations().isTime() == true>
                            <#assign isChecked = "checked"/>
                        </#if>
                        <div class="flexed align-items-center justify-content-between border p-2 rounded wrapped">
                            <label class="mb-0 pr-1 text">Times</label>
                            <input type="checkbox" class="rounded-0 bg-prime" disabled ${isChecked}/>
                        </div>
                    </div>

                    <div class="col-lg-6 mt-3 mb-3">
                        <#assign isChecked = "" />
                        <#if corpusConfig.getOther().isEnableEmbeddings() == true>
                            <#assign isChecked = "checked"/>
                        </#if>
                        <div class="flexed align-items-center justify-content-between border p-2 rounded wrapped">
                            <label class="mb-0 pr-1 text">Embeddings</label>
                            <input type="checkbox" class="rounded-0 bg-prime" disabled ${isChecked}/>
                        </div>
                    </div>

                    <div class="col-lg-6 mt-3 mb-3">
                        <#assign isChecked = "" />
                        <#if corpusConfig.getOther().isEnableRAGBot() == true>
                            <#assign isChecked = "checked"/>
                        </#if>
                        <div class="flexed align-items-center justify-content-between border p-2 rounded wrapped">
                            <label class="mb-0 pr-1 text">RAGBot</label>
                            <input type="checkbox" class="rounded-0 bg-prime" disabled ${isChecked}/>
                        </div>
                    </div>

                    <div class="col-lg-6 mt-3 mb-3">
                        <#assign isChecked = "" />
                        <#if corpusConfig.getAnnotations().isWikipediaLink() == true>
                            <#assign isChecked = "checked"/>
                        </#if>
                        <div class="flexed align-items-center justify-content-between border p-2 rounded wrapped">
                            <label class="mb-0 pr-1 text">WikiLinks</label>
                            <input type="checkbox" class="rounded-0 bg-prime" disabled ${isChecked}/>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- corpus tsne plot -->
    <div class="col-md-8 w-100 m-0 p-0 border-left position-relative" style="height: 100vh !important;">
        <div class="corpus-tsne-plot h-100 w-100">
            <div class="simple-loader"></div>
            <div class="error-msg display-none">${languageResource.get("corpusTsnePlotMissing")}</div>
        </div>
    </div>

</div>