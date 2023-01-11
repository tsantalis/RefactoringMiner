package org.refactoringminer.astDiff.matchers;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.MultiMappingStore;
import com.github.gumtreediff.tree.FakeTree;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.TreeUtils;
import com.github.gumtreediff.utils.Pair;

/**
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 * @since   2022-12-26 12:19 a.m.
 */
public class ExtendedMultiMappingStore extends MultiMappingStore implements Iterable<Mapping> {
	private TreeContext srcTC;
	private TreeContext dstTC;

	public ExtendedMultiMappingStore(TreeContext srcTC, TreeContext dstTC) {
		super();
		this.srcTC = srcTC;
		this.dstTC = dstTC;
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
			if (getDsts(_src).size() > 1)
				continue;
			Tree _dst = getDsts(_src).iterator().next();
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
		MappingStore monoStore = new MappingStore(srcTC.getRoot(),dstTC.getRoot());
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

	public boolean areSrcsUnmapped(Collection<Tree> srcs, Tree dst) {
		for (Tree src : srcs)
			if (isSrcMapped(src)) {
				Set<Tree> dstForSrc = this.getDsts(src);
				for (Tree dstMapped : dstForSrc) {
					if (TreeUtils.preOrder(dst).contains(dstMapped))
						return false;
				}
			}
		return true;
	}

	/**
	 * Return whether or not all the given destination nodes are unmapped.
	 */
	public boolean areDstsUnmapped(Collection<Tree> dsts, Tree src) {
		for (Tree dst : dsts)
			if (isDstMapped(dst)) {
				Set<Tree> srcForDst = this.getSrcs(dst);
				for (Tree srcMapped : srcForDst) {
					if (TreeUtils.preOrder(src).contains(srcMapped))
						return false;
				}
			}
		return true;
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

	public List<MappingExportModel> exportModelList() {
		List<MappingExportModel> exportList = new ArrayList<>();
		for (Mapping mapping : getMappings()) {
			MappingExportModel mappingExportModel = new MappingExportModel(
					mapping.first.getType().name,
					mapping.first.getLabel(),
					mapping.first.getPos(),
					mapping.first.getEndPos(),
					mapping.first.hashCode(),
					(mapping.first.getParent() == null) ? "" : mapping.first.getParent().getType().name,
					mapping.second.getType().name,
					mapping.second.getLabel(),
					mapping.second.getPos(),
					mapping.second.getEndPos(),
					mapping.second.hashCode(),
					(mapping.second.getParent() == null) ? "" : mapping.second.getParent().getType().name
					);
			exportList.add(mappingExportModel);
		}
		exportList.sort(
				Comparator.comparing(
						MappingExportModel::getFirstPos)
						.thenComparing(exportModel -> -1 * exportModel.getFirstEndPos())
						.thenComparing(MappingExportModel::getFirstType)
		);
		return exportList;
	}

	public String exportString() throws JsonProcessingException {
		List<MappingExportModel> mappingExportModels = this.exportModelList();
		ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(mappingExportModels);
	}

	public void exportToFile(File outputFile) throws IOException {
		List<MappingExportModel> mappingExportModels = this.exportModelList();
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, mappingExportModels);
	}

	public static class MappingExportModel implements Serializable {
		String firstType,secondType,firstLabel,secondLabel,firstParentType,secondParentType;
		int firstPos,secondPos,firstEndPos,secondEndPos;
		@JsonIgnore
		int firstHash,secondHash;

		public MappingExportModel(String firstType, String firstLabel,int firstPos,int firstEndPos, int firstHash, String firstParentType,
								  String secondType, String secondLabel, int secondPos, int secondEndPos, int secondHash, String secondParentType) {
			this.firstType = firstType;
			this.secondType = secondType;
			this.firstLabel = firstLabel;
			this.secondLabel = secondLabel;
			this.firstPos = firstPos;
			this.secondPos = secondPos;
			this.firstEndPos = firstEndPos;
			this.secondEndPos = secondEndPos;
			this.firstHash = firstHash;
			this.secondHash = secondHash;
			this.firstParentType = firstParentType;
			this.secondParentType = secondParentType;
		}

		public int getFirstHash() {
			return firstHash;
		}

		public int getSecondHash() {
			return secondHash;
		}

		public void setFirstHash(int firstHash) {
			this.firstHash = firstHash;
		}

		public void setSecondHash(int secondHash) {
			this.secondHash = secondHash;
		}

		public String getFirstType() {
			return firstType;
		}

		public void setFirstType(String firstType) {
			this.firstType = firstType;
		}

		public String getSecondType() {
			return secondType;
		}

		public void setSecondType(String secondType) {
			this.secondType = secondType;
		}

		public String getFirstLabel() {
			return firstLabel;
		}

		public void setFirstLabel(String firstLabel) {
			this.firstLabel = firstLabel;
		}

		public String getSecondLabel() {
			return secondLabel;
		}

		public void setSecondLabel(String secondLabel) {
			this.secondLabel = secondLabel;
		}

		public int getFirstPos() {
			return firstPos;
		}

		public void setFirstPos(int firstPos) {
			this.firstPos = firstPos;
		}

		public int getSecondPos() {
			return secondPos;
		}

		public void setSecondPos(int secondPos) {
			this.secondPos = secondPos;
		}

		public int getFirstEndPos() {
			return firstEndPos;
		}

		public void setFirstEndPos(int firstEndPos) {
			this.firstEndPos = firstEndPos;
		}

		public int getSecondEndPos() {
			return secondEndPos;
		}

		public void setSecondEndPos(int secondEndPos) {
			this.secondEndPos = secondEndPos;
		}

		public String getFirstParentType() {
			return firstParentType;
		}

		public void setFirstParentType(String firstParentType) {
			this.firstParentType = firstParentType;
		}

		public String getSecondParentType() {
			return secondParentType;
		}

		public void setSecondParentType(String secondParentType) {
			this.secondParentType = secondParentType;
		}
	}
}
