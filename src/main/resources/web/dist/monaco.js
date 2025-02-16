function mymonaco(config) {
    require.config({paths: {'vs': 'https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.39.0/min/vs'}});
    require(['vs/editor/editor.main'], initializeEditors);
    function initializeEditors() {
        const left_container_id = config.lcid; /*'left-container';*/
        const right_container_id = config.rcid;/*'right-container';*/
        const leftContainer = document.getElementById(left_container_id);
        const rightContainer = document.getElementById(right_container_id);
        let isLayoutUpdating = false;
        let isInitialLayout = true; // Flag to track the initial layout
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

            leftEditor.getAction('editor.foldAll').run();
            rightEditor.getAction('editor.foldAll').run();

            if (config.spv === true) {
                const updateEditorsLayout = () => {
                    if (isLayoutUpdating) return; // Prevent recursion during layout updates

                    isLayoutUpdating = true;

                    const leftHeight = leftEditor.getContentHeight();
                    const rightHeight = rightEditor.getContentHeight();
                    const editorHeight = Math.max(leftHeight, rightHeight);

                    // Initially set the height to 500px, then update dynamically
                    if (isInitialLayout) {
                        leftContainer.style.height = '500px';
                        rightContainer.style.height = '500px';
                        isInitialLayout = false; // Disable the initial layout update flag
                    } else {
                        // Update the height only if it has changed
                        if (leftContainer.style.height !== `${editorHeight}px` || rightContainer.style.height !== `${editorHeight}px`) {
                            leftContainer.style.height = `${editorHeight}px`;
                            rightContainer.style.height = `${editorHeight}px`;
                        }
                    }

                    // Update editor layout
                    leftEditor.layout();
                    rightEditor.layout();

                    isLayoutUpdating = false;
                };

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
            }
            window.leftEditor = leftEditor;
            window.rightEditor = rightEditor;
        });
    }
}


function varInits(config, rightEditor, leftEditor) {
    rightEditor.getModel().moved = config.moved;
    leftEditor.getModel().moved = config.moved;
    rightEditor.getModel().id = config.id
    leftEditor.getModel().id = config.id
}