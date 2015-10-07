package br.ufmg.dcc.labsoft.refdetector.model;

public class SDMethod extends SDEntity {

	private EntitySet<SDMethod> callers;

	public SDMethod(int id, String fullName) {
		super(id, fullName);
		this.callers = new EntitySet<SDMethod>();
	}

	public EntitySet<SDMethod> callers() {
		return this.callers;
	}

	public boolean delegatesTo(SDMethod extractedMethod) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isDeprecated() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isTestCode() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isOverriden() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isRecursive() {
		// TODO Auto-generated method stub
		return false;
	}

	public void addCaller(SDMethod method) {
		this.callers.add(method);
	}

}
