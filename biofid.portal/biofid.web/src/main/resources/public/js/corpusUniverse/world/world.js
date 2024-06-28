import { FontLoader } from 'three/addons/loaders/FontLoader.js';

import {createCamera, focusPoint, tweenCameraToPos} from './components/camera.js';
import { createCube } from './components/cube.js';
import { createLights } from './components/lights.js';
import { createScene } from './components/scene.js';
import { ThreeText, addTextOntoCube } from './components/3text.js';

import { createRenderer } from './systems/renderer.js';
import { createControls } from './systems/controls.js';
import { Resizer } from './systems/Resizer.js';
import { Loop } from './systems/Loop.js';
import { Raycaster, Vector2, Vector3 } from 'three';
import { createRaycaster, getIntersectedObjects } from './systems/raycaster.js';
import {Network} from "./components/Models/Network.js";

let camera;
let renderer;
let scene;
let loop;
let controls;
let worldContainer;
let raycaster;

let lastHoveredNode = null;
let currentFocusedNode = null;

class World {
    constructor(container, universeId) {
        this.universeId = universeId;
        this.network = undefined;
        this.loading = true;
        this.font = undefined;

        camera = createCamera();
        renderer = createRenderer();
        scene = createScene();
        loop = new Loop(camera, scene, renderer);
        raycaster = createRaycaster();

        // Add all events here
        renderer.domElement.addEventListener('click', this.handleOnMouseClick);
        renderer.domElement.addEventListener('mousemove', this.handleMouseMove);

        container.append(renderer.domElement);

        controls = createControls(camera, renderer.domElement);
        const { ambientLight, mainLight } = createLights();

        loop.updatables.push(controls);
        scene.add(ambientLight, mainLight);

        const fontLoader = new FontLoader();
        fontLoader.load('/fonts/Open_Sans_Regular.json', (font) => {
            this.onFontLoaded(font);
        });

        worldContainer = container;
        const resizer = new Resizer(container, camera, renderer);
    }

    // Once the font loaded, we can interact with and create text 
    onFontLoaded(font) {
        this.font = font;
        console.log('Font loaded');
    }

    async init() {
        console.log('World initiated.');
    }

    setNetworkFromDto(networkDto){
        this.loading = true;
        let newNetwork = new Network(networkDto.level);
        newNetwork.setNodesFromDto(networkDto.nodes);

        this.network = newNetwork;
        this.loading = false;
    }

    getUniverseId(){ return this.universeId; }

    /**
     * Function that takes the current network in the world and redraws it completly
     * @returns {Promise<void>}
     */
    async redrawNetwork(){
        this.loading = true;

        if(this.network === undefined || this.network.getNodes().length === 0){
            console.error("Can't redraw network as network is undefined or empty");
            return;
        }

        await this.network.drawNetwork(this.font, scene, loop, camera);
        // After we drew the network, we focus the first node
        //focusPoint(camera, controls, this.network.getNodes()[0].getTsne3d());
        focusNode(this.network.getNodes()[0].getObjectMesh());
        this.loading = false;
    }

    handleOnMouseClick(event){
        const intersects = getIntersectedObjects(event, worldContainer, raycaster, camera, scene);

        if(intersects.length === 0){
            unfocusNode();
            return;
        }

        const clickedObj = intersects[0].object;
        if(clickedObj.geometry.type === 'SphereGeometry'){
            focusNode(clickedObj);
        }
    }

    handleMouseMove(event){
        const intersects = getIntersectedObjects(event, worldContainer, raycaster, camera, scene);
        // To check if we hover an item of interest, we shoot a ray and see if it hits anything
        if(intersects.length === 0){
            if(lastHoveredNode != null){
                if(currentFocusedNode == null || lastHoveredNode.userData.id !== currentFocusedNode.userData.id)
                    lastHoveredNode.material.color.copy(lastHoveredNode.userData.defaultColor);
            }
            lastHoveredNode = null;
            $('html, body').css('cursor', 'default');
            return;
        }

        $('html, body').css('cursor', 'pointer');
        const hoveredObject = intersects[0].object;
        if(hoveredObject.userData.noHover === true) return;

        // Handle the different object types
        // Nodes
        if(hoveredObject.geometry.type === 'SphereGeometry'){
            // If the hovered node is the currenly selected one
            if(currentFocusedNode != null && hoveredObject.userData.id === currentFocusedNode.userData.id) return;
            // When we hover over the node that is already being hovered
            if(lastHoveredNode != null && lastHoveredNode.userData.id === hoveredObject.userData.id) return;
            hoveredObject.material.color.set('gold');

            lastHoveredNode = hoveredObject;
        }
    }

    focusNodeByDocumentId(documentId){
        if(this.network === undefined || this.network.length === 0 || this.loading) return;
        focusNode(this.network.getNodeByDocumentId(documentId).getObjectMesh());
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

function unfocusNode(){
    controls.autoRotate = false;
    if(currentFocusedNode != null) currentFocusedNode.material.color.copy(currentFocusedNode.userData.defaultColor);
    currentFocusedNode = null;
}

function focusNode(nodeMesh){
    // Unfocusing potential current nodes
    unfocusNode();
    controls.autoRotate = true;
    const nodePos = nodeMesh.position;
    nodeMesh.material.color.set('gold');
    tweenCameraToPos(camera, controls, nodePos, 1, 3);
    currentFocusedNode = nodeMesh;
}


export { World };
