package gr.uom.java.xmi;

import java.util.ArrayList;
import java.util.List;

import gr.uom.java.xmi.diff.UMLCommentListDiff;

public class UMLCommentGroup {
	private List<UMLComment> group;

	public UMLCommentGroup() {
		this.group = new ArrayList<UMLComment>();
	}

	public UMLCommentGroup(List<UMLComment> group) {
		this.group = group;
	}

	public void addComment(UMLComment c) {
		group.add(c);
	}

	public List<UMLComment> getGroup() {
		return group;
	}

	public boolean sameText(UMLCommentGroup other) {
		if(this.group.size() == other.group.size() && this.group.size() > 1) {
			int matches = 0;
			for(int i=0; i<this.group.size(); i++) {
				if(this.group.get(i).getText().equals(other.group.get(i).getText())) {
					matches++;
				}
			}
			return matches == this.group.size();
		}
		return false;
	}

	public boolean subGroupSameText(UMLCommentGroup other) {
		if(this.group.size() < other.group.size() && this.group.size() > 1) {
			int matches = 0;
			for(int i=0; i<this.group.size(); i++) {
				for(int j=0; j<other.group.size(); j++) {
					if(this.group.get(i).getText().equals(other.group.get(j).getText())) {
						matches++;
						break;
					}
				}
			}
			return matches == this.group.size();
		}
		else if(other.group.size() < this.group.size() && other.group.size() > 1) {
			int matches = 0;
			for(int j=0; j<other.group.size(); j++) {
				for(int i=0; i<this.group.size(); i++) {
					if(this.group.get(i).getText().equals(other.group.get(j).getText())) {
						matches++;
						break;
					}
				}
			}
			return matches == other.group.size();
		}
		return false;
	}

	public boolean modifiedMatchingText(UMLCommentGroup other) {
		if(this.group.size() == other.group.size() && this.group.size() > 1) {
			UMLCommentListDiff diff = new UMLCommentListDiff(this, other);
			if(diff.getCommonComments().size() == this.group.size()) {
				return true;
			}
		}
		return false;
	}
}
