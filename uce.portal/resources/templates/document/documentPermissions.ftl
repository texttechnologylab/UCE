<div class="uce-document-permissions-container">
    <#if !(uceDocumentPermissions?? && uceDocumentPermissions?has_content) || uceDocumentPermissions?size == 0>
        <div class="light-border rounded p-3 bg-light card-shadow">
            <p class="text-center mb-0 text">There are no permissions for this document, everybody can access it.</p>
        </div>
    <#else>
        <table class="uce-document-permissions-table light-border rounded p-3 bg-light card-shadow">
            <thead>
                <tr>
                    <th>Type</th>
                    <th>Name</th>
                    <th>Access level</th>
                </tr>
            </thead>
            <tbody>
                <#list uceDocumentPermissions as permisson>
                    <tr>
                        <td>${permisson.getType()}</td>
                        <td>${permisson.getName()}</td>
                        <td>${permisson.getLevel()}</td>
                    </tr>
                </#list>
            </tbody>
        </table>
    </#if>
</div>