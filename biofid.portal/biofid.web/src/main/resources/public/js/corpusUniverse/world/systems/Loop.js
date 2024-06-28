import { Clock } from "https://cdn.skypack.dev/three@0.132.2";
import { TWEEN } from 'https://unpkg.com/three@0.139.0/examples/jsm/libs/tween.module.min.js';

const clock = new Clock();

class Loop {
    constructor(camera, scene, renderer) {
        this.camera = camera;
        this.scene = scene;
        this.renderer = renderer;
        this.updatables = [];
    }

    start() {
        this.renderer.setAnimationLoop(() => {
            // tell every animated object to tick forward one frame
            this.tick();

            // render a frame
            this.renderer.render(this.scene, this.camera);

            // Update tween
            //TWEEN.update();
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
