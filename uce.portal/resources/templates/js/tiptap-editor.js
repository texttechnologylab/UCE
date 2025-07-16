import { Editor } from "@tiptap/core";
import StarterKit from "@tiptap/starter-kit";

/**
 * Handles the text editing capabilities of the TipTap editor
 */
export class TipTapEditor {
    /**
     * Constructor for the text editor
     */
    constructor() {
        this.editorContainer = null;
        this.editor = null;
        this.initContainer();
        this.initEventListeners();
    }

    /**
     * Creates the editor UI
     *
     * The editor contains:
     *   - toolbar with Bold/Italic (prob not needed) and Save buttons
     *   - text editing area
     */
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

    /**
     * Sets up event listeners for the text editor
     *
     * Supports:
     *   - opening the editor using the Edit Document button
     *   - performing certain actions using buttons on the editor's toolbar
     */
    initEventListeners() {
        // listener for the button that opens the editor
        document.querySelector('.edit-document-btn').addEventListener('click', () => {
            this.openEditor();
        });

        // listener for toolbar actions 
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

    /**
     * Collects text nodes from a DOM node, handlind the addition of newlines 
     * when <br> tags are found.
     * @param {Node} node
     *          The DOM node used for collecting the text content
     * @returns {string[]}
     *          Array of text parts
     */
    collectTextNodes(node) {
        const textNodes = [];
        let prevWasBr = false;

        const collect = (currentNode) => {
            if (currentNode.nodeType === Node.TEXT_NODE && currentNode.textContent.trim() !== '') {
                textNodes.push(currentNode.textContent);
                prevWasBr = false;
            } else if (currentNode.nodeType === Node.ELEMENT_NODE) {
                if (currentNode.tagName === 'BR') {
                    if (prevWasBr) {
                        textNodes.push('\n\n');
                    } else {
                        textNodes.push('\n');
                    }
                    prevWasBr = true;
                } else if (currentNode.classList.contains('ruby-text')) {
                    textNodes.push(currentNode.textContent);
                    prevWasBr = false;
                } else {
                    Array.from(currentNode.childNodes).forEach(collect);
                    prevWasBr = false;
                }
            }
        };

        collect(node);
        return textNodes;
    }

    /**
    * Formats the collected text nodes into HTML paragraphs.
    * @param {string[]} textNodes 
    *           The array of text parts.
    * @returns {string} 
    *           The formatted HTML string.
    */
    formatParagraphText(textNodes) {
        let paraText = textNodes.join(' ');
        paraText = paraText.replace(/(?<=\s-|—)\s+(?=-|—)/g, '');   // removes whitespace between dashes
        paraText = paraText.replace(/\s+([.,;:!?)])/g, '$1');       // removes whitespace before punctuation marks and )
        paraText = paraText.replace(/(\()\s+/g, '$1');              // removes whitespace immediately after (

        let editorContent = '';
        if (paraText.trim()) {
            const subPara = paraText.split('\n\n');
            subPara.forEach(text => {
                const trimmedText = text.trim();
                if (trimmedText) {
                    const addBr = trimmedText.replace(/\n/g, '<br>');
                    editorContent += `<p>${addBr}</p>`;
                }
            });
        }
        return editorContent;
    }

    /**
    * Opens the editor and loads the content from the document.
    *
    * TODO:
    *   1. FIX weird artifact at the end of the editor.
    *       * BUG appears because of random paragraph being selected
    */
    openEditor() {
        const documentContent = document.querySelector('.document-content');
        if (!documentContent) return;

        // create a copy to preserve original document
        const contentCopy = documentContent.cloneNode(true);

        // remove non-text elements
        contentCopy.querySelectorAll('.blurrer, .text-center, .multi-annotation-popup').forEach(el => {
            el.remove();
        });

        let editorContent = '';
        const paragraphs = contentCopy.querySelectorAll('.page-content, .paragraph');

        paragraphs.forEach(paragraphEl => {
            const textNodes = this.collectTextNodes(paragraphEl);
            const paragraphHTML = this.formatParagraphText(textNodes);
            editorContent += paragraphHTML;
        });

        // initialize editor
        if (this.editor) this.editor.destroy();

        this.editor = new Editor({
            element: this.editorContainer.querySelector('.tiptap-editor'),
            extensions: [
                StarterKit.configure({
                    heading: false,
                    codeBlock: false
                }),
            ],
            content: editorContent,
        });

        this.editorContainer.style.display = 'block';
    }

    /**
     * Saves the content from the editor back into the original document structure.
     * It intelligently updates, adds, or removes paragraphs as needed.
     */
    saveContent() {
        const documentContent = document.querySelector('.document-content');
        if (documentContent && this.editor) {
            const newContentHTML = this.editor.getHTML();
            const temp = document.createElement('div');
            temp.innerHTML = newContentHTML;
            const newParagraphs = Array.from(temp.querySelectorAll('p'));

            const oldParagraphs = Array.from(documentContent.querySelectorAll('.page-content .paragraph'));
            const pageContents = documentContent.querySelectorAll('.page-content');

            const oldParagraphsCount = oldParagraphs.length;
            const newParagraphsCount = newParagraphs.length;

            // Update existing paragraphs with the new content.
            const minCount = Math.min(oldParagraphsCount, newParagraphsCount);
            for (let i = 0; i < minCount; i++) {
                oldParagraphs[i].innerHTML = newParagraphs[i].innerHTML;
            }

            if (newParagraphsCount > oldParagraphsCount) {
                // If there are new paragraphs, add them to the last page.
                const lastPage = pageContents.length > 0 ? pageContents[pageContents.length - 1] : null;
                if (lastPage) {
                    for (let i = oldParagraphsCount; i < newParagraphsCount; i++) {
                        const paraDiv = document.createElement('div');
                        paraDiv.className = 'paragraph';
                        paraDiv.innerHTML = newParagraphs[i].innerHTML;
                        lastPage.appendChild(paraDiv);
                    }
                }
            } else if (newParagraphsCount < oldParagraphsCount) {
                // If paragraphs were removed, delete the extra ones from the DOM.
                for (let i = newParagraphsCount; i < oldParagraphsCount; i++) {
                    oldParagraphs[i].remove();
                }
            }
        }
        this.closeEditor();
    }

    /**
     * Destroys the editor instance and hides the editor container.
     */
    closeEditor() {
        if (this.editor) {
            this.editor.destroy();
            this.editor = null;
        }
        this.editorContainer.style.display = 'none';
    }
}
