document.addEventListener("DOMContentLoaded", () => {
    let selectedDiffs = [];

    document.querySelectorAll('input[name="fileSelect"]').forEach(el => {
        el.addEventListener('change', () => {
            const fileId = el.value;

            if (el.checked) {
                if (selectedDiffs.length >= 2) {
                    const removed = selectedDiffs.shift();
                    document.querySelector(`input[value="${removed.value}"]`).checked = false;
                }

                selectedDiffs.push({
                    value: el.value,
                    id: el.dataset.id,
                    path: el.dataset.path,
                    type: el.dataset.type
                });
            } else {
                selectedDiffs = selectedDiffs.filter(item => item.value !== fileId);
            }

            const compareBtn = document.getElementById('compareBtn');

            function isValidPair() {
                if (selectedDiffs.length !== 2) return false;
                const types = selectedDiffs.map(f => f.type);
                const [a, b] = types;

                return (
                    (a === "added" && b === "deleted") ||
                    (a === "deleted" && b === "added") ||
                    (a === "modified" && b === "added") ||
                    (a === "added" && b === "modified") ||
                    (a === "deleted" && b === "modified") ||
                    (a === "modified" && b === "deleted")
                );
            }

            if (compareBtn) {
                compareBtn.style.display = isValidPair() ? 'block' : 'none';
            }
        });
    });

    window.handleCompareClick = () => {
        if (selectedDiffs.length === 2) {
            const a = selectedDiffs[0];
            const b = selectedDiffs[1];

            let file1, file2;

            if (
                (a.type === "deleted" && b.type === "added") ||
                (a.type === "modified" && b.type === "added") ||
                (a.type === "deleted" && b.type === "modified")
            ) {
                file1 = a;
                file2 = b;
            } else {
                file1 = b;
                file2 = a;
            }

            const url = `/onDemand?file1=${encodeURIComponent(file1.path)}&file2=${encodeURIComponent(file2.path)}`;
            window.location.href = url;
        }
    };
});
