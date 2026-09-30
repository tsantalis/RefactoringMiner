function loadSingleMonacoEditor({ id, value, language = 'java', comments = [] }) {
    if (!window.monaco) {
        require.config({
            paths: { vs: MONACO_VS_URL }
        });
        require(['vs/editor/editor.main'], function () {
            pinCodiconFont();
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

