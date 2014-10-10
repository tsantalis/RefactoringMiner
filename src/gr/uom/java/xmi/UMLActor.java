package gr.uom.java.xmi;

import java.util.ArrayList;
import java.util.List;

public class UMLActor implements Comparable<UMLActor> {
	private String name;
    private String xmiID;
    private boolean isAbstract;
    private List<String> generalizationXmiIdList;
    private List<UMLGeneralization> generalizationList;
    private List<String> dependencyXmiIdList;
    private List<UMLDependency> dependencyList;

    public UMLActor(String name, String xmiID) {
    	this.name = name;
        this.xmiID = xmiID;
        this.isAbstract = false;
        this.generalizationXmiIdList = new ArrayList<String>();
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
    	
    	if(o instanceof UMLActor) {
    		UMLActor umlActor = (UMLActor)o;
    		return this.name.equals(umlActor.name);
    	}
    	return false;
    }

    public String toString() {
    	return getName();
    }

	public int compareTo(UMLActor umlActor) {
		return this.toString().compareTo(umlActor.toString());
	}
}
