package br.ufmg.dcc.labsoft.refdetector.model;

public class SDMethod extends SDEntity {

	private String signature;
	private boolean testAnnotation;
	private boolean deprecatedAnnotation;
	private int numberOfStatements;
	private EntitySet<SDMethod> callers;
	private EntitySet<SDMethod> origins;

	public SDMethod(SDModel.Snapshot snapshot, int id, String signature, SDContainerEntity container) {
		super(snapshot, id, container.fullName() + "#" + signature, container);
		this.signature = signature;
		this.callers = new EntitySet<SDMethod>();
		this.origins = new EntitySet<SDMethod>();
	}

	public EntitySet<SDMethod> callers() {
		return this.callers;
	}

	public EntitySet<SDMethod> origins() {
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

	// Builder methods
	
	public void addCaller(SDMethod method) {
		this.callers.add(method);
	}

	public void addOrigin(SDMethod method) {
		this.origins.add(method);
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

}
