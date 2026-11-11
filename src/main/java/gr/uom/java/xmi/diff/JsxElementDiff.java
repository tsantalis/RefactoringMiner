package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import gr.uom.java.xmi.decomposition.JsxElement;
import gr.uom.java.xmi.decomposition.JsxElement.JsxAttribute;
import gr.uom.java.xmi.decomposition.JsxElement.JsxChild;

/**
 * The difference between two JSX elements: the attributes are paired by name (common, changed value, removed, added),
 * and the children in order, i.e., the identical children in their longest common subsequence,
 * and between them the nested elements with the same tag (compared recursively) and the other children by position.
 */
public class JsxElementDiff {
	private final JsxElement element1;
	private final JsxElement element2;
	private final List<JsxAttribute> commonAttributes = new ArrayList<>();
	private final List<Pair<JsxAttribute, JsxAttribute>> changedAttributes = new ArrayList<>();
	private final List<JsxAttribute> removedAttributes = new ArrayList<>();
	private final List<JsxAttribute> addedAttributes = new ArrayList<>();
	private final List<Pair<JsxChild, JsxChild>> commonChildren = new ArrayList<>();
	private final List<Pair<JsxChild, JsxChild>> changedChildren = new ArrayList<>();
	private final List<JsxElementDiff> childElementDiffs = new ArrayList<>();
	private final List<JsxChild> removedChildren = new ArrayList<>();
	private final List<JsxChild> addedChildren = new ArrayList<>();

	public JsxElementDiff(JsxElement element1, JsxElement element2) {
		this.element1 = element1;
		this.element2 = element2;
		diffAttributes();
		diffChildren();
	}

	private void diffAttributes() {
		Map<String, JsxAttribute> attributes2 = new LinkedHashMap<>();
		for(JsxAttribute attribute2 : element2.getAttributes()) {
			attributes2.putIfAbsent(key(attribute2), attribute2);
		}
		for(JsxAttribute attribute1 : element1.getAttributes()) {
			JsxAttribute attribute2 = attributes2.remove(key(attribute1));
			if(attribute2 == null)
				removedAttributes.add(attribute1);
			else if(attribute1.equals(attribute2))
				commonAttributes.add(attribute1);
			else
				changedAttributes.add(Pair.of(attribute1, attribute2));
		}
		addedAttributes.addAll(attributes2.values());
	}

	//the attributes are paired by name, and the spread attributes by their expression
	private static String key(JsxAttribute attribute) {
		return attribute.spread() ? "{..." + attribute.name() + "}" : attribute.name();
	}

	private void diffChildren() {
		List<JsxChild> children1 = element1.getChildren();
		List<JsxChild> children2 = element2.getChildren();
		int[][] lcs = new int[children1.size() + 1][children2.size() + 1];
		for(int i = children1.size() - 1; i >= 0; i--) {
			for(int j = children2.size() - 1; j >= 0; j--) {
				lcs[i][j] = children1.get(i).string().equals(children2.get(j).string()) ? lcs[i + 1][j + 1] + 1 : Math.max(lcs[i + 1][j], lcs[i][j + 1]);
			}
		}
		int i = 0, j = 0, gapStart1 = 0, gapStart2 = 0;
		while(i < children1.size() && j < children2.size()) {
			if(children1.get(i).string().equals(children2.get(j).string())) {
				diffGap(children1.subList(gapStart1, i), children2.subList(gapStart2, j));
				commonChildren.add(Pair.of(children1.get(i), children2.get(j)));
				i++;
				j++;
				gapStart1 = i;
				gapStart2 = j;
			}
			else if(lcs[i + 1][j] >= lcs[i][j + 1]) {
				i++;
			}
			else {
				j++;
			}
		}
		diffGap(children1.subList(gapStart1, children1.size()), children2.subList(gapStart2, children2.size()));
	}

	//the changed children between two identical ones: the nested elements with the same tag in order, then the other children by position when equally many
	private void diffGap(List<JsxChild> gap1, List<JsxChild> gap2) {
		List<JsxChild> remaining1 = new ArrayList<>(gap1);
		List<JsxChild> remaining2 = new ArrayList<>(gap2);
		int start2 = 0;
		for(JsxChild child1 : gap1) {
			if(!child1.isElement())
				continue;
			for(int k = start2; k < remaining2.size(); k++) {
				JsxChild child2 = remaining2.get(k);
				if(child2.isElement() && child1.element().getTag().equals(child2.element().getTag())) {
					changedChildren.add(Pair.of(child1, child2));
					childElementDiffs.add(new JsxElementDiff(child1.element(), child2.element()));
					remaining1.removeIf(c -> c == child1);
					remaining2.remove(k);
					start2 = k;
					break;
				}
			}
		}
		List<JsxChild> others1 = remaining1.stream().filter(c -> !c.isElement()).toList();
		List<JsxChild> others2 = remaining2.stream().filter(c -> !c.isElement()).toList();
		if(others1.size() == others2.size()) {
			for(int k = 0; k < others1.size(); k++) {
				changedChildren.add(Pair.of(others1.get(k), others2.get(k)));
			}
			remaining1.removeIf(c -> others1.stream().anyMatch(o -> o == c));
			remaining2.removeIf(c -> others2.stream().anyMatch(o -> o == c));
		}
		removedChildren.addAll(remaining1);
		addedChildren.addAll(remaining2);
	}

	//the elements are counterparts when they have the same tag, or a renamed tag with at least half of their attributes and children in common
	public boolean isCounterpart() {
		if(element1.getTag().equals(element2.getTag()))
			return true;
		int common = commonAttributes.size() + commonChildren.size();
		int parts = Math.max(element1.getAttributes().size() + element1.getChildren().size(), element2.getAttributes().size() + element2.getChildren().size());
		return common > 0 && 2*common >= parts;
	}

	public boolean isTagChanged() {
		return !element1.getTag().equals(element2.getTag());
	}

	public boolean isEmpty() {
		return !isTagChanged() && changedAttributes.isEmpty() && removedAttributes.isEmpty() && addedAttributes.isEmpty() &&
				changedChildren.isEmpty() && removedChildren.isEmpty() && addedChildren.isEmpty();
	}

	public JsxElement getElement1() {
		return element1;
	}

	public JsxElement getElement2() {
		return element2;
	}

	public List<JsxAttribute> getCommonAttributes() {
		return commonAttributes;
	}

	public List<Pair<JsxAttribute, JsxAttribute>> getChangedAttributes() {
		return changedAttributes;
	}

	public List<JsxAttribute> getRemovedAttributes() {
		return removedAttributes;
	}

	public List<JsxAttribute> getAddedAttributes() {
		return addedAttributes;
	}

	public List<Pair<JsxChild, JsxChild>> getCommonChildren() {
		return commonChildren;
	}

	public List<Pair<JsxChild, JsxChild>> getChangedChildren() {
		return changedChildren;
	}

	public List<JsxElementDiff> getChildElementDiffs() {
		return childElementDiffs;
	}

	public List<JsxChild> getRemovedChildren() {
		return removedChildren;
	}

	public List<JsxChild> getAddedChildren() {
		return addedChildren;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("<").append(element1.getTag()).append(">");
		if(isTagChanged())
			sb.append(" -> <").append(element2.getTag()).append(">");
		for(Pair<JsxAttribute, JsxAttribute> pair : changedAttributes)
			sb.append("\n  changed attribute ").append(pair.getLeft()).append(" -> ").append(pair.getRight());
		for(JsxAttribute attribute : removedAttributes)
			sb.append("\n  removed attribute ").append(attribute);
		for(JsxAttribute attribute : addedAttributes)
			sb.append("\n  added attribute ").append(attribute);
		for(JsxElementDiff diff : childElementDiffs)
			if(!diff.isEmpty())
				sb.append("\n  child ").append(diff.toString().replace("\n", "\n  "));
		for(Pair<JsxChild, JsxChild> pair : changedChildren)
			if(!pair.getLeft().isElement())
				sb.append("\n  changed child ").append(pair.getLeft()).append(" -> ").append(pair.getRight());
		for(JsxChild child : removedChildren)
			sb.append("\n  removed child ").append(child);
		for(JsxChild child : addedChildren)
			sb.append("\n  added child ").append(child);
		return sb.toString();
	}
}
