package br.ufmg.dcc.labsoft.refdetector.model;

import br.ufmg.dcc.labsoft.refdetector.model.builder.SourceRepresentation;


public class SDMethod extends SDEntity {

	private String signature;
	private final boolean constructor;
	private boolean testAnnotation;
	private boolean deprecatedAnnotation;
	private int numberOfStatements;
	private Multiset<SDMethod> callers;
	private Multiset<SDMethod> origins;
	private SourceRepresentation body;

	public SDMethod(SDModel.Snapshot snapshot, int id, String signature, SDContainerEntity container, boolean constructor) {
		super(snapshot, id, container.fullName() + "#" + signature, container);
		this.signature = signature;
		this.callers = new Multiset<SDMethod>();
		this.origins = new Multiset<SDMethod>();
		this.constructor = constructor;
	}

	public Multiset<SDMethod> callers() {
		return this.callers;
	}
	
	@Override
	public String simpleName() {
		return signature;
	}

	public String identifier() {
	    return signature.substring(0, signature.indexOf('('));
	}

	public String parameters() {
	    return signature.substring(signature.indexOf('('));
	}
	
	public Multiset<SDMethod> origins() {
		return this.origins;
	}

	public boolean delegatesTo(SDMethod other) {
		return other.callers.contains(this) && numberOfStatements <= 3;
	}

	public boolean isDeprecated() {
		return deprecatedAnnotation;
	}

	@Override
	public boolean isTestCode() {
		return testAnnotation || container.isTestCode();
	}

	public boolean isOverriden() {
		SDType parent = (SDType) this.container;
		for (SDType subtype : parent.subtypes()) {
			String overridenMethodKey = subtype.fullName() + "#" + signature;
			if (snapshot.find(SDMethod.class, overridenMethodKey) != null) {
				return true;
			}
		}
		return false;
	}

	public boolean isRecursive() {
		return this.callers().contains(this);
	}

	public SourceRepresentation sourceCode() {
		return this.body;
	}
	
	@Override
	protected final String getNameSeparator() {
		return "#";
	}

	public int invocationsCount(SDMethod caller) {
		return callers.getMultiplicity(caller);
	}
	
	public boolean isConstructor() {
		return constructor;
	}

	// Builder methods
	
	public void addCaller(SDMethod method) {
		this.callers.add(method);
	}

	public void addOrigin(SDMethod method, int multiplicity) {
		this.origins.add(method, multiplicity);
	}

	public void setTestAnnotation(boolean testAnnotation) {
		this.testAnnotation = testAnnotation;
	}

	public void setDeprecatedAnnotation(boolean deprecatedAnnotation) {
		this.deprecatedAnnotation = deprecatedAnnotation;
	}

	public void setNumberOfStatements(int numberOfStatements) {
		this.numberOfStatements = numberOfStatements;
	}

	public void setSourceCode(SourceRepresentation body) {
		this.body = body;
	}


}
