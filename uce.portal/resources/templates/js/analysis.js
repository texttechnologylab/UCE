// analysis.js
document.addEventListener("DOMContentLoaded", function () {
    var toggler = document.getElementsByClassName("tree-caret");
    var i;

    for (i = 0; i < toggler.length; i++) {
        toggler[i].addEventListener("click", function() {
            this.parentElement.querySelector(".tree-nested").classList.toggle("tree-active");
            this.classList.toggle("tree-caret-down");
        });
    }
});

function toggleBranch(element, icon, expand) {
    if (!element || !element.classList.contains("nested")) return;

    if (expand) {
        element.classList.add("active");
        if (icon) {
            icon.classList.remove("fa-chevron-right");
            icon.classList.add("fa-chevron-down");
        }
    } else {
        element.classList.remove("active");
        if (icon) {
            icon.classList.remove("fa-chevron-down");
            icon.classList.add("fa-chevron-right");
        }

        // Alle Untergruppen einklappen
        element.querySelectorAll(".nested").forEach(sub => {
            sub.classList.remove("active");
            const subIcon = sub.previousElementSibling?.querySelector(".toggle-icon");
            if (subIcon) {
                subIcon.classList.remove("fa-chevron-down");
                subIcon.classList.add("fa-chevron-right");
            }
        });
    }
}