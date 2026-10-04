//the ranges of the lines from pos to endPos, skipping the leading whitespace of each line
function getLineRangesNoLeadingWhiteSpace(pos, endPos, editor) {
    const model = editor.getModel();
    // Helper function to get adjusted column position by skipping leading whitespace
    function getAdjustedColumn(lineNumber, column) {
        const lineContent = model.getLineContent(lineNumber);
        const trimmedStartColumn = lineContent.search(/\S|$/) + 1;
        return Math.max(column, trimmedStartColumn);
    }
    if (pos.lineNumber === endPos.lineNumber) {
        return [new monaco.Range(pos.lineNumber, getAdjustedColumn(pos.lineNumber, pos.column), endPos.lineNumber, endPos.column)];
    }
    const ranges = [];
    // the first line
    ranges.push(new monaco.Range(pos.lineNumber, getAdjustedColumn(pos.lineNumber, pos.column), pos.lineNumber, model.getLineMaxColumn(pos.lineNumber)));
    // each line in between
    for (let line = pos.lineNumber + 1; line < endPos.lineNumber; line++) {
        ranges.push(new monaco.Range(line, getAdjustedColumn(line, 1), line, model.getLineMaxColumn(line)));
    }
    // the last line
    ranges.push(new monaco.Range(endPos.lineNumber, getAdjustedColumn(endPos.lineNumber, 1), endPos.lineNumber, endPos.column));
    return ranges;
}
//the decorations coloring the range, without the tooltips, which are added by getTooltipDecorations
function getDecorationNoLeadingWhiteSpace(range, pos, endPos, editor) {
    return getLineRangesNoLeadingWhiteSpace(pos, endPos, editor).map(lineRange => ({
        range: lineRange,
        options: {
            className: range.kind,
            //inlineClassName: range.kind, // Use this instead of className
            zIndex: range.index,
            overviewRuler: {
                color: getEditColor(range.kind),
            },
        },
    }));
}
//marks the decorations that only show tooltips, so that they are ignored when locating the clicked AST node
const TOOLTIP_DECORATION = 'ast-tooltip';
function isTooltipDecoration(decoration) {
    return decoration.options && decoration.options.description === TOOLTIP_DECORATION;
}
//Monaco shows the hover messages of all decorations containing the mouse position, so the tooltips of a parent AST node would be repeated
//in all its child AST nodes; the tooltips of each AST node are shown only in the parts of its range that are not covered by nested AST nodes with tooltips
function getTooltipDecorations(ranges, editor) {
    const model = editor.getModel();
    const text = model.getValue();
    //the tooltips of each AST node, as an AST node may have multiple ranges (i.e., kinds) with tooltips
    const nodes = new Map();
    ranges.forEach(range => {
        if (!range.tooltip) return;
        const key = range.from + ':' + range.to;
        if (!nodes.has(key)) {
            nodes.set(key, {from: range.from, to: range.to, messages: [], tooltips: new Set()});
        }
        const node = nodes.get(key);
        if (node.tooltips.has(range.tooltip)) return;
        node.tooltips.add(range.tooltip);
        node.messages.push({
            value: range.requestPath
                ? `[${range.tooltip}](${new URL(range.requestPath, window.location.href).href})`
                : range.tooltip,
            isTrusted: true
        });
    });
    const sorted = [...nodes.values()].sort((a, b) => a.from - b.from || b.to - a.to);
    const decorations = [];
    sorted.forEach(node => {
        //the nested AST nodes with tooltips, which show their own tooltips
        const nested = sorted.filter(other => other !== node && other.from >= node.from && other.to <= node.to);
        let start = node.from;
        nested.forEach(other => {
            if (other.from > start) {
                addTooltipSegment(start, other.from);
            }
            start = Math.max(start, other.to);
        });
        if (start < node.to) {
            addTooltipSegment(start, node.to);
        }
        function addTooltipSegment(from, to) {
            if (text.substring(from, to).trim().length === 0) return;
            const pos = model.getPositionAt(from);
            const endPos = model.getPositionAt(to);
            getLineRangesNoLeadingWhiteSpace(pos, endPos, editor).forEach(lineRange => {
                if (lineRange.isEmpty()) return;
                decorations.push({
                    range: lineRange,
                    options: {
                        description: TOOLTIP_DECORATION,
                        hoverMessage: node.messages,
                    },
                });
            });
        }
    });
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
                value: range.requestPath
                    ? `[${range.tooltip}](${new URL(range.requestPath, window.location.href).href})`
                    : range.tooltip,
                isTrusted: true
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

//returns the range of the opening curly brace at the start of the given range, or preceding it with only whitespace in between,
//i.e., the Kotlin statements node of a block does not include the curly braces
function findOpeningCurlyBrace(model, range) {
	const text = model.getValue();
	let offset = model.getOffsetAt(range.getStartPosition());
	if(text.charAt(offset) !== '{') {
		offset--;
		while(offset >= 0 && /\s/.test(text.charAt(offset))) {
			offset--;
		}
	}
	if(offset < 0 || text.charAt(offset) !== '{') {
		return null;
	}
	return monaco.Range.fromPositions(model.getPositionAt(offset), model.getPositionAt(offset + 1));
}
//checks if the innermost ranges containing the clicked range have no match in the other side of the diff
function innermostRangeWithoutMatch(config, index, activatedRange) {
	const side = index === 0 ? config.left : config.right;
	const kindsWithoutMatch = index === 0 ? ["deleted", "moveOut"] : ["inserted", "moveIn"];
	//compute the offsets of the clicked range in the content of the clicked side
	const lines = side.content.split('\n');
	let startOffset = 0;
	for(let i = 0; i < activatedRange.startLineNumber - 1; i++) {
		startOffset += lines[i].length + 1;
	}
	startOffset += activatedRange.startColumn - 1;
	let endOffset = 0;
	for(let i = 0; i < activatedRange.endLineNumber - 1; i++) {
		endOffset += lines[i].length + 1;
	}
	endOffset += activatedRange.endColumn - 1;
	let innermost = [];
	side.ranges.forEach(range => {
		if(range.from <= startOffset && range.to >= endOffset) {
			if(innermost.length === 0 || range.to - range.from < innermost[0].to - innermost[0].from) {
				innermost = [range];
			}
			else if(range.to - range.from === innermost[0].to - innermost[0].from) {
				innermost.push(range);
			}
		}
	});
	//the innermost ranges with the same span are the clicked node and its single-child wrappers, i.e., Kotlin lambda_parameters -> variable_declaration -> sink,
	//so the clicked node has a match, unless all of them have no match
	return innermost.length > 0 && innermost.every(range => kindsWithoutMatch.includes(range.kind));
}
function onClickHelper(config, index, activatedRange, ed, dstIndex) {
	var exit = [];
	if(index === 0) {
		config.left.ranges.forEach(range => { 
			let fromLine = offsetToLineNumber(config.left.content, range.from);
			let toLine = offsetToLineNumber(config.left.content, range.to);
			if(fromLine <= activatedRange.startLineNumber && toLine >= activatedRange.startLineNumber) {
				//the deleted ASTs and the ASTs moved out to another file have no match in the right side
				if(range.kind === "deleted" || range.kind === "moveOut") {
					if(fromLine === toLine) {
						// the column matters
						let fromColumn = offsetToLineColumn(config.left.content, range.from);
						let toColumn = offsetToLineColumn(config.left.content, range.to);
						if(fromColumn.column <= activatedRange.startColumn && toColumn.column >= activatedRange.startColumn) {
							exit.push(true);
						}
					}
					else {
						exit.push(true);
					}
				}
				else if(range.kind === "moved" || range.kind === "updated" || range.kind.startsWith("mm")) {
					exit.push(false);
				}
			}
		});
	}
	else if(index === 1) {
		config.right.ranges.forEach(range => { 
			let fromLine = offsetToLineNumber(config.right.content, range.from);
			let toLine = offsetToLineNumber(config.right.content, range.to);
			if(fromLine <= activatedRange.startLineNumber && toLine >= activatedRange.startLineNumber) {
				//the inserted ASTs and the ASTs moved in from another file have no match in the left side
				if(range.kind === "inserted" || range.kind === "moveIn") {
					if(fromLine === toLine) {
						// the column matters
						let fromColumn = offsetToLineColumn(config.right.content, range.from);
						let toColumn = offsetToLineColumn(config.right.content, range.to);
						if(fromColumn.column <= activatedRange.startColumn && toColumn.column >= activatedRange.startColumn) {
							exit.push(true);
						}
					}
					else {
						exit.push(true);
					}
				}
				else if(range.kind === "moved" || range.kind === "updated" || range.kind.startsWith("mm")) {
					exit.push(false);
				}
			}
		});
	}
	if(!exit.includes(false) && exit.length > 0) {
		return;
	}
	//the innermost range containing the clicked range is deleted/inserted or moved out to/in from another file,
	//even if there are moved, updated, or multi-mapped ranges in the same line, i.e., the method containing a statement moved to another file
	if(innermostRangeWithoutMatch(config, index, activatedRange)) {
		return;
	}
    candidates = config.mappings
        .filter(mapping =>
            mapping[index].startColumn <= activatedRange.startColumn
            && mapping[index].startLineNumber <= activatedRange.startLineNumber
            //the mapping of the clicked range itself is a candidate, i.e., a lambda parameter mapped to a try resource name
            && !(mapping[index].endLineNumber === activatedRange.endLineNumber &&
                mapping[index].endColumn < activatedRange.endColumn)
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
				//the click is on an inner line of a multi-line mapping, i.e., an unmapped line within a method or class, nothing should be highlighted
				const clickedLine = activatedRange.startLineNumber;
				if(clickedLine !== mappings[i][index].startLineNumber && clickedLine !== mappings[i][index].endLineNumber) {
					continue;
				}
                if(mappings[i][dstIndex].startLineNumber === mappings[i][dstIndex].endLineNumber && mappings[i][index].startLineNumber === mappings[i][index].endLineNumber) {
                    onClick(ed, mappings[i], dstIndex);
                }
				else if(mappings[i][dstIndex].endLineNumber - mappings[i][dstIndex].startLineNumber == mappings[i][index].endLineNumber - mappings[i][index].startLineNumber) {
					onClick(ed, mappings[i], dstIndex);
				}
				else {
					//the mapping spans a different number of lines in each side, i.e., a composite statement reformatted during migration
					//highlight only the first line of the destination range to avoid highlighting a large region
					const dstRange = mappings[i][dstIndex];
					const firstLine = dstRange.startLineNumber;
					let firstLineRange = new monaco.Range(firstLine, dstRange.startColumn, firstLine, ed.getModel().getLineMaxColumn(firstLine));
					//a block starting with an opening curly brace, highlight only the opening curly brace of the destination block
					const srcRange = mappings[i][index];
					const srcContent = index === 0 ? config.left.content : config.right.content;
					if(srcContent.split('\n')[srcRange.startLineNumber - 1].charAt(srcRange.startColumn - 1) === '{') {
						const braceRange = findOpeningCurlyBrace(ed.getModel(), dstRange);
						if(braceRange) {
							firstLineRange = braceRange;
						}
					}
					const firstLineMapping = [];
					firstLineMapping[index] = mappings[i][index];
					firstLineMapping[dstIndex] = firstLineRange;
					onClick(ed, firstLineMapping, dstIndex);
				}
            }
        }
    }
}
function editorMouseDown(config, srcEditor, dstEditor, index, destIndex) {
    return (event) => {
        if (event.target.range) {
            const allDecorations = srcEditor.getModel().getDecorationsInRange(event.target.range, srcEditor.id, true)
                .filter(decoration => !isTooltipDecoration(decoration))
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