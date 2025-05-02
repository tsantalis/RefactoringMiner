document.addEventListener("DOMContentLoaded", () => {
    let selectedDiffs = [];

    document.querySelectorAll('input[name="fileSelect"]').forEach(el => {
        el.addEventListener('change', () => {
            const fileId = el.value;

            if (el.checked) {
                if (selectedDiffs.length >= 2) {
                    // Remove the oldest selection
                    const removed = selectedDiffs.shift();
                    document.querySelector(`input[value="${removed.value}"]`).checked = false;
                }

                // Push the full metadata object
                selectedDiffs.push({
                    value: el.value,
                    id: el.dataset.id,
                    path: el.dataset.path,
                    type: el.dataset.type
                });
            } else {
                // Remove deselected item
                selectedDiffs = selectedDiffs.filter(item => item.value !== fileId);
            }

            const compareBtn = document.getElementById('compareBtn');

            function isValidSelection() {
                if (selectedDiffs.length !== 2) return false;
                const types = selectedDiffs.map(item => item.type);
                return types.includes("deleted") && types.includes("added");
            }

            if (compareBtn) {
                compareBtn.style.display = isValidSelection() ? 'block' : 'none';
            }
        });
    });

    // Expose handler globally
    window.handleCompareClick = () => {
        if (selectedDiffs.length === 2) {
            let deletedFile = selectedDiffs.find(f => f.type === "deleted");
            let addedFile = selectedDiffs.find(f => f.type === "added");

            if (deletedFile && addedFile) {
                const url = `/onDemand?file1=${encodeURIComponent(deletedFile.path)}&file2=${encodeURIComponent(addedFile.path)}`;
                window.location.href = url;
            } else {
                alert("You must select one 'deleted' and one 'added' file.");
            }
        }
    };
});
