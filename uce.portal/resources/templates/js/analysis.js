document.querySelectorAll('.tree-toggle').forEach(toggle => {
    toggle.addEventListener('click', function (e) {

        if (e.target.tagName.toLowerCase() === 'input' || e.target.tagName.toLowerCase() === 'label') {
            return;
        }

        const parent = this.parentElement;
        const nested = parent.querySelector('.nested');
        const icon = this.querySelector('.toggle-icon');

        if (nested) {
            nested.classList.toggle('active');
        }

        if (icon) {
            icon.classList.toggle('open');
        }
    });
});


const masterCheckbox = document.getElementById('all-models-checkbox');

masterCheckbox.addEventListener('change', function () {
    const allCheckboxes = document.querySelectorAll('.analysis-treeview input[type="checkbox"]:not(#all-models-checkbox)');
    allCheckboxes.forEach(cb => cb.checked = this.checked);


    updateFieldVisibility('factchecking', 'claim-field-wrapper', 'claim-text');
    updateFieldVisibility('cohesion', 'text-field-wrapper', 'input-text');
    updateFieldVisibility('stance', 'stance-field-wrapper', 'stance-text');
});


document.querySelectorAll('.group-checkbox').forEach(groupCheckbox => {
    groupCheckbox.addEventListener('change', function(e) {
        const groupItem = this.closest('li');
        const modelCheckboxes = groupItem.querySelectorAll('.model-checkbox');

        modelCheckboxes.forEach(cb => {
            cb.checked = this.checked;
        });

        updateMasterCheckbox();
        updateFieldVisibility('factchecking', 'claim-field-wrapper', 'claim-text');
        updateFieldVisibility('cohesion', 'text-field-wrapper', 'input-text');
        updateFieldVisibility('stance', 'stance-field-wrapper', 'stance-text');

        e.stopPropagation(); // Verhindert Baum-Öffnen beim Checkbox-Klick
    });
});


document.querySelectorAll('.model-checkbox').forEach(modelCheckbox => {
    modelCheckbox.addEventListener('change', function(e) {
        const modelItem = this.closest('li'); // Das ist das Modell-<li>
        const groupItem = modelItem.closest('ol.nested').closest('li'); // Jetzt:

        const groupCheckbox = groupItem.querySelector('.group-checkbox');
        const modelCheckboxes = groupItem.querySelectorAll('.model-checkbox');

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

        updateMasterCheckbox();

        updateFieldVisibility('factchecking', 'claim-field-wrapper', 'claim-text');
        updateFieldVisibility('cohesion', 'text-field-wrapper', 'input-text');
        updateFieldVisibility('stance', 'stance-field-wrapper', 'stance-text');

        e.stopPropagation();
    });
});

function updateMasterCheckbox() {
    const masterCheckbox = document.getElementById('all-models-checkbox');
    const groupCheckboxes = document.querySelectorAll('.group-checkbox');

    const total = groupCheckboxes.length;
    const checked = Array.from(groupCheckboxes).filter(cb => cb.checked).length;
    const indeterminate = Array.from(groupCheckboxes).some(cb => cb.indeterminate);

    if (checked === total && !indeterminate) {
        masterCheckbox.checked = true;
        masterCheckbox.indeterminate = false;
    } else if (checked === 0 && !indeterminate) {
        masterCheckbox.checked = false;
        masterCheckbox.indeterminate = false;
    } else {
        masterCheckbox.checked = false;
        masterCheckbox.indeterminate = true;
    }
}


function updateFieldVisibility(keyword, wrapperId, inputId) {
    const checkboxes = document.querySelectorAll('.model-checkbox');
    const anyChecked = Array.from(checkboxes).some(cb => cb.id.toLowerCase().includes(keyword) && cb.checked);
    const wrapper = document.getElementById(wrapperId);

    if (wrapper) {
        console.log(wrapper)
        console.log(anyChecked)
        wrapper.style.display = anyChecked ? 'block' : 'none';
        if (!anyChecked) {
            const input = document.getElementById(inputId);
            if (input) input.value = ''; // optional: reset field
        }
    }
}

const uploadBtn = document.getElementById('upload-btn');
const fileInput = document.getElementById('file-input');
const textarea = document.getElementById('analysis-input');

uploadBtn.addEventListener('click', () => {
    fileInput.click();
});

fileInput.addEventListener('change', () => {
    const file = fileInput.files[0];
    if (file) {
        const reader = new FileReader();
        reader.onload = function(e) {
            textarea.value = e.target.result;
            textarea.parentNode.dataset.replicatedValue = textarea.value;
        };
        reader.readAsText(file);
    }
});
