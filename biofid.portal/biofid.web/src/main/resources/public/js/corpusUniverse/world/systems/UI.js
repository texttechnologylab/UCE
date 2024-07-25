import {Vector3} from 'three';

class UI {
    constructor(uiContainer, camera) {
        this.coordinatesContainer = uiContainer.querySelector('.coordinates');
        this.camera = camera;
        this.previousCameraPosition = new Vector3(0, 0, 0);

        this.start();
    }

    start() {
        this.updateCoordinates();

        // Update the UI every X milliseconds.
        // Ive tried using the animation loop, or tracking the camera changing, but
        // that happens Waaaay too often. So instead, we build our own little update loop.
        setTimeout(() => {
            this.start();
        }, 100);
    }

    async openNodeInspector(node, planet){
        const $inspectorWindow = $('.inspector-window');
        $inspectorWindow.show();

        // First, clone the template and put it into the inspector
        const $template = $('.inspector-window-node-content[data-type="template"]').clone();
        $template.get(0).setAttribute('data-type', 'instance');
        $template.removeClass('display-none');
        $inspectorWindow.find('.content').html($template);

        // Now fill the content template instance
        if(planet === null){
            $inspectorWindow.find('.content-group[data-type="planet-association"]').hide();
        } else{
            $inspectorWindow.find('.content-group[data-type="planet-association"] a').html(planet.getName());
        }

        let content = undefined;
        // Wrap AJAX call in a Promise to use await properly
        let result = await new Promise((resolve, reject) => {
            $.ajax({
                url: "/api/corpusUniverse/nodeInspectorContent?documentId=" + node.getDocumentId().toString(),
                type: "GET",
                success: function (response) {
                    content = response;
                    resolve(response);
                },
                error: function (xhr, status, error) {
                    console.error(xhr.responseText);
                    reject(error);
                }
            });
        });

        if(!result || content === undefined) return;

        // Add the fetched inspector content view.
        $inspectorWindow.find('.content-group[data-type="document-data"]').html(content);
    }

    updateCoordinates() {
        if (this.coordinatesContainer === undefined || this.camera === undefined) return;

        // Compare current camera position with previous position
        if (this.cameraPositionChanged(0.3)) {
            // Update the coordinates UI
            this.coordinatesContainer.querySelector('.x').innerHTML = Number(this.camera.position.x).toFixed(2);
            this.coordinatesContainer.querySelector('.y').innerHTML = Number(this.camera.position.y).toFixed(2);
            this.coordinatesContainer.querySelector('.z').innerHTML = Number(this.camera.position.z).toFixed(2);

            // Update the previous position
            this.previousCameraPosition.copy(this.camera.position);
        }
    }

    cameraPositionChanged(positionChangeThreshold) {
        const distance = this.camera.position.distanceTo(this.previousCameraPosition);
        return distance > positionChangeThreshold;
    }

}

export {UI}
