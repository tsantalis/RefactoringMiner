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
            <div style="display: flex; align-items: center; gap: 6px;">
                <img src="${comment.avatarUrl}" alt="${comment.author}'s avatar" style="width: 20px; height: 20px; border-radius: 50%;">
                <strong>${comment.author}</strong>
                <span style="color: #888; font-size: 11px;">(created at ${formattedDate})</span>
            </div>
                <div style="margin-top: 4px;">${marked.parse(comment.text)}</div>
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