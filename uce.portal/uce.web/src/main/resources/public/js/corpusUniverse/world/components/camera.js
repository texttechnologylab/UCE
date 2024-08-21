import { PerspectiveCamera, OrthographicCamera } from 'https://cdn.skypack.dev/three@0.132.2';
// import { TWEEN } from 'https://unpkg.com/three@0.139.0/examples/jsm/libs/tween.module.min.js';
import { Raycaster, Vector2, Vector3 } from 'three';

function createMinimapCamera(){
    let minimapCamera = new OrthographicCamera(-50, 50, 50, -50, 1, 1500);
    minimapCamera.position.set(0, 300, 0); // Position it above the scene
    minimapCamera.lookAt(0, 0, 0); // Look at the center of the scene
    return minimapCamera;
}

function createCamera() {
  const camera = new PerspectiveCamera(
    35, // 35 fov = Field Of View
    1, // aspect ratio (dummy value)
    0.1, // near clipping plane
    1500, // far clipping plane (distance)
  );

  // move the camera back so we can view the scene
  camera.position.set(0, 0, 500);

  return camera;
}

function tweenCameraToPos(camera, controls, targetPos, duration, zoom, xOffset = 0) {
    controls.enabled = false;

    // Create initial target object for smooth tweening of the camera focus
    let initialTarget = controls.target.clone();
    let target = { x: initialTarget.x, y: initialTarget.y, z: initialTarget.z };

    // Tween the camera position
    gsap.to(camera.position, {
        duration: duration,
        x: targetPos.x + xOffset,
        y: targetPos.y,
        z: targetPos.z + zoom,
        onUpdate: function() {
            // Update the controls target smoothly
            controls.target.set(target.x, target.y, target.z);
            controls.update();
        },
        onComplete: function() {
            controls.enabled = true;
        }
    });

    // Tween the target position for smooth camera focus
    gsap.to(target, {
        duration: duration,
        x: targetPos.x,
        y: targetPos.y,
        z: targetPos.z,
        onUpdate: function() {
            controls.target.set(target.x, target.y, target.z);
            controls.update();
        }
    });
}

function focusPoint(camera, controls, position3d){
    const nodeX = position3d[0];
    const nodeY = position3d[1];
    const nodeZ = position3d[2];
    camera.position.set(nodeX, nodeY, nodeZ + 20);
    controls.target.set(nodeX, nodeY, nodeZ);
}

export { createCamera, tweenCameraToPos, focusPoint, createMinimapCamera };
