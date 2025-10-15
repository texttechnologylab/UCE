
<div class="w-100 group-box bg-ghost light-border p-0 card-shadow position-relative linkable-space">
    <div class="w-100 bg-lightgray pb-2 pt-2 pl-3 pr-3 border-bottom-1 flexed justify-content-between align-items-center">
        <h6 class="mb-0 text-dark">Linkable Space</h6>
    </div>
    <!-- 'parent-drawflow' MUST BE THE FIRST CLASS orderwise: https://github.com/jerosoler/Drawflow/issues/469 -->
    <div class="parent-drawflow linkable-space-container p-2" style="height: ${height}px; width: 100%"></div>
</div>

<script>
    $(document).ready(function(){
       window.flowVizHandler.createNewFromLinkableNode('${unique}', $('.wiki-page .linkable-space-container').get(0));
    });
</script>