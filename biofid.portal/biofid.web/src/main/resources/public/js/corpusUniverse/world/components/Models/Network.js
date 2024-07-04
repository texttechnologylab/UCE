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

class Network {
    constructor(level) {
        this.nodes = [];
        this.isReducedView = false;
        this.level = level;
        this.clusterEpsilon = 6; // TODO: Maybe spread the tsne3d coordinates more
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

    getNodeByDocumentId(documentId) {
        return this.nodes.find(n => n.getDocumentId().toString() === documentId.toString());
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

            this.nodes.push(new Node(n.documentId, n.tsne2d, n.tsne3d, n.primaryTopic, n.documentLength))
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
        this.addDbscanClustering();
    }

    /**
     * Clusters the currently given nodes and draws those clusters as well.
     */
    addDbscanClustering() {
        if (this.scene === undefined) return;

        const points = this.nodes.map(n => n.getTsne3dAsVec());
        const {clusters, noise} = dbscan(points, this.clusterEpsilon, this.clusterMinPoints);

        // After we've calculated the clusters, make them unique in the universe with colors and edges.
        clusters.forEach((cluster, index) => {
            const color = new Color(Math.random(), Math.random(), Math.random());
            const center = calculateClusterCenter(cluster);

            // Spawn a sphere in the center of the cluster?...
            const sphere = createSphere(0.3, 30, 30);
            sphere.material.color.set(new Color(0, 0, 0));
            sphere.position.set(new Vector3(center.x, center.y, center.z));
            this.loop.updatables.push(sphere);
            this.scene.add(sphere);

            let hasConvexMesh = false;

            // Create the convex hull mesh for each cluster
            const clusterPoints = cluster.map(point => new Vector3(point.x, point.y, point.z)).filter(p => p !== undefined);

            // Remove duplicates
            const uniqueClusterPoints = clusterPoints.filter((point, index, self) =>
                    index === self.findIndex((t) => (
                        t.x === point.x && t.y === point.y && t.z === point.z
                    ))
            );

            if (uniqueClusterPoints.length >= 4) { // Convex hull requires at least 3 points
                try {
                    // Create the default cluster mesh
                    const convexGeometry = new ConvexGeometry(uniqueClusterPoints);
                    const meshMaterial = new MeshBasicMaterial(
                        {color: color, opacity: 0.4, transparent: true, side: DoubleSide});
                    const convexMesh = new Mesh(convexGeometry, meshMaterial);
                    convexMesh.userData.center = center;

                    // Create a second convex hull mesh with scaled-down vertices towards the center
                    const smallerVertices = uniqueClusterPoints.map(v => {
                        const direction = new Vector3().copy(v).sub(center).normalize(); // Direction towards the center
                        const scaledVertex = new Vector3().copy(v).sub(direction.multiplyScalar(0.01)); // Scale down by 1% towards the center
                        return scaledVertex;
                    }); // Scale down by 0.99
                    const insideConvexGeometry = new ConvexGeometry(smallerVertices);
                    const insideMaterial = new MeshBasicMaterial(
                        {color: color, opacity: 1.0, transparent: true, side: BackSide});
                    const insideConvexMesh = new Mesh(insideConvexGeometry, insideMaterial);
                    insideConvexMesh.userData.noClick = true;
                    convexMesh.add(insideConvexMesh);

                    // Optional: Create edges for better visualization of the convex hull
                    const edges = new EdgesGeometry(convexGeometry);
                    const edgesMaterial = new LineBasicMaterial({color: color, opacity: 0.5, transparent: true});
                    const lineSegments = new LineSegments(edges, edgesMaterial);
                    lineSegments.userData.defaultColor = color;
                    lineSegments.userData.defaultOpacity = 0.5;

                    convexMesh.add(lineSegments);

                    // Add the convex to scene
                    this.scene.add(convexMesh);
                    hasConvexMesh = true;
                } catch (error) {
                    console.error(error);
                    console.warn("Couldnt build convex mesh for cluster.");
                }
            }

            cluster.forEach(point => {
                const node = this.nodes.find(n => n.hasSamePosition([point.x, point.y, point.z]));
                if (node) {
                    if(!hasConvexMesh){
                        // Add the edges to the center of the cluster.
                        // TODO: These edges are very costly. Maybe do it differently?
                        const edge = createEdge(new Vector3(center.x, center.y, center.z), new Vector3(point.x, point.y, point.z));
                        edge.userData.noHover = true;
                        edge.material.color.set(color);
                        edge.material.opacity = 0.2;
                        this.loop.updatables.push(edge);
                        this.scene.add(edge);
                    }

                    node.getObjectMesh().material.color.set(color);
                    node.setNewDefaultColor(color);
                }
            })
        });
    }

}

export {Network};