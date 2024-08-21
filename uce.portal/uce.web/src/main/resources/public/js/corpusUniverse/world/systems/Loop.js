import { Clock } from "https://cdn.skypack.dev/three@0.132.2";
// import { TWEEN } from 'https://unpkg.com/three@0.139.0/examples/jsm/libs/tween.module.min.js';

const clock = new Clock();

class Loop {
    constructor(camera, scene, renderer) {
        this.camera = camera;
        this.scene = scene;
        this.renderer = renderer;
        this.updatables = [];
        this.minimapRenderer = undefined;
        this.minimapCamera = undefined;
    }

    addMinimap(minimapRenderer, minimapCamera){
        this.minimapRenderer = minimapRenderer;
        this.minimapCamera = minimapCamera;
    }

    start() {
        this.renderer.setAnimationLoop(() => {
            // tell every animated object to tick forward one frame
            this.tick();

            // render a frame
            this.renderer.render(this.scene, this.camera);

            if(this.minimapRenderer !== undefined) this.minimapRenderer.render(this.scene, this.minimapCamera);
        });
    }

    stop() {
        this.renderer.setAnimationLoop(null);
    }

    tick() {
        // only call the getDelta function once per frame!
        const delta = clock.getDelta();

        for (const object of this.updatables) {
            object.tick(delta);
        }
    }
}

export { Loop };
