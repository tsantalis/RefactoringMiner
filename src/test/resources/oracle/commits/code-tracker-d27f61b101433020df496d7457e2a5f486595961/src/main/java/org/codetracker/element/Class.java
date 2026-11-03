package org.codetracker.element;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.UMLAbstractClass;
import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLComment;
import gr.uom.java.xmi.UMLImport;
import gr.uom.java.xmi.UMLJavadoc;
import gr.uom.java.xmi.UMLPackage;

import org.codetracker.api.Version;

import static org.codetracker.util.Util.annotationsToString;

import java.util.Optional;
import java.util.function.Predicate;

public class Class extends BaseCodeElement {
    private final UMLAbstractClass umlClass;
    private final String identifierIgnoringVersionAndAnnotation;
    private final String identifierIgnoringVersionAndModifiers;

    private Class(UMLAbstractClass umlClass, String identifierExcludeVersion, String identifierExcludeVersionAndAnnotation, String identifierIgnoringVersionAndModifiers, String name, String filePath, Version version) {
        super(identifierExcludeVersion, name, filePath, version);
        this.umlClass = umlClass;
        this.identifierIgnoringVersionAndAnnotation = identifierExcludeVersionAndAnnotation;
        this.identifierIgnoringVersionAndModifiers = identifierIgnoringVersionAndModifiers;
    }

    public BaseCodeElement of(Version version) {
    	return of(this.umlClass, version);
    }

    public static Class of(UMLAbstractClass umlClass, Version version) {
        String sourceFolder = umlClass.getLocationInfo().getSourceFolder();
        String packageName = umlClass.getPackageName();
        String name = umlClass.getName().replace(umlClass.getPackageName(), "").replace(".", "");
        String modifiersString = new ModifiersBuilder()
                .isFinal(umlClass.isFinal())
                .isStatic(umlClass.isStatic())
                .isAbstract(umlClass.isAbstract())
                .build();
        String visibility = umlClass.getVisibility().toString();
        String identifierExcludeVersion = String.format("%s%s.(%s)%s(%s)%s%s", sourceFolder, packageName, visibility, modifiersString, umlClass.getTypeDeclarationKind(), name, annotationsToString(umlClass.getAnnotations()));
        String identifierExcludeVersionAndAnnotation = String.format("%s%s.(%s)%s(%s)%s", sourceFolder, packageName, visibility, modifiersString, umlClass.getTypeDeclarationKind(), name);
        String identifierExcludeVersionAndModifiers = String.format("%s%s.(%s)%s%s", sourceFolder, packageName, umlClass.getTypeDeclarationKind(), name, annotationsToString(umlClass.getAnnotations()));
        String className = String.format("%s%s.(%s)%s%s(%d)", sourceFolder, packageName, visibility, modifiersString, name, umlClass.getLocationInfo().getStartLine());
        return new Class(umlClass, identifierExcludeVersion, identifierExcludeVersionAndAnnotation, identifierExcludeVersionAndModifiers, className, umlClass.getLocationInfo().getFilePath(), version);
    }

    public boolean differInFormatting(Class other) {
    	if (umlClass instanceof UMLClass && other.umlClass instanceof UMLClass) {
	    	String thisSignature = ((UMLClass) umlClass).getActualSignature();
			String otherSignature = ((UMLClass) other.umlClass).getActualSignature();
			if (thisSignature != null && otherSignature != null) {
	    		return !thisSignature.equals(otherSignature) && thisSignature.replaceAll("\\s+","").equals(otherSignature.replaceAll("\\s+",""));
	    	}
    	}
    	return false;
    }

	public int signatureStartLine() {
		int classSignatureStartLine = -1;
		if (umlClass instanceof UMLClass) {
			UMLClass clazz = (UMLClass) umlClass;
			if (clazz.getModifiers().size() > 0)
				classSignatureStartLine = clazz.getModifiers().get(0).getLocationInfo().getStartLine();
			else if (clazz.getTypeParameters().size() > 0)
				classSignatureStartLine = clazz.getTypeParameters().get(0).getLocationInfo().getStartLine();
			else if (clazz.getSuperclass() != null)
				classSignatureStartLine = clazz.getSuperclass().getLocationInfo().getStartLine();
		}
		return classSignatureStartLine;
	}

    public boolean isMultiLine() {
    	if (umlClass instanceof UMLClass && ((UMLClass)umlClass).getActualSignature() != null) {
    		return ((UMLClass)umlClass).getActualSignature().contains("\n");
    	}
    	return false;
    }

    public Package findPackage(Predicate<Package> equalOperator) {
    	if (umlClass instanceof UMLClass) {
        	Optional<UMLPackage> umlPackage = ((UMLClass) umlClass).getPackageDeclaration();
        	if (umlPackage.isPresent()) {
	        	Package pack = Package.of(umlPackage.get(), this);
	        	if (pack != null && equalOperator.test(pack)) {
	                return pack;
	            }
        	}
    	}
    	return null;
    }

    public Import findImport(Predicate<Import> equalOperator) {
    	for (UMLImport umlImport : umlClass.getImportedTypes()) {
            Import imp = Import.of(umlImport, this);
            if (imp != null && equalOperator.test(imp)) {
                return imp;
            }
        }
    	return null;
    }


    public Annotation findAnnotation(Predicate<Annotation> equalOperator) {
        for (UMLAnnotation umlAnnotation : umlClass.getAnnotations()) {
        	Annotation annotation = Annotation.of(umlAnnotation, this);
            if (annotation != null && equalOperator.test(annotation)) {
                return annotation;
            }
        }
        return null;
    }

    public Comment findComment(Predicate<Comment> equalOperator) {
        for (UMLComment umlComment : umlClass.getComments()) {
            Comment comment = Comment.of(umlComment, this);
            if (comment != null && equalOperator.test(comment)) {
                return comment;
            }
        }
        if (umlClass instanceof UMLClass) {
        	UMLJavadoc javadoc = ((UMLClass) umlClass).getJavadoc();
        	if (javadoc != null) {
        		Comment comment = Comment.of(javadoc, this);
        		if (comment != null && equalOperator.test(comment)) {
                    return comment;
                }
        	}
        	UMLJavadoc packageJavadoc = ((UMLClass) umlClass).getPackageDeclarationJavadoc();
        	if (packageJavadoc != null) {
        		Comment comment = Comment.of(packageJavadoc, this);
        		if (comment != null && equalOperator.test(comment)) {
                    return comment;
                }
        	}
        	for (UMLComment umlComment : ((UMLClass) umlClass).getPackageDeclarationComments()) {
                Comment comment = Comment.of(umlComment, this);
                if (comment != null && equalOperator.test(comment)) {
                    return comment;
                }
            }
        }
        return null;
    }

    public UMLAbstractClass getUmlClass() {
        return umlClass;
    }

    public boolean equalIdentifierIgnoringVersionAndAnnotation(Class clazz) {
        return this.identifierIgnoringVersionAndAnnotation.equals(clazz.identifierIgnoringVersionAndAnnotation);
    }

    public boolean equalIdentifierIgnoringVersionAndModifiers(Class clazz) {
        return this.identifierIgnoringVersionAndModifiers.equals(clazz.identifierIgnoringVersionAndModifiers);
    }

	public void checkClosingBracket(int lineNumber) {
		if (getLocation().getEndLine() == lineNumber) {
			setClosingCurlyBracket(true);
		}
	}

    @Override
    public LocationInfo getLocation() {
        return umlClass.getLocationInfo();
    }
}
