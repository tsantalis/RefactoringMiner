package br.ufmg.dcc.labsoft.refdetector.model;

public class SDMethod extends SDEntity {

	private EntitySet<SDMethod> callers;
	private EntitySet<SDMethod> origins;
	
	private boolean testAnnotation;
	private boolean deprecatedAnnotation;
	private int numberOfStatements;

	public SDMethod(int id, String fullName, SDContainerEntity container) {
		super(id, fullName, container);
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
		// TODO Auto-generated method stub
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
