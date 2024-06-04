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
    Promise.all([
        fetchEditorContent(config.left.url).then(content => createEditor('left-container', content)),
        fetchEditorContent(config.right.url).then(content => createEditor('right-container', content))
    ]).then(([leftEditor, rightEditor]) => {

        setDecorations(leftEditor, config.left.ranges);
        setDecorations(rightEditor, config.right.ranges);

        handleMouseDown(leftEditor, rightEditor, config.mappings);
        handleMouseDown(rightEditor, leftEditor, config.mappings);

        window.leftEditor = leftEditor;
        window.rightEditor = rightEditor;
    });
});

function fetchEditorContent(url) {
    return fetch(url).then(response => response.text());
}
function createEditor(containerId, content) {
    return monaco.editor.create(document.getElementById(containerId), getEditorOptions(content));
}
function setDecorations(editor, ranges) {
    const decorations = ranges.map(range => getDecoration(
        range,
        editor.getModel().getPositionAt(range.from),
        editor.getModel().getPositionAt(range.to)
    ));
    editor.deltaDecorations([], decorations);
}
function handleMouseDown(editor, targetEditor, mappings) {
    editor.onMouseDown(event => {
        if (event.target.range) {
            const allDecorations = editor.getModel().getDecorationsInRange(event.target.range, editor.id, true);
            if (allDecorations.length >= 1) {
                let activatedRange = allDecorations[0].range;
                for (let i = 1; i < allDecorations.length; i++) {
                    const candidateRange = allDecorations[i].range;
                    if (activatedRange.containsRange(candidateRange)) {
                        activatedRange = candidateRange;
                    }
                }
                const mapping = mappings.find(mapping => mapping[0].equalsRange(activatedRange) || mapping[1].equalsRange(activatedRange));
                if (mapping) {
                    const targetRange = mapping[0].equalsRange(activatedRange) ? mapping[1] : mapping[0];
                    targetEditor.revealRangeInCenter(targetRange);
                }
            }
        }
    });
}