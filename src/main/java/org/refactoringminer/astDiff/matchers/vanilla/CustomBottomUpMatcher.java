package org.refactoringminer.astDiff.matchers.vanilla;

import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeUtils;
import com.github.gumtreediff.tree.Type;
import com.github.gumtreediff.utils.SequenceAlgorithms;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import java.util.*;

/**
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 * @since   2022-12-27 8:46 p.m.
 */
public class CustomBottomUpMatcher implements Matcher {
	private final Constants LANG1;
	private final Constants LANG2;
	public CustomBottomUpMatcher(Constants LANG1, Constants LANG2) {
		this.LANG1 = LANG1;
		this.LANG2 = LANG2;
	}

	@Override
	public MappingStore match(Tree src, Tree dst, MappingStore mappings) {
		for (Tree t : src.postOrder()) {
			if (t.isRoot()) {
				if (t.getType().name.equals(dst.getType().name))
				{
					if (!mappings.isSrcMapped(t) && !mappings.isDstMapped(dst))
						mappings.addMapping(t, dst);
				}
				lastChanceMatch(mappings, t, dst);
				break;
			}
			else if (!(mappings.isSrcMapped(t) || t.isLeaf())) {
				List<Tree> candidates = getDstCandidates(mappings, t);
				Tree best = null;
				var max = -1D;
				List<Tree> tDescendants = descendants(t, LANG1);

				for (var candidate : candidates) {
					//t is from src, candidate is from dst
					if (operatorMappedElsewhere(mappings, t, candidate))
						continue;
					List<Tree> candidateDescendants = descendants(candidate, LANG2);
					var threshold = 1D / (1D + Math.log(candidateDescendants.size() + tDescendants.size()));
					var sim = chawatheSimilarity(tDescendants, candidateDescendants, mappings);
					if (sim > max && sim >= threshold) {
						max = sim;
						best = candidate;
					}
				}

				if (best != null) {
					lastChanceMatch(mappings, t, best);
					//t is from src, best is from dst
					boolean checkOperatorOfInfixExpression = checkInfixExpression(mappings, t, best, LANG1, LANG2);
					if (checkOperatorOfInfixExpression) {
						mappings.addMapping(t, best);
						if (t.getType().name.equals(LANG1.INFIX_EXPRESSION))
							keepOperandsInInfixExpression(mappings, t, best);
					}
				}
			}
			else if (mappings.isSrcMapped(t) && mappings.hasUnmappedSrcChildren(t)
					&& mappings.hasUnmappedDstChildren(mappings.getDstForSrc(t)))
				lastChanceMatch(mappings, t, mappings.getDstForSrc(t));
		}
		separatorMatching(mappings, src);
		return mappings;
	}

	//a qualified name counts as one node, as its names are the parts of a single name, i.e., v1.y in v1.y * v2.z - v1.z * v2.y -> y3 * z2 - z3 * y2
	private static List<Tree> descendants(Tree tree, Constants LANG) {
		List<Tree> descendants = new ArrayList<>();
		for (Tree child : tree.getChildren()) {
			descendants.add(child);
			if (!child.getType().name.equals(LANG.QUALIFIED_NAME))
				descendants.addAll(descendants(child, LANG));
		}
		return descendants;
	}

	private static double chawatheSimilarity(List<Tree> srcDescendants, List<Tree> dstDescendants, MappingStore mappings) {
		Set<Tree> dsts = new HashSet<>(dstDescendants);
		int common = 0;
		for (Tree src : srcDescendants) {
			if (mappings.isSrcMapped(src) && dsts.contains(mappings.getDstForSrc(src)))
				common++;
		}
		return (double) common / Math.max(srcDescendants.size(), dstDescendants.size());
	}

	//the operands of mapped infix expressions are mapped with each other by position, when an operand is mapped outside the other infix expression,
	//i.e., format.getShape() == JsonFormat.Shape.STRING -> (shape == JsonFormat.Shape.STRING) || format.hasPattern(), format.getShape() is mapped with shape instead of format.hasPattern()
	private void keepOperandsInInfixExpression(MappingStore mappings, Tree t, Tree best) {
		if (t.getChildren().size() != best.getChildren().size())
			return;
		for (int i = 0; i < t.getChildren().size(); i++) {
			Tree operand1 = t.getChild(i);
			Tree operand2 = best.getChild(i);
			Tree mapped = mappings.getDstForSrc(operand1);
			if (mapped == null || best.getDescendants().contains(mapped) || !mappings.areDstsUnmapped(TreeUtils.preOrder(operand2)))
				continue;
			List<Tree> mappedSubtree = mapped.getDescendants();
			for (Tree src : operand1.getDescendants()) {
				if (mappings.isSrcMapped(src) && mappedSubtree.contains(mappings.getDstForSrc(src)))
					mappings.removeMapping(src, mappings.getDstForSrc(src));
			}
			mappings.removeMapping(operand1, mapped);
			mappings.addMapping(operand1, operand2);
			lastChanceMatch(mappings, operand1, operand2);
		}
	}

	//an infix expression is not a counterpart of another one, whose operator is not the counterpart of its operator,
	//i.e., format.getShape() == JsonFormat.Shape.STRING -> (shape == JsonFormat.Shape.STRING) || format.hasPattern(), when == is mapped inside the parentheses
	private boolean operatorMappedElsewhere(MappingStore mappings, Tree t, Tree candidate) {
		if (!t.getType().name.equals(LANG1.INFIX_EXPRESSION))
			return false;
		Tree a = TreeUtilFunctions.findChildByType(t, LANG1.INFIX_EXPRESSION_OPERATOR);
		Tree b = TreeUtilFunctions.findChildByType(candidate, LANG2.INFIX_EXPRESSION_OPERATOR);
		return a != null && mappings.isSrcMapped(a) && mappings.getDstForSrc(a) != b;
	}

	private static boolean checkInfixExpression(MappingStore mappings, Tree t, Tree best, Constants LANG1, Constants LANG2) {
		boolean checkOperatorOfInfixExpression = true;
		if (t.getType().name.equals(LANG1.INFIX_EXPRESSION))
		{
			checkOperatorOfInfixExpression = false;
			Tree a = TreeUtilFunctions.findChildByType(t, LANG1.INFIX_EXPRESSION_OPERATOR);
			Tree b = TreeUtilFunctions.findChildByType(best, LANG2.INFIX_EXPRESSION_OPERATOR);
			if (mappings.getDstForSrc(a) == b) checkOperatorOfInfixExpression = true;
		}
		return checkOperatorOfInfixExpression;
	}

	protected List<Tree> getDstCandidates(MappingStore mappings, Tree src) {
		List<Tree> seeds = new ArrayList<>();
		for (Tree c : src.getDescendants()) {
			Tree m = mappings.getDstForSrc(c);
			if (m != null)
				seeds.add(m);
		}
		List<Tree> candidates = new ArrayList<>();
		Set<Tree> visited = new HashSet<>();
		for (var seed : seeds) {
			while (seed.getParent() != null) {
				var parent = seed.getParent();
				if (visited.contains(parent))
					break;
				visited.add(parent);
				if (parent.getType() == src.getType() && !mappings.isDstMapped(parent) && !parent.isRoot())
					candidates.add(parent);
				seed = parent;
			}
		}
		return candidates;
	}

	protected void lastChanceMatch(MappingStore mappings, Tree src, Tree dst) {
		lcsEqualMatching(mappings, src, dst);
		lcsStructureMatching(mappings, src, dst);
		histogramMatching(mappings, src, dst);
	}

	//the separators of a list, i.e., the commas, have no identity of their own and stand between the elements of the list,
	//so they are mapped after the elements, to the comma after the counterpart of the element before them, i.e., the comma after "use_direct_io" -> "load_mode",
	//or else to the comma before the counterpart of the element after them, i.e., '/health', {} -> this.path(), {},
	//or else by position, when the lists have the same length, i.e., [DIRECTIONS.HORIZONTAL, DIRECTIONS.VERTICAL] -> ['horizontal', 'vertical'],
	//and the commas between unmapped elements remain unmapped, i.e., the comma after the removed /*.use_mmap =*/ true
	//is not mapped to the comma after the added /*.load_mode =*/ LLAMA_LOAD_MODE_MMAP
	private void separatorMatching(MappingStore mappings, Tree src) {
		for (Tree separator1 : src.preOrder()) {
			if (!isSeparator(separator1, LANG1) || mappings.isSrcMapped(separator1) || separator1.getParent() == null)
				continue;
			Tree list1 = separator1.getParent();
			int i = separator1.positionInParent();
			Tree separator2 = null;
			if (i > 0) {
				Tree element2 = counterpartElement(mappings, list1.getChild(i - 1));
				if (element2 != null && element2.positionInParent() + 1 < element2.getParent().getChildren().size())
					separator2 = element2.getParent().getChild(element2.positionInParent() + 1);
			}
			if (!isUnmappedSeparator(mappings, separator2, separator1) && i + 1 < list1.getChildren().size()) {
				Tree element2 = counterpartElement(mappings, list1.getChild(i + 1));
				separator2 = element2 != null && element2.positionInParent() > 0 ? element2.getParent().getChild(element2.positionInParent() - 1) : null;
			}
			if (!isUnmappedSeparator(mappings, separator2, separator1)) {
				Tree list2 = mappings.getDstForSrc(list1);
				separator2 = list2 != null && list2.getChildren().size() == list1.getChildren().size() ? list2.getChild(i) : null;
			}
			if (isUnmappedSeparator(mappings, separator2, separator1))
				mappings.addMapping(separator1, separator2);
		}
	}

	private boolean isUnmappedSeparator(MappingStore mappings, Tree separator2, Tree separator1) {
		return separator2 != null && isSeparator(separator2, LANG2) && separator2.getLabel().equals(separator1.getLabel()) && !mappings.isDstMapped(separator2);
	}

	//the counterpart of an element is the element of the counterpart list containing the counterpart of the element or of its leading part,
	//i.e., button -> button: any, where button is mapped inside the parameter, or local_device = receive_prep_result.local_device -> local_device,
	//where the captured name is mapped, but not of a part after it, i.e., Chart.defaults.global in globalDefaults = Chart.defaults.global,
	//which is moved to another element, or else its counterpart in another list
	private static Tree counterpartElement(MappingStore mappings, Tree element1) {
		Tree counterpart = mappings.getDstForSrc(element1);
		Tree list2 = mappings.getDstForSrc(element1.getParent());
		if (list2 != null) {
			for (Tree part = element1; part != null; part = part.isLeaf() ? null : part.getChild(0)) {
				if (mappings.isSrcMapped(part)) {
					Tree element2 = mappings.getDstForSrc(part);
					while (element2.getParent() != null && element2.getParent() != list2)
						element2 = element2.getParent();
					if (element2.getParent() == list2)
						return element2;
					break;
				}
			}
		}
		return counterpart != null && counterpart.getParent() != null ? counterpart : null;
	}

	private static boolean isSeparator(Tree tree, Constants LANG) {
		return tree.isLeaf() && tree.getType().name.equals(LANG.COMMA);
	}

	protected void lcsEqualMatching(MappingStore mappings, Tree src, Tree dst) {
		List<Tree> unmappedSrcChildren = new ArrayList<>();
		for (Tree c : src.getChildren())
			if (!mappings.isSrcMapped(c) && !isSeparator(c, LANG1))
				unmappedSrcChildren.add(c);

		List<Tree> unmappedDstChildren = new ArrayList<>();
		for (Tree c : dst.getChildren())
			if (!mappings.isDstMapped(c) && !isSeparator(c, LANG2))
				unmappedDstChildren.add(c);

		List<int[]> lcs = SequenceAlgorithms.longestCommonSubsequenceWithIsomorphism(
				unmappedSrcChildren, unmappedDstChildren);
		for (int[] x : lcs) {
			var t1 = unmappedSrcChildren.get(x[0]);
			var t2 = unmappedDstChildren.get(x[1]);
			if (mappings.areSrcsUnmapped(TreeUtils.preOrder(t1)) && mappings.areDstsUnmapped(
					TreeUtils.preOrder(t2)))
				mappings.addMappingRecursively(t1, t2);
		}
	}

	protected void lcsStructureMatching(MappingStore mappings, Tree src, Tree dst) {
		List<Tree> unmappedSrcChildren = new ArrayList<>();
		for (Tree c : src.getChildren())
			if (!mappings.isSrcMapped(c) && !isSeparator(c, LANG1))
				unmappedSrcChildren.add(c);

		List<Tree> unmappedDstChildren = new ArrayList<>();
		for (Tree c : dst.getChildren())
			if (!mappings.isDstMapped(c) && !isSeparator(c, LANG2))
				unmappedDstChildren.add(c);

		List<int[]> lcs = SequenceAlgorithms.longestCommonSubsequenceWithIsostructure(
				unmappedSrcChildren, unmappedDstChildren);
		for (int[] x : lcs) {
			var t1 = unmappedSrcChildren.get(x[0]);
			var t2 = unmappedDstChildren.get(x[1]);
			if (mappings.areSrcsUnmapped(
					TreeUtils.preOrder(t1)) && mappings.areDstsUnmapped(TreeUtils.preOrder(t2)))
				mappings.addMappingRecursively(t1, t2);
		}
	}

	protected void histogramMatching(MappingStore mappings, Tree src, Tree dst) {
		Map<Type, List<Tree>> srcHistogram = new HashMap<>();
		for (var c :  src.getChildren()) {
			if (mappings.isSrcMapped(c) || isSeparator(c, LANG1))
				continue;
			srcHistogram.putIfAbsent(c.getType(), new ArrayList<>());
			srcHistogram.get(c.getType()).add(c);
		}

		Map<Type, List<Tree>> dstHistogram = new HashMap<>();
		for (var c : dst.getChildren()) {
			if (mappings.isDstMapped(c) || isSeparator(c, LANG2))
				continue;
			dstHistogram.putIfAbsent(c.getType(), new ArrayList<>());
			dstHistogram.get(c.getType()).add(c);
		}

		for (Type t : srcHistogram.keySet()) {
			if (dstHistogram.containsKey(t) && srcHistogram.get(t).size() == 1 && dstHistogram.get(t).size() == 1) {
				var srcChild = srcHistogram.get(t).get(0);
				var dstChild = dstHistogram.get(t).get(0);
				mappings.addMapping(srcChild, dstChild);
				lastChanceMatch(mappings, srcChild, dstChild);
			}
		}
	}
}
