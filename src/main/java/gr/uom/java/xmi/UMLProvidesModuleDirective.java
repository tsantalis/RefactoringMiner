package gr.uom.java.xmi;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ProvidesDirective;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.LeafExpression;

public class UMLProvidesModuleDirective extends UMLAbstractModuleDirective {
	private List<LeafExpression> implementations;
	
	public UMLProvidesModuleDirective(CompilationUnit cu, String sourceFolder, String filePath, ProvidesDirective directive) {
		super(new LeafExpression(cu, sourceFolder, filePath, directive.getName(), CodeElementType.DIRECTIVE_NAME, null));
		this.implementations = new ArrayList<>();
		this.locationInfo = new LocationInfo(cu, sourceFolder, filePath, directive, CodeElementType.PROVIDES_DIRECTIVE);
		
		List<Name> implementations = directive.implementations();
		for(Name impl : implementations) {
			LeafExpression expr = new LeafExpression(cu, sourceFolder, filePath, impl, CodeElementType.IMPLEMENTATION_NAME, null);
			this.implementations.add(expr);
		}
	}

	public List<LeafExpression> getImplementations() {
		return implementations;
	}

	private List<String> getImplementationsAsStrings() {
		return implementations.stream().map(i -> i.getString()).collect(Collectors.toList());
	}

	public boolean equalImplementations(UMLProvidesModuleDirective other) {
		return this.getImplementationsAsStrings().equals(other.getImplementationsAsStrings());
	}
}
