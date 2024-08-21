import { OrbitControls } from 'three/addons/controls/OrbitControls.js';

function createControls(camera, canvas) {
    const controls = new OrbitControls(camera, canvas);

    // damping and auto rotation require
    // the controls to be updated each frame

    controls.autoRotate = false;
    controls.target.set(0, 0, 0);
    controls.enableDamping = true;
    controls.enableRotate = true;

    controls.tick = () => controls.update();

    return controls;
}

export { createControls };