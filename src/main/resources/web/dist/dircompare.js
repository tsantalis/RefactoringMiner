document.addEventListener("DOMContentLoaded", () => {
	document.querySelectorAll('code').forEach((el) => {
	    hljs.highlightElement(el);
	  });
	document.querySelectorAll('input[id="refactoringType"]').forEach(type => {
		type.addEventListener('change', () => {
			const innerText = type.parentNode.childNodes[1].innerText;
			const refactoringType = innerText.substring(0, innerText.indexOf(" ("));
			document.querySelectorAll('li[class="list-group-item"]').forEach(li => {
				if(li.innerText.startsWith(refactoringType) || refactoringType === "ALL") {
					if(type.checked === false) {
						li.style.display = 'none';
					}
					else {
						li.style.display = 'block';
					}
				}
			});
			if(refactoringType === "ALL") {
				document.querySelectorAll('input[id="refactoringType"]').forEach(checkbox => {
					checkbox.checked = type.checked;
				});
			}
		});
	});
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
                    (a === "added" && (b === "deleted" || b === "modified" || b === "renamed")) ||
                    (a === "deleted" && (b === "added" || b === "modified" || b === "renamed")) ||
                    ((a === "modified" || a === "renamed") && (b === "added" || b === "deleted")) ||
                    (a === "added" && (b === "modified" || b === "renamed")) ||
                    (a === "deleted" && (b === "modified" || b === "renamed")) ||
                    ((a === "modified" || a === "renamed") && (b === "deleted" || b === "added"))
                );
            }

            if (compareBtn) {
                compareBtn.style.display = isValidPair() ? 'block' : 'none';
            }
        });
    });

    window.handleCompareClick = () => {
        if (selectedDiffs.length !== 2) return;

        const a = selectedDiffs[0];
        const b = selectedDiffs[1];

        // Normalize for pairing logic: `renamed` behaves like `modified`
        const norm = t => (t === "renamed" ? "modified" : t);
        const A = norm(a.type);
        const B = norm(b.type);

        // Keep original intent:
        // (deleted, added) OR (modified/renamed, added) OR (deleted, modified/renamed) -> [a,b]
        // otherwise swap
        let file1 = a, file2 = b;
        if (!(
            (A === "deleted"  && B === "added") ||
            (A === "modified" && B === "added") ||
            (A === "deleted"  && B === "modified")
        )) {
            file1 = b;
            file2 = a;
        }

        // Helper: for a `renamed` item, pick side of the "old|new" path
        function resolvePath(item, otherType) {
            if (item.type !== "renamed") return item.path;

            // Expect "before|after"
            const [before, after] = String(item.path).split("|");

            if (otherType === "added")   return (before ?? "").trim() || item.path; // use BEFORE
            if (otherType === "deleted") return (after  ?? "").trim() || item.path; // use AFTER

            // default when paired with modified/renamed: prefer AFTER, fallback to BEFORE
            return (after ?? before ?? item.path).trim();
        }

        const path1 = resolvePath(file1, file2.type);
        const path2 = resolvePath(file2, file1.type);

        const url = `/onDemand?file1=${encodeURIComponent(path1)}&file2=${encodeURIComponent(path2)}`;
        window.location.href = url;
    };
});
