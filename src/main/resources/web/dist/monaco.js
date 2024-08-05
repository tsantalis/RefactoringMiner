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
            alwaysConsumeMouseWheel: false
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
    if (edit === "inserted") return 'green';
    else if (edit === "deleted") return 'red';
    else if (edit === "updated") return 'yellow';
    else if (edit === "moved") return 'blue';
    else if (edit === "mm" || "mm updOnTop") return 'purple';
    else return "black";
}

function getDecoration(range, pos, endPos) {
    return {
        range: new monaco.Range(pos.lineNumber, pos.column, endPos.lineNumber, endPos.column),
        options: {
            className: range.kind,
            zIndex: range.index,
            hoverMessage: {
                value: range.tooltip,
            },
            overviewRuler: {
                color: getEditColor(range.kind),
            },
        },
    };
}

function monaco(config) {
    require.config({paths: {'vs': 'https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.39.0/min/vs'}});
    require(['vs/editor/editor.main'], initializeEditors);


    function initializeEditors() {
        const left_container_id = config.lcid; /*'left-container';*/
        const right_container_id = config.rcid;/*'right-container';*/
        const leftContainer = document.getElementById(left_container_id);
        const rightContainer = document.getElementById(right_container_id);
        Promise.all(
            [
                fetch(config.left.url)
                    .then(result => result.text())
                    .then(text => monaco.editor.create(leftContainer, getEditorOptions(config, text))),
                fetch(config.right.url)
                    .then(result => result.text())
                    .then(text => monaco.editor.create(rightContainer, getEditorOptions(config, text)))
            ]
        ).then(([leftEditor, rightEditor]) => {
            config.mappings = config.mappings.map(mapping =>
                [
                    monaco.Range.fromPositions(leftEditor.getModel().getPositionAt(mapping[0]), leftEditor.getModel().getPositionAt(mapping[1])),
                    monaco.Range.fromPositions(rightEditor.getModel().getPositionAt(mapping[2]), rightEditor.getModel().getPositionAt(mapping[3])),
                ]);
            const leftDecorations = config.left.ranges.map(range => getDecoration(
                range,
                leftEditor.getModel().getPositionAt(range.from),
                leftEditor.getModel().getPositionAt(range.to)
            ));
            leftEditor.getModel().decorations = leftDecorations;
            leftEditor.deltaDecorations([], leftDecorations);
            leftEditor.onMouseDown((event) => {
                if (event.target.range) {
                    const allDecorations = leftEditor.getModel().getDecorationsInRange(event.target.range, leftEditor.id, true)
                    // .filter(decoration => decoration.options.className == "updated" || decoration.options.className == "moved");
                    if (allDecorations.length >= 1) {
                        let activatedRange = allDecorations[0].range;
                        if (allDecorations.length > 1) {
                            for (let i = 1; i < allDecorations.length; i = i + 1) {
                                const candidateRange = allDecorations[i].range;
                                if (activatedRange.containsRange(candidateRange))
                                    activatedRange = candidateRange;
                            }
                        }
                        const mapping = config.mappings.find(mapping => mapping[0].equalsRange(activatedRange))
                        if (mapping)
                            if (mapping.length > 1)
                                rightEditor.revealRangeInCenter(mapping[1]);
                    }
                }
            });
            const rightDecorations = config.right.ranges.map(range => getDecoration(
                range,
                rightEditor.getModel().getPositionAt(range.from),
                rightEditor.getModel().getPositionAt(range.to)
            ));
            rightEditor.getModel().decorations = rightDecorations;
            rightEditor.deltaDecorations([], rightDecorations);
            rightEditor.onMouseDown((event) => {
                if (event.target.range) {
                    const allDecorations = rightEditor.getModel().getDecorationsInRange(event.target.range, rightEditor.id, true)
                    // .filter(decoration => decoration.options.className == "updated" || decoration.options.className == "moved");
                    if (allDecorations.length >= 1) {
                        let activatedRange = allDecorations[0].range;
                        if (allDecorations.length > 1) {
                            for (let i = 1; i < allDecorations.length; i = i + 1) {
                                const candidateRange = allDecorations[i].range;
                                if (activatedRange.containsRange(candidateRange)) activatedRange = candidateRange;
                            }
                        }
                        const mapping = config.mappings.find(mapping => mapping[1].equalsRange(activatedRange))
                        if (mapping)
                            if (mapping.length > 1)
                                leftEditor.revealRangeInCenter(mapping[0]);
                    }
                }
            });

            setAllFoldings(config, leftEditor, rightEditor);
            leftEditor.getAction('editor.foldAll').run();
            rightEditor.getAction('editor.foldAll').run();

            if (config.spv === true) {
                const updateEditorsLayout = () => {
                    const leftHeight = leftEditor.getContentHeight();
                    const rightHeight = rightEditor.getContentHeight();
                    const editorHeight = Math.max(leftHeight, rightHeight);

                    leftContainer.style.overflow = 'auto';
                    rightContainer.style.overflow = 'auto';

                    leftContainer.style.height = `${editorHeight}px`;
                    rightContainer.style.height = `${editorHeight}px`;
                    leftEditor.layout();
                    rightEditor.layout();
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

function getLinesToFold(config, model, margin = 5) {

    decorations = model.decorations;
    const lineCount = model.getLineCount();
    const linesToKeepVisible = new Set();

    decorations.forEach(decoration => {
        if (config.moved){
            if (decoration.options.className === "inserted" || decoration.options.className === "deleted")
            {
                return;
            }
        }
        for (let line = Math.max(1, decoration.range.startLineNumber - margin);
             line <= Math.min(lineCount, decoration.range.endLineNumber + margin);
             line++) {
            linesToKeepVisible.add(line);
        }
    });

    const linesToFold = [];
    for (let line = 1; line <= lineCount; line++) {
        if (!linesToKeepVisible.has(line)) {
            linesToFold.push(line);
        }
    }
    return linesToFold;
}
function createFoldingRanges(linesToFold) {
    // Ensure linesToFold is sorted
    linesToFold.sort((a, b) => a - b);

    const ranges = [];
    let start = linesToFold[0];
    let end = linesToFold[0];

    for (let i = 1; i < linesToFold.length; i++) {
        if (linesToFold[i] === end + 1) {
            // Continue the current range
            end = linesToFold[i];
        } else {
            // Push the completed range
            ranges.push(new monaco.Range(start, 1, end, 1));
            // Start a new range
            start = linesToFold[i];
            end = linesToFold[i];
        }
    }
    // Push the last range
    ranges.push(new monaco.Range(start, 1, end, 1));
    return ranges;
}

function setAllFoldings(config, leftEditor, rightEditor) {
    const foldingRangeProvider = {
        provideFoldingRanges: (model, context, token) => {
            let rangesToFold = createFoldingRanges(getLinesToFold(config, model, 2));
            return rangesToFold.map(range => ({
                start: range.startLineNumber,
                end: range.endLineNumber,
                kind: monaco.languages.FoldingRangeKind.Region,
            }));
        },
    };
    //Assume both languages are the same
    var languageId = rightEditor.getModel().getLanguageId();
    if (!monaco.languages.getLanguages().some(lang => lang.id === languageId && lang.foldingRangeProvider)) {
        monaco.languages.registerFoldingRangeProvider(languageId, foldingRangeProvider);
    }
}