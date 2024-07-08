package gr.uom.java.xmi.diff;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import gr.uom.java.xmi.UMLComment;

public class UMLCommentListDiff {
	private List<Pair<UMLComment, UMLComment>> commonComments;
	private List<UMLComment> deletedComments;
	private List<UMLComment> addedComments;
	private boolean manyToManyReformat;

	public UMLCommentListDiff(List<UMLComment> commentsBefore, List<UMLComment> commentsAfter) {
		this.commonComments = new ArrayList<Pair<UMLComment,UMLComment>>();
		this.deletedComments = new ArrayList<UMLComment>();
		this.addedComments = new ArrayList<UMLComment>();
		List<UMLComment> deletedComments = new ArrayList<UMLComment>(commentsBefore);
		List<UMLComment> addedComments = new ArrayList<UMLComment>(commentsAfter);
		if(commentsBefore.size() <= commentsAfter.size()) {
			for(UMLComment comment : commentsBefore) {
				List<Integer> matchingIndices = findAllMatchingIndices(commentsAfter, comment);
				for(Integer index : matchingIndices) {
					if(!alreadyMatchedComment(comment, commentsAfter.get(index))) {
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
				for(Integer index : matchingIndices) {
					if(!alreadyMatchedComment(commentsBefore.get(index), comment)) {
						Pair<UMLComment, UMLComment> pair = Pair.of(commentsBefore.get(index), comment);
						commonComments.add(pair);
						deletedComments.remove(commentsBefore.get(index));
						addedComments.remove(comment);
						break;
					}
				}
			}
		}
		processModifiedComments(deletedComments, addedComments);
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
				if(longestSubSequence != null) {
					//make all pair combinations
					for(UMLComment deletedComment : deletedComments) {
						if(containsAnySubSequence(deletedTokenSequenceMap.get(deletedComment), longestSubSequence)) {
							for(UMLComment addedComment : addedComments) {
								if(containsAnySubSequence(addedTokenSequenceMap.get(addedComment), longestSubSequence)) {
									if(!alreadyMatchedComment(deletedComment, addedComment)) {
										Pair<UMLComment, UMLComment> pair = Pair.of(deletedComment, addedComment);
										commonComments.add(pair);
										deletedToBeDeleted.add(deletedComment);
										addedToBeDeleted.add(addedComment);
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
				if(longestSubSequence != null) {
					//make all pair combinations
					for(UMLComment deletedComment : deletedComments) {
						if(containsAnySubSequence(deletedTokenSequenceMap.get(deletedComment), longestSubSequence)) {
							for(UMLComment addedComment : addedComments) {
								if(containsAnySubSequence(addedTokenSequenceMap.get(addedComment), longestSubSequence)) {
									if(!alreadyMatchedComment(deletedComment, addedComment) ||
											(longestSubSequence.containsAll(deletedTokenSequenceMap.get(deletedComment)) &&
											deletedTokenSequenceMap.get(deletedComment).size() > 1)) {
										Pair<UMLComment, UMLComment> pair = Pair.of(deletedComment, addedComment);
										commonComments.add(pair);
										deletedToBeDeleted.add(deletedComment);
										addedToBeDeleted.add(addedComment);
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
		this.deletedComments.addAll(deletedComments);
		this.addedComments.addAll(addedComments);
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

	private boolean containsAnySubSequence(List<String> list, List<String> longestSubSequence) {
		if(list.size() > 1 && Collections.indexOfSubList(longestSubSequence, list) != -1)
			return true;
		for(int i=longestSubSequence.size(); i>1; i--) {
			List<String> subList = longestSubSequence.subList(0,i);
			int index = Collections.indexOfSubList(list, subList);
			if(index != -1) {
				return true;
			}
		}
		for(int i=0; i<longestSubSequence.size(); i++) {
			List<String> subList = longestSubSequence.subList(i,longestSubSequence.size());
			int index = Collections.indexOfSubList(list, subList);
			if(index != -1) {
				return true;
			}
		}
		return false;
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
