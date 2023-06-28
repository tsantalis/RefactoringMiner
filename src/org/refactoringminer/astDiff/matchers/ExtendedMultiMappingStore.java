package org.refactoringminer.astDiff.matchers;

import java.util.*;

import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.MultiMappingStore;
import com.github.gumtreediff.tree.FakeTree;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeUtils;
import com.github.gumtreediff.utils.Pair;

/**
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 * @since   2022-12-26 12:19 a.m.
 */
public class ExtendedMultiMappingStore extends MultiMappingStore implements Iterable<Mapping> {
	private final Tree src;
	private final Tree dst;

	public ExtendedMultiMappingStore(Tree srcTree, Tree dstTree) {
		super();
		this.src = srcTree;
		this.dst = dstTree;
	}

	public boolean isDstMultiMapped(Tree dstTree) {
		if (!hasDst(dstTree))
			return false;
		if (getSrcs(dstTree) == null)
			return false;
		if (getSrcs(dstTree).size() > 1)
			return true;
		if (getSrcs(dstTree).size() == 0)
			return false;
		Tree mappedSrc = getSrcs(dstTree).iterator().next();
		if (!hasSrc(mappedSrc))
			return false;
		return getDsts(mappedSrc).size() > 1;
	}

	public boolean isSrcMultiMapped(Tree srcTree) {
		if (!hasSrc(srcTree))
			return false;
		if (getDsts(srcTree) == null)
			return false;
		if (getDsts(srcTree).size() > 1)
			return true;
		if (getDsts(srcTree).size() == 0)
			return false;
		Tree mappedSrc = getDsts(srcTree).iterator().next();
		if (!hasDst(mappedSrc))
			return false;
		return getSrcs(mappedSrc).size() > 1;
	}

	private Map<Tree,Tree> getSrcToDstMono() {
		Map<Tree,Tree> monos = new HashMap<>();
		for (Tree _src : allMappedSrcs())
		{
			Set<Tree> dsts = getDsts(_src);
			if (dsts.size() > 1)
				continue;
			if (dsts.size() == 0)
				continue;
			Tree _dst = dsts.iterator().next();
			if (getSrcs(_dst).size() > 1)
				continue;
			monos.put(_src,_dst);
		}
		return monos;
	}

	private Map<Tree,Tree> getDstToSrcMono() {
		Map<Tree,Tree> monos = new HashMap<>();
		for (Tree _dst : allMappedDsts())
		{
			if (getSrcs(_dst).size() > 1)
				continue;
			Tree _src = getSrcs(_dst).iterator().next();
			if (getDsts(_src).size() > 1)
				continue;
			monos.put(_dst,_src);
		}
		return monos;
	}

	public void mergeMappings(MultiMappingStore addon) {
		if (addon == null) return;
		for (Mapping m : addon.getMappings())
		{
			this.addMapping(m.first,m.second);
		}
	}

	public MappingStore getMonoMappingStore() {
		MappingStore monoStore = new MappingStore(src,dst);
		for (Map.Entry<Tree,Tree> entry : getSrcToDstMono().entrySet())
			monoStore.addMapping(entry.getKey(),entry.getValue());
		return monoStore;
	}

	public Map<Tree,Set<Tree>> dstToSrcMultis() {
		Map<Tree,Set<Tree>> multis = new HashMap<>();
		for (Tree _dst : allMappedDsts())
		{
			if (getSrcs(_dst).size() > 1 && !(_dst instanceof FakeTree))
				multis.put(_dst,getSrcs(_dst));
			else
			{
				if (getSrcs(_dst).size() > 0) {
					Tree mappedSrc = getSrcs(_dst).iterator().next();
					if (getDsts(mappedSrc).size() > 1  && !(_dst instanceof FakeTree))
						multis.put(_dst,getSrcs(_dst));
				}
			}
		}
		return multis;
	}

	public Map<Tree,Set<Tree>> srcToDstMultis() {
		Map<Tree,Set<Tree>> multis = new HashMap<>();
		for (Tree _src : allMappedSrcs())
		{
			if (getDsts(_src).size() > 1)
				multis.put(_src,getDsts(_src));
			else
			{
				Tree mappedSrc = getDsts(_src).iterator().next();
				if (getSrcs(mappedSrc).size() > 1)
					multis.put(_src,getDsts(_src));
			}
		}
		return multis;
	}

	public void replaceMapping(Tree src, Tree dst) {
		if (this.getDsts(src) != null)
		{
			Set<Tree> dstForSrcList = new LinkedHashSet<>(this.getDsts(src));
			for (Tree dstForSrc : dstForSrcList)
				removeMapping(src,dstForSrc);
		}
		if (this.getSrcs(dst) != null)
		{
			Set<Tree> srcForDstList = new LinkedHashSet<>(this.getSrcs(dst));
			for (Tree srcForDst : srcForDstList)
				removeMapping(srcForDst,dst);
		}
		addMapping(src, dst);
	}

	public void addListOfMapping(List<Pair<Tree,Tree>> pairList) {
		if (pairList == null) return;
		for (Pair<Tree,Tree> pair : pairList) {
			addMapping(pair.first,pair.second);
		}
	}

	public boolean isSrcMapped(Tree src) {
		return hasSrc(src);
	}

	public boolean isDstMapped(Tree dst) {
		return hasDst(dst);
	}
	public void addMappingRecursively(Tree src, Tree dst) {
		addMapping(src, dst);
		if (src.getChildren() != null)
			for (int i = 0; i < src.getChildren().size(); i++)
				if (dst.getChildren().size() == src.getChildren().size())
					//TODO: Must investigate why the problem happens related to java docs:
					//https://github.com/Graylog2/graylog2-server/commit/2ef067fc70055fc4d55c75937303414ddcf07e0e
					addMappingRecursively(src.getChild(i), dst.getChild(i));
	}

	public void add(MappingStore match) {
		for (Mapping mapping : match) {
			addMapping(mapping.first,mapping.second);
		}
	}

	public void addWithMaps(MappingStore match,Map<Tree,Tree> srcCopy, Map<Tree,Tree> dstCopy) {
		for (Mapping mapping : match) {
			Tree realSrc = srcCopy.get(mapping.first);
			Tree realDst = dstCopy.get(mapping.second);
			if (realSrc != null && realDst != null)
				this.addMapping(realSrc,realDst);
		}
	}

	public void replaceWithMaps(MappingStore match, Map<Tree,Tree> srcCopy, Map<Tree,Tree> dstCopy) {
		for (Mapping mapping : match) {
			Tree realSrc = srcCopy.get(mapping.first);
			Tree realDst = dstCopy.get(mapping.second);
			if (realSrc != null && realDst != null)
				this.replaceMapping(realSrc,realDst);
		}
	}

	public void replaceWithOptimizedMappings(ExtendedMultiMappingStore optimizationMappings) {
		for (Mapping optimizationMapping : optimizationMappings) {
			Tree srcMapped = optimizationMapping.first;
			Tree dstMapped = optimizationMapping.second;
			if (this.getDsts(srcMapped) != null)
			{
				Set<Tree> dstForSrcList = new LinkedHashSet<>(this.getDsts(srcMapped));
				for (Tree dstForSrc : dstForSrcList)
					removeMapping(srcMapped,dstForSrc);
			}
			if (this.getSrcs(dstMapped) != null)
			{
				Set<Tree> srcForDstList = new LinkedHashSet<>(this.getSrcs(dstMapped));
				for (Tree srcForDst : srcForDstList)
					removeMapping(srcForDst,dstMapped);
			}
		}
		for (Mapping optimizationMapping : optimizationMappings) {
			this.addMapping(optimizationMapping.first,optimizationMapping.second);
		}
	}
}
