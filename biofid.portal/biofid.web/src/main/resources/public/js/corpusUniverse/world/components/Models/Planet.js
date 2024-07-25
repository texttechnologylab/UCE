import {createDefaultNodeSphere, createSphere} from "./../sphere.js";
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
import {ConvexGeometry} from 'three/addons/geometries/ConvexGeometry.js';
import {calculateClusterCenter, calculatePlanetCenter} from "./../../systems/mathUtils.js";
import {createEdge} from "./../edge.js";

const PlanetType = {
    PLANET: 'Planet',
    DWARF_PLANET: 'Dwarf Planet',
    ASTEROID: "Asteroid",
    STAR: 'Star'
}

/**
 * A planet is a basically a bag of nodes. Its a cluster representation which has its gravity pull
 * due to the force direction we apply.
 */
class Planet {

    constructor(cluster, nodes) {
        this.cluster = cluster;
        this.center3d = undefined;
        this.id = generateUUID();
        this.objectMesh = undefined;
        this.type = PlanetType.STAR;

        // Filter the nodes to remove duplicates
        this.nodes = nodes.filter((obj1, i, arr) =>
            arr.findIndex(obj2 => (obj2.getId() === obj1.getId())) === i
        );
        this.nodes.forEach(node => node.setPlanetId(this.id));

        // the name of the planet is a combination of all the nodes names
        // TODO: Think about how we determine the name of the planet
        this.name = this.nodes[0].getPrimaryTopic();
    }

    getNodes() {
        return this.nodes;
    }
    getName(){
        return this.name;
    }

    getId() {
        return this.id;
    }

    /**
     * Draws the nodes of this planet and other features within the current scene
     */
    drawPlanet(scene, loop) {
        const color = new Color(Math.random(), Math.random(), Math.random());
        this.center3d = calculatePlanetCenter(this);

        // Spawn a sphere in the center of the cluster?...
        const sphere = createSphere(0.3, 30, 30);
        sphere.material.color.set(new Color(0, 0, 0));
        sphere.position.set(this.center3d.x, this.center3d.y, this.center3d.z);
        //loop.updatables.push(sphere);
        //scene.add(sphere);

        let hasConvexMesh = false;

        // Create the convex hull mesh for each cluster
        const clusterPoints = this.nodes.map(node => node.getTsne3dAsVec()).filter(p => p !== undefined);

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
                    { color: color, opacity: 0.3, transparent: true, side: DoubleSide });
                const convexMesh = new Mesh(convexGeometry, meshMaterial);
                convexMesh.userData.center = this.center3d;
                convexMesh.userData.id = this.id;

                // Create a second convex hull mesh with scaled-down vertices towards the center
                const smallerVertices = uniqueClusterPoints.map(v => {
                    const direction = new Vector3().copy(v).sub(this.center3d).normalize(); // Direction towards the center
                    const scaledVertex = new Vector3().copy(v).sub(direction.multiplyScalar(0.01)); // Scale down by 1% towards the center
                    return scaledVertex;
                });
                const insideConvexGeometry = new ConvexGeometry(smallerVertices);
                const insideMaterial = new MeshBasicMaterial(
                    { color: color, opacity: 1.0, transparent: true, side: BackSide });
                const insideConvexMesh = new Mesh(insideConvexGeometry, insideMaterial);
                insideConvexMesh.userData.noClick = true;
                //convexMesh.add(insideConvexMesh);

                // create edges for better visualization of the convex hull
                const edges = new EdgesGeometry(convexGeometry);
                const edgesMaterial = new LineBasicMaterial({ color: color, opacity: 0.5, transparent: true });
                const lineSegments = new LineSegments(edges, edgesMaterial);
                lineSegments.userData.defaultColor = color;
                lineSegments.userData.defaultOpacity = 0.5;

                convexMesh.add(lineSegments);

                // Add the convex to scene
                scene.add(convexMesh);
                this.objectMesh = convexMesh;
                hasConvexMesh = true;
            } catch (error) {
                console.error(error);
                console.warn("Couldnt build convex mesh for cluster.");
            }
        }

        this.nodes.forEach(node => {
            if (node) {
                if (!hasConvexMesh) {
                    const point = node.getTsne3dAsVec();
                    // Add the edges to the center of the cluster.
                    const edge = createEdge(new Vector3(this.center3d.x, this.center3d.y, this.center3d.z), point);
                    edge.userData.noHover = true;
                    edge.material.color.set(color);
                    edge.material.opacity = 0.2;
                    loop.updatables.push(edge);
                    scene.add(edge);
                }

                node.getObjectMesh().material.color.set(color);
                node.setNewDefaultColor(color);
            }
        });
    }

}

export {Planet};