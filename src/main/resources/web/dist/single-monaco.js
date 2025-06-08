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


function addInlineComments(editor, comments) {
    const container = editor.getDomNode().parentElement;
    let popup = document.getElementById('monaco-comment-popup');
    if (!popup) {
        popup = document.createElement('div');
        popup.id = 'monaco-comment-popup';
        popup.className = 'inline-comment';
        document.body.appendChild(popup);
    }

    const decorations = comments.map(comment => ({
        range: new monaco.Range(comment.line, 1, comment.line, 1),
        options: {
            isWholeLine: true,
            linesDecorationsClassName: 'comment-icon'
        }
    }));

    editor.deltaDecorations([], decorations);

    editor.onMouseMove((e) => {
        const target = e.target;
        const hoveredLine = target.position?.lineNumber;
        const comment = comments.find(c => c.line === hoveredLine);

        if (
            target.type === monaco.editor.MouseTargetType.GUTTER_LINE_DECORATIONS &&
            comment
        ) {
            const top = editor.getTopForLineNumber(comment.line);
            popup.className = `inline-comment ${comment.status}`;
            popup.innerHTML = `<strong>${comment.author}</strong>: ${comment.text}`;
            popup.style.display = 'block';

            const editorRect = editor.getDomNode().getBoundingClientRect();
            popup.style.top = `${editorRect.top + top + 20}px`;
            popup.style.left = `${editorRect.left + 60}px`;
        } else {
            popup.style.display = 'none';
        }
    });

    editor.onMouseLeave(() => {
        popup.style.display = 'none';
    });
}

