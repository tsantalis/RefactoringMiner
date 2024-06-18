/*
 * This file is part of GumTree.
 *
 * GumTree is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GumTree is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GumTree.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2011-2015 Jean-Rémy Falleri <jr.falleri@gmail.com>
 * Copyright 2011-2015 Floréal Morandat <florealm@gmail.com>
 */

let margin = 2;
function getEditorOptions(text) {
    return {
        value: text,
        readOnly: true,
        language: getLanguage(),
        automaticLayout: true,
        scrollBeyondLastLine: false,
        lineDecorationsWidth: 0,
        glyphMargin: false,
        minimap: {
            enabled: false,
        },
        wordWrap: 'on' // Enables word wrap
    };
}

function getLanguage() {
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
require.config({ paths: { 'vs': '/monaco/min/vs' }});
require(['vs/editor/editor.main'], function() {
    Promise.all(
        [
            fetch(config.left.url)
                .then(result => result.text())
                .then(text => monaco.editor.create(document.getElementById('left-container'), getEditorOptions(text))),
            fetch(config.right.url)
                .then(result => result.text())
                .then(text => monaco.editor.create(document.getElementById('right-container'), getEditorOptions(text)))
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
        setAllFoldings(leftEditor, rightEditor);
        leftEditor.getAction('editor.foldAll').run();
        rightEditor.getAction('editor.foldAll').run();
        window.leftEditor = leftEditor;
        window.rightEditor = rightEditor;
    });
});

function getLinesToFold(model, margin = 5) {

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

function setAllFoldings(leftEditor, rightEditor) {
    const foldingRangeProvider = {
        provideFoldingRanges: (model, context, token) => {
            let rangesToFold = createFoldingRanges(getLinesToFold(model, margin));
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