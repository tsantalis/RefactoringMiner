package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.AnnotationProvider;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.LeafMapping;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;

public class ParameterizeTestRefactoring extends ChangeTypeRefactoring {
	private UMLOperation removedOperation;
	private UMLOperation parameterizedTestOperation;
	private UMLOperationBodyMapper bodyMapper;
	private UMLOperation dataProviderBefore;
	private UMLOperation dataProviderAfter;
	private List<LeafMapping> data;
	private List<DataProviderOverride> dataProviderOverrides;

	public ParameterizeTestRefactoring(UMLOperationBodyMapper bodyMapper) {
		this.bodyMapper = bodyMapper;
		this.removedOperation = bodyMapper.getOperation1();
		this.parameterizedTestOperation = bodyMapper.getOperation2();
		this.data = new ArrayList<LeafMapping>();
		this.dataProviderOverrides = new ArrayList<DataProviderOverride>();
	}

	public UMLOperation getDataProviderBefore() {
		return dataProviderBefore;
	}

	public UMLOperation getDataProviderAfter() {
		return dataProviderAfter;
	}

	public void setDataProvider(UMLOperation dataProviderBefore, UMLOperation dataProviderAfter) {
		this.dataProviderBefore = dataProviderBefore;
		this.dataProviderAfter = dataProviderAfter;
	}

	public List<LeafMapping> getData() {
		return data;
	}

	public void addDataMapping(LeafMapping newLeafMapping) {
		for(LeafMapping oldLeafMapping : data) {
			if(oldLeafMapping.getFragment1().getLocationInfo().equals(newLeafMapping.getFragment1().getLocationInfo()) &&
					oldLeafMapping.getFragment2().getLocationInfo().equals(newLeafMapping.getFragment2().getLocationInfo())) {
				return;
			}
		}
		getBodyMapper().addMapping(newLeafMapping);
		data.add(newLeafMapping);
	}

	/**
	 * A data provider pairing supplied by a subclass that inherits {@link #parameterizedTestOperation}
	 * without overriding it (e.g. a subclass redeclaring a same-named static @MethodSource provider,
	 * relying on JUnit5 resolving it against the runtime test class via Java static-method hiding).
	 */
	public static class DataProviderOverride {
		private final UMLOperation dataProviderBefore;
		private final UMLOperation dataProviderAfter;
		private final List<LeafMapping> data;
		private final UMLOperationBodyMapper bodyMapper;

		/**
		 * @param bodyMapper a standalone mapper for (dataProviderBefore, dataProviderAfter), already
		 * populated with {@code data}. Deliberately NOT registered in the subclass's own
		 * operationBodyMapperList by the caller: doing so makes it visible to model-wide cross-class
		 * passes (e.g. consistent-rename inference), which then misreport it as a genuine
		 * Rename/Change Return Type/Remove Annotation refactoring between two methods that only
		 * happen to share the DataProvider's identity, not an actual rename.
		 */
		public DataProviderOverride(UMLOperation dataProviderBefore, UMLOperation dataProviderAfter, List<LeafMapping> data, UMLOperationBodyMapper bodyMapper) {
			this.dataProviderBefore = dataProviderBefore;
			this.dataProviderAfter = dataProviderAfter;
			this.data = data;
			this.bodyMapper = bodyMapper;
		}

		public UMLOperation getDataProviderBefore() {
			return dataProviderBefore;
		}

		public UMLOperation getDataProviderAfter() {
			return dataProviderAfter;
		}

		public List<LeafMapping> getData() {
			return data;
		}

		public UMLOperationBodyMapper getBodyMapper() {
			return bodyMapper;
		}
	}

	public List<DataProviderOverride> getDataProviderOverrides() {
		return dataProviderOverrides;
	}

	public void addDataProviderOverride(DataProviderOverride override) {
		for(LeafMapping leafMapping : override.getData()) {
			getBodyMapper().addMapping(leafMapping);
		}
		dataProviderOverrides.add(override);
	}

	@Override
	public AnnotationProvider getProviderBefore() {
		return removedOperation;
	}

	@Override
	public AnnotationProvider getProviderAfter() {
		return parameterizedTestOperation;
	}

	public Optional<String> getTemplateParameterBefore() {
		return Optional.of(removedOperation.toQualifiedString());
	}

	public String getTemplateParameterAfter() {
		return parameterizedTestOperation.toQualifiedString();
	}

	public UMLOperationBodyMapper getBodyMapper() {
		return bodyMapper;
	}

	public UMLOperation getRemovedOperation() {
		return removedOperation;
	}

	public UMLOperation getParameterizedTestOperation() {
		return parameterizedTestOperation;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(removedOperation.codeRange()
				.setDescription("removed method declaration")
				.setCodeElement(removedOperation.toString()));
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(parameterizedTestOperation.codeRange()
				.setDescription("parameterized test method declaration")
				.setCodeElement(parameterizedTestOperation.toString()));
		return ranges;
	}

	@Override
	public RefactoringType getRefactoringType() {
		return RefactoringType.PARAMETERIZE_TEST;
	}

	@Override
	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getRemovedOperation().getLocationInfo().getFilePath(), getRemovedOperation().getClassName()));
		return pairs;
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getParameterizedTestOperation().getLocationInfo().getFilePath(), getParameterizedTestOperation().getClassName()));
		return pairs;
	}

	@Override
	public int hashCode() {
		return Objects.hash(parameterizedTestOperation, removedOperation);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ParameterizeTestRefactoring other = (ParameterizeTestRefactoring) obj;
		return Objects.equals(parameterizedTestOperation, other.parameterizedTestOperation)
				&& Objects.equals(removedOperation, other.removedOperation);
	}
}
