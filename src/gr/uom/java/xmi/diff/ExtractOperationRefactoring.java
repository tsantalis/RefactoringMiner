package gr.uom.java.xmi.diff;

import java.util.LinkedHashSet;
import java.util.Set;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.replacement.Replacement;

public class ExtractOperationRefactoring implements Refactoring {
	private UMLOperation extractedOperation;
	private UMLOperation sourceOperationBeforeExtraction;
	private UMLOperation sourceOperationAfterExtraction;
	private Set<Replacement> replacements;
	private Set<AbstractCodeFragment> extractedCodeFragments;

	public ExtractOperationRefactoring(UMLOperationBodyMapper bodyMapper, UMLOperation sourceOperationAfterExtraction) {
		this.extractedOperation = bodyMapper.getOperation2();
		this.sourceOperationBeforeExtraction = bodyMapper.getOperation1();
		this.sourceOperationAfterExtraction = sourceOperationAfterExtraction;
		this.replacements = bodyMapper.getReplacements();
		this.extractedCodeFragments = new LinkedHashSet<AbstractCodeFragment>();
		for(AbstractCodeMapping mapping : bodyMapper.getMappings()) {
			this.extractedCodeFragments.add(mapping.getFragment1());
		}
	}

	public ExtractOperationRefactoring(UMLOperationBodyMapper bodyMapper, UMLOperation extractedOperation, UMLOperation sourceOperationBeforeExtraction, UMLOperation sourceOperationAfterExtraction) {
		this.extractedOperation = extractedOperation;
		this.sourceOperationBeforeExtraction = sourceOperationBeforeExtraction;
		this.sourceOperationAfterExtraction = sourceOperationAfterExtraction;
		this.replacements = bodyMapper.getReplacements();
		this.extractedCodeFragments = new LinkedHashSet<AbstractCodeFragment>();
		for(AbstractCodeMapping mapping : bodyMapper.getMappings()) {
			this.extractedCodeFragments.add(mapping.getFragment1());
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(extractedOperation);
		sb.append(" extracted from ");
		sb.append(sourceOperationBeforeExtraction);
		sb.append(" in class ");
		sb.append(getClassName());
		return sb.toString();
	}

	private String getClassName() {
		return getSourceOperationBeforeExtraction().getClassName();
	}

	public UMLOperation getExtractedOperation() {
		return extractedOperation;
	}

	public UMLOperation getSourceOperationBeforeExtraction() {
		return sourceOperationBeforeExtraction;
	}

	public UMLOperation getSourceOperationAfterExtraction() {
		return sourceOperationAfterExtraction;
	}

	public Set<Replacement> getReplacements() {
		return replacements;
	}

	public Set<AbstractCodeFragment> getExtractedCodeFragments() {
		return extractedCodeFragments;
	}

	public CodeRange getSourceOperationCodeRangeBeforeExtraction() {
		return codeRange(sourceOperationBeforeExtraction);
	}

	public CodeRange getSourceOperationCodeRangeAfterExtraction() {
		return codeRange(sourceOperationAfterExtraction);
	}

	public CodeRange getExtractedOperationCodeRange() {
		return codeRange(extractedOperation);
	}

	private CodeRange codeRange(UMLOperation operation) {
		LocationInfo info = operation.getLocationInfo();
		return new CodeRange(info.getFilePath(),
				info.getStartLine(), info.getEndLine(),
				info.getStartColumn(), info.getEndColumn());
	}

	public CodeRange getExtractedCodeRangeFromSourceOperation() {
		String filePath = null;
		int minStartLine = 0;
		int maxEndLine = 0;
		int startColumn = 0;
		int endColumn = 0;
		
		for(AbstractCodeFragment fragment : extractedCodeFragments) {
			LocationInfo info = fragment.getLocationInfo();
			filePath = info.getFilePath();
			if(minStartLine == 0 || info.getStartLine() < minStartLine) {
				minStartLine = info.getStartLine();
				startColumn = info.getStartColumn();
			}
			if(info.getEndLine() > maxEndLine) {
				maxEndLine = info.getEndLine();
				endColumn = info.getEndColumn();
			}
		}
		return new CodeRange(filePath, minStartLine, maxEndLine, startColumn, endColumn);
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.EXTRACT_OPERATION;
	}
}
