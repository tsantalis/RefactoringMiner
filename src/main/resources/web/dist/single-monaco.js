function loadSingleMonacoEditor({ id, value, language = 'java' }) {
    if (!window.monaco) {
        require.config({
            paths: { vs: 'https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.39.0/min/vs' }
        });
        require(['vs/editor/editor.main'], function () {
            _createEditor(id, value, language);
        });
    } else {
        _createEditor(id, value, language);
    }
}

function _createEditor(id, value, language) {
    const el = document.getElementById(id);
    if (el) {
        monaco.editor.create(el, {
            value: value,
            language: language,
            readOnly: true,
            automaticLayout: true,
            theme: 'vs',
            scrollBeyondLastLine: false,
            minimap: { enabled: false },
        });
    }
}
