import { Color, Scene } from 'https://cdn.skypack.dev/three@0.132.2';

function createScene() {
  const scene = new Scene();

  scene.background = null;

  return scene;
}

export { createScene };
