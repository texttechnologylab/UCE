document.querySelectorAll('.tree-toggle').forEach(toggle => {
    toggle.addEventListener('click', function (e) {
        if (e.target.tagName.toLowerCase() === 'input' || e.target.tagName.toLowerCase() === 'label') {
            return;
        }

        const parent = this.parentElement;
        const nested = parent.querySelector('.nested');
        const icon = this.querySelector('.toggle-icon');

        if (nested) nested.classList.toggle('active');
        if (icon) icon.classList.toggle('open');
    });
});


const nlpMasterCheckbox = document.getElementById('all-analysis-models-checkbox');

nlpMasterCheckbox.addEventListener('change', function () {
    const allNlpCheckboxes = document.querySelectorAll('.nlp-group-checkbox, .nlp-model-checkbox');
    allNlpCheckboxes.forEach(cb => cb.checked = this.checked);
    updateNlpMasterCheckbox();
    updateAllFieldVisibilities();
});

document.querySelectorAll('.nlp-group-checkbox').forEach(groupCheckbox => {
    groupCheckbox.addEventListener('change', function (e) {
        const groupItem = this.closest('li');
        const modelCheckboxes = groupItem.querySelectorAll('.nlp-model-checkbox');
        modelCheckboxes.forEach(cb => cb.checked = this.checked);
        updateNlpMasterCheckbox();
        updateAllFieldVisibilities();
        e.stopPropagation();
    });
});

document.querySelectorAll('.nlp-model-checkbox').forEach(modelCheckbox => {
    modelCheckbox.addEventListener('change', function (e) {
        const groupItem = this.closest('ol.nested').closest('li');
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
        updateAllFieldVisibilities();
        e.stopPropagation();
    });
});

function updateNlpMasterCheckbox() {
    const groupCheckboxes = document.querySelectorAll('.nlp-group-checkbox');
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


document.querySelectorAll('.ttlab-group-checkbox').forEach(groupCheckbox => {
    groupCheckbox.addEventListener('change', function (e) {
        const groupItem = this.closest('li');
        const modelCheckboxes = groupItem.querySelectorAll('.ttlab-model-checkbox');
        modelCheckboxes.forEach(cb => cb.checked = this.checked);
        e.stopPropagation();
    });
});

document.querySelectorAll('.ttlab-model-checkbox').forEach(modelCheckbox => {
    modelCheckbox.addEventListener('change', function (e) {
        const groupItem = this.closest('ol.nested').closest('li');
        const groupCheckbox = groupItem.querySelector('.ttlab-group-checkbox');
        const modelCheckboxes = groupItem.querySelectorAll('.ttlab-model-checkbox');

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

        e.stopPropagation();
    });
});


function updateAllFieldVisibilities() {
    updateFieldVisibility('factchecking', 'claim-field-wrapper', 'claim-text');
    updateFieldVisibility('cohesion ', 'text-field-wrapper', 'input-text');
    updateFieldVisibility('stance ', 'stance-field-wrapper', 'stance-text');
    updateFieldVisibility('llm', 'llm-field-wrapper', 'llm-text');
}

function updateFieldVisibility(keyword, wrapperId, inputId) {
    const checkboxes = document.querySelectorAll('.nlp-model-checkbox');
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

const uploadBtn = document.getElementById('analysis-upload-btn');
const fileInput = document.getElementById('file-input');
const textarea = document.getElementById('analysis-input');

uploadBtn.addEventListener('click', () => {
    fileInput.click();
});

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

function toggleCard(button) {
    const card = button.closest('.analysis-ta-card');
    const isCollapsed = card.classList.toggle('collapsed');
    button.setAttribute('aria-expanded', !isCollapsed);
}

document.querySelectorAll('.ta-collapse-toggle-btn').forEach(button => {
    button.addEventListener('click', function (e) {
        e.stopPropagation();
        toggleCard(this);
    });
});
