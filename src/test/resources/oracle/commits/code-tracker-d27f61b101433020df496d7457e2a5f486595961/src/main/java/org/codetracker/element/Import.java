package org.codetracker.element;

import org.codetracker.api.Version;
import org.codetracker.util.Util;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.UMLAbstractClass;
import gr.uom.java.xmi.UMLImport;

public class Import extends BaseCodeElement {
	private final UMLImport umlImport;
	private final UMLAbstractClass clazz;
	
	public Import(UMLImport umlImport, UMLAbstractClass clazz, String identifierIgnoringVersion, String name, String filePath, Version version) {
		super(identifierIgnoringVersion, name, filePath, version);
		this.umlImport = umlImport;
		this.clazz = clazz;
	}

    public BaseCodeElement of(Version version) {
    	return of(this.umlImport, this.clazz,  version);
    }

    public UMLImport getUmlImport() {
		return umlImport;
	}

	public UMLAbstractClass getClazz() {
		return clazz;
	}

	public static Import of(UMLImport umlImport, UMLAbstractClass clazz, Version version) {
        Class c = Class.of(clazz, version);
        return of(umlImport, c);
    }

	public static Import of(UMLImport umlImport, Class clazz) {
		LocationInfo importLocationInfo = umlImport.getLocationInfo();
		String statementType = importLocationInfo.getCodeElementType().name();
		String name = String.format("%s$%s(%d-%d)", clazz.getName(), statementType, importLocationInfo.getStartLine(), importLocationInfo.getEndLine());
		String sha512 = Util.getSHA512(umlImport.getName());
		String identifierExcludeVersion = String.format(
				"%s$%s:{%s}",
				clazz.getIdentifierIgnoringVersion(),
				statementType,
				sha512
				);
		return new Import(umlImport, clazz.getUmlClass(), identifierExcludeVersion, name, importLocationInfo.getFilePath(), clazz.getVersion());
	}

	@Override
	public LocationInfo getLocation() {
		return umlImport.getLocationInfo();
	}
}
