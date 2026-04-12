package gr.uom.java.xmi;

import java.util.List;

public interface AnnotationProvider extends LocationInfoProvider {
	List<UMLAnnotation> getAnnotations();

	default String getClassName() {
		if(this instanceof UMLOperation op)
			return op.getClassName();
		else if(this instanceof UMLAbstractClass umlClass)
			return umlClass.getName();
		else if(this instanceof UMLAttribute attr)
			return attr.getClassName();
		return null;
	}
}
