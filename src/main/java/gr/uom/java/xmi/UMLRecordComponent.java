package gr.uom.java.xmi;

public class UMLRecordComponent extends UMLAttribute {

	public UMLRecordComponent(String name, UMLType type, LocationInfo locationInfo) {
		super(name, type, locationInfo);
		setVisibility(Visibility.PRIVATE);
		setFinal(true);
	}
}
