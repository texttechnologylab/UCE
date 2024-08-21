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
    const outlineGeometry = new SphereGeometry(radius * 1.05, widthSegments, heightSegments);
    const outlineMaterial = new MeshBasicMaterial({
        color: 'rgba(30, 30, 30)',
        // wireframe: true,
        side: BackSide,
    });
    const outline = new Mesh(outlineGeometry, outlineMaterial);
    outline.userData.noHover = true;
    sphere.add(outline);

    sphere.tick = (delta) => {
    };

    return sphere;
}

function createDefaultNodeSphere(textLength) {
    // TODO: Adjust this with maybe the amount of named entities??
    const complexity = Math.min(Math.max(textLength / 5000, 3), 40); // Ensure it's between 5 and 40
    return createSphere(0.3, complexity, complexity);
}

export { createSphere, createDefaultNodeSphere };
