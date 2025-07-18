// Toggle Sidebar
document.querySelectorAll('.tree-toggle').forEach(toggle => {
    toggle.addEventListener('click', function (e) {
        if (e.target.tagName.toLowerCase() === 'input' || e.target.tagName.toLowerCase() === 'label') return;

        const parent = this.parentElement;
        const nested = parent.querySelector('.nested');
        const icon = this.querySelector('.toggle-icon');

        if (nested) nested.classList.toggle('active');
        if (icon) icon.classList.toggle('open');
    });
});

// Master-Checkbox
const nlpMasterCheckbox = document.getElementById('all-analysis-models-checkbox');

nlpMasterCheckbox.addEventListener('change', function () {
    const allCheckboxes = document.querySelectorAll(
        '.nlp-group-checkbox, .nlp-model-checkbox, .ttlab-group-checkbox, .ttlab-subgroup-checkbox, .ttlab-model-checkbox'
    );
    // allCheckboxes.forEach(cb => cb.checked = this.checked);
    allCheckboxes.forEach(cb => {
        if (!cb.disabled) cb.checked = this.checked;
    });
    // Set indeterminate state to false for all checkboxes
    document.querySelectorAll('input[type="checkbox"]').forEach(cb => cb.indeterminate = false);
    updateAllFieldVisibilities();
});

// NLP Group-Checkbox
document.querySelectorAll('.nlp-group-checkbox').forEach(groupCheckbox => {
    groupCheckbox.addEventListener('change', function (e) {
        const groupItem = this.closest('li');
        const modelCheckboxes = groupItem.querySelectorAll('.nlp-model-checkbox');
        // modelCheckboxes.forEach(cb => cb.checked = this.checked);
        modelCheckboxes.forEach(cb => {
            if (!cb.disabled) cb.checked = this.checked;
        });
        updateNlpGroupState(this);
        updateAllFieldVisibilities();
        e.stopPropagation();
    });
});

// NLP Modell-Checkbox
document.querySelectorAll('.nlp-model-checkbox').forEach(modelCheckbox => {
    modelCheckbox.addEventListener('change', function (e) {
        updateNlpGroupState(this);
        updateAllFieldVisibilities();
        e.stopPropagation();
    });
});

function updateNlpGroupState(changedCheckbox) {
    const groupItem = changedCheckbox.closest('ol.nested')?.closest('li');
    if (!groupItem) return;
    const groupCheckbox = groupItem.querySelector('.nlp-group-checkbox');
    const modelCheckboxes = groupItem.querySelectorAll('.nlp-model-checkbox');

    const allChecked = Array.from(modelCheckboxes).every(cb => cb.checked);
    const noneChecked = Array.from(modelCheckboxes).every(cb => !cb.checked);

    if (allChecked) {
        groupCheckbox.checked = true;
        groupCheckbox.indeterminate = false;
    } else if (noneChecked) {
        groupCheckbox.checked = false;
        groupCheckbox.indeterminate = false;
    } else {
        groupCheckbox.checked = false;
        groupCheckbox.indeterminate = true;
    }

    updateNlpMasterCheckbox();
}

function updateNlpMasterCheckbox() {
    const groupCheckboxes = document.querySelectorAll('.nlp-group-checkbox, .ttlab-group-checkbox');
    const total = groupCheckboxes.length;
    const checked = Array.from(groupCheckboxes).filter(cb => cb.checked).length;
    const indeterminate = Array.from(groupCheckboxes).some(cb => cb.indeterminate);

    if (checked === total && !indeterminate) {
        nlpMasterCheckbox.checked = true;
        nlpMasterCheckbox.indeterminate = false;
    } else if (checked === 0 && !indeterminate) {
        nlpMasterCheckbox.checked = false;
        nlpMasterCheckbox.indeterminate = false;
    } else {
        nlpMasterCheckbox.checked = false;
        nlpMasterCheckbox.indeterminate = true;
    }
}

// TTLAB Checkboxes
document.querySelectorAll('.ttlab-group-checkbox, .ttlab-subgroup-checkbox').forEach(groupCheckbox => {
    groupCheckbox.addEventListener('change', function (e) {
        const groupItem = this.closest('li');
        const childCheckboxes = groupItem.querySelectorAll('input[type="checkbox"]');
        // childCheckboxes.forEach(cb => cb.checked = this.checked);
        childCheckboxes.forEach(cb => {
            if (!cb.disabled) cb.checked = this.checked;
        });
        updateTtlabParentCheckboxStates(this);
        updateNlpMasterCheckbox();
        e.stopPropagation();
    });
});

document.querySelectorAll('.ttlab-model-checkbox').forEach(modelCheckbox => {
    modelCheckbox.addEventListener('change', function (e) {
        updateTtlabParentCheckboxStates(this);
        updateNlpMasterCheckbox();
        e.stopPropagation();
    });
});

function updateTtlabParentCheckboxStates(checkbox) {
    let current = checkbox.closest('ul.nested, ol.nested');
    while (current) {
        const parentLi = current.closest('li');
        const parentCheckbox = parentLi?.querySelector('input[type="checkbox"]:not(.ttlab-model-checkbox)');

        if (parentCheckbox) {
            const relevantCheckboxes = current.querySelectorAll('input[type="checkbox"]:not(.tree-toggle input)');
            const childCheckboxes = Array.from(relevantCheckboxes).filter(cb => cb !== parentCheckbox && cb.closest('li') === cb.closest('li'));

            const allChecked = childCheckboxes.every(cb => cb.checked);
            const noneChecked = childCheckboxes.every(cb => !cb.checked);

            if (allChecked) {
                parentCheckbox.checked = true;
                parentCheckbox.indeterminate = false;
            } else if (noneChecked) {
                parentCheckbox.checked = false;
                parentCheckbox.indeterminate = false;
            } else {
                parentCheckbox.checked = false;
                parentCheckbox.indeterminate = true;
            }
        }

        current = parentLi?.closest('ul.nested, ol.nested');
    }
}

// Update visibility of fields based on checkbox states
function updateAllFieldVisibilities() {
    updateFieldVisibility('factchecking', 'claim-field-wrapper', 'claim-text');
    updateFieldVisibility('cohesion', 'text-field-wrapper', 'input-text');
    updateFieldVisibility('stance', 'stance-field-wrapper', 'stance-text');
    updateFieldVisibility('llm', 'llm-field-wrapper', 'llm-text');
}

function updateFieldVisibility(keyword, wrapperId, inputId) {
    const checkboxes = document.querySelectorAll('.nlp-model-checkbox, .ttlab-model-checkbox');
    const anyChecked = Array.from(checkboxes).some(cb => cb.id.toLowerCase().includes(keyword) && cb.checked);
    const wrapper = document.getElementById(wrapperId);

    if (wrapper) {
        wrapper.style.display = anyChecked ? 'block' : 'none';
        if (!anyChecked) {
            const input = document.getElementById(inputId);
            if (input) input.value = '';
        }
    }
}

// Upload-Button
const uploadBtn = document.getElementById('analysis-upload-btn');
const fileInput = document.getElementById('file-input');
const textarea = document.getElementById('analysis-input');

uploadBtn.addEventListener('click', () => fileInput.click());

fileInput.addEventListener('change', () => {
    const file = fileInput.files[0];
    if (file) {
        const reader = new FileReader();
        reader.onload = function (e) {
            textarea.value = e.target.result;
            textarea.parentNode.dataset.replicatedValue = textarea.value;
        };
        reader.readAsText(file);
    }
});

// Sidebar open/close
function toggleSidebar(id) {
    const section = document.getElementById(id);
    if (!section) return;

    const isVisible = section.style.display === "block";
    section.style.display = isVisible ? "none" : "block";
}

// Toggle Card Collapse
function toggleCard(button) {
    const card = button.closest('.analysis-ta-card');
    const isCollapsed = card.classList.toggle('collapsed');
    button.setAttribute('aria-expanded', !isCollapsed);
}

// Sidebar-Sections initial display
document.getElementById('nlp-tools').style.display = 'none';
document.getElementById('history').style.display = 'none';

// Drag and drop for draggable sections
const container = document.getElementById('analysis-main-content');
let draggedEl = null;

document.querySelectorAll('.draggable-section').forEach(section => {
    section.addEventListener('dragstart', () => {
        draggedEl = section;
        section.classList.add('dragging');
    });

    section.addEventListener('dragend', () => {
        draggedEl = null;
        section.classList.remove('dragging');
        document.querySelectorAll('.draggable-section').forEach(el => el.classList.remove('drag-over'));
    });

    section.addEventListener('dragover', (e) => {
        e.preventDefault();
        if (section !== draggedEl) {
            section.classList.add('drag-over');
        }
    });

    section.addEventListener('dragleave', () => {
        section.classList.remove('drag-over');
    });

    section.addEventListener('drop', (e) => {
        e.preventDefault();
        if (section !== draggedEl) {
            section.classList.remove('drag-over');
            const allSections = Array.from(container.children);
            const draggedIndex = allSections.indexOf(draggedEl);
            const dropIndex = allSections.indexOf(section);

            if (draggedIndex < dropIndex) {
                container.insertBefore(draggedEl, section.nextSibling);
            } else {
                container.insertBefore(draggedEl, section);
            }
        }
    });
});

document.querySelectorAll('.ta-collapse-toggle-btn').forEach(button => {
    button.addEventListener('click', function (e) {
        e.stopPropagation();
        toggleCard(this);
    });
});

(function initTreeviewResizer() {
    const resizer = document.getElementById('resizer');
    const treeviewPanel = document.getElementById('treeview-panel');

    if (!resizer || !treeviewPanel) return;

    let startX = 0;
    let startWidth = 0;

    const parsePx = (value) => parseInt(value.replace('px', ''), 10);

    const onMouseMove = (e) => {
        const dx = e.clientX - startX;

        const computedStyle = window.getComputedStyle(treeviewPanel);
        const minWidth = parsePx(computedStyle.minWidth) || 200;

        const maxWidth = window.innerWidth - 100;
        const newWidth = Math.min(maxWidth, Math.max(minWidth, startWidth + dx));

        treeviewPanel.style.width = newWidth + "px";
    };

    const onMouseUp = () => {
        document.removeEventListener('mousemove', onMouseMove);
        document.removeEventListener('mouseup', onMouseUp);
    };

    resizer.addEventListener('mousedown', (e) => {
        e.preventDefault();
        startX = e.clientX;
        startWidth = treeviewPanel.getBoundingClientRect().width;
        document.addEventListener('mousemove', onMouseMove);
        document.addEventListener('mouseup', onMouseUp);
    });
})();


