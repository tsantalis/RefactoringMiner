package gr.uom.java.xmi;

import com.intellij.psi.*;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static gr.uom.java.xmi.decomposition.PsiUtils.findFirstChildOfType;

public class UMLTypePsiParser {
    private UMLTypePsiParser() {}

    /**
     * Construct UMLType from Psi type and typeElement
     *
     * @param typeElement Element associated with type declaration position
     * @param type        Real type (differs from typeElement.getType() on C-style arrays)
     */
    public static UMLType extractTypeObject(PsiFile file, String filePath, PsiTypeElement typeElement, PsiType type) {
        UMLType umlType = extractType(file, filePath, typeElement, type);
        umlType.locationInfo = new LocationInfo(file, filePath, typeElement, LocationInfo.CodeElementType.TYPE);
        addAnnotations(file, filePath, typeElement, umlType);
        return umlType;
    }

    public static UMLType extractTypeObject(PsiFile file, String filePath, PsiTypeElement typeElement) {
        return extractTypeObject(file, filePath, typeElement, typeElement.getType());
    }

    /**
     * Extract Class type with optional array dimensions
     */
    public static UMLType extractTypeObject(PsiFile file, String filePath, PsiJavaCodeReferenceElement typeElement, PsiType type) {
        UMLType umlType = extractTypeObject(file, filePath, typeElement);
        if (type instanceof PsiArrayType) {
            umlType.arrayDimension = type.getArrayDimensions();
        }
        return umlType;
    }

    /**
     * Extract Class type without array dimensions
     */
    public static UMLType extractTypeObject(PsiFile file, String filePath, PsiJavaCodeReferenceElement typeElement) {
        List<Reference> referenceElements = parseReferences(file, filePath, typeElement);
        StringBuilder packageStr = new StringBuilder();
        int position = 0;
        UMLType processedType = null;
        for (; position < referenceElements.size(); position++) {
            Reference referenceElement = referenceElements.get(position);
            if (packageStr.length() != 0) {
                packageStr.append('.');
            }
            packageStr.append(referenceElement.identifier);
            if (referenceElement.generics != null || position + 1 == referenceElements.size()) {
                processedType = new LeafType(packageStr.toString());
                processedType.locationInfo = referenceElement.locationInfo;
                if (referenceElement.generics != null)
                    processedType.typeArguments = referenceElement.generics;
                position++;
                break;
            }
        }
        for (; position < referenceElements.size(); position++) {
            Reference referenceElement = referenceElements.get(position);
            LeafType lastType = new LeafType(referenceElement.identifier);
            if (referenceElement.generics != null) {
                lastType.typeArguments = referenceElement.generics;
                lastType.locationInfo = referenceElement.locationInfo;
            }
            processedType = new CompositeType(processedType, lastType);
        }
        return processedType;
    }

    /**
     * Extract type for primitives and array of primitives
     */
    public static LeafType extractTypeObject(PsiFile file, String filePath, PsiKeyword typeKeyword) {
        LeafType umlType = new LeafType(Formatter.format(typeKeyword));
        umlType.locationInfo = new LocationInfo(file, filePath, typeKeyword, LocationInfo.CodeElementType.TYPE);
        umlType.arrayDimension = TypeUtils.arrayDimensions(typeKeyword);
        Arrays.stream(typeKeyword.getParent().getChildren())
            .filter(element -> element instanceof PsiAnnotation)
            .map(annotation -> new UMLAnnotation(file, filePath, (PsiAnnotation) annotation))
            .forEach(umlType.annotations::add);
        return umlType;
    }

    public static LeafType extractVarType(PsiFile file, String filePath, PsiTypeElement varElement) {
        LeafType umlType = new LeafType("var");
        umlType.locationInfo = new LocationInfo(file, filePath, varElement, LocationInfo.CodeElementType.TYPE);
        addAnnotations(file, filePath, varElement, umlType);
        return umlType;
    }

    private static void addAnnotations(PsiFile file, String filePath, PsiTypeElement typeElement, UMLType umlType) {
        PsiModifierList modifierList = getPrecedingModifiersList(typeElement);
        if (modifierList != null) {
            Arrays.stream(modifierList.getChildren())
                .filter(element -> element instanceof PsiAnnotation)
                .map(annotation -> new UMLAnnotation(file, filePath, (PsiAnnotation) annotation))
                .forEach(umlType.annotations::add);
        }
    }

    private static PsiModifierList getPrecedingModifiersList(PsiTypeElement typeElement) {
        PsiElement previousModifier = typeElement;
        while (previousModifier != null) {
            previousModifier = previousModifier.getPrevSibling();
            if (previousModifier instanceof PsiModifierList) {
                return (PsiModifierList) previousModifier;
            }
        }
        return null;
    }

    private static UMLType extractType(PsiFile file, String filePath, PsiTypeElement typeElement, PsiType type) {
        if (type instanceof PsiDisjunctionType) {
            List<UMLType> umlTypes = Arrays.stream(typeElement.getChildren())
                .filter(element -> element instanceof PsiTypeElement)
                .map(element -> (PsiTypeElement) element)
                .map(dTypeElement -> extractTypeObject(file, filePath, dTypeElement))
                .collect(Collectors.toList());
            return new ListCompositeType(umlTypes, ListCompositeType.Kind.UNION);
        } else if (type instanceof PsiWildcardType) {
            PsiWildcardType wildcardType = (PsiWildcardType) type;
            if (wildcardType.isBounded()) {
                PsiTypeElement bound = (PsiTypeElement) typeElement.getLastChild();
                return new WildcardType(extractTypeObject(file, filePath, bound,
                    wildcardType.getBound()), !wildcardType.isSuper());
            } else {
                return new WildcardType(null, true);
            }
        } else if (type instanceof PsiEllipsisType) {
            typeElement = findFirstChildOfType(typeElement, PsiTypeElement.class);
            UMLType ellipsisType = extractTypeObject(file, filePath, typeElement);
            ellipsisType.arrayDimension++;
            return ellipsisType;
        } else if (type instanceof PsiArrayType) {
            if (typeElement.getType() instanceof PsiArrayType) {
                typeElement = findFirstChildOfType(typeElement, PsiTypeElement.class);
            }
            UMLType arrayType = extractTypeObject(file, filePath, typeElement);
            arrayType.arrayDimension = type.getArrayDimensions();
            return arrayType;
        } else {
            PsiJavaCodeReferenceElement typeReference =
                findFirstChildOfType(typeElement, PsiJavaCodeReferenceElement.class);
            if (typeReference != null) {
                return extractTypeObject(file, filePath, typeReference);
            }
            PsiKeyword typeKeyword = findFirstChildOfType(typeElement, PsiKeyword.class);
            return new LeafType(Formatter.format(typeKeyword));
        }
    }

    private static List<Reference> parseReferences(PsiFile file, String filePath, PsiJavaCodeReferenceElement typeReference) {
        PsiJavaCodeReferenceElement reference = findFirstChildOfType(typeReference, PsiJavaCodeReferenceElement.class);
        List<Reference> previousReferences;
        if (reference == null) {
            previousReferences = new ArrayList<>();
        } else {
            previousReferences = parseReferences(file, filePath, reference);
        }

        String identifier = Formatter.format(findFirstChildOfType(typeReference, PsiIdentifier.class));

        PsiReferenceParameterList typeParameters = findFirstChildOfType(typeReference, PsiReferenceParameterList.class);
        List<UMLType> generics = parseGenericTypes(file, filePath, typeParameters);

        LocationInfo locationInfo = new LocationInfo(file, filePath, typeReference, LocationInfo.CodeElementType.TYPE);

        Reference referenceElement = new Reference(identifier, generics, locationInfo);
        previousReferences.add(referenceElement);
        return previousReferences;
    }

    private static List<UMLType> parseGenericTypes(PsiFile file, String filePath,
                                                   @Nullable PsiReferenceParameterList genericTypes) {
        if (genericTypes == null || genericTypes.getFirstChild() == null) {
            return null;
        }
        PsiTypeElement[] types = genericTypes.getTypeParameterElements();
        if (types.length == 1 && types[0].getType() instanceof PsiDiamondType) {
            return Collections.emptyList();
        }
        return Arrays.stream(genericTypes.getTypeParameterElements())
            .map(type -> extractTypeObject(file, filePath, type))
            .collect(Collectors.toList());
    }

    private static class Reference {
        public final String identifier;
        public final List<UMLType> generics;
        public final LocationInfo locationInfo;

        public Reference(String identifier, List<UMLType> generics, LocationInfo locationInfo) {
            this.identifier = identifier;
            this.generics = generics;
            this.locationInfo = locationInfo;
        }
    }
}
