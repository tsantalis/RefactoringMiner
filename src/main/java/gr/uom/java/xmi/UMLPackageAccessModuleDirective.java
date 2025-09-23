package gr.uom.java.xmi;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExportsDirective;
import org.eclipse.jdt.core.dom.ModulePackageAccess;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.OpensDirective;
import org.eclipse.jdt.core.dom.ProvidesDirective;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.LeafExpression;

public class UMLPackageAccessModuleDirective extends UMLAbstractModuleDirective {
	private List<LeafExpression> modules;
	private boolean exports;
	private boolean opens;
	
	public UMLPackageAccessModuleDirective(CompilationUnit cu, String sourceFolder, String filePath, ModulePackageAccess directive) {
		super(new LeafExpression(cu, sourceFolder, filePath, directive.getName(), CodeElementType.DIRECTIVE_NAME, null));
		this.modules = new ArrayList<>();
		CodeElementType codeElementType = null;
		if(directive instanceof ExportsDirective) {
			exports = true;
			codeElementType = CodeElementType.EXPORTS_DIRECTIVE;
		}
		else if(directive instanceof OpensDirective) {
			opens = true;
			codeElementType = CodeElementType.OPENS_DIRECTIVE;
		}
		this.locationInfo = new LocationInfo(cu, sourceFolder, filePath, directive, codeElementType);
		
		List<Name> implementations = directive.modules();
		for(Name impl : implementations) {
			LeafExpression expr = new LeafExpression(cu, sourceFolder, filePath, impl, CodeElementType.IMPLEMENTATION_NAME, null);
			this.modules.add(expr);
		}
	}

	public List<LeafExpression> getModules() {
		return modules;
	}

	private List<String> getModulesAsStrings() {
		return modules.stream().map(i -> i.getString()).collect(Collectors.toList());
	}

	public boolean equalType(UMLPackageAccessModuleDirective other) {
		return this.exports == other.exports && this.opens == other.opens;
	}

	public boolean equalModules(UMLPackageAccessModuleDirective other) {
		return this.getModulesAsStrings().equals(other.getModulesAsStrings());
	}

	@Override
	public int hashCode() {
		return super.hashCode() + Objects.hash(exports, opens);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UMLPackageAccessModuleDirective other = (UMLPackageAccessModuleDirective) obj;
		return super.equals(obj) && Objects.equals(exports, other.exports) && Objects.equals(opens, other.opens);
	}

}
