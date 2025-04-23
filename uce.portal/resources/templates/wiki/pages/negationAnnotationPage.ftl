<div class="wiki-page container">

    <!-- breadcrumbs -->
    <div class="mb-3">
        <#include "*/wiki/components/breadcrumbs.ftl">
    </div>

    <!-- metadata header -->
    <div>
        <#include "*/wiki/components/metadata.ftl">
    </div>

    <hr class="mt-2 mb-4"/>


    <!-- the document this is from -->
    <div class="mt-4 mb-3 w-100 p-0 m-0 justify-content-center flexed align-items-start">
        <div class="document-card w-100">
            <#assign document = vm.getDocument()>
            <#assign searchId = "">
            <#assign mainAnno = vm.getCue()>
            <#assign offsetList = vm.getOffsetList()>
            <#include '*/search/components/documentCardContent.ftl' >
        </div>
    </div>

    <!-- Table for four lists -->
    <div class="mt-4">
        <table class="nodes-table">
            <thead>
            <tr>
                <th>Type</th>
                <th>Start</th>
                <th>Begin</th>
                <th>Value</th>
            </tr>
            </thead>
            <tbody>
            <!-- List 0 -->
            <tr class="list-separator">
                <td colspan="3">Cue</td>
            </tr>
                <tr class="nodes-list-div">
                    <td></td>
                    <td>${vm.getCue().getBegin()}</td>
                    <td>${vm.getCue().getEnd()}</td>
                    <td>${vm.getCue().getCoveredText()}</td>
                </tr>

            <!-- List 1 -->
            <tr class="list-separator">
                <td colspan="3">Scope</td>
            </tr>
            <#list vm.getScopeList() as item>
                <tr class="nodes-list-div">
                    <td></td>
                    <td>${item.getBegin()}</td>
                    <td>${item.getEnd()}</td>
                    <td>${item.getCoveredText()}</td>
                </tr>
            </#list>

            <!-- List 2 -->
            <tr class="list-separator">
                <td colspan="3">XScope</td>
            </tr>
            <#list vm.getXscopeList() as item>
                <tr class="nodes-list-div">
                    <td></td>
                    <td>${item.getBegin()}</td>
                    <td>${item.getEnd()}</td>
                    <td>${item.getCoveredText()}</td>
                </tr>
            </#list>

            <!-- List 3 -->
            <tr class="list-separator">
                <td colspan="3">Focus</td>
            </tr>
            <#list vm.getFocusList() as item>
                <tr class="nodes-list-div">
                    <td></td>
                    <td>${item.getBegin()}</td>
                    <td>${item.getEnd()}</td>
                    <td>${item.getCoveredText()}</td>
                </tr>
            </#list>

            <!-- List 4 -->
            <tr class="list-separator">
                <td colspan="3">Event</td>
            </tr>
            <#list vm.getEventList() as item>
                <tr class="nodes-list-div">
                    <td></td>
                    <td>${item.getBegin()}</td>
                    <td>${item.getEnd()}</td>
                    <td>${item.getCoveredText()}</td>
                </tr>
            </#list>
            </tbody>
        </table>
    </div>

</div>
