package gr.uom.java.xmi.decomposition;

import static gr.uom.java.xmi.diff.UMLCommentListDiff.findAllMatchingIndices;

import java.util.ArrayList;
import java.util.List;

import gr.uom.java.xmi.UMLComment;
import gr.uom.java.xmi.VariableDeclarationContainer;

public class CodeBlockBetweenComments {
	private List<AbstractCodeFragment> leaves = new ArrayList<AbstractCodeFragment>();
	private List<CompositeStatementObject> innerNodes = new ArrayList<CompositeStatementObject>();
	private UMLComment startComment;
	private UMLComment endComment;
	private UMLComment afterEndComment;
	private int startCommentPosition;
	private int endCommentPosition;
	private int afterEndCommentPosition;
	
	public CodeBlockBetweenComments(UMLComment startComment, UMLComment endComment) {
		this.startComment = startComment;
		this.endComment = endComment;
	}

	public List<AbstractCodeFragment> getLeaves() {
		return leaves;
	}

	public List<CompositeStatementObject> getInnerNodes() {
		return innerNodes;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(startComment).append("\n");
		for(AbstractCodeFragment leaf : leaves) {
			sb.append(leaf.getLocationInfo().getStartLine());
			sb.append(": ");
			sb.append(leaf);
		}
		if(endComment != null) {
			sb.append(endComment);
		}
		return sb.toString();
	}

	public boolean compatible(CodeBlockBetweenComments other) {
		if(this.startComment.getText().equals(other.startComment.getText())) {
			if(this.startCommentPosition == other.startCommentPosition && (this.endComment == null || other.endComment == null)) {
				return true;
			}
			boolean equalEnd = false;
			if(this.endComment != null && other.endComment != null) {
				equalEnd = this.endComment.getText().equals(other.endComment.getText());
			}
			else if(this.endComment == null && other.endComment == null) {
				equalEnd = true;
			}
			return equalEnd && this.startCommentPosition == other.startCommentPosition && this.endCommentPosition == other.endCommentPosition;
		}
		return false;
	}

	public boolean compatibleWithAfterEnd(CodeBlockBetweenComments other) {
		if(this.startComment.getText().equals(other.startComment.getText())) {
			if(this.startCommentPosition == other.startCommentPosition && this.endComment == null && other.afterEndComment == null) {
				return true;
			}
			if(this.startCommentPosition == other.startCommentPosition && this.afterEndComment == null && other.endComment == null) {
				return true;
			}
			boolean equalEnd = false;
			boolean equalEndPosition = false;
			if(this.endComment != null && other.afterEndComment != null && this.endComment.getText().equals(other.afterEndComment.getText())) {
				equalEnd = true;
				equalEndPosition = this.endCommentPosition == other.afterEndCommentPosition;
			}
			if(this.afterEndComment != null && other.endComment != null && this.afterEndComment.getText().equals(other.endComment.getText())) {
				equalEnd = true;
				equalEndPosition = this.afterEndCommentPosition == other.endCommentPosition;
			}
			return equalEnd && equalEndPosition && this.startCommentPosition == other.startCommentPosition;
		}
		return false;
	}

	public static CodeBlockBetweenComments generateCodeBlock(AbstractCodeFragment fragment, VariableDeclarationContainer container) {
		List<UMLComment> comments = container.getComments();
		for(int i=0; i<comments.size(); i++) {
			UMLComment startComment = comments.get(i);
			if(container.getBody() != null && startComment.getLocationInfo().before(container.getBody().getCompositeStatement().getLocationInfo())) {
				continue;
			}
			int startIndex = i;
			List<Integer> allMatchingIndicesForStart = findAllMatchingIndices(container.getComments(), startComment);
			UMLComment endComment = i == comments.size()-1 ? null : comments.get(i+1);
			CodeBlockBetweenComments block = new CodeBlockBetweenComments(startComment, endComment);
			block.startCommentPosition = allMatchingIndicesForStart.indexOf(startIndex);
			if(endComment != null) {
				int endIndex = i+1;
				List<Integer> allMatchingIndicesForEnd = findAllMatchingIndices(container.getComments(), endComment);
				block.endCommentPosition = allMatchingIndicesForEnd.indexOf(endIndex);
			}
			else {
				block.endCommentPosition = -1;
			}
			UMLComment afterEndComment = i >= comments.size()-2 ? null : comments.get(i+2);
			block.afterEndComment = afterEndComment;
			if(afterEndComment != null) {
				int afterEndIndex = i+2;
				List<Integer> allMatchingIndicesForAfterEnd = findAllMatchingIndices(container.getComments(), afterEndComment);
				block.afterEndCommentPosition = allMatchingIndicesForAfterEnd.indexOf(afterEndIndex);
			}
			else {
				block.afterEndCommentPosition = -1;
			}
			if(startComment.getLocationInfo().before(fragment.getLocationInfo())) {
				if(endComment != null) {
					if(fragment.getLocationInfo().before(endComment.getLocationInfo())) {
						return block;
					}
				}
				else {
					return block;
				}
			}
		}
		return null;
	}

	public static List<CodeBlockBetweenComments> generateCodeBlocks(List<AbstractCodeFragment> leaves, VariableDeclarationContainer container) {
		List<CodeBlockBetweenComments> list = new ArrayList<CodeBlockBetweenComments>();
		List<UMLComment> comments = container.getComments();
		for(int i=0; i<comments.size(); i++) {
			UMLComment startComment = comments.get(i);
			if(container.getBody() != null && startComment.getLocationInfo().before(container.getBody().getCompositeStatement().getLocationInfo())) {
				continue;
			}
			int startIndex = i;
			List<Integer> allMatchingIndicesForStart = findAllMatchingIndices(container.getComments(), startComment);
			UMLComment endComment = i == comments.size()-1 ? null : comments.get(i+1);
			CodeBlockBetweenComments block = new CodeBlockBetweenComments(startComment, endComment);
			block.startCommentPosition = allMatchingIndicesForStart.indexOf(startIndex);
			if(endComment != null) {
				int endIndex = i+1;
				List<Integer> allMatchingIndicesForEnd = findAllMatchingIndices(container.getComments(), endComment);
				block.endCommentPosition = allMatchingIndicesForEnd.indexOf(endIndex);
			}
			else {
				block.endCommentPosition = -1;
			}
			UMLComment afterEndComment = i >= comments.size()-2 ? null : comments.get(i+2);
			block.afterEndComment = afterEndComment;
			if(afterEndComment != null) {
				int afterEndIndex = i+2;
				List<Integer> allMatchingIndicesForAfterEnd = findAllMatchingIndices(container.getComments(), afterEndComment);
				block.afterEndCommentPosition = allMatchingIndicesForAfterEnd.indexOf(afterEndIndex);
			}
			else {
				block.afterEndCommentPosition = -1;
			}
			for(AbstractCodeFragment leaf : leaves) {
				if(startComment.getLocationInfo().before(leaf.getLocationInfo())) {
					if(endComment != null) {
						if(leaf.getLocationInfo().before(endComment.getLocationInfo())) {
							block.leaves.add(leaf);
						}
					}
					else {
						block.leaves.add(leaf);
					}
				}
			}
			list.add(block);
		}
		return list;
	}
}
