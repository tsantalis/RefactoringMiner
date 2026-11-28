function getEditorOptions(config, text) {
    return {
        value: text,
        readOnly: true,
        language: getLanguage(config),
        automaticLayout: config.spv !== true,
        scrollBeyondLastLine: false,
        lineDecorationsWidth: 0,
        glyphMargin: false,
        scrollbar: {
            alwaysConsumeMouseWheel: false,
            vertical: config.spv === true ? 'hidden' : 'auto'
        },
        minimap: {
            enabled: false,
        },
        wordWrap: 'on' // Enables word wrap
    };
}
function getLanguage(config) {
    let extension = config.file.split('.').pop().toLowerCase();
    switch (extension) {
        case "java":
            return "java";
        case "py":
            return "python";
        case "kt":
            return "kotlin";
        case "ts":
            return "typescript";
        case "tsx":
            return "typescript";
        case "js":
            return "javascript";
        case "jsx":
            return "javascript";
        case "c":
            return "c";
        case "cpp":
            return "cpp";
        case "h":
            return "cpp";
        case "hpp":
            return "cpp";
        case "cc":
            return "cpp";
        default:
            return undefined;
    }
}
//the color of the marks of the given kind (i.e., "deleted", "mm updOnTop") in the scroll bar, from monaco.css, so that the code, the scroll bar
//and the legend, which shows the same classes, use one palette; the scroll bar uses the stronger --ruler-color of the kind, as the pale
//backgrounds of the marks hardly show on it
const editColors = new Map();
function getEditColor(kind) {
    if (!editColors.has(kind)) {
        const mark = document.createElement('span');
        mark.className = kind;
        document.body.appendChild(mark);
        const style = getComputedStyle(mark);
        const color = style.getPropertyValue('--ruler-color').trim() || style.backgroundColor;
        mark.remove();
        if (!color || color === 'rgba(0, 0, 0, 0)' || color === 'transparent') {
            return 'black';
        }
        editColors.set(kind, color);
    }
    return editColors.get(kind);
}
