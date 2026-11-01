package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.refactoringminer.api.Refactoring;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLComment;
import gr.uom.java.xmi.UMLEnumConstant;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;

/**
 * The comments are assigned to the program element they precede or follow on the same line, and are compared only between matched elements.
 * A comment whose element changes, i.e., the section comment // JabRef-specific fields above the first of reordered enum constants (issue #993),
 * or that loses its element, i.e., a header comment separated by an empty line from the next function after the commit, is left deleted and added.
 * This pass matches such comments, left unmatched by all the comment diffs of the elements in a pair of files, by identical text.
 */
public class CommentsMovedBetweenElements {
	private final List<UMLComment> deletedComments = new ArrayList<UMLComment>();
	private final List<UMLComment> addedComments = new ArrayList<UMLComment>();
	//the locations of the corresponding elements, i.e., the attributes, enum constants and operations of the class diffs
	private final Map<LocationInfo, LocationInfo> correspondingLocations = new LinkedHashMap<LocationInfo, LocationInfo>();
	//the extra identical comments multi-mapped by a class diff, which are released when they have an actual counterpart
	private final Map<UMLComment, Pair<UMLCommentListDiff, Pair<UMLComment, UMLComment>>> multiMappings = new LinkedHashMap<UMLComment, Pair<UMLCommentListDiff, Pair<UMLComment, UMLComment>>>();

	public static List<UMLCommentListDiff> match(UMLModelDiff modelDiff, Set<Refactoring> refactorings) {
		Map<Pair<String, String>, CommentsMovedBetweenElements> filePairs = new LinkedHashMap<Pair<String, String>, CommentsMovedBetweenElements>();
		List<UMLClassBaseDiff> classDiffs = new ArrayList<UMLClassBaseDiff>();
		classDiffs.addAll(modelDiff.getCommonClassDiffList());
		classDiffs.addAll(modelDiff.getClassRenameDiffList());
		classDiffs.addAll(modelDiff.getClassMoveDiffList());
		for(UMLClassBaseDiff classDiff : classDiffs) {
			String filePath1 = classDiff.getOriginalClass().getLocationInfo().getFilePath();
			String filePath2 = classDiff.getNextClass().getLocationInfo().getFilePath();
			filePairs.computeIfAbsent(Pair.of(filePath1, filePath2), k -> new CommentsMovedBetweenElements()).collect(classDiff);
		}
		//the comments before a method moved to another file stay in the file, i.e., a section comment above a function moved to a new module
		for(Refactoring refactoring : refactorings) {
			if(refactoring instanceof MoveOperationRefactoring move) {
				UMLOperationBodyMapper mapper = move.getBodyMapper();
				UMLCommentListDiff diff = new UMLCommentListDiff(leadingComments(mapper.getContainer1()), leadingComments(mapper.getContainer2()));
				CommentsMovedBetweenElements filePair1 = filePair(filePairs, mapper.getContainer1().getLocationInfo().getFilePath(), true);
				if(filePair1 != null) {
					filePair1.deletedComments.addAll(diff.getDeletedComments());
				}
				CommentsMovedBetweenElements filePair2 = filePair(filePairs, mapper.getContainer2().getLocationInfo().getFilePath(), false);
				if(filePair2 != null) {
					filePair2.addedComments.addAll(diff.getAddedComments());
				}
			}
		}
		List<UMLCommentListDiff> diffs = new ArrayList<UMLCommentListDiff>();
		for(Map.Entry<Pair<String, String>, CommentsMovedBetweenElements> entry : filePairs.entrySet()) {
			CommentsMovedBetweenElements filePair = entry.getValue();
			//the classes removed from (added to) the files of the pair
			for(UMLClass removedClass : modelDiff.getRemovedClasses()) {
				if(removedClass.getLocationInfo().getFilePath().equals(entry.getKey().getLeft())) {
					filePair.deletedComments.addAll(elementComments(removedClass));
				}
			}
			for(UMLClass addedClass : modelDiff.getAddedClasses()) {
				if(addedClass.getLocationInfo().getFilePath().equals(entry.getKey().getRight())) {
					filePair.addedComments.addAll(elementComments(addedClass));
				}
			}
			UMLCommentListDiff diff = filePair.matchIdenticalComments();
			if(!diff.getCommonComments().isEmpty()) {
				diffs.add(diff);
			}
		}
		return diffs;
	}

	//the pair of the file before (after), preferably the pair of the file with itself
	private static CommentsMovedBetweenElements filePair(Map<Pair<String, String>, CommentsMovedBetweenElements> filePairs, String filePath, boolean before) {
		CommentsMovedBetweenElements sameFile = filePairs.get(Pair.of(filePath, filePath));
		if(sameFile != null) {
			return sameFile;
		}
		for(Map.Entry<Pair<String, String>, CommentsMovedBetweenElements> entry : filePairs.entrySet()) {
			if((before ? entry.getKey().getLeft() : entry.getKey().getRight()).equals(filePath)) {
				return entry.getValue();
			}
		}
		return null;
	}

	private void collect(UMLClassBaseDiff classDiff) {
		UMLCommentListDiff classCommentListDiff = classDiff.getCommentListDiff();
		addLeftovers(classCommentListDiff);
		for(Pair<UMLComment, UMLComment> pair : classCommentListDiff.getMultiMappedDeletedComments()) {
			deletedComments.add(pair.getLeft());
			multiMappings.put(pair.getLeft(), Pair.of(classCommentListDiff, pair));
		}
		for(Pair<UMLComment, UMLComment> pair : classCommentListDiff.getMultiMappedAddedComments()) {
			addedComments.add(pair.getRight());
			multiMappings.put(pair.getRight(), Pair.of(classCommentListDiff, pair));
		}
		for(Pair<UMLEnumConstant, UMLEnumConstant> pair : classDiff.getCommonEnumConstants()) {
			addLeftovers(pair.getLeft().getComments(), pair.getRight().getComments());
			correspondingLocations.put(pair.getLeft().getLocationInfo(), pair.getRight().getLocationInfo());
		}
		for(UMLEnumConstantDiff enumConstantDiff : classDiff.getEnumConstantDiffList()) {
			addLeftovers(enumConstantDiff.getRemovedEnumConstant().getComments(), enumConstantDiff.getAddedEnumConstant().getComments());
			correspondingLocations.put(enumConstantDiff.getRemovedEnumConstant().getLocationInfo(), enumConstantDiff.getAddedEnumConstant().getLocationInfo());
		}
		for(Pair<UMLAttribute, UMLAttribute> pair : classDiff.getCommonAtrributes()) {
			addLeftovers(pair.getLeft().getComments(), pair.getRight().getComments());
			correspondingLocations.put(pair.getLeft().getLocationInfo(), pair.getRight().getLocationInfo());
		}
		for(UMLAttributeDiff attributeDiff : classDiff.getAttributeDiffList()) {
			addLeftovers(attributeDiff.getRemovedAttribute().getComments(), attributeDiff.getAddedAttribute().getComments());
			correspondingLocations.put(attributeDiff.getRemovedAttribute().getLocationInfo(), attributeDiff.getAddedAttribute().getLocationInfo());
		}
		for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
			addLeftovers(leadingComments(mapper.getContainer1()), leadingComments(mapper.getContainer2()));
			correspondingLocations.put(mapper.getContainer1().getLocationInfo(), mapper.getContainer2().getLocationInfo());
		}
		for(UMLEnumConstant enumConstant : classDiff.getRemovedEnumConstants()) {
			deletedComments.addAll(enumConstant.getComments());
		}
		for(UMLEnumConstant enumConstant : classDiff.getAddedEnumConstants()) {
			addedComments.addAll(enumConstant.getComments());
		}
		for(UMLAttribute attribute : classDiff.getRemovedAttributes()) {
			deletedComments.addAll(attribute.getComments());
		}
		for(UMLAttribute attribute : classDiff.getAddedAttributes()) {
			addedComments.addAll(attribute.getComments());
		}
		for(UMLOperation operation : classDiff.getRemovedOperations()) {
			deletedComments.addAll(leadingComments(operation));
		}
		for(UMLOperation operation : classDiff.getAddedOperations()) {
			addedComments.addAll(leadingComments(operation));
		}
	}

	//the comments of a removed (added) class and of its members, except the comments within operation bodies
	private static List<UMLComment> elementComments(UMLClass umlClass) {
		List<UMLComment> comments = new ArrayList<UMLComment>(umlClass.getComments());
		for(UMLEnumConstant enumConstant : umlClass.getEnumConstants()) {
			comments.addAll(enumConstant.getComments());
		}
		for(UMLAttribute attribute : umlClass.getAttributes()) {
			comments.addAll(attribute.getComments());
		}
		for(UMLOperation operation : umlClass.getOperations()) {
			comments.addAll(leadingComments(operation));
		}
		return comments;
	}

	//the comments before the declaration of an operation, as the comments within its body are matched by the mappers of the statements they belong to
	private static List<UMLComment> leadingComments(VariableDeclarationContainer container) {
		List<UMLComment> comments = new ArrayList<UMLComment>();
		for(UMLComment comment : container.getComments()) {
			if(comment.getLocationInfo().getEndOffset() <= container.getLocationInfo().getStartOffset()) {
				comments.add(comment);
			}
		}
		return comments;
	}

	private void addLeftovers(List<UMLComment> comments1, List<UMLComment> comments2) {
		if(comments1.isEmpty() && comments2.isEmpty()) {
			return;
		}
		addLeftovers(new UMLCommentListDiff(comments1, comments2));
	}

	private void addLeftovers(UMLCommentListDiff diff) {
		deletedComments.addAll(diff.getDeletedComments());
		addedComments.addAll(diff.getAddedComments());
	}

	private UMLCommentListDiff matchIdenticalComments() {
		UMLCommentListDiff diff = new UMLCommentListDiff();
		List<UMLComment> comments1 = sortedDistinct(deletedComments);
		List<UMLComment> comments2 = sortedDistinct(addedComments);
		//the groups of consecutive comments first, i.e., a header block, then the single comments
		List<List<UMLComment>> groups1 = groups(comments1);
		List<List<UMLComment>> groups2 = groups(comments2);
		for(List<UMLComment> group1 : groups1) {
			if(group1.size() < 2) {
				continue;
			}
			List<List<UMLComment>> candidates = new ArrayList<List<UMLComment>>();
			for(List<UMLComment> group2 : groups2) {
				if(sameText(group1, group2)) {
					candidates.add(group2);
				}
			}
			if(candidates.size() == 1 && groups1.stream().filter(g -> sameText(g, candidates.get(0))).count() == 1) {
				for(int i=0; i<group1.size(); i++) {
					addCommonComment(diff, Pair.of(group1.get(i), candidates.get(0).get(i)));
				}
				comments1.removeAll(group1);
				comments2.removeAll(candidates.get(0));
			}
		}
		for(UMLComment comment1 : new ArrayList<UMLComment>(comments1)) {
			List<UMLComment> candidates = new ArrayList<UMLComment>();
			for(UMLComment comment2 : comments2) {
				if(comment1.getText().equals(comment2.getText())) {
					candidates.add(comment2);
				}
			}
			long sameTextBefore = comments1.stream().filter(c -> c.getText().equals(comment1.getText())).count();
			UMLComment match = null;
			if(candidates.size() == 1 && sameTextBefore == 1) {
				match = candidates.get(0);
			}
			else if(candidates.size() > 1 || sameTextBefore > 1) {
				//an identical comment following the corresponding elements, i.e., a section comment after the same enum constant
				List<UMLComment> anchored = new ArrayList<UMLComment>();
				for(UMLComment candidate : candidates) {
					if(correspondingPreviousElements(comment1, candidate)) {
						anchored.add(candidate);
					}
				}
				if(anchored.size() == 1) {
					match = anchored.get(0);
				}
			}
			if(match != null) {
				addCommonComment(diff, Pair.of(comment1, match));
				comments1.remove(comment1);
				comments2.remove(match);
			}
		}
		return diff;
	}

	private void addCommonComment(UMLCommentListDiff diff, Pair<UMLComment, UMLComment> pair) {
		diff.addCommonComment(pair);
		for(UMLComment comment : List.of(pair.getLeft(), pair.getRight())) {
			Pair<UMLCommentListDiff, Pair<UMLComment, UMLComment>> multiMapping = multiMappings.get(comment);
			if(multiMapping != null) {
				multiMapping.getLeft().removeMultiMapping(multiMapping.getRight());
			}
		}
	}

	private boolean correspondingPreviousElements(UMLComment comment1, UMLComment comment2) {
		if(comment1.getPreviousLocations().isEmpty() || comment2.getPreviousLocations().isEmpty()) {
			return false;
		}
		LocationInfo corresponding = correspondingLocations.get(comment1.getPreviousLocations().get(0));
		return corresponding != null && corresponding.equals(comment2.getPreviousLocations().get(0));
	}

	private static List<UMLComment> sortedDistinct(List<UMLComment> comments) {
		Set<UMLComment> distinct = new LinkedHashSet<UMLComment>(comments);
		List<UMLComment> sorted = new ArrayList<UMLComment>(distinct);
		sorted.sort(Comparator.comparingInt(c -> c.getLocationInfo().getStartOffset()));
		return sorted;
	}

	//the comments on consecutive lines
	private static List<List<UMLComment>> groups(List<UMLComment> sortedComments) {
		List<List<UMLComment>> groups = new ArrayList<List<UMLComment>>();
		List<UMLComment> group = new ArrayList<UMLComment>();
		for(UMLComment comment : sortedComments) {
			if(!group.isEmpty() && group.get(group.size()-1).getLocationInfo().getEndLine() + 1 != comment.getLocationInfo().getStartLine()) {
				groups.add(group);
				group = new ArrayList<UMLComment>();
			}
			group.add(comment);
		}
		if(!group.isEmpty()) {
			groups.add(group);
		}
		return groups;
	}

	private static boolean sameText(List<UMLComment> group1, List<UMLComment> group2) {
		if(group1.size() != group2.size()) {
			return false;
		}
		for(int i=0; i<group1.size(); i++) {
			if(!group1.get(i).getText().equals(group2.get(i).getText())) {
				return false;
			}
		}
		return true;
	}
}
