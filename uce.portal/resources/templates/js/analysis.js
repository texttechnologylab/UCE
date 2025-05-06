// Pfeile und Baumstruktur togglen
document.querySelectorAll('.tree-toggle').forEach(toggle => {
    toggle.addEventListener('click', function (e) {
        // Wenn auf Checkbox oder Label geklickt wird → NICHT klappen!
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

// Wenn die Master-"Modelle"-Checkbox geändert wird
const masterCheckbox = document.getElementById('all-models-checkbox');

masterCheckbox.addEventListener('change', function () {
    const allCheckboxes = document.querySelectorAll('.analysis-treeview input[type="checkbox"]:not(#all-models-checkbox)');
    allCheckboxes.forEach(cb => cb.checked = this.checked);
});

// Wenn eine Gruppen-Checkbox geändert wird
document.querySelectorAll('.group-checkbox').forEach(groupCheckbox => {
    groupCheckbox.addEventListener('change', function(e) {
        const groupItem = this.closest('li');
        const modelCheckboxes = groupItem.querySelectorAll('.model-checkbox');

        modelCheckboxes.forEach(cb => {
            cb.checked = this.checked;
        });

        updateMasterCheckbox();
        e.stopPropagation(); // Verhindert Baum-Öffnen beim Checkbox-Klick
    });
});

document.querySelectorAll('.model-checkbox').forEach(modelCheckbox => {
    modelCheckbox.addEventListener('change', function () {
        const isFactChecking = this.id.toLowerCase().includes('factchecking');
        const isCoherence = this.id.toLowerCase().includes('cohesion');
        const isStance = this.id.toLowerCase().includes('stance');
        if (isFactChecking) {
            const wrapper = document.getElementById('claim-field-wrapper');
            if (this.checked) {
                wrapper.style.display = 'block';
            } else {
                wrapper.style.display = 'none';
                document.getElementById('claim-text').value = ''; // optional: reset field
            }
        }
        if (isCoherence) {
            const wrapper = document.getElementById('text-field-wrapper');
            if (this.checked) {
                wrapper.style.display = 'block';
            } else {
                wrapper.style.display = 'none';
                document.getElementById('input-text').value = ''; // optional: reset field
            }
        }
        if (isStance) {
            const wrapper = document.getElementById('stance-field-wrapper');
            if (this.checked) {
                wrapper.style.display = 'block';
            } else {
                wrapper.style.display = 'none';
                document.getElementById('stance-text').value = ''; // optional: reset field
            }
        }
    });
});

// document.querySelectorAll('.model-checkbox').forEach(modelCheckbox => {
//     modelCheckbox.addEventListener('change', function () {
//         const isFactChecking = this.id.toLowerCase().includes('cohesion');
//         if (isFactChecking) {
//             const wrapper = document.getElementById('text-field-wrapper');
//             if (this.checked) {
//                 wrapper.style.display = 'block';
//             } else {
//                 wrapper.style.display = 'none';
//                 document.getElementById('input-text').value = ''; // optional: reset field
//             }
//         }
//     });
// });


// Wenn eine Modell-Checkbox geändert wird
document.querySelectorAll('.model-checkbox').forEach(modelCheckbox => {
    modelCheckbox.addEventListener('change', function(e) {
        const groupItem = this.closest('ul').parentElement;
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
        e.stopPropagation(); // Verhindert Baum-Öffnen beim Checkbox-Klick
    });
});

// Update-Funktion für die Master-"Modelle"-Checkbox
function updateMasterCheckbox() {
    const groupCheckboxes = document.querySelectorAll('.group-checkbox');
    const allChecked = Array.from(groupCheckboxes).every(cb => cb.checked);
    const noneChecked = Array.from(groupCheckboxes).every(cb => !cb.checked);

    if (allChecked) {
        masterCheckbox.checked = true;
        masterCheckbox.indeterminate = false;
    } else if (noneChecked) {
        masterCheckbox.checked = false;
        masterCheckbox.indeterminate = false;
    } else {
        masterCheckbox.checked = false;
        masterCheckbox.indeterminate = true;
    }
}

const uploadBtn = document.getElementById('upload-btn');
const fileInput = document.getElementById('file-input');
const textarea = document.getElementById('input');

uploadBtn.addEventListener('click', () => {
    fileInput.click();
});

fileInput.addEventListener('change', () => {
    const file = fileInput.files[0];
    if (file) {
        const reader = new FileReader();
        reader.onload = function(e) {
            textarea.value = e.target.result;
            textarea.parentNode.dataset.replicatedValue = textarea.value; // aktualisiert auch dein Dataset
        };
        reader.readAsText(file);
    }
});

