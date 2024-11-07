function getDecorationNoLeadingWhiteSpace(range, pos, endPos, editor) {
    const decorations = [];
    // Helper function to get adjusted column position by skipping leading whitespace
    function getAdjustedPos(lineNumber, column) {
        const lineContent = editor.getModel().getLineContent(lineNumber);
        const trimmedStartColumn = lineContent.search(/\S|$/) + 1;
        return Math.max(column, trimmedStartColumn);
    }
    if (pos.lineNumber === endPos.lineNumber) {
        // Single-line decoration
        const adjustedPos = {
            lineNumber: pos.lineNumber,
            column: getAdjustedPos(pos.lineNumber, pos.column),
        };
        decorations.push({
            range: new monaco.Range(adjustedPos.lineNumber, adjustedPos.column, endPos.lineNumber, endPos.column),
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
        });
    }
    else {
        // Multi-line decoration
        // Decorate the first line
        const adjustedStartPos = {
            lineNumber: pos.lineNumber,
            column: getAdjustedPos(pos.lineNumber, pos.column),
        };
        decorations.push({
            range: new monaco.Range(adjustedStartPos.lineNumber, adjustedStartPos.column, pos.lineNumber, editor.getModel().getLineMaxColumn(pos.lineNumber)),
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
        });
        // Decorate each line in between, avoiding leading whitespace
        for (let line = pos.lineNumber + 1; line < endPos.lineNumber; line++) {
            const adjustedColumn = getAdjustedPos(line, 1); // Adjust for leading whitespace
            decorations.push({
                range: new monaco.Range(line, adjustedColumn, line, editor.getModel().getLineMaxColumn(line)),
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
            });
        }
        // Decorate the last line, skipping leading whitespace
        const adjustedEndPos = {
            lineNumber: endPos.lineNumber,
            column: getAdjustedPos(endPos.lineNumber, 1),
        };
        decorations.push({
            range: new monaco.Range(endPos.lineNumber, adjustedEndPos.column, endPos.lineNumber, endPos.column),
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
        });
    }
    // return [];
    return decorations;
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
function onClick(ed, mappings, dstIndex) {
    mapping = mappings[0]
    ed.revealRangeInCenter(mapping[dstIndex]);

    highlightDuration = 1000

    for (let i = 0; i < mappings.length; i++) {
        currentMapping = mappings[i];
        mappingElement = currentMapping[dstIndex]
        const decorationId = ed.deltaDecorations([], [
            {
                range: mappingElement,
                options: {
                    className: 'highlighted-range',
                    inlineClassName: 'highlighted-range-inline',
                }
            }
        ]);
        setTimeout(() => {
            ed.deltaDecorations([decorationId], []);  // Remove the decoration
        }, highlightDuration);
    }
}
function onClickHelper(config, index, activatedRange, ed, dstIndex) {
    candidates = config.mappings
        .filter(mapping =>
            mapping[index].startColumn <= activatedRange.startColumn
            && mapping[index].startLineNumber <= activatedRange.startLineNumber
            && !(mapping[index].endLineNumber === activatedRange.endLineNumber &&
                mapping[index].endColumn <= activatedRange.endColumn)
        );
    candidates = candidates.filter(candidate => candidate[index].containsRange(activatedRange))
    candidates
        .sort((a, b) => {
            if (b[index].startLineNumber !== a[index].startLineNumber) {
                return b[index].startLineNumber - a[index].startLineNumber;
            }
            if (b[index].startColumn !== a[index].startColumn) {
                return b[index].startColumn - a[index].startColumn;
            }
            if (a[index].endLineNumber !== b[index].endLineNumber) {
                return a[index].endLineNumber - b[index].endLineNumber;
            }
            return a[index].endColumn - b[index].endColumn;
        });
    // Find all candidates that tie with the first candidate (in case of multi-mapping
    const mappings = candidates.filter(candidate =>
        candidate[index].startLineNumber === candidates[0][index].startLineNumber &&
        candidate[index].startColumn === candidates[0][index].startColumn &&
        candidate[index].endLineNumber === candidates[0][index].endLineNumber &&
        candidate[index].endColumn === candidates[0][index].endColumn
    );
    if (mappings) {
        if (mappings.length >= 1) {
            onClick(ed, mappings, dstIndex);
        }
    }
}
function editorMouseDown(config, srcEditor, dstEditor, index, destIndex) {
    return (event) => {
        if (event.target.range) {
            const allDecorations = srcEditor.getModel().getDecorationsInRange(event.target.range, srcEditor.id, true)
            if (allDecorations.length >= 1) {
                let activatedRange = allDecorations[0].range;
                if (allDecorations.length > 1) {
                    for (let i = 1; i < allDecorations.length; i = i + 1) {
                        const candidateRange = allDecorations[i].range;
                        if (activatedRange.containsRange(candidateRange))
                            activatedRange = candidateRange;
                    }
                }
                onClickHelper(config, index, activatedRange, dstEditor, destIndex);
            }
        }
    };
}
function deltaDecorations(ed, dec) {
    ed.getModel().decorations = dec;
    ed.deltaDecorations([], dec);
}