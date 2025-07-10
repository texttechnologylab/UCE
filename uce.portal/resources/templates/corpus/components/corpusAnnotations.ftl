<div>
    <div class="row m-0 pl-0 pr-0">
        <div class="col-lg-4 annotation-entry">
            <#assign isChecked = "" />
            <#if corpusConfig.getAnnotations().isOCRPage() == true>
                <#assign isChecked = "checked"/>
            </#if>
            <div class="flexed align-items-center justify-content-between border p-2 rounded wrapped">
                <label class="mb-0 pr-1 color-dark">OCRPages</label>
                <input type="checkbox" class="rounded-0 bg-prime" disabled ${isChecked}/>
            </div>
        </div>

        <div class="col-lg-4 annotation-entry">
            <#assign isChecked = "" />
            <#if corpusConfig.getAnnotations().isOCRParagraph() == true>
                <#assign isChecked = "checked"/>
            </#if>
            <div class="flexed align-items-center justify-content-between border p-2 rounded wrapped">
                <label class="mb-0 pr-1 color-dark">OCRParagraphs</label>
                <input type="checkbox" class="rounded-0 bg-prime" disabled ${isChecked}/>
            </div>
        </div>

        <div class="col-lg-4 annotation-entry">
            <#assign isChecked = "" />
            <#if corpusConfig.getAnnotations().isOCRBlock() == true>
                <#assign isChecked = "checked"/>
            </#if>
            <div class="flexed align-items-center justify-content-between border p-2 rounded wrapped">
                <label class="mb-0 pr-1 color-dark">OCRBlocks</label>
                <input type="checkbox" class="rounded-0 bg-prime" disabled ${isChecked}/>
            </div>
        </div>

        <div class="col-lg-4 annotation-entry">
            <#assign isChecked = "" />
            <#if corpusConfig.getAnnotations().isOCRLine() == true>
                <#assign isChecked = "checked"/>
            </#if>
            <div class="flexed align-items-center justify-content-between border p-2 rounded wrapped">
                <label class="mb-0 pr-1 color-dark">OCRLines</label>
                <input type="checkbox" class="rounded-0 bg-prime" disabled ${isChecked}/>
            </div>
        </div>

        <div class="col-lg-4 annotation-entry">
            <#assign isChecked = "" />
            <#if corpusConfig.getAnnotations().getTaxon().isAnnotated() == true>
                <#assign isChecked = "checked"/>
            </#if>
            <div class="flexed align-items-center justify-content-between border p-2 rounded wrapped">
                <label class="mb-0 pr-1 color-dark">Taxa</label>
                <input type="checkbox" class="rounded-0 bg-prime" disabled ${isChecked}/>
            </div>
        </div>

        <div class="col-lg-4 annotation-entry">
            <#assign isChecked = "" />
            <#if corpusConfig.getAnnotations().getTaxon().isBiofidOnthologyAnnotated() == true>
                <#assign isChecked = "checked"/>
            </#if>
            <div class="flexed align-items-center justify-content-between border p-2 rounded wrapped">
                <label class="mb-0 pr-1 color-dark">BIOfid Ont.</label>
                <input type="checkbox" class="rounded-0 bg-prime" disabled ${isChecked}/>
            </div>
        </div>

        <div class="col-lg-4 annotation-entry">
            <#assign isChecked = "" />
            <#if corpusConfig.getAnnotations().isSrLink() == true>
                <#assign isChecked = "checked"/>
            </#if>
            <div class="flexed align-items-center justify-content-between border p-2 rounded wrapped">
                <label class="mb-0 pr-1 color-dark">SRLinks</label>
                <input type="checkbox" class="rounded-0 bg-prime" disabled ${isChecked}/>
            </div>
        </div>

        <div class="col-lg-4 annotation-entry">
            <#assign isChecked = "" />
            <#if corpusConfig.getAnnotations().isNamedEntity() == true>
                <#assign isChecked = "checked"/>
            </#if>
            <div class="flexed align-items-center justify-content-between border p-2 rounded wrapped">
                <label class="mb-0 pr-1 color-dark">Named-Entities</label>
                <input type="checkbox" class="rounded-0 bg-prime" disabled ${isChecked}/>
            </div>
        </div>

        <div class="col-lg-4 annotation-entry">
            <#assign isChecked = "" />
            <#if corpusConfig.getAnnotations().isGeoNames() == true>
                <#assign isChecked = "checked"/>
            </#if>
            <div class="flexed align-items-center justify-content-between border p-2 rounded wrapped">
                <label class="mb-0 pr-1 color-dark">GeoNames</label>
                <input type="checkbox" class="rounded-0 bg-prime" disabled ${isChecked}/>
            </div>
        </div>

        <div class="col-lg-4 annotation-entry">
            <#assign isChecked = "" />
            <#if corpusConfig.getAnnotations().isSentence() == true>
                <#assign isChecked = "checked"/>
            </#if>
            <div class="flexed align-items-center justify-content-between border p-2 rounded wrapped">
                <label class="mb-0 pr-1 color-dark">Sentences</label>
                <input type="checkbox" class="rounded-0 bg-prime" disabled ${isChecked}/>
            </div>
        </div>

        <div class="col-lg-4 annotation-entry">
            <#assign isChecked = "" />
            <#if corpusConfig.getAnnotations().isLemma() == true>
                <#assign isChecked = "checked"/>
            </#if>
            <div class="flexed align-items-center justify-content-between border p-2 rounded wrapped">
                <label class="mb-0 pr-1 color-dark">Lemma</label>
                <input type="checkbox" class="rounded-0 bg-prime" disabled ${isChecked}/>
            </div>
        </div>

        <div class="col-lg-4 annotation-entry">
            <#assign isChecked = "" />
            <#if corpusConfig.getAnnotations().isTime() == true>
                <#assign isChecked = "checked"/>
            </#if>
            <div class="flexed align-items-center justify-content-between border p-2 rounded wrapped">
                <label class="mb-0 pr-1 color-dark">Times</label>
                <input type="checkbox" class="rounded-0 bg-prime" disabled ${isChecked}/>
            </div>
        </div>

        <div class="col-lg-4 annotation-entry">
            <#assign isChecked = "" />
            <#if corpusConfig.getOther().isEnableEmbeddings() == true>
                <#assign isChecked = "checked"/>
            </#if>
            <div class="flexed align-items-center justify-content-between border p-2 rounded wrapped">
                <label class="mb-0 pr-1 color-dark">Embeddings</label>
                <input type="checkbox" class="rounded-0 bg-prime" disabled ${isChecked}/>
            </div>
        </div>

        <div class="col-lg-4 annotation-entry">
            <#assign isChecked = "" />
            <#if corpusConfig.getOther().isEnableRAGBot() == true>
                <#assign isChecked = "checked"/>
            </#if>
            <div class="flexed align-items-center justify-content-between border p-2 rounded wrapped">
                <label class="mb-0 pr-1 color-dark">RAGBot</label>
                <input type="checkbox" class="rounded-0 bg-prime" disabled ${isChecked}/>
            </div>
        </div>

        <div class="col-lg-4 annotation-entry">
            <#assign isChecked = "" />
            <#if corpusConfig.getAnnotations().isWikipediaLink() == true>
                <#assign isChecked = "checked"/>
            </#if>
            <div class="flexed align-items-center justify-content-between border p-2 rounded wrapped">
                <label class="mb-0 pr-1 color-dark">WikiLinks</label>
                <input type="checkbox" class="rounded-0 bg-prime" disabled ${isChecked}/>
            </div>
        </div>

        <div class="col-lg-4 annotation-entry">
            <#assign isChecked = "" />
            <#if corpusConfig.getAnnotations().isCompleteNegation() == true>
                <#assign isChecked = "checked"/>
            </#if>
            <div class="flexed align-items-center justify-content-between border p-2 rounded wrapped">
                <label class="mb-0 pr-1 color-dark">Negations</label>
                <input type="checkbox" class="rounded-0 bg-prime" disabled ${isChecked}/>
            </div>
        </div>

        <div class="col-lg-4 annotation-entry">
            <#assign isChecked = "" />
            <#if corpusConfig.getAnnotations().isUnifiedTopic() == true>
                <#assign isChecked = "checked"/>
            </#if>
            <div class="flexed align-items-center justify-content-between border p-2 rounded wrapped">
                <label class="mb-0 pr-1 color-dark">Unified Topics</label>
                <input type="checkbox" class="rounded-0 bg-prime" disabled ${isChecked}/>
            </div>
        </div>

        <div class="col-lg-4 annotation-entry">
            <#assign isChecked = "" />
            <#if corpusConfig.getAnnotations().isEmotion() == true>
                <#assign isChecked = "checked"/>
            </#if>
            <div class="flexed align-items-center justify-content-between border p-2 rounded wrapped">
                <label class="mb-0 pr-1 color-dark">Emotions</label>
                <input type="checkbox" class="rounded-0 bg-prime" disabled ${isChecked}/>
            </div>
        </div>
    </div>
</div>