package gr.uom.java.xmi;

public class UMLRecordComponent extends UMLAttribute {

	public UMLRecordComponent(String name, UMLType type, LocationInfo locationInfo, String className) {
		super(name, type, locationInfo, className);
		setVisibility(Visibility.PRIVATE);
		setFinal(true);
	}
}
