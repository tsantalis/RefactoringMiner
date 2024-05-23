package org.refactoringminer.astDiff.actions.editscript;

import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.model.*;
import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.FakeTree;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.TreeUtils;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;
import org.refactoringminer.astDiff.actions.model.MoveIn;
import org.refactoringminer.astDiff.actions.model.MoveOut;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;

import java.util.*;

import static org.refactoringminer.astDiff.utils.Helpers.findNameByTree;

/**
 * An edit script generator based upon Chawathe algorithm supporting multi-mappings.
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 */
public class ExtendedChawatheScriptGenerator implements ExtendedEditScriptGenerator {
	private Tree origSrc;
	private Tree cpySrc;
	private Tree origDst;
	private MappingStore origMappings;
	private MappingStore cpyMappings;
	private Set<Tree> dstInOrder;
	private Set<Tree> srcInOrder;
	private EditScript actions;
	private Map<Tree, Tree> origToCopy;
	private Map<Tree, Tree> copyToOrig;
	private ExtendedMultiMappingStore multiMappingStore;
	private Map<String, TreeContext> childContextMap;
	private Map<String, TreeContext> parentContextMap;

	@Override
	public EditScript computeActions(ExtendedMultiMappingStore ms, Map<String, TreeContext> parentContextMap, Map<String, TreeContext> childContextMap) {
		this.parentContextMap = parentContextMap;
		this.childContextMap = childContextMap;
		this.multiMappingStore = ms;
		initWith(ms.getMonoMappingStore());
		generate();
		processMultiMappings();
		return actions;
	}
	private void processMultiMappings() {
		for (Action action : new SimplifiedMultiMoveActionGenerator().computeActions(multiMappingStore)) {
			actions.add(action);
		}
	}

	public void initWith(MappingStore ms) {
		this.origSrc = ms.src;
		this.cpySrc = this.origSrc.deepCopy();
		this.origDst = ms.dst;
		this.origMappings = ms;

		origToCopy = new HashMap<>();
		copyToOrig = new HashMap<>();
		Iterator<Tree> cpyTreeIterator = TreeUtils.preOrderIterator(cpySrc);
		for (Tree origTree: TreeUtils.preOrder(origSrc)) {
			Tree cpyTree = cpyTreeIterator.next();
			origToCopy.put(origTree, cpyTree);
			copyToOrig.put(cpyTree, origTree);
		}

		cpyMappings = new MappingStore(ms.src,ms.dst);
		for (Mapping m: origMappings)
			cpyMappings.addMapping(origToCopy.get(m.first), m.second);
	}

	public EditScript generate() {
		Tree srcFakeRoot = new FakeTree(cpySrc);
		Tree dstFakeRoot = new FakeTree(origDst);
		cpySrc.setParent(srcFakeRoot);
		origDst.setParent(dstFakeRoot);

		actions = new EditScript();
		dstInOrder = new HashSet<>();
		srcInOrder = new HashSet<>();

		cpyMappings.addMapping(srcFakeRoot, dstFakeRoot);

		List<Tree> bfsDst = TreeUtils.breadthFirst(origDst);
		for (Tree x: bfsDst) {
			Tree w;
			Tree y = x.getParent();
			Tree z = cpyMappings.getSrcForDst(y);
			if (!cpyMappings.isDstMapped(x)) {
				if(!multiMappingStore.isDstMultiMapped(x)) {
					int k = findPos(x);
					// Insertion case : insert new node.
					w = new FakeTree();
					// In order to use the real nodes from the second tree, we
					// furnish x instead of w
					Action ins = new Insert(x, copyToOrig.get(z), k);
					actions.add(ins);
					copyToOrig.put(w, x);
					cpyMappings.addMapping(w, x);
					if(z != null)
						z.insertChild(w, k);
				}
				else
				{
					continue;
				}
			} else {

				w = cpyMappings.getSrcForDst(x);
				if (w == null)
				{
					if (origMappings.getSrcForDst(x).equals(TreeUtilFunctions.getFakeTreeInstance()))
					{

					}
					else {
						TreeUtilFunctions.getFinalRoot(origMappings.getSrcForDst(x));
						String nameByTree = findNameByTree(parentContextMap, origMappings.getSrcForDst(x));
						if (nameByTree != null)
							actions.add(new MoveIn(origMappings.getSrcForDst(x), x, nameByTree, +1));
					}
				}
				else if (!x.equals(origDst)) { // TODO => x != origDst // Case of the root
						Tree v = w.getParent();
						if (!w.getLabel().equals(x.getLabel())) {
							actions.add(new Update(copyToOrig.get(w), x.getLabel()));
							w.setLabel(x.getLabel());
						}
						if (z != null) {
							if (!z.equals(v)) {
								int k = findPos(x);
								Action mv = new Move(copyToOrig.get(w), copyToOrig.get(z), k);
								actions.add(mv);
								int oldk = w.positionInParent();
								w.getParent().getChildren().remove(oldk);
								z.insertChild(w, k);
							}
						}
						else {
							if (multiMappingStore.isDstMultiMapped(y))
							{
								int k = findPos(x);
								Action mv = new Move(copyToOrig.get(w), null, k);
								actions.add(mv);
							}
						}
				}
			}

			if (w != null) {
				srcInOrder.add(w);
				dstInOrder.add(x);
				alignChildren(w, x);
			}
		}

		for (Tree w : cpySrc.postOrder()) {
			if (!cpyMappings.isSrcMapped(w) && !multiMappingStore.isSrcMultiMapped(copyToOrig.get(w))) {
				actions.add(new Delete(copyToOrig.get(w)));

			}
			else if (!multiMappingStore.isSrcMultiMapped(copyToOrig.get(w)))
			{
				if (cpyMappings.getDstForSrc(w).equals(TreeUtilFunctions.getFakeTreeInstance()))
				{

				}
				else if (!TreeUtilFunctions.getFinalRoot(cpyMappings.getDstForSrc(w)).equals(origDst))
				{
					Tree a = cpyMappings.getDstForSrc(w);
					String nameByTree = findNameByTree(childContextMap, a);
					if (nameByTree != null)
						actions.add(new MoveOut(copyToOrig.get(w),cpyMappings.getDstForSrc(w), nameByTree,+1));
				}
			}
			else if (multiMappingStore.isSrcMultiMapped(copyToOrig.get(w)))
			{
				Tree origSrc = copyToOrig.get(w);
				Set<Tree> dstCandidates = multiMappingStore.getDsts(origSrc);
				if (dstCandidates != null){
					for (Tree dstCandidate : dstCandidates) {
						if (dstCandidate.equals(TreeUtilFunctions.getFakeTreeInstance())) {}
						else if (!TreeUtilFunctions.getFinalRoot(dstCandidate).equals(origDst)) {
							String nameByTree = findNameByTree(childContextMap, dstCandidate);
							if (nameByTree != null)
								actions.add(new MoveOut(origSrc, dstCandidate, nameByTree, +1));
						}
					}
				}
			}
		}
		origDst.setParent(null);
		return actions;
	}

	private void alignChildren(Tree w, Tree x) {
		srcInOrder.removeAll(w.getChildren());
		dstInOrder.removeAll(x.getChildren());

		List<Tree> s1 = new ArrayList<>();
		for (Tree c: w.getChildren())
			if (cpyMappings.isSrcMapped(c))
				if (x.getChildren().contains(cpyMappings.getDstForSrc(c)))
					s1.add(c);

		List<Tree> s2 = new ArrayList<>();
		for (Tree c: x.getChildren())
			if (cpyMappings.isDstMapped(c))
				if (w.getChildren().contains(cpyMappings.getSrcForDst(c)))
					s2.add(c);

		List<Mapping> lcs = lcs(s1, s2);

		for (Mapping m : lcs) {
			srcInOrder.add(m.first);
			dstInOrder.add(m.second);
		}

		for (Tree b: s2 ) { // iterate through s2 first, to ensure left-to-right insertions
			for (Tree a : s1) {
				if (cpyMappings.has(a, b)) {
					if (!lcs.contains(new Mapping(a, b))) {
						a.getParent().getChildren().remove(a); // remove this node directly.
						int k = findPos(b); // find insert position AFTER removing node from old place.
						Action mv = new Move(copyToOrig.get(a), copyToOrig.get(w), k);
						actions.add(mv);
						w.getChildren().add(k, a);
						a.setParent(w);
						srcInOrder.add(a);
						dstInOrder.add(b);
					}
				}
			}
		}
	}

	private int findPos(Tree x) {
		Tree y = x.getParent();
		List<Tree> siblings = y.getChildren();

		for (Tree c : siblings) {
			if (dstInOrder.contains(c)) {
				if (c.equals(x)) return 0;
				else break;
			}
		}

		int xpos = x.positionInParent();
		Tree v = null;
		for (int i = 0; i < xpos; i++) {
			Tree c = siblings.get(i);
			if (dstInOrder.contains(c)) v = c;
		}

		//if (v == null) throw new RuntimeException("No rightmost sibling in order");
		if (v == null) return 0;

		Tree u = cpyMappings.getSrcForDst(v);
		// siblings = u.getParent().getChildren();
		// int upos = siblings.indexOf(u);
		int upos = u.positionInParent();
		// int r = 0;
		// for (int i = 0; i <= upos; i++)
		// if (srcInOrder.contains(siblings.get(i))) r++;
		return upos + 1;
	}

	private List<Mapping> lcs(List<Tree> x, List<Tree> y) {
		int m = x.size();
		int n = y.size();
		List<Mapping> lcs = new ArrayList<>();

		int[][] opt = new int[m + 1][n + 1];
		for (int i = m - 1; i >= 0; i--) {
			for (int j = n - 1; j >= 0; j--) {
				if (cpyMappings.getSrcForDst(y.get(j)).equals(x.get(i))) opt[i][j] = opt[i + 1][j + 1] + 1;
				else  opt[i][j] = Math.max(opt[i + 1][j], opt[i][j + 1]);
			}
		}

		int i = 0, j = 0;
		while (i < m && j < n) {
			if (cpyMappings.getSrcForDst(y.get(j)).equals(x.get(i))) {
				lcs.add(new Mapping(x.get(i), y.get(j)));
				i++;
				j++;
			} else if (opt[i + 1][j] >= opt[i][j + 1]) i++;
			else j++;
		}

		return lcs;
	}
}
