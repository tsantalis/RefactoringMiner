package gr.uom.java.xmi;

import java.util.ArrayList;
import java.util.List;

public class UMLUseCase implements Comparable<UMLUseCase> {
	private String name;
    private String xmiID;
    private boolean isAbstract;
    private List<String> extendXmiIdList;
    private List<String> includeXmiIdList;
    private List<String> generalizationXmiIdList;
    private List<UMLInclude> includeList;
    private List<UMLExtend> extendList;
    private List<UMLGeneralization> generalizationList;
    private List<String> dependencyXmiIdList;
    private List<UMLDependency> dependencyList;
    
    public UMLUseCase(String name, String xmiID) {
    	this.name = name;
        this.xmiID = xmiID;
        this.isAbstract = false;
        this.extendXmiIdList = new ArrayList<String>();
        this.includeXmiIdList = new ArrayList<String>();
        this.generalizationXmiIdList = new ArrayList<String>();
        this.includeList = new ArrayList<UMLInclude>();
        this.extendList = new ArrayList<UMLExtend>();
        this.generalizationList = new ArrayList<UMLGeneralization>();
        this.dependencyXmiIdList = new ArrayList<String>();
        this.dependencyList = new ArrayList<UMLDependency>();
    }

    public String getName() {
    	return name;
    }

    public String getXmiID() {
        return xmiID;
    }

	public boolean isAbstract() {
		return isAbstract;
	}

	public void setAbstract(boolean isAbstract) {
		this.isAbstract = isAbstract;
	}

	public void addExtendXmiID(String xmiID) {
		this.extendXmiIdList.add(xmiID);
	}

	public void addIncludeXmiID(String xmiID) {
		this.includeXmiIdList.add(xmiID);
	}

	public List<String> getExtendXmiIdList() {
		return extendXmiIdList;
	}

	public List<String> getIncludeXmiIdList() {
		return includeXmiIdList;
	}

	public void addInclude(UMLInclude include) {
		this.includeList.add(include);
	}

	public void addExtend(UMLExtend extend) {
		this.extendList.add(extend);
	}

	public void addGeneralizationXmiID(String xmiID) {
		this.generalizationXmiIdList.add(xmiID);
	}

    public List<String> getGeneralizationXmiIdList() {
		return generalizationXmiIdList;
	}

	public void addGeneralization(UMLGeneralization generalization) {
		this.generalizationList.add(generalization);
	}

	public void addDependencyXmiID(String xmiID) {
		this.dependencyXmiIdList.add(xmiID);
	}

    public List<String> getDependencyXmiIdList() {
		return dependencyXmiIdList;
	}

    public void addDependency(UMLDependency dependency) {
    	this.dependencyList.add(dependency);
    }

	public boolean equals(Object o) {
    	if(this == o) {
    		return true;
    	}
    	
    	if(o instanceof UMLUseCase) {
    		UMLUseCase umlUseCase = (UMLUseCase)o;
    		return this.name.equals(umlUseCase.name);
    	}
    	return false;
    }

    public String toString() {
    	return getName();
    }

	public int compareTo(UMLUseCase umlUseCase) {
		return this.toString().compareTo(umlUseCase.toString());
	}
}
