function getLinesToFold(config, model, margin = 5) {

    decorations = model.decorations;
    const lineCount = model.getLineCount();
    const linesToKeepVisible = new Set();

    decorations.forEach(decoration => {
        if (model.moved){
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
            if (model.id !== config.id) return;
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