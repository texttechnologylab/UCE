import {Node} from "./Node.js";
import {focusPoint, tweenCameraToPos} from "./../camera.js";
import {addNoise, calculateClusterCenter, dbscan} from "./../../systems/mathUtils.js";
import { Color, Vector3 } from 'three';
import {createEdge} from "./../edge.js";
import {createCube} from "./../cube.js";

class Network {
    constructor(level) {
        this.nodes = [];
        this.level = level;
        this.clusterEpsilon = 6;
        this.clusterMinPoints = 2;

        this.font = undefined;
        this.scene = undefined;
        this.loop = undefined;
        this.camera = undefined;
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
                n.tsne3d = addNoise(n.tsne3d, 1.34);
            }
            this.nodes.push(new Node(n.documentId, n.tsne2d, n.tsne3d, n.primaryTopic, n.documentLength))
        });
    }

    /**
     * Draws the network onto the canvas
     * @returns {Promise<void>}
     */
    async drawNetwork(font, scene, loop, camera){
        if(this.nodes.length === 0) return;
        this.font = font;
        this.scene = scene;
        this.loop = loop;
        this.camera = camera;

        for (const n of this.nodes) {
            await n.draw(font, scene, loop, camera);
        }
        // After we drew the nodes, cluster them.
        this.addDbscanClustering();
    }

    /**
     * Clusters the currently given nodes and draws those clusters as well.
     */
    addDbscanClustering(){
        if(this.scene === undefined) return;

        const points = this.nodes.map(n => n.getTsne3dAsVec());
        const {clusters, noise} = dbscan(points, this.clusterEpsilon, this.clusterMinPoints);

        // After we've calculated the clusters, make them unique in the universe with colors and edges.
        clusters.forEach((cluster, index) => {
            const color = new Color(Math.random(), Math.random(), Math.random());
            const center = calculateClusterCenter(cluster);

            cluster.forEach(point => {
                const node = this.nodes.find(n => n.hasSamePosition([point.x, point.y, point.z]));
                if(node){
                    // Add the edges to the center of the cluster.
                    const edge = createEdge(new Vector3(center.x, center.y, center.z), new Vector3(point.x, point.y, point.z));
                    edge.userData.noHover = true;
                    edge.material.color.set(new Color(0.65, 0.65, 0.65));
                    this.loop.updatables.push(edge);
                    this.scene.add(edge);

                    node.getObjectMesh().material.color.set(color);
                    node.setNewDefaultColor(color);
                }
            })
        });
    }

}

export { Network };