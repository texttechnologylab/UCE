import { TipTapEditor } from './tiptap-editor.js';

document.addEventListener('DOMContentLoaded', () => {
    if (document.querySelector('.edit-document-btn')) {
        window.tiptapEditor = new TipTapEditor();
    }
});