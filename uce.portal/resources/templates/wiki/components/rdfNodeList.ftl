<div class="nodes-list-div">
    <#list rdfNodes as node>
        <div class="node-div ml-2" data-expanded="false" data-children="false">
            <div class="flexed pl-1 pr-1 pb-1 pt-1 align-items-center justify-content-between node">
                <p class="w-100 mb-0 small-font">
                    <i class="mb-0 mr-1 small-font text color-secondary fab fa-connectdevelop"></i>
                    <#assign splitted = node.getPredicate().getValue()?split("/")>
                    <span class="mb-0">${splitted[splitted?size - 1]}</span>
                </p>
                <i class="mb-0 ml-2 mr-2 small-font text fab fa-hive"></i>
                <#if node.getObject()?has_content>
                    <#if node.getObject().getType() == "uri">
                        <a class="w-100 mb-0 text-right small-font color-prime clickable clickable-rdf-node"
                           data-triplettype="subject" data-value="${node.getObject().getValue()}">
                            ${node.getObject().getValue()}
                        </a>
                    <#else>
                        <p class="w-100 mb-0 text-right small-font text">${node.getObject().getValue()}</p>
                    </#if>
                </#if>
            </div>
        </div>
    </#list>
</div>
