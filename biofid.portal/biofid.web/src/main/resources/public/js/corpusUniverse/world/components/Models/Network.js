import {Node} from "./Node.js";
import {focusPoint, tweenCameraToPos} from "./../camera.js";

class Network {
    constructor(level) {
        this.nodes = [];
        this.level = level;
    }

    getNodes(){return this.nodes;}
    getNodeByDocumentId(documentId){return this.nodes.find(n => n.getDocumentId().toString() === documentId.toString());}

    setNodesFromDto(nodeDtos){
        this.nodes = []
        nodeDtos.forEach(n =>
        {
            // Sometimes, the coordinates of the nodes are exactly the same. In those cases
            // I move them just a tiny bit so that they dont stack 1:1 on each other.
            if(this.nodes.filter(node => node.hasSamePosition(n.tsne3d))){
                n.tsne3d = addNoise(n.tsne3d, 2);
            }
            this.nodes.push(new Node(n.documentId, n.tsne2d, n.tsne3d, n.primaryTopic))
        });
    }

    /**
     * Draws the network onto the canvas
     * @returns {Promise<void>}
     */
    async drawNetwork(font, scene, loop, camera){
        if(this.nodes.length === 0) return;
        for (const n of this.nodes) {
            await n.draw(font, scene, loop, camera);
        }
    }


}

export { Network };