import { WebGLRenderer, PCFSoftShadowMap } from 'https://cdn.skypack.dev/three@0.132.2';

function createRenderer() {
  const renderer = new WebGLRenderer({ antialias: true, alpha: true });
  // turn on the physically correct lighting model
  renderer.physicallyCorrectLights = true;

  return renderer;
}

export { createRenderer };
