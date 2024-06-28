import {
    Mesh,
    MathUtils,
    SphereGeometry,
    Vector3,
} from 'https://cdn.skypack.dev/three@0.132.2';
import { MeshBasicMaterial, Group, BackSide } from 'three';

function createSphere(radius, widthSegments, heightSegments) {
    const geometry = new SphereGeometry(radius, widthSegments, heightSegments);

    const material = new MeshBasicMaterial({
        color: 'gold',
        transparent: true,
        wireframe: true,
        opacity: 0.5,
    });

    const sphere = new Mesh(geometry, material);

    // Create an outline sphere
    const outlineGeometry = new SphereGeometry(radius * 1.1, widthSegments, heightSegments);
    const outlineMaterial = new MeshBasicMaterial({
        color: 'rgba(125, 125, 125, 0.8)',
        // wireframe: true,
        side: BackSide,
    });
    const outline = new Mesh(outlineGeometry, outlineMaterial);
    outline.userData.noHover = true;
    //sphere.add(outline);

    sphere.tick = (delta) => {
    };

    return sphere;
}

function createDefaultNodeSphere() {
    return createSphere(0.3, 32, 16);
}

export { createSphere, createDefaultNodeSphere };
