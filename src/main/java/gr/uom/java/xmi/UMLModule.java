package gr.uom.java.xmi;

import java.util.ArrayList;
import java.util.List;

public class UMLModule {
	private LocationInfo locationInfo;
	private boolean open;
	private String name;
	private List<UMLComment> comments;
	private List<UMLAnnotation> annotations;
	private UMLJavadoc javadoc;
	private List<UMLAbstractModuleDirective> directives;

	public UMLModule(String name, LocationInfo locationInfo) {
		this.name = name;
		this.locationInfo = locationInfo;
		this.comments = new ArrayList<>();
		this.annotations = new ArrayList<>();
		this.directives = new ArrayList<>();
	}

	public String getName() {
		return name;
	}

	public LocationInfo getLocationInfo() {
		return locationInfo;
	}

	public List<UMLAbstractModuleDirective> getDirectives() {
		return directives;
	}

	public boolean isOpen() {
		return open;
	}

	public void setOpen(boolean open) {
		this.open = open;
	}

	public UMLJavadoc getJavadoc() {
		return javadoc;
	}

	public void setJavadoc(UMLJavadoc javadoc) {
		this.javadoc = javadoc;
	}

	public List<UMLComment> getComments() {
		return comments;
	}

    public List<UMLAnnotation> getAnnotations() {
		return annotations;
	}

    public void addAnnotation(UMLAnnotation annotation) {
    	annotations.add(annotation);
    }

    public void addDirective(UMLAbstractModuleDirective directive) {
    	directives.add(directive);
    }
}
