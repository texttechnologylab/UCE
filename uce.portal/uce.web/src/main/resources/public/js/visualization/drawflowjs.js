class DrawflowJS {

    constructor(target) {
        this.target = target;
        this.editor = new Drawflow(target);
        this.editor.start();
        this.nodes = [];
        this.placedNodes = new Map(); // key: unique, value: node ID + position
        this.connectionSet = new Set();  // in constructor
        //this.editor.editor_mode = 'view';
    }

    init(nodeDto) {
        console.log(nodeDto);
        this.placedNodes = new Map();
        this.nodes = [];
        this.placeNodeDto(nodeDto, 1, 1); // root starts at (1,1)
        activatePopovers();
    }

    createNodeFromNodeDto(nodeDto){
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

    addLabelNode(labelText, depth, row) {
        const spacingX = 100;
        const spacingY = 300;

        const posX = depth * spacingX;
        const posY = row * spacingY;

        const html = `
            <div class="p-2 label color-secondary large-font">
                ${labelText}
            </div>
        `;
        const id = this.editor.addNode('label', 1, 1, posX, posY, 'html', {}, html);
        return id;
    }

    placeNodeDto(nodeDto, depth = 0, row = 0) {
        if (this.placedNodes.has(nodeDto.unique)) {
            return this.placedNodes.get(nodeDto.unique).id;
        }

        const spacingX = 600;
        const spacingY = 300;

        const node = this.createNodeFromNodeDto(nodeDto);
        const posX = depth * spacingX;
        const posY = row * spacingY;

        node.id = this.editor.addNode(node.type, 1, 1, posX, posY, 'html', node, node.html);
        this.nodes.push(node);
        this.placedNodes.set(nodeDto.unique, { id: node.id, posX, posY });

        let currentRow = row;

        // === Handle fromNodes ===
        if (Array.isArray(nodeDto.fromNodes)) {
            nodeDto.fromNodes.forEach((fromNodeDto, i) => {
                const fromId = this.placeNodeDto(fromNodeDto, depth - 2, currentRow++);
                const label = fromNodeDto.link.type;
                const connectionKey = `${fromId}->${node.id}:${label}`;
                const reverseKey = `${node.id}->${fromId}:${label}`;

                if (!this.connectionSet.has(reverseKey)) {
                    const labelNodeId = this.addLabelNode(label, depth - 1, currentRow - 1);
                    this.editor.addConnection(fromId, labelNodeId, 'output_1', 'input_1');
                    this.editor.addConnection(labelNodeId, node.id, 'output_1', 'input_1');
                    this.connectionSet.add(connectionKey);
                }
            });
        }

        // === Handle toNodes ===
        if (Array.isArray(nodeDto.toNodes)) {
            nodeDto.toNodes.forEach((toNodeDto, i) => {
                const toId = this.placeNodeDto(toNodeDto, depth + 2, currentRow++);
                const label = toNodeDto.link.type;
                const connectionKey = `${node.id}->${toId}:${label}`;
                const reverseKey = `${toId}->${node.id}:${label}`;

                if (!this.connectionSet.has(reverseKey)) {
                    const labelNodeId = this.addLabelNode(label, depth + 1, currentRow - 1);
                    this.editor.addConnection(node.id, labelNodeId, 'output_1', 'input_1');
                    this.editor.addConnection(labelNodeId, toId, 'output_1', 'input_1');
                    this.connectionSet.add(connectionKey);
                }
            });
        }

        return node.id;
    }
}

export {DrawflowJS}