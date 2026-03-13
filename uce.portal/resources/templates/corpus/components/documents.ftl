<#list documents as document>
    <div class=" m-0 p-3 flexed justify-content-center h-100 w-100 align-items-start">
        <div class="document-card">
            <#assign searchId = "">
            <#include '*/search/components/documentCardContent.ftl' >

            <div class="d-flex justify-content-end p-2 mt-2 border-top">
                <button class="btn btn-sm btn-outline-danger"
                        onclick="deleteDocument(${document.getId()})"
                        title="Delete this Document">
                    <i class="fas fa-trash-alt mr-1"></i>
                </button>
            </div>
            
        </div>
    </div>
</#list>

<script>
    /**
     * Deletes a document based on its ID
     */
    function deleteDocument(documentId){
        if (!confirm("Are you sure you want to delete this document?")){
            return;
        }
        const btn = event.currentTarget;
        const ogHtml = btn.innerHTML;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
        btn.disabled = true;

        fetch('/api/document/delete?id=' + documentId, {
            method: 'DELETE'
        })
            .then(async response => {
                if (response.ok){
                    $(btn).closest('.flexed').fadeOut(300,function (){$(this).remove();})
                }else{
                    const msg = await response.text();
                    alert("Error when trying to delete document: " + msg);
                    btn.innerHTML = ogHtml;
                    btn.disabled = false;
                }
            })
            .catch(e => {
                console.error(e);
                alert("Unexpected Error " + err.message);
                btn.innerHTML = ogHtml;
                btn.disabled = false;
            })
    }
</script>