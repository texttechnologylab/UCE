import { TipTapEditor } from './tiptap-editor.js';

/**
 * Initializes the TipTap editor functionality when the DOM is fully loaded.
 * It checks for the presence of an element with the class 'edit-document-btn'
 * If found, creates a new instance of the TipTapEditor, making it accessible 
 * via `window.tiptapEditor`.
 */
document.addEventListener('DOMContentLoaded', () => {
    // Check if the button that triggers the editor exists.
    if (document.querySelector('.edit-document-btn')) {
        // If it exists, create a new editor instance.
        window.tiptapEditor = new TipTapEditor();
    }
});
