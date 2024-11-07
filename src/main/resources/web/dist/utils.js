function getEditorOptions(config, text) {
    return {
        value: text,
        readOnly: true,
        language: getLanguage(config),
        automaticLayout: true,
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
    if (extension === "java")
        return "java";
    else
        return undefined;
}
function getEditColor(edit) {
    let color = 'black';
    if (edit === "inserted") color = 'green';
    else if (edit === "deleted") color = 'red';
    else if (edit === "updated") color = 'yellow';
    else if (edit === "moved") color = 'blue';
    else if (edit === "mm" || "mm updOnTop") color = 'purple';
    return color
}