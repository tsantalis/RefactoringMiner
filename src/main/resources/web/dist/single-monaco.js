function loadSingleMonacoEditor({ id, value, language = 'java', comments = [] }) {
    if (!window.monaco) {
        require.config({
            paths: { vs: 'https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.39.0/min/vs' }
        });
        require(['vs/editor/editor.main'], function () {
            const editor = _createEditor(id, value, language);
            addInlineComments(editor, comments || []);
        });
    } else {
        const editor = _createEditor(id, value, language);
        addInlineComments(editor, comments || []);
    }
}

function _createEditor(id, value, language) {
    const el = document.getElementById(id);
    if (el) {
        return monaco.editor.create(el, {
            value: value,
            language: language,
            readOnly: true,
            automaticLayout: true,
            theme: 'vs',
            scrollBeyondLastLine: false,
            minimap: { enabled: false },
        });
    }
    return null;
}

