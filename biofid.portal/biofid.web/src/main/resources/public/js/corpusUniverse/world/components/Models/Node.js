import {createDefaultNodeSphere, createSphere} from "./../sphere.js";
import {Vector3} from 'three';

class Node {

    constructor(documentId, tsne2d, tsne3d, primaryTopic, documentLength, title) {
        this.documentId = documentId;
        this.tsne2d = tsne2d;
        this.tsne3d = tsne3d;
        this.title = title;
        this.documentLength = documentLength;
        this.primaryTopic = primaryTopic;
        this.id = generateUUID();
        this.neighbors = [];
        this.objectMesh = undefined;
        this.planetId = undefined;
    }

    getPlanetId(){
        return this.planetId;
    }

    getId() {
        return this.id;
    }

    getTsne3d() {
        return this.tsne3d;
    }

    getTsne3dAsVec() {
        return new Vector3(this.tsne3d[0], this.tsne3d[1], this.tsne3d[2]);
    }

    getDocumentId() {
        return this.documentId;
    }

    getObjectMesh() {
        return this.objectMesh;
    }

    setNewDefaultColor(color) {
        this.objectMesh.userData.defaultColor = color;
    }

    setPlanetId(id){
        this.planetId = id;
    }

    getPrimaryTopic() {
        return this.primaryTopic;
    }

    setNeighbors(neighbors) {
        this.neighbors = neighbors;
    }

    getNeighbors() {
        return this.neighbors;
    }

    /**
     * Sets a new position to this node, also in the mesh of the world.
     * @param pos
     */
    set3dPosition(pos) {
        this.tsne3d = pos;
        if (this.objectMesh !== undefined) {
            this.objectMesh.position.set(pos[0], pos[1], pos[2]);
        }
    }

    hasSamePosition(pos) {
        return this.tsne3d[0] === pos[0]
            && this.tsne3d[1] === pos[1]
            && this.tsne3d[2] === pos[2]
    }

    /**
     * Places this node onto the screen.
     */
    async draw(font, scene, loop, camera) {
        const sphereMesh = createDefaultNodeSphere(this.documentLength);
        sphereMesh.position.set(
            this.tsne3d[0],
            this.tsne3d[1],
            this.tsne3d[2]);
        sphereMesh.material.color.set('gray');
        sphereMesh.material.opacity = 1;
        sphereMesh.renderOrder = 3;

        sphereMesh.userData.primaryTopic = this.primaryTopic;
        sphereMesh.userData.documentId = this.documentId;
        sphereMesh.userData.defaultColor = sphereMesh.material.color.clone();
        sphereMesh.userData.id = this.id;

        this.objectMesh = sphereMesh;
        loop.updatables.push(sphereMesh);
        scene.add(sphereMesh);
    }

}

export {Node};