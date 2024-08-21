import {Node} from "./Node.js";
import {focusPoint, tweenCameraToPos} from "./../camera.js";
import {addNoise, calculateClusterCenter, dbscan} from "./../../systems/mathUtils.js";
import {
    BackSide,
    DoubleSide,
    Color,
    Vector3,
    Mesh,
    MeshBasicMaterial,
    BufferGeometry,
    LineSegments,
    LineBasicMaterial,
    EdgesGeometry
} from 'three';
import {createEdge} from "./../edge.js";
import {createCube} from "./../cube.js";
import {createSphere} from "./../sphere.js";

import {ConvexGeometry} from 'three/addons/geometries/ConvexGeometry.js';
import {Planet} from "./Planet.js";

class Network {
    constructor(level) {
        this.nodes = [];
        this.planets = [];
        this.isReducedView = false;
        this.level = level;
        this.clusterEpsilon = 4.5;
        this.clusterMinPoints = 2;
        this.nodePosScaling = 3.2;

        this.font = undefined;
        this.scene = undefined;
        this.loop = undefined;
        this.camera = undefined;
    }

    getNodes() {
        return this.nodes;
    }

    /**
     * the neighbors are the other nodes in the planet, if this node belongs to a planet.
     * @param node
     */
    getNeighborsOfNode(node){
        const planet = this.planets.find(p => p.getId() === node.getPlanetId());
        if(planet !== undefined) return planet.getNodes().filter(n => n.getId() !== node.getId());
        return [];
    }

    getPlanetOfNode(node){
        if(node.getPlanetId() === undefined) return null;
        return this.planets.find(p => p.getId() === node.getPlanetId());
    }

    getNodeByDocumentId(documentId) {
        return this.nodes.find(n => n.getDocumentId().toString() === documentId.toString());
    }

    getNodeById(id){
        return this.nodes.find(n => n.getId().toString() === id.toString());
    }

    setIsReducedView(isReduced) {
        this.isReducedView = isReduced;
    }

    setNodesFromDto(nodeDtos) {
        this.nodes = []
        nodeDtos.forEach(n => {
            // Sometimes, the coordinates of the nodes are exactly the same. In those cases
            // I move them just a tiny bit so that they dont stack 1:1 on each other.
            if (this.nodes.filter(node => node.hasSamePosition(n.tsne3d))) {
                n.tsne3d = addNoise(n.tsne3d, 1.34);
            }

            if (!this.isReducedView) {
                // The universe often seems a bit cluttered. Hence, I scale every position up, so they
                // expand more.
                n.tsne3d = n.tsne3d.map(i => i * this.nodePosScaling);
            }

            this.nodes.push(new Node(n.documentId, n.tsne2d, n.tsne3d, n.primaryTopic, n.documentLength, n.title))
        });
    }

    /**
     * Draws the network onto the canvas
     * @returns {Promise<void>}
     */
    async drawNetwork(font, scene, loop, camera) {
        if (this.nodes.length === 0) return;
        this.font = font;
        this.scene = scene;
        this.loop = loop;
        this.camera = camera;

        for (const n of this.nodes) {
            await n.draw(font, scene, loop, camera);
        }

        // After we drew the nodes, cluster them.
        console.log('Clustering...');
        this.planets = this.calculatePlanets();

        // After clustering, we apply force push
        console.log('Applying Force Direction...');
        this.applyDirectionForce(this.nodes);

        // Draw the planets. Always call this after applying any force.
        this.planets.forEach(planet => planet.drawPlanet(this.scene, this.loop));

        console.log('Drew the network.');
    }

    /**
     * A classical force directed push which is known by force direct graphs e.g.
     * https://observablehq.com/@d3/force-directed-graph-component
     * @param nodes
     * @param iterations
     * @param repulsionForce
     * @param attractionForce
     */
    applyDirectionForce(nodes,
                        iterations = 50,
                        repulsionForce = 150,
                        attractionForce = 0.00035) {

        for (let i = 0; i < iterations; i++) {
            // Calculate repulsive forces
            nodes.forEach((nodeA, indexA) => {
                nodes.forEach((nodeB, indexB) => {
                    if (nodeA === undefined || nodeB === undefined) return;
                    if (indexA !== indexB) {
                        let dx = nodeA.getTsne3d()[0] - nodeB.getTsne3d()[0];
                        let dy = nodeA.getTsne3d()[1] - nodeB.getTsne3d()[1];
                        let dz = nodeA.getTsne3d()[2] - nodeB.getTsne3d()[2];
                        let distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                        if (distance < 1) distance = 1; // Avoid division by zero

                        let force = repulsionForce / (distance * distance);
                        let new3dPos = []
                        new3dPos[0] = nodeA.getTsne3d()[0] + (dx / distance) * force;
                        new3dPos[1] = nodeA.getTsne3d()[1] + (dy / distance) * force;
                        new3dPos[2] = nodeA.getTsne3d()[2] + (dz / distance) * force;
                        nodeA.set3dPosition(new3dPos);
                    }
                });
            });

            // Calculate attractive forces
            nodes.forEach(nodeA => {
                this.getNeighborsOfNode(nodeA).forEach(nodeB => {
                    let dx = nodeA.getTsne3d()[0] - nodeB.getTsne3d()[0];
                    let dy = nodeA.getTsne3d()[1] - nodeB.getTsne3d()[1];
                    let dz = nodeA.getTsne3d()[2] - nodeB.getTsne3d()[2];
                    let distance = Math.min(999, Math.sqrt(dx * dx + dy * dy + dz * dz));

                    let force = (distance * distance) * attractionForce;
                    let newPosA = [];
                    newPosA[0] = nodeA.getTsne3d()[0] - (dx / distance) * force;
                    newPosA[1] = nodeA.getTsne3d()[1] - (dy / distance) * force;
                    newPosA[2] = nodeA.getTsne3d()[2] - (dz / distance) * force;
                    nodeA.set3dPosition(newPosA);

                    let newPosB = [];
                    newPosB[0] = nodeB.getTsne3d()[0] + (dx / distance) * force;
                    newPosB[1] = nodeB.getTsne3d()[1] + (dy / distance) * force;
                    newPosB[2] = nodeB.getTsne3d()[2] + (dz / distance) * force;
                    nodeB.set3dPosition(newPosB)
                });
            });
        }
    }

    /**
     * Calculates planets by performing DB clusters and returns them.
     */
    calculatePlanets() {
        if (this.scene === undefined) return;

        const points = this.nodes.map(n => n.getTsne3dAsVec());
        const {clusters, noise} = dbscan(points, this.clusterEpsilon, this.clusterMinPoints);

        // Foreach cluster, we return a planet.
        return clusters.map(cluster => new Planet(
            cluster,
            cluster.map(point => this.nodes.find(n => n.hasSamePosition([point.x, point.y, point.z])))));
    }
}

export {Network};