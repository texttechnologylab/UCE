import { BufferGeometry, Line, LineBasicMaterial } from 'three';

function createEdge(fromVec3, toVec3) {

    const geometry = new BufferGeometry().setFromPoints([fromVec3, toVec3]);
    const material = new LineBasicMaterial({ color: 'white', linewidth: 0.5 });

    const line = new Line(geometry, material);
    line.material.transparent = true;
    line.material.opacity = 0.5;
    line.tick = (delta) => {
    };

    return line;
}

export { createEdge };

