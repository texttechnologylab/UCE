import { Editor } from "@tiptap/core";
import StarterKit from "@tiptap/starter-kit";

export class TipTapEditor {
    constructor() {
        this.editorContainer = null;
        this.editor = null;
        this.initContainer();
        this.initEventListeners();
    }

    initContainer() {
        this.editorContainer = document.createElement('div');
        this.editorContainer.className = 'tiptap-editor-container';
        this.editorContainer.innerHTML =
            `<div class="tiptap-toolbar">
                <button data-action="bold"><i class="fas fa-bold"></i></button>
                <button data-action="italic"><i class="fas fa-italic"></i></button>
                <button data-action="save" class="ml-auto btn btn-primary">Save</button>
                <button data-action="close"><i class="fas fa-times"></i></button>
            </div>
            <div class="tiptap-editor"></div>`;
        document.body.appendChild(this.editorContainer);
    }

    initEventListeners() {
        document.querySelector('.edit-document-btn').addEventListener('click', () => {
            this.openEditor();
        });

        this.editorContainer.addEventListener('click', (e) => {
            const button = e.target.closest('button');
            if (!button) return;

            const action = button.dataset.action;
            if (action) {
                e.preventDefault();
                switch (action) {
                    case 'bold':
                        this.editor.chain().focus().toggleBold().run();
                        break;
                    case 'italic':
                        this.editor.chain().focus().toggleItalic().run();
                        break;
                    case 'save':
                        this.saveContent();
                        break;
                    case 'close':
                        this.closeEditor();
                        break;
                }
            }
        });
    }

    openEditor() {
        const documentContent = document.querySelector('.document-content');
        if (!documentContent) {
            console.error('Document content area not found!');
            return;
        }

        const content = Array.from(documentContent.querySelectorAll('.paragraph'))
                             .map(p => p.innerHTML)
                             .join('');

        if (this.editor) {
            this.editor.destroy();
        }

        this.editor = new Editor({
            element: this.editorContainer.querySelector('.tiptap-editor'),
            extensions: [
                StarterKit.configure({}),
            ],
            content: content,
        });

        this.editorContainer.style.display = 'block';
    }

    saveContent() {
        const documentContent = document.querySelector('.document-content');
        if (documentContent && this.editor) {
            const newContent = this.editor.getHTML();
            const fstParagraph = documentContent.querySelector(".paragraph");
            if (fstParagraph) {
                fstParagraph.innerHTML = newContent;
            }
        }
        this.closeEditor();
    }

    closeEditor() {
        if (this.editor) {
            this.editor.destroy();
            this.editor = null;
        }
        this.editorContainer.style.display = 'none';
    }
}
