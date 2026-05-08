package gr.uom.java.xmi;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.diff.CodeRange;

public class UMLNamedExport implements Serializable, LocationInfoProvider {
	private LocationInfo locationInfo;
	private List<LeafExpression> specifiers;
	private LeafExpression source;

	public UMLNamedExport(LocationInfo locationInfo, List<LeafExpression> specifiers) {
		this.locationInfo = locationInfo;
		this.specifiers = specifiers;
	}

	public boolean equalSpecifiers(UMLNamedExport other) {
		List<String> names1 = specifiers.stream()
				.map(expr -> expr.getString())
				.collect(Collectors.toList());
		List<String> names2 = other.specifiers.stream()
				.map(expr -> expr.getString())
				.collect(Collectors.toList());
		return names1.containsAll(names2) && names2.containsAll(names1);
	}

	private boolean equalSpecifiersInOrder(UMLNamedExport other) {
		List<String> names1 = specifiers.stream()
				.map(expr -> expr.getString())
				.collect(Collectors.toList());
		List<String> names2 = other.specifiers.stream()
				.map(expr -> expr.getString())
				.collect(Collectors.toList());
		return names1.equals(names2);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((source == null) ? 
			specifiers.stream()
			.map(expr -> expr.getString())
			.collect(Collectors.toList()).hashCode() :
			source.getString().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UMLNamedExport other = (UMLNamedExport) obj;
		if(source != null && other.source != null)
			return Objects.equals(source.getString(), other.source.getString());
		else if(source == null && other.source == null)
			return equalSpecifiersInOrder(other);
		return false;
	}

	public LeafExpression getSource() {
		return source;
	}

	public void setSource(LeafExpression source) {
		this.source = source;
	}

	public List<LeafExpression> getSpecifiers() {
		return specifiers;
	}

	@Override
	public LocationInfo getLocationInfo() {
		return locationInfo;
	}

	@Override
	public CodeRange codeRange() {
		return locationInfo.codeRange();
	}
}
