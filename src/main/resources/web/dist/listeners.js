let originalEditor, modifiedEditor;
let originalDecorations = [];
let modifiedDecorations = [];

window.addEventListener("message", (event) => {


    if (event.data.type === "GET_DECORATIONS") {

        if (!originalEditor || !modifiedEditor) {
            const editors = monaco.editor.getEditors();
            if (editors?.length >= 2) {
                originalEditor = editors[0];
                modifiedEditor = editors[1];
            }
        }

        const originalMap = {};
        const modifiedMap = {};

        function hashDecoration(decoration) {
            const raw = `${decoration.side}|${decoration.startLineNumber}:${decoration.startColumn}-${decoration.endLineNumber}:${decoration.endColumn}|${decoration.startOffset}-${decoration.endOffset}|${decoration.className}|${decoration.hover}`;
            let hash = 0;
            for (let i = 0; i < raw.length; i++) {
                const chr = raw.charCodeAt(i);
                hash = ((hash << 5) - hash) + chr;
                hash |= 0; // Convert to 32bit int
            }
            return hash.toString();
        }

        function collectDecorations(editor, side, targetMap) {
            const model = editor.getModel();
            const decorations = editor.getDecorationsInRange(model.getFullModelRange()) || [];

            decorations.forEach(d => {
                const range = d.range;
                const startOffset = model.getOffsetAt({ lineNumber: range.startLineNumber, column: range.startColumn });
                const endOffset = model.getOffsetAt({ lineNumber: range.endLineNumber, column: range.endColumn });

                const base = {
                    side,
                    startLineNumber: range.startLineNumber,
                    startColumn: range.startColumn,
                    endLineNumber: range.endLineNumber,
                    endColumn: range.endColumn,
                    startOffset,
                    endOffset,
                    className: d.options?.className || '',
                    hover: typeof d.options?.hoverMessage === 'string'
                        ? d.options.hoverMessage
                        : Array.isArray(d.options?.hoverMessage)
                            ? d.options.hoverMessage.map(h => h.value).join(', ')
                            : ''
                };

                base.hash = hashDecoration(base);

                for (let line = range.startLineNumber; line <= range.endLineNumber; line++) {
                    if (!targetMap[line]) {
                        targetMap[line] = [];
                    }
                    targetMap[line].push({ ...base });
                }
            });
        }

        if (originalEditor) collectDecorations(originalEditor, 'original', originalMap);
        if (modifiedEditor) collectDecorations(modifiedEditor, 'modified', modifiedMap);

        window.parent.postMessage({
            type: "DIFF_DECORATIONS",
            from: window.name || "unknown",
            original: originalMap,
            modified: modifiedMap
        }, "*");
    }


    if (event.data.type === "HIGHLIGHT_LINES") {
        const decorations = event.data.lines.map(line => ({
            range: new monaco.Range(line + 1, 1, line + 1, 1),
            options: {
                isWholeLine: true,
                className: "highlight-line"
            }
        }));

        if (event.data.target === "original" && originalEditor) {
            originalDecorations = originalEditor.deltaDecorations(originalDecorations, decorations);
        } else if (event.data.target === "modified" && modifiedEditor) {
            modifiedDecorations = modifiedEditor.deltaDecorations(modifiedDecorations, decorations);
        } else if (event.data.target === "both") {
            if (originalEditor) {
                originalDecorations = originalEditor.deltaDecorations(originalDecorations, decorations);
            }
            if (modifiedEditor) {
                modifiedDecorations = modifiedEditor.deltaDecorations(modifiedDecorations, decorations);
            }
        }
    }
});

// Add style for highlight
const style = document.createElement("style");
style.textContent = `
    .highlight-line {
        background-color: rgba(0, 0, 0, 0.2) !important;
    }
`;
document.head.appendChild(style);
