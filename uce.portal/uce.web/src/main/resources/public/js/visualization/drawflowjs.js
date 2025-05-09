class DrawflowJS {

    constructor(target) {
        this.target = target;
        this.editor = new Drawflow(target);
        this.editor.start();
        this.nodes = [];
        this.placedNodes = new Map(); // key: unique, value: node ID + position
        this.placedConnections = new Map();
        this.rowPerDepth = new Map();
        //this.editor.editor_mode = 'view';
    }

    init(nodeDto) {
        console.log(nodeDto);
        this.placedNodes = new Map();
        this.nodes = [];
        this.placeNodeDto(nodeDto, 1, 1); // root starts at (1,1)
        activatePopovers();
    }

    createNodeFromNodeDto(nodeDto) {
        return {
            type: nodeDto.type,
            html: `
                <div>
                    <p class="node-title">${nodeDto.type}</p>
                    <div>${nodeDto.nodeHtml}</div>
                </div>`,
            unique: nodeDto.unique,
            id: -1,
            posX: 0,
            posY: 0
        };
    }

    addLabelNode(labelText, depth, toNodeUnique) {
        const spacingX = 1000;

        const posX = depth > 0 ? depth * spacingX : 150;
        const toNode = this.placedNodes.get(toNodeUnique);

        const posY = toNode ? toNode.posY : 0;  // default to 0 if missing

        const html = `
        <div class="p-2 label color-secondary large-font">
            ${labelText}
        </div>
    `;
        const id = this.editor.addNode('label', 1, 1, posX, posY, 'html', {}, html);
        return id;
    }

    placeNodeDto(nodeDto, depth = 0) {
        if (this.placedNodes.has(nodeDto.unique)) {
            return this.placedNodes.get(nodeDto.unique).id;
        }

        const spacingX = 650;
        const spacingY = 225;

        // Get and increment row for this depth
        let row = this.rowPerDepth.get(depth) || 0;
        this.rowPerDepth.set(depth, row + 1);

        const posX = depth * spacingX;
        const posY = row * spacingY;

        const node = this.createNodeFromNodeDto(nodeDto);
        node.id = this.editor.addNode(node.type, 1, 1, posX, posY, 'html', node, node.html);
        this.nodes.push(node);
        this.placedNodes.set(nodeDto.unique, {id: node.id, posX, posY});

        // === Handle fromNodes ===
        if (Array.isArray(nodeDto.fromNodes)) {
            nodeDto.fromNodes.forEach((fromNodeDto) => {
                const fromId = this.placeNodeDto(fromNodeDto, depth - 2);
                const label = fromNodeDto.link.type;
                const connectionKey = `${fromId}->${node.id}:${label}`;
                const reverseKey = `${node.id}->${fromId}:${label}`;

                if (!this.placedConnections.has(reverseKey)) {
                    const labelNodeId = this.addLabelNode(label, depth - 1, fromNodeDto.unique);
                    this.editor.addConnection(fromId, labelNodeId, 'output_1', 'input_1');
                    this.editor.addConnection(labelNodeId, node.id, 'output_1', 'input_1');
                    this.placedConnections.set(connectionKey, {from: fromId, label: labelNodeId, to: node.id});
                    // Add arrow direction
                    this.target.querySelector(
                        `.drawflow .connection.node_in_node-${node.id}.node_out_node-${labelNodeId} path`)
                        .classList.add('arrow-right');
                } else {
                    // If this connection already exists, then we have a bidirectional. In this case
                    // show it in the UI appropriately.
                    const existingConnection = this.placedConnections.get(reverseKey);
                    this.target.querySelector(
                        `.drawflow .connection.node_in_node-${existingConnection.label}.node_out_node-${existingConnection.from} path`)
                        .classList.add('arrow-left');
                    this.target.querySelector(
                        `.drawflow .connection.node_in_node-${existingConnection.to}.node_out_node-${existingConnection.label} path`)
                        .classList.add('arrow-right');
                }
            });
        }

        // === Handle toNodes ===
        if (Array.isArray(nodeDto.toNodes)) {
            nodeDto.toNodes.forEach((toNodeDto) => {
                const toId = this.placeNodeDto(toNodeDto, depth + 2);
                const label = toNodeDto.link.type;
                const connectionKey = `${node.id}->${toId}:${label}`;
                const reverseKey = `${toId}->${node.id}:${label}`;

                if (!this.placedConnections.has(reverseKey)) {
                    const labelNodeId = this.addLabelNode(label, depth + 1, toNodeDto.unique);
                    this.editor.addConnection(node.id, labelNodeId, 'output_1', 'input_1');
                    this.editor.addConnection(labelNodeId, toId, 'output_1', 'input_1');
                    this.placedConnections.set(connectionKey, {from: node.id, label: labelNodeId, to: toId});
                    // Add arrow direction
                    this.target.querySelector(
                        `.drawflow .connection.node_in_node-${toId}.node_out_node-${labelNodeId} path`)
                        .classList.add('arrow-right');
                } else {
                    // If this connection already exists, then we have a bidirectional. In this case
                    // show it in the UI appropriately.
                    const existingConnection = this.placedConnections.get(reverseKey);
                    this.target.querySelector(
                        `.drawflow .connection.node_in_node-${existingConnection.label}.node_out_node-${existingConnection.from} path`)
                        .classList.add('arrow-left');
                    this.target.querySelector(
                        `.drawflow .connection.node_in_node-${existingConnection.to}.node_out_node-${existingConnection.label} path`)
                        .classList.add('arrow-right');
                }
            });
        }

        return node.id;
    }
}

export {DrawflowJS}