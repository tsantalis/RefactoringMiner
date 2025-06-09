const maxAllowedHeight = 10000;

function mymonaco(config) {
    require.config({paths: {'vs': 'https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.39.0/min/vs'}});
    require(['vs/editor/editor.main'], initializeEditors);
    function initializeEditors() {
        const left_container_id = config.lcid; /*'left-container';*/
        const right_container_id = config.rcid;/*'right-container';*/
        const leftContainer = document.getElementById(left_container_id);
        const rightContainer = document.getElementById(right_container_id);

        Promise.all([
            Promise.resolve(monaco.editor.create(leftContainer, getEditorOptions(config, config.left.content))),
            Promise.resolve(monaco.editor.create(rightContainer, getEditorOptions(config, config.right.content)))
        ]).then(([leftEditor, rightEditor]) => {
            config.mappings = config.mappings.map(mapping =>
                [
                    monaco.Range.fromPositions(leftEditor.getModel().getPositionAt(mapping[0]), leftEditor.getModel().getPositionAt(mapping[1])),
                    monaco.Range.fromPositions(rightEditor.getModel().getPositionAt(mapping[2]), rightEditor.getModel().getPositionAt(mapping[3])),
                ]);
            const leftDecorations = config.left.ranges.map(range => getDecorationNoLeadingWhiteSpace(
                range,
                leftEditor.getModel().getPositionAt(range.from),
                leftEditor.getModel().getPositionAt(range.to),
                leftEditor
            )).flat();
            const rightDecorations = config.right.ranges.map(range => getDecorationNoLeadingWhiteSpace(
                range,
                rightEditor.getModel().getPositionAt(range.from),
                rightEditor.getModel().getPositionAt(range.to),
                rightEditor
            )).flat();
            deltaDecorations(leftEditor, leftDecorations);
            deltaDecorations(rightEditor, rightDecorations);
            varInits(config, rightEditor, leftEditor);
            leftEditor.onMouseDown(editorMouseDown(config, leftEditor, rightEditor, 0, 1));
            rightEditor.onMouseDown(editorMouseDown(config, rightEditor, leftEditor, 1, 0));

            setAllFoldings(config, leftEditor, rightEditor);


            if (config.spv === true) {
                const updateEditorsLayout = () => {
                    const leftHeight = leftEditor.getContentHeight();
                    const rightHeight = rightEditor.getContentHeight();
                    let maxHeight = Math.max(leftHeight, rightHeight);
                    const editorHeight = maxHeight > maxAllowedHeight ? maxAllowedHeight : maxHeight;
                    leftContainer.style.height = editorHeight + 'px';
                    rightContainer.style.height = editorHeight + 'px';
                    leftEditor.layout();
                    rightEditor.layout();
                };

                Promise.all([
                    leftEditor.getAction('editor.foldAll').run(),
                    rightEditor.getAction('editor.foldAll').run()]
                ).then(updateEditorsLayout);

                leftEditor.onDidContentSizeChange(updateEditorsLayout);
                rightEditor.onDidContentSizeChange(updateEditorsLayout);

                const accordion = document.getElementById('accordion');
                const parent_container = accordion ? accordion : document.body;

                rightContainer.addEventListener('wheel', (e) => {
                    e.preventDefault();
                    parent_container.scrollTop += e.deltaY;
                    parent_container.scrollLeft += e.deltaX;
                });
                leftContainer.addEventListener('wheel', (e) => {
                    e.preventDefault();
                    parent_container.scrollTop += e.deltaY;
                    parent_container.scrollLeft += e.deltaX;
                });
                window.addEventListener("resize", updateEditorsLayout.bind(this));
            }
            else {
                leftEditor.getAction('editor.foldAll').run();
                rightEditor.getAction('editor.foldAll').run();
            }
            addInlineComments(leftEditor, config.left_comments || []);
            addInlineComments(rightEditor, config.right_comments || []);
            window.leftEditor = leftEditor;
            window.rightEditor = rightEditor;
        });
    }
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

    // Create decorations for all lines with comments
    const decoratedLines = new Set(comments.map(c => c.line));
    const decorations = Array.from(decoratedLines).map(line => ({
        range: new monaco.Range(line, 1, line, 1),
        options: {
            isWholeLine: true,
            linesDecorationsClassName: 'comment-icon'
        }
    }));

    editor.deltaDecorations([], decorations);

    editor.onMouseMove((e) => {
        const target = e.target;
        const hoveredLine = target.position?.lineNumber;
        const lineComments = comments
            .filter(c => c.line === hoveredLine)
            .sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));

        if (
            [monaco.editor.MouseTargetType.GUTTER_LINE_DECORATIONS, monaco.editor.MouseTargetType.GUTTER_GLYPH_MARGIN].includes(target.type) &&
            lineComments.length > 0
        ) {
            const commentHtml = lineComments.map(comment => {
                const formattedDate = new Date(comment.createdAt).toLocaleDateString('en-US', {
                    year: 'numeric',
                    month: 'short',
                    day: 'numeric'
                });
                return `
                    <div class="${comment.status}">
                        <strong>${comment.author}</strong>
                        <span style="color: #888; font-size: 11px;">(created at ${formattedDate})</span>
                        <div>${comment.text}</div>
                    </div>
                    <hr style="margin: 6px 0; border: none; border-top: 1px solid #ddd;">
                `;
            }).join('');

            popup.innerHTML = commentHtml;
            popup.style.display = 'block';
            popup.style.top = `${e.event.browserEvent.clientY + 10}px`;
            popup.style.left = `${e.event.browserEvent.clientX + 10}px`;
        } else {
            popup.style.display = 'none';
        }
    });

    editor.onMouseLeave(() => {
        popup.style.display = 'none';
    });
}



function varInits(config, rightEditor, leftEditor) {
    rightEditor.getModel().moved = config.moved;
    leftEditor.getModel().moved = config.moved;
    rightEditor.getModel().id = config.id
    leftEditor.getModel().id = config.id
}