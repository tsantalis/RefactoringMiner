package gr.uom.java.xmi;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.UsesDirective;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.LeafExpression;

public class UMLUsesModuleDirective extends UMLAbstractModuleDirective {

	public UMLUsesModuleDirective(CompilationUnit cu, String sourceFolder, String filePath, UsesDirective directive) {
		super(new LeafExpression(cu, sourceFolder, filePath, directive.getName(), CodeElementType.DIRECTIVE_NAME, null));
		this.locationInfo = new LocationInfo(cu, sourceFolder, filePath, directive, CodeElementType.USES_DIRECTIVE);
	}

}
