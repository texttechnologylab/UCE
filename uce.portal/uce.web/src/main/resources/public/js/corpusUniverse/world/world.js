import {FontLoader} from 'three/addons/loaders/FontLoader.js';

import {createCamera, createMinimapCamera, focusPoint, tweenCameraToPos} from './components/camera.js';
import {createCube} from './components/cube.js';
import {createLights} from './components/lights.js';
import {createScene} from './components/scene.js';
import {ThreeText, addTextOntoCube} from './components/3text.js';

import {createRenderer} from './systems/renderer.js';
import {createControls} from './systems/controls.js';
import {Resizer} from './systems/Resizer.js';
import {Loop} from './systems/Loop.js';
import {UI} from './systems/UI.js';
import {Raycaster, Vector2, Vector3} from 'three';
import {createRaycaster, getIntersectedObjects} from './systems/raycaster.js';
import {Network} from "./components/Models/Network.js";
import {calculateCenter} from "./systems/mathUtils.js";

let camera;
let minimapCamera;
let renderer;
let ui;
let minimapRenderer;
let scene;
let loop;
let controls;
let worldContainer;
let raycaster;

let lastHoveredNode = null;
let currentFocusedNode = null;
let lastHoveredConvex = null;

class World {
    constructor(container, universeId) {
        this.universeId = universeId;
        this.network = undefined;
        this.loading = true;
        this.font = undefined;
        this.isReducedView = false;
        this.currentCenter = new Vector3();

        camera = createCamera();
        renderer = createRenderer();
        scene = createScene();
        loop = new Loop(camera, scene, renderer);
        raycaster = createRaycaster();
        // In the reduced view, we don't have a UI.
        const uiElement = document.querySelector('.corpus-universe-container-ui');
        if(uiElement)ui = new UI(uiElement, camera);

        // Add all events here
        renderer.domElement.addEventListener('mousedown', (event) => this.handleOnMouseClick(event));
        renderer.domElement.addEventListener('mousemove', this.handleMouseMove);

        container.append(renderer.domElement);

        controls = createControls(camera, renderer.domElement);
        const {ambientLight, mainLight} = createLights();

        loop.updatables.push(controls);
        scene.add(ambientLight, mainLight);

        const fontLoader = new FontLoader();
        fontLoader.load('/fonts/Open_Sans_Regular.json', (font) => {
            this.onFontLoaded(font);
        });

        worldContainer = container;
        const resizer = new Resizer(container, camera, renderer);

        // At the end, setup the minimap renderer. we do thi by adding a second camera.
        // TODO: Im not yet convinced we need a minimap and in what form...
        if (this.isReducedView) return;
        // Minimap renderer
        //minimapRenderer = createRenderer();
        //minimapRenderer.domElement.classList.add('minimap');
        //document.querySelector('.corpus-universe-container-ui').append(minimapRenderer.domElement);

        // Minimap camera
        //minimapCamera = createMinimapCamera();
        //loop.addMinimap(minimapRenderer, minimapCamera);
    }

    // Once the font loaded, we can interact with and create text 
    onFontLoaded(font) {
        this.font = font;
        console.log('Font loaded');
    }

    async init() {
        console.log('World initiated.');
    }

    setNetworkFromDto(networkDto) {
        this.loading = true;
        let newNetwork = new Network(networkDto.level);
        newNetwork.setIsReducedView(this.isReducedView);
        newNetwork.setNodesFromDto(networkDto.nodes);

        this.currentCenter = calculateCenter(newNetwork.getNodes().map(n => n.getTsne3dAsVec()));
        this.network = newNetwork;
        this.loading = false;
    }

    getUniverseId() {
        return this.universeId;
    }

    getCurrentCenter() {
        return this.currentCenter;
    }

    setIsReducedView(isReducedView) {
        this.isReducedView = isReducedView;
    }

    /**
     * Function that takes the current network in the world and redraws it completly
     * @returns {Promise<void>}
     */
    async redrawNetwork() {
        this.loading = true;

        if (this.network === undefined || this.network.getNodes().length === 0) {
            console.error("Can't redraw network as network is undefined or empty");
            return;
        }

        await this.network.drawNetwork(this.font, scene, loop, camera);
        // After we drew the network, we focus the first node
        //focusPoint(camera, controls, this.network.getNodes()[0].getTsne3d());
        focusNode(this.network.getNodes()[0].getObjectMesh(), this.network);
        this.loading = false;
    }

    handleOnMouseClick(event) {
        controls.autoRotate = false;

        // Since we have many controls for left mouse and right mouse for navigation,
        // the button to click something in the 3d space is the middle mouse button only
        if (event.button !== 1) return;

        const intersects = getIntersectedObjects(event, worldContainer, raycaster, camera, scene);

        if (intersects.length === 0) {
            unfocusNode();
            return;
        }

        const clickedObj = intersects[0].object;
        if (clickedObj.geometry.type === 'SphereGeometry') {
            focusNode(clickedObj, this.network);
        } else if (clickedObj.geometry.type === 'BufferGeometry') {
            if (!clickedObj.userData.noClick) {
                const center = clickedObj.userData.center;
                const planetId = clickedObj.userData.id;
                focusCoordinate(new Vector3(center.x, center.y, center.z));
            }
        }

    }

    handleMouseMove(event) {
        const intersects = getIntersectedObjects(event, worldContainer, raycaster, camera, scene);

        // To check if we hover an item of interest, we shoot a ray and see if it hits anything
        // Handle dehighlithing last hovered node
        if (lastHoveredNode != null) {
            if (currentFocusedNode == null || lastHoveredNode.userData.id !== currentFocusedNode.userData.id)
                lastHoveredNode.material.color.copy(lastHoveredNode.userData.defaultColor);
        }
        // Dehighlight last hovered convex
        if (lastHoveredConvex != null) {
            lastHoveredConvex.material.color.set(lastHoveredConvex.userData.defaultColor);
            lastHoveredConvex.material.opacity = lastHoveredConvex.userData.defaultOpacity;
        }
        lastHoveredNode = null;
        lastHoveredConvex = null;
        $('html, body').css('cursor', 'default');

        if (intersects.length === 0) return;
        const hoveredObject = intersects[0].object;
        if (hoveredObject.userData.noHover === true) return;

        $('html, body').css('cursor', 'pointer');
        // Handle the different object types
        // Nodes
        if (hoveredObject.geometry.type === 'SphereGeometry') {
            // If the hovered node is the currenly selected one
            if (currentFocusedNode != null && hoveredObject.userData.id === currentFocusedNode.userData.id) return;
            // When we hover over the node that is already being hovered
            if (lastHoveredNode != null && lastHoveredNode.userData.id === hoveredObject.userData.id) return;
            hoveredObject.material.color.set('gold');

            lastHoveredNode = hoveredObject;
        } else if (hoveredObject.geometry.type === 'EdgesGeometry' || hoveredObject.geometry.type === 'BufferGeometry') {
            // Handles the cluster convexes.
            let lines = hoveredObject.children;
            if (lines.length === 0) return;
            lines = lines[0];
            if(lines !== undefined){
                lines.material.color.set('black');
                lines.material.opacity = 1;
                lastHoveredConvex = lines;
            }
        }
    }

    focusNodeByDocumentId(documentId) {
        if (this.network === undefined || this.network.length === 0 || this.loading) return;
        focusNode(this.network.getNodeByDocumentId(documentId).getObjectMesh(), this.network);
    }

    render() {
        // draw a single frame
        renderer.render(scene, camera);
    }

    start() {
        loop.start();
    }

    stop() {
        loop.stop();
    }
}

function unfocusNode() {
    controls.autoRotate = false;
    if (currentFocusedNode != null) currentFocusedNode.material.color.copy(currentFocusedNode.userData.defaultColor);
    currentFocusedNode = null;
}

function focusNode(nodeMesh, network) {
    // Unfocusing potential current nodes
    unfocusNode();
    controls.autoRotate = true;
    const nodePos = nodeMesh.position;
    nodeMesh.material.color.set('gold');
    tweenCameraToPos(camera, controls, nodePos, 1, 3);
    currentFocusedNode = nodeMesh;
    const node = network.getNodeById(nodeMesh.userData.id);
    if(ui !== undefined) ui.openNodeInspector(node, network.getPlanetOfNode(node));
}

function focusCoordinate(pointVector) {
    //controls.autoRotate = true;
    tweenCameraToPos(camera, controls, pointVector, 1, 0.5);
}


export {World};
