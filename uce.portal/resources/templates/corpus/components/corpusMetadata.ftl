<div class="row m-0 p-0">
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

    <div class="col-lg-6 entry">
        <label class="mb-0 pr-3 pl-3 text">${languageResource.get("pages")}</label>
        <input readonly type="text" class="form-control rounded-0" value="${pagesCount}"/>
    </div>
</div>