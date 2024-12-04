package gr.uom.java.xmi.diff;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;

import gr.uom.java.xmi.UMLComment;
import gr.uom.java.xmi.UMLCommentGroup;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.LocationInfo.CodeElementType;

public class UMLCommentListDiff {
	private List<Pair<UMLComment, UMLComment>> commonComments;
	private List<UMLComment> deletedComments;
	private List<UMLComment> addedComments;
	private boolean manyToManyReformat;
	private Set<AbstractCodeMapping> mappings;

	public UMLCommentListDiff(List<UMLComment> commentsBefore, List<UMLComment> commentsAfter, Set<AbstractCodeMapping> mappings) {
		this.mappings = mappings;
		init(commentsBefore, commentsAfter);
	}

	public UMLCommentListDiff(List<UMLComment> commentsBefore, List<UMLComment> commentsAfter) {
		this.mappings = Collections.emptySet();
		init(commentsBefore, commentsAfter);
	}

	private void init(List<UMLComment> commentsBefore, List<UMLComment> commentsAfter) {
		this.commonComments = new ArrayList<Pair<UMLComment,UMLComment>>();
		this.deletedComments = new ArrayList<UMLComment>();
		this.addedComments = new ArrayList<UMLComment>();
		List<UMLComment> deletedComments = new ArrayList<UMLComment>(commentsBefore);
		List<UMLComment> addedComments = new ArrayList<UMLComment>(commentsAfter);
		//check if there exist comment groups, consecutive line comments
		List<UMLCommentGroup> groupsBefore = createCommentGroups(commentsBefore);
		List<UMLCommentGroup> groupsAfter = createCommentGroups(commentsAfter);
		int groupsBeforeSize = groupsBefore.size();
		int groupsAfterSize = groupsAfter.size();
		List<UMLCommentGroup> groupsBeforeToBeRemoved = new ArrayList<UMLCommentGroup>();
		List<UMLCommentGroup> groupsAfterToBeRemoved = new ArrayList<UMLCommentGroup>();
		for(UMLCommentGroup groupBefore : groupsBefore) {
			for(UMLCommentGroup groupAfter : groupsAfter) {
				if(groupBefore.sameText(groupAfter) && !groupsAfterToBeRemoved.contains(groupAfter)) {
					for(int i=0; i<groupBefore.getGroup().size(); i++) {
						UMLComment commentBefore = groupBefore.getGroup().get(i);
						UMLComment commentAfter = groupAfter.getGroup().get(i);
						Pair<UMLComment, UMLComment> pair = Pair.of(commentBefore, commentAfter);
						commonComments.add(pair);
						deletedComments.remove(commentBefore);
						addedComments.remove(commentAfter);
					}
					groupsBeforeToBeRemoved.add(groupBefore);
					groupsAfterToBeRemoved.add(groupAfter);
					break;
				}
			}
		}
		groupsBefore.removeAll(groupsBeforeToBeRemoved);
		groupsAfter.removeAll(groupsAfterToBeRemoved);
		for(UMLCommentGroup groupBefore : groupsBefore) {
			for(UMLCommentGroup groupAfter : groupsAfter) {
				if(groupBefore.subGroupSameText(groupAfter) && !groupsAfterToBeRemoved.contains(groupAfter)) {
					for(int i=0; i<groupBefore.getGroup().size(); i++) {
						for(int j=0; j<groupAfter.getGroup().size(); j++) {
							UMLComment commentBefore = groupBefore.getGroup().get(i);
							UMLComment commentAfter = groupAfter.getGroup().get(j);
							if(commentBefore.getText().equals(commentAfter.getText())) {
								Pair<UMLComment, UMLComment> pair = Pair.of(commentBefore, commentAfter);
								commonComments.add(pair);
								deletedComments.remove(commentBefore);
								addedComments.remove(commentAfter);
								break;
							}
						}
					}
					groupsBeforeToBeRemoved.add(groupBefore);
					groupsAfterToBeRemoved.add(groupAfter);
					break;
				}
			}
		}
		groupsBefore.removeAll(groupsBeforeToBeRemoved);
		groupsAfter.removeAll(groupsAfterToBeRemoved);
		for(UMLCommentGroup groupBefore : groupsBefore) {
			for(UMLCommentGroup groupAfter : groupsAfter) {
				if(groupBefore.modifiedMatchingText(groupAfter) && !groupsAfterToBeRemoved.contains(groupAfter)) {
					for(int i=0; i<groupBefore.getGroup().size(); i++) {
						UMLComment commentBefore = groupBefore.getGroup().get(i);
						UMLComment commentAfter = groupAfter.getGroup().get(i);
						Pair<UMLComment, UMLComment> pair = Pair.of(commentBefore, commentAfter);
						commonComments.add(pair);
						deletedComments.remove(commentBefore);
						addedComments.remove(commentAfter);
					}
					groupsBeforeToBeRemoved.add(groupBefore);
					groupsAfterToBeRemoved.add(groupAfter);
					break;
				}
			}
		}
		groupsBefore.removeAll(groupsBeforeToBeRemoved);
		groupsAfter.removeAll(groupsAfterToBeRemoved);
		if(!(allRemainingCommentsBelongToGroups(deletedComments, groupsBefore) && allRemainingCommentsBelongToGroups(addedComments, groupsAfter)) ||
				(groupsBeforeSize <= 1 && groupsAfterSize <= 1)) {
			processRemainingComments(deletedComments, addedComments);
		}
		else {
			this.deletedComments.addAll(deletedComments);
			this.addedComments.addAll(addedComments);
		}
	}

	private boolean allRemainingCommentsBelongToGroups(List<UMLComment> comments, List<UMLCommentGroup> groups) {
		int matches = 0;
		for(UMLComment comment : comments) {
			for(UMLCommentGroup group : groups) {
				if(group.getGroup().contains(comment) && group.getGroup().size() > 1) {
					matches++;
					break;
				}
			}
		}
		return matches == comments.size();
	}

	public UMLCommentListDiff(UMLCommentGroup groupBefore, UMLCommentGroup groupAfter) {
		this.commonComments = new ArrayList<Pair<UMLComment,UMLComment>>();
		this.deletedComments = new ArrayList<UMLComment>();
		this.addedComments = new ArrayList<UMLComment>();
		this.mappings = Collections.emptySet();
		processRemainingComments(groupBefore.getGroup(), groupAfter.getGroup());
	}

	private boolean mappedParent(UMLComment left, UMLComment right) {
		if(left.getParent() != null && right.getParent() != null) {
			if(left.getParent().getParent() == null && right.getParent().getParent() == null) {
				return true;
			}
			for(AbstractCodeMapping mapping : mappings) {
				if(mapping.getFragment1().equals(left.getParent()) &&
						mapping.getFragment2().equals(right.getParent())) {
					return true;
				}
			}
		}
		return false;
	}

	private void processRemainingComments(List<UMLComment> commentsBefore, List<UMLComment> commentsAfter) {
		List<UMLComment> deletedComments = new ArrayList<UMLComment>(commentsBefore);
		List<UMLComment> addedComments = new ArrayList<UMLComment>(commentsAfter);
		if(commentsBefore.size() <= commentsAfter.size()) {
			for(UMLComment comment : commentsBefore) {
				List<Integer> matchingIndices = findAllMatchingIndices(commentsAfter, comment);
				List<Boolean> mappedParent = new ArrayList<Boolean>();
				if(matchingIndices.size() > 1) {
					for(Integer index : matchingIndices) {
						mappedParent.add(mappedParent(comment, commentsAfter.get(index)));
					}
				}
				int i = -1;
				for(Integer index : matchingIndices) {
					i++;
					if(!alreadyMatchedComment(comment, commentsAfter.get(index))) {
						if(mappedParent.contains(true) && mappedParent.get(i) == false) {
							continue;
						}
						Pair<UMLComment, UMLComment> pair = Pair.of(comment, commentsAfter.get(index));
						commonComments.add(pair);
						deletedComments.remove(comment);
						addedComments.remove(commentsAfter.get(index));
						break;
					}
				}
			}
		}
		else {
			for(UMLComment comment : commentsAfter) {
				List<Integer> matchingIndices = findAllMatchingIndices(commentsBefore, comment);
				List<Boolean> mappedParent = new ArrayList<Boolean>();
				if(matchingIndices.size() > 1) {
					for(Integer index : matchingIndices) {
						mappedParent.add(mappedParent(commentsBefore.get(index), comment));
					}
				}
				int i = -1;
				for(Integer index : matchingIndices) {
					i++;
					if(!alreadyMatchedComment(commentsBefore.get(index), comment)) {
						if(mappedParent.contains(true) && mappedParent.get(i) == false) {
							continue;
						}
						Pair<UMLComment, UMLComment> pair = Pair.of(commentsBefore.get(index), comment);
						commonComments.add(pair);
						deletedComments.remove(commentsBefore.get(index));
						addedComments.remove(comment);
						break;
					}
				}
			}
		}
		if(addedComments.isEmpty()) {
			//check potential multi-mappings
			for(UMLComment deletedComment : new ArrayList<>(deletedComments)) {
				for(Pair<UMLComment, UMLComment> pair : commonComments) {
					if(pair.getLeft().getText().equals(deletedComment.getText()) &&
							pair.getRight().getText().equals(deletedComment.getText()) &&
							!deletedComment.isCommentedCode() && !deletedComment.nestedInCatchBlock()) {
						Pair<UMLComment, UMLComment> newPair = Pair.of(deletedComment, pair.getRight());
						commonComments.add(newPair);
						deletedComments.remove(deletedComment);
						break;
					}
				}
			}
		}
		if(deletedComments.isEmpty()) {
			//check potential multi-mappings
			for(UMLComment addedComment : new ArrayList<>(addedComments)) {
				for(Pair<UMLComment, UMLComment> pair : commonComments) {
					if(pair.getLeft().getText().equals(addedComment.getText()) &&
							pair.getRight().getText().equals(addedComment.getText()) &&
							!addedComment.isCommentedCode() && !addedComment.nestedInCatchBlock()) {
						Pair<UMLComment, UMLComment> newPair = Pair.of(pair.getLeft(), addedComment);
						commonComments.add(newPair);
						addedComments.remove(addedComment);
						break;
					}
				}
			}
		}
		processModifiedComments(deletedComments, addedComments);
	}

	private List<UMLCommentGroup> createCommentGroups(List<UMLComment> commentsBefore) {
		List<UMLCommentGroup> groups = new ArrayList<UMLCommentGroup>();
		UMLCommentGroup currentGroup = new UMLCommentGroup();
		for(int i=0; i<commentsBefore.size(); i++) {
			UMLComment current = commentsBefore.get(i);
			if(current.getLocationInfo().getCodeElementType().equals(CodeElementType.LINE_COMMENT)) {
				if(i-1 >= 0) {
					UMLComment previous = commentsBefore.get(i-1);
					if(previous.getLocationInfo().getCodeElementType().equals(CodeElementType.LINE_COMMENT)) {
						if(previous.getLocationInfo().getStartLine() + 1 == current.getLocationInfo().getStartLine()) {
							//consecutive line comments
							currentGroup.addComment(current);
						}
						else {
							//make new group
							if (currentGroup.getGroup().size() > 0) {
								groups.add(new UMLCommentGroup(currentGroup.getGroup()));
							}
							currentGroup = new UMLCommentGroup();
							currentGroup.addComment(current);
						}
					}
					else {
						//make new group
						if (currentGroup.getGroup().size() > 0) {
							groups.add(new UMLCommentGroup(currentGroup.getGroup()));
						}
						currentGroup = new UMLCommentGroup();
						currentGroup.addComment(current);
					}
				}
				else {
					//this is the first line comment
					currentGroup.addComment(current);
				}
			}
			else if (currentGroup.getGroup().size() > 0) {
				//there is a comment of different type
				groups.add(new UMLCommentGroup(currentGroup.getGroup()));
				currentGroup = new UMLCommentGroup();
			}
		}
		if(!currentGroup.getGroup().isEmpty())
			groups.add(currentGroup);
		return groups;
	}

	private List<Integer> findAllMatchingIndices(List<UMLComment> fragments, UMLComment comment) {
		List<Integer> matchingIndices = new ArrayList<>();
		for(int i=0; i<fragments.size(); i++) {
			UMLComment element = fragments.get(i);
			if(comment.getText().equals(element.getText())) {
				matchingIndices.add(i);
			}
		}
		return matchingIndices;
	}

	private void processModifiedComments(List<UMLComment> deletedComments, List<UMLComment> addedComments) {
		if(deletedComments.isEmpty() || addedComments.isEmpty()) {
			this.deletedComments.addAll(deletedComments);
			this.addedComments.addAll(addedComments);
			return;
		}
		List<UMLCommentGroup> groupsBefore = createCommentGroups(deletedComments);
		List<UMLCommentGroup> groupsAfter = createCommentGroups(addedComments);
		if(groupsBefore.size() <= groupsAfter.size()) {
			for(UMLCommentGroup groupBefore : groupsBefore) {
				for(UMLCommentGroup groupAfter : groupsAfter) {
					processModifiedComments(groupBefore, groupAfter);
				}
			}
		}
		else {
			for(UMLCommentGroup groupAfter : groupsAfter) {
				for(UMLCommentGroup groupBefore : groupsBefore) {
					processModifiedComments(groupBefore, groupAfter);
				}
			}
		}
		if(groupsBefore.isEmpty() && groupsAfter.isEmpty()) {
			UMLCommentGroup groupBefore = new UMLCommentGroup();
			for(UMLComment comment : deletedComments)
				groupBefore.addComment(comment);
			UMLCommentGroup groupAfter = new UMLCommentGroup();
			for(UMLComment comment : addedComments)
				groupAfter.addComment(comment);
			processModifiedComments(groupBefore, groupAfter);
			this.deletedComments.addAll(groupBefore.getGroup());
			this.addedComments.addAll(groupAfter.getGroup());
		}
		for(UMLCommentGroup group : groupsBefore) {
			this.deletedComments.addAll(group.getGroup());
		}
		for(UMLCommentGroup group : groupsAfter) {
			this.addedComments.addAll(group.getGroup());
		}
	}

	private void processModifiedComments(UMLCommentGroup groupBefore, UMLCommentGroup groupAfter) {
		List<UMLComment> deletedComments = groupBefore.getGroup();
		List<UMLComment> addedComments = groupAfter.getGroup();
		//match comments differing only in opening/closing quotes
		if(deletedComments.size() <= addedComments.size()) {
			for(UMLComment deletedComment : new ArrayList<>(deletedComments)) {
				for(UMLComment addedComment : new ArrayList<>(addedComments)) {
					String trimmed1 = deletedComment.getText().replaceAll("^\"|\"$", "");
					String trimmed2 = addedComment.getText().replaceAll("^\"|\"$", "");
					if(trimmed1.equals(trimmed2)) {
						Pair<UMLComment, UMLComment> pair = Pair.of(deletedComment, addedComment);
						commonComments.add(pair);
						deletedComments.remove(deletedComment);
						addedComments.remove(addedComment);
					}
				}
			}
		}
		else {
			for(UMLComment addedComment : new ArrayList<>(addedComments)) {
				for(UMLComment deletedComment : new ArrayList<>(deletedComments)) {
					String trimmed1 = deletedComment.getText().replaceAll("^\"|\"$", "");
					String trimmed2 = addedComment.getText().replaceAll("^\"|\"$", "");
					if(trimmed1.equals(trimmed2)) {
						Pair<UMLComment, UMLComment> pair = Pair.of(deletedComment, addedComment);
						commonComments.add(pair);
						deletedComments.remove(deletedComment);
						addedComments.remove(addedComment);
					}
				}
			}
		}
		List<UMLComment> deletedToBeDeleted = new ArrayList<UMLComment>();
		List<UMLComment> addedToBeDeleted = new ArrayList<UMLComment>();
		if(deletedComments.size() == addedComments.size()) {
			for(int i=0; i<deletedComments.size(); i++) {
				UMLComment deletedComment = deletedComments.get(i);
				UMLComment addedComment = addedComments.get(i);
				if(deletedComment.getText().replaceAll("\\s", "").equals(addedComment.getText().replaceAll("\\s", ""))) {
					Pair<UMLComment, UMLComment> pair = Pair.of(deletedComment, addedComment);
					commonComments.add(pair);
					deletedToBeDeleted.add(deletedComment);
					addedToBeDeleted.add(addedComment);
				}
			}
			deletedComments.removeAll(deletedToBeDeleted);
			addedComments.removeAll(addedToBeDeleted);
		}
		//check if all deleted comments match all added comments
		StringBuilder deletedSB = new StringBuilder();
		List<String> deletedTokenSequence = new ArrayList<String>();
		Map<UMLComment, List<String>> deletedTokenSequenceMap = new LinkedHashMap<>();
		for(UMLComment deletedComment : deletedComments) {
			String text = deletedComment.getText();
			deletedSB.append(text);
			List<String> splitToWords = splitToWords(text);
			deletedTokenSequence.addAll(splitToWords);
			deletedTokenSequenceMap.put(deletedComment, splitToWords);
		}
		StringBuilder addedSB = new StringBuilder();
		List<String> addedTokenSequence = new ArrayList<String>();
		Map<UMLComment, List<String>> addedTokenSequenceMap = new LinkedHashMap<>();
		for(UMLComment addedComment : addedComments) {
			String text = addedComment.getText();
			addedSB.append(text);
			List<String> splitToWords = splitToWords(text);
			addedTokenSequence.addAll(splitToWords);
			addedTokenSequenceMap.put(addedComment, splitToWords);
		}
		if(deletedSB.toString().replaceAll("\\s", "").equals(addedSB.toString().replaceAll("\\s", ""))) {
			//make all pair combinations
			for(UMLComment deletedComment : deletedComments) {
				for(UMLComment addedComment : addedComments) {
					Pair<UMLComment, UMLComment> pair = Pair.of(deletedComment, addedComment);
					commonComments.add(pair);
				}
			}
			if(deletedComments.size() >= 1 && addedComments.size() >= 1) {
				manyToManyReformat = true;
			}
		}
		else {
			//match comments that one contains a subsequence of the other
			if(deletedTokenSequence.size() <= addedTokenSequence.size()) {
				List<String> longestSubSequence = null;
				for(int i=0; i<deletedTokenSequence.size(); i++) {
					for(int j=i+1; j<deletedTokenSequence.size(); j++) {
						List<String> subList = deletedTokenSequence.subList(i,j+1);
						if(subList.size() > 2) {
							int indexOfSubList = Collections.indexOfSubList(addedTokenSequence, subList);
							if(indexOfSubList != -1) {
								if(longestSubSequence == null) {
									longestSubSequence = subList;
								}
								else if(subList.containsAll(longestSubSequence) && subList.size() > longestSubSequence.size()) {
									longestSubSequence = subList;
								}
							}
						}
					}
					if(longestSubSequence != null && longestSubSequence.equals(deletedTokenSequence)) {
						break;
					}
				}
				if(longestSubSequence != null && nonPunctuationWords(longestSubSequence) > 1) {
					//make all pair combinations
					boolean entireSubSequenceMatched = false;
					for(UMLComment deletedComment : deletedComments) {
						List<String> containsAnySubSequenceInDeleted = containsAnySubSequence(deletedTokenSequenceMap.get(deletedComment), longestSubSequence);
						if(containsAnySubSequenceInDeleted != null) {
							for(UMLComment addedComment : addedComments) {
								List<String> containsAnySubSequenceInAdded = containsAnySubSequence(addedTokenSequenceMap.get(addedComment), longestSubSequence);
								if(containsAnySubSequenceInAdded != null) {
									if(!alreadyMatchedComment(deletedComment, addedComment)) {
										if(entireSubSequenceMatched && (!containsAnySubSequenceInDeleted.equals(longestSubSequence) || !containsAnySubSequenceInAdded.equals(longestSubSequence))) {
											continue;
										}
										Pair<UMLComment, UMLComment> pair = Pair.of(deletedComment, addedComment);
										commonComments.add(pair);
										deletedToBeDeleted.add(deletedComment);
										addedToBeDeleted.add(addedComment);
										if(containsAnySubSequenceInAdded.equals(longestSubSequence) && containsAnySubSequenceInDeleted.equals(longestSubSequence)) {
											entireSubSequenceMatched = true;
										}
									}
								}
							}
						}
					}
					if(deletedComments.size() >= 1 && addedComments.size() >= 1) {
						manyToManyReformat = true;
					}
					deletedComments.removeAll(deletedToBeDeleted);
					addedComments.removeAll(addedToBeDeleted);
				}
			}
			else {
				List<String> longestSubSequence = null;
				for(int i=0; i<addedTokenSequence.size(); i++) {
					for(int j=i+1; j<addedTokenSequence.size(); j++) {
						List<String> subList = addedTokenSequence.subList(i,j+1);
						if(subList.size() > 2) {
							int indexOfSubList = Collections.indexOfSubList(deletedTokenSequence, subList);
							if(indexOfSubList != -1) {
								if(longestSubSequence == null) {
									longestSubSequence = subList;
								}
								else if(subList.containsAll(longestSubSequence) && subList.size() > longestSubSequence.size()) {
									longestSubSequence = subList;
								}
							}
						}
					}
					if(longestSubSequence != null && longestSubSequence.equals(addedTokenSequence)) {
						break;
					}
				}
				if(longestSubSequence != null && nonPunctuationWords(longestSubSequence) > 1) {
					//make all pair combinations
					boolean entireSubSequenceMatched = false;
					for(UMLComment deletedComment : deletedComments) {
						List<String> containsAnySubSequenceInDeleted = containsAnySubSequence(deletedTokenSequenceMap.get(deletedComment), longestSubSequence);
						if(containsAnySubSequenceInDeleted != null) {
							for(UMLComment addedComment : addedComments) {
								List<String> containsAnySubSequenceInAdded = containsAnySubSequence(addedTokenSequenceMap.get(addedComment), longestSubSequence);
								if(containsAnySubSequenceInAdded != null) {
									if(!alreadyMatchedComment(deletedComment, addedComment) ||
											(longestSubSequence.containsAll(deletedTokenSequenceMap.get(deletedComment)) &&
											deletedTokenSequenceMap.get(deletedComment).size() > 1)) {
										if(entireSubSequenceMatched && (!containsAnySubSequenceInDeleted.equals(longestSubSequence) || !containsAnySubSequenceInAdded.equals(longestSubSequence))) {
											continue;
										}
										Pair<UMLComment, UMLComment> pair = Pair.of(deletedComment, addedComment);
										commonComments.add(pair);
										deletedToBeDeleted.add(deletedComment);
										addedToBeDeleted.add(addedComment);
										if(containsAnySubSequenceInAdded.equals(longestSubSequence) && containsAnySubSequenceInDeleted.equals(longestSubSequence)) {
											entireSubSequenceMatched = true;
										}
									}
								}
							}
						}
					}
					if(deletedComments.size() >= 1 && addedComments.size() >= 1) {
						manyToManyReformat = true;
					}
					deletedComments.removeAll(deletedToBeDeleted);
					addedComments.removeAll(addedToBeDeleted);
				}
			}
		}
		if(deletedComments.size() > addedComments.size()) {
			for(UMLComment addedComment : addedComments) {
				String text = addedComment.getText();
				for(int i=0; i<deletedComments.size()-1; i++) {
					List<UMLComment> matches = findConcatenatedMatch(deletedComments, text, i);
					if(matches.size() > 0) {
						for(UMLComment match : matches) {
							Pair<UMLComment, UMLComment> pair = Pair.of(match, addedComment);
							commonComments.add(pair);
							deletedToBeDeleted.add(match);
						}
						addedToBeDeleted.add(addedComment);
						break;
					}
				}
			}
		}
		else {
			for(UMLComment deletedComment : deletedComments) {
				String text = deletedComment.getText();
				for(int i=0; i<addedComments.size()-1; i++) {
					List<UMLComment> matches = findConcatenatedMatch(addedComments, text, i);
					if(matches.size() > 0) {
						for(UMLComment match : matches) {
							Pair<UMLComment, UMLComment> pair = Pair.of(deletedComment, match);
							commonComments.add(pair);
							addedToBeDeleted.add(match);
						}
						deletedToBeDeleted.add(deletedComment);
						break;
					}
				}
			}
		}
		deletedComments.removeAll(deletedToBeDeleted);
		addedComments.removeAll(addedToBeDeleted);
		//match comments that one contains the other
		if(deletedComments.size() <= addedComments.size()) {
			for(UMLComment deletedComment : new ArrayList<>(deletedComments)) {
				if(deletedComment.getText().length() > 2) {
					for(UMLComment addedComment : new ArrayList<>(addedComments)) {
						if(addedComment.getText().length() > 2) {
							if(deletedComment.getText().contains(addedComment.getText()) || addedComment.getText().contains(deletedComment.getText())) {
								Pair<UMLComment, UMLComment> pair = Pair.of(deletedComment, addedComment);
								commonComments.add(pair);
								deletedComments.remove(deletedComment);
								addedComments.remove(addedComment);
							}
						}
					}
				}
			}
		}
		else {
			for(UMLComment addedComment : new ArrayList<>(addedComments)) {
				if(addedComment.getText().length() > 2) {
					for(UMLComment deletedComment : new ArrayList<>(deletedComments)) {
						if(deletedComment.getText().length() > 2) {
							if(deletedComment.getText().contains(addedComment.getText()) || addedComment.getText().contains(deletedComment.getText())) {
								Pair<UMLComment, UMLComment> pair = Pair.of(deletedComment, addedComment);
								commonComments.add(pair);
								deletedComments.remove(deletedComment);
								addedComments.remove(addedComment);
							}
						}
					}
				}
			}
		}
	}

	private int nonPunctuationWords(List<String> longestSubSequence) {
		int count = 0;
		for(String str : longestSubSequence) {
			if(!Pattern.matches("\\p{Punct}", str)) {
				count++;
			}
		}
		return count;
	}

	private boolean alreadyMatchedComment(UMLComment deletedComment, UMLComment addedComment) {
		for(Pair<UMLComment, UMLComment> pair : commonComments) {
			if(pair.getLeft() == deletedComment) {
				if(pair.getLeft().getText().contains(pair.getRight().getText()) || pair.getRight().getText().contains(pair.getLeft().getText()))
					return true;
			}
			if(pair.getRight() == addedComment) {
				if(pair.getLeft().getText().contains(pair.getRight().getText()) || pair.getRight().getText().contains(pair.getLeft().getText()))
					return true;
			}
		}
		return false;
	}

	private List<UMLComment> findConcatenatedMatch(List<UMLComment> comments, String text, int startIndex) {
		StringBuilder concatText = new StringBuilder();
		for(int i=startIndex; i<comments.size(); i++) {
			concatText.append(comments.get(i).getText());
			if(concatText.toString().replaceAll("\\s", "").equals(text.replaceAll("\\s", ""))) {
				return new ArrayList<>(comments.subList(startIndex, i+1));
			}
		}
		return Collections.emptyList();
	}

	private List<String> containsAnySubSequence(List<String> list, List<String> longestSubSequence) {
		if(list.size() > 1 && Collections.indexOfSubList(longestSubSequence, list) != -1)
			return longestSubSequence;
		for(int i=longestSubSequence.size(); i>1; i--) {
			List<String> subList = longestSubSequence.subList(0,i);
			int index = Collections.indexOfSubList(list, subList);
			if(index != -1) {
				return subList;
			}
		}
		for(int i=0; i<longestSubSequence.size(); i++) {
			List<String> subList = longestSubSequence.subList(i,longestSubSequence.size());
			int index = Collections.indexOfSubList(list, subList);
			if(index != -1) {
				return subList;
			}
		}
		return null;
	}

	private List<String> splitToWords(String sentence) {
		ArrayList<String> words = new ArrayList<String>();
		BreakIterator boundary = BreakIterator.getWordInstance();
		boundary.setText(sentence);
		int start = boundary.first();
		for (int end = boundary.next();
				end != BreakIterator.DONE;
				start = end, end = boundary.next()) {
			String word = sentence.substring(start,end);
			if(!word.isBlank())
				words.add(word);
		}
		return words;
	}

	public List<Pair<UMLComment, UMLComment>> getCommonComments() {
		return commonComments;
	}

	public List<UMLComment> getDeletedComments() {
		return deletedComments;
	}

	public List<UMLComment> getAddedComments() {
		return addedComments;
	}

	public boolean isManyToManyReformat() {
		return manyToManyReformat;
	}
}
