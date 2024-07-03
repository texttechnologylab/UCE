import {
  BoxBufferGeometry,
  Mesh,
  MathUtils,
  MeshStandardMaterial,
  TextureLoader
} from 'https://cdn.skypack.dev/three@0.132.2';

function createMaterial() {
  // create a "standard" material using
  // the texture we just loaded as a color map
  const material = new MeshStandardMaterial({
    // map: texture,
    color: 'white'
  });

  return material;
}

function createCube(sizeX, sizeY, sizeZ) {
  const geometry = new BoxBufferGeometry(sizeX, sizeY, sizeZ);

  const material = createMaterial();
  const cube = new Mesh(geometry, material);

  cube.material.transparent = true;
  cube.material.opacity = 0.5;

  const radiansPerSecond = MathUtils.degToRad(30);

  cube.tick = (delta) => {
  };

  return cube;
}

export { createCube };

