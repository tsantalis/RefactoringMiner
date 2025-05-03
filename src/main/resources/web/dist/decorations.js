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
                //inlineClassName: range.kind, // Use this instead of className
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
                //inlineClassName: range.kind, // Use this instead of className
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
                    //inlineClassName: range.kind, // Use this instead of className
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
                //inlineClassName: range.kind, // Use this instead of className
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
            //inlineClassName: range.kind, // Use this instead of className
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
function onClick(ed, mapping, dstIndex) {
    const highlightDuration = 1000;
    const mainMapping = mapping[dstIndex];

    // Force unfold by setting selection and running the unfold action
    ed.setSelection(mainMapping);
    ed.revealRangeInCenterIfOutsideViewport(mainMapping);

    // Trigger built-in unfold for the current selection
    ed.getAction('editor.unfold').run();

    const decorationId = ed.deltaDecorations([], [
        {
            range: mainMapping,
            options: {
                className: 'highlighted-range',
                inlineClassName: 'highlighted-range-inline',
            }
        }
    ]);
    setTimeout(() => {
        ed.deltaDecorations([decorationId], []);
    }, highlightDuration);
}


function offsetToLineNumber(text, offset) {
  if (offset < 0 || offset > text.length) {
    return -1; // Invalid offset
  }
  const lines = text.substring(0, offset).split('\n');
  return lines.length;
}

function offsetToLineColumn(text, offset) {
  let line = 1;
  let column = 1;
  for (let i = 0; i < offset; i++) {
    if (text[i] === '\n') {
      line++;
      column = 1;
    } else {
      column++;
    }
  }
  return { line, column };
}

function onClickHelper(config, index, activatedRange, ed, dstIndex) {
	var exit = false;
	if(index === 0) {
		config.left.ranges.forEach(range => { 
			let fromLine = offsetToLineNumber(config.left.content, range.from);
			let toLine = offsetToLineNumber(config.left.content, range.to);
			if(fromLine <= activatedRange.startLineNumber && toLine >= activatedRange.startLineNumber) {
				if(range.kind === "deleted") {
					if(fromLine === toLine) {
						// the column matters
						let fromColumn = offsetToLineColumn(config.left.content, range.from);
						let toColumn = offsetToLineColumn(config.left.content, range.to);
						if(fromColumn.column <= activatedRange.startColumn && toColumn.column >= activatedRange.startColumn) {
							exit = true;
						}
					}
					else {
						exit = true;
					}
				}
				else if(range.kind === "moved" || range.kind === "updated" || range.kind.startsWith("mm")) {
					exit = false;
				}
			}
		});
	}
	else if(index === 1) {
		config.right.ranges.forEach(range => { 
			let fromLine = offsetToLineNumber(config.right.content, range.from);
			let toLine = offsetToLineNumber(config.right.content, range.to);
			if(fromLine <= activatedRange.startLineNumber && toLine >= activatedRange.startLineNumber) {
				if(range.kind === "inserted") {
					if(fromLine === toLine) {
						// the column matters
						let fromColumn = offsetToLineColumn(config.right.content, range.from);
						let toColumn = offsetToLineColumn(config.right.content, range.to);
						if(fromColumn.column <= activatedRange.startColumn && toColumn.column >= activatedRange.startColumn) {
							exit = true;
						}
					}
					else {
						exit = true;
					}
				}
				else if(range.kind === "moved" || range.kind === "updated" || range.kind.startsWith("mm")) {
					exit = false;
				}
			}
		});
	}
	if(exit) {
		return;
	}
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
            //select the mappings that span one line in both sides
            for (var i = 0; i < mappings.length; i++) {
                if(mappings[i][dstIndex].startLineNumber === mappings[i][dstIndex].endLineNumber && mappings[i][index].startLineNumber === mappings[i][index].endLineNumber) {
                    onClick(ed, mappings[i], dstIndex);
                }
            }
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