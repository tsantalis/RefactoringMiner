package extension.umladapter;


import extension.ast.node.LangASTNode;
import extension.ast.node.TypeObjectEnum;
import extension.ast.node.declaration.LangMethodDeclaration;
import extension.ast.node.declaration.LangSingleVariableDeclaration;
import extension.ast.node.declaration.LangTypeDeclaration;
import extension.ast.node.expression.LangAssignment;
import extension.ast.node.expression.LangFieldAccess;
import extension.ast.node.expression.LangSimpleName;
import extension.ast.node.metadata.LangAnnotation;
import extension.ast.node.metadata.comment.LangComment;
import extension.ast.node.statement.LangBlock;
import extension.ast.node.statement.LangExpressionStatement;
import extension.ast.node.unit.LangCompilationUnit;
import extension.base.LangASTUtil;
import extension.base.LangSupportedEnum;
import extension.umladapter.processor.UMLAdapterVariableProcessor;
import gr.uom.java.xmi.*;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.OperationBody;
import gr.uom.java.xmi.decomposition.VariableDeclaration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.gen.treesitterng.PythonTreeSitterNgTreeGenerator;

import static extension.umladapter.UMLAdapterUtil.extractUMLImports;
import static extension.umladapter.processor.UMLAdapterVariableProcessor.processVariableDeclarations;

public class UMLModelAdapter {
    private final UMLModel umlModel;

    public UMLModelAdapter(Map<String, String> langSupportedFiles) throws IOException {
        this(langSupportedFiles, false);
    }

    public UMLModelAdapter(Map<String, String> langSupportedFiles, boolean astDiff) throws IOException {
        this.umlModel = new UMLModel(Collections.emptySet());
        // Parse files to custom AST
        Map<String, LangASTNode> langASTMap = parseLangSupportedFiles(langSupportedFiles, astDiff);

        // Create UML model directly from custom AST
        populateUMLModel(langASTMap, langSupportedFiles);
    }

    private static void distributeComments(List<UMLComment> compilationUnitComments, LocationInfo codeElementLocationInfo, List<UMLComment> codeElementComments) {
        ListIterator<UMLComment> listIterator = compilationUnitComments.listIterator(compilationUnitComments.size());
        while(listIterator.hasPrevious()) {
            UMLComment comment = listIterator.previous();
            LocationInfo commentLocationInfo = comment.getLocationInfo();
            if(codeElementLocationInfo.subsumes(commentLocationInfo) ||
                    codeElementLocationInfo.sameLine(commentLocationInfo) ||
                    (commentLocationInfo.startsAtTheEndLineOf(codeElementLocationInfo) && !codeElementLocationInfo.getCodeElementType().equals(CodeElementType.ANONYMOUS_CLASS_DECLARATION)) ||
                    (codeElementLocationInfo.nextLine(commentLocationInfo) && !codeElementLocationInfo.getCodeElementType().equals(CodeElementType.ANONYMOUS_CLASS_DECLARATION)) ||
                    (codeElementComments.size() > 0 && codeElementComments.get(0).getLocationInfo().nextLine(commentLocationInfo))) {
                codeElementComments.add(0, comment);
            }
            if(commentLocationInfo.nextLine(codeElementLocationInfo) || commentLocationInfo.rightAfterNextLine(codeElementLocationInfo)) {
                comment.addPreviousLocation(codeElementLocationInfo);
            }
        }
        compilationUnitComments.removeAll(codeElementComments);
    }

    private Map<String, LangASTNode> parseLangSupportedFiles(Map<String, String> langSupportedFiles, boolean astDiff) throws IOException {
        Map<String, LangASTNode> result = new HashMap<>();

        for (Map.Entry<String, String> entry : langSupportedFiles.entrySet()) {
            LangSupportedEnum language = LangSupportedEnum.fromFileName(entry.getKey());
            LangASTNode ast = LangASTUtil.getLangAST(
                    language, // fileName for language detection
                    entry.getValue()); // code content
           // System.out.print("AST Structure: " + ast.toString());
            result.put(entry.getKey(), ast);
            if (astDiff) {
                ByteArrayInputStream is = new ByteArrayInputStream(entry.getValue().getBytes());
                try {
                    TreeContext treeContext = new PythonTreeSitterNgTreeGenerator().generateFrom().stream(is);
                    List<Tree> trees = TreeUtilFunctions.findChildrenByTypeRecursively(treeContext.getRoot(), "comment");
                    List<UMLComment> comments = new ArrayList<UMLComment>();
                    for(Tree t : trees) {
                        String sourceFolder = UMLAdapterUtil.extractSourceFolder(entry.getKey());
                        LocationInfo location = new LocationInfo(sourceFolder, entry.getKey(), t, CodeElementType.LINE_COMMENT);
                        UMLComment comment = new UMLComment(t.getLabel(), location);
                        comments.add(comment);
                    }
                    this.umlModel.getCommentMap().put(entry.getKey(), comments);
                    this.umlModel.getTreeContextMap().put(entry.getKey(), treeContext);
                }
                catch(Exception e) {
                    
                }
            }
        }

        return result;
    }

    private void populateUMLModel(Map<String, LangASTNode> astMap, Map<String, String> langSupportedFiles) {
        // Process each AST and populate the UML model
        for (Map.Entry<String, LangASTNode> entry : astMap.entrySet()) {
            String filename = entry.getKey();
            LangASTNode ast = entry.getValue();

            // Extract UML entities from AST
            extractUMLEntities(ast, umlModel, filename, langSupportedFiles.get(filename));
        }
    }

    public static void extractUMLEntities(LangASTNode ast, UMLModel model, String filename, String fileContent) {
        if (ast instanceof LangCompilationUnit compilationUnit) {
            // Process imports
            List<UMLImport> imports = extractUMLImports(compilationUnit, filename);
            List<UMLComment> comments = model.getCommentMap().containsKey(filename) ? model.getCommentMap().get(filename) : new ArrayList<>();

            for (LangTypeDeclaration typeDecl : compilationUnit.getTypes()) {
                UMLClass umlClass = createUMLClass(typeDecl, filename, imports, fileContent, comments);
                if (!typeDecl.getSuperClassNames().isEmpty()) {
                    String packageName = UMLAdapterUtil.extractPackageName(filename);
                    LangSimpleName primarySuperClassRaw = typeDecl.getSuperClassNames().get(0);
                    String primarySuperClass = UMLAdapterUtil.resolveQualifiedTypeName(primarySuperClassRaw.getIdentifier(), imports, packageName);
                    model.addGeneralization(new UMLGeneralization(umlClass, primarySuperClass));
                    for (int i = 1; i < typeDecl.getSuperClassNames().size(); i++) {
                        LangSimpleName additionalSuperClassRaw = typeDecl.getSuperClassNames().get(i);
                        String additionalSuperClass = UMLAdapterUtil.resolveQualifiedTypeName(additionalSuperClassRaw.getIdentifier(), imports, packageName);
                        // Create additional generalization for multiple inheritance support
                        model.addGeneralization(new UMLGeneralization(umlClass, additionalSuperClass));
                    }
                }
                model.addClass(umlClass);
                if(umlClass.getContainer().isPresent()) {
                    for(UMLClass nestedClass : umlClass.getContainer().get().getNestedClasses()) {
                        model.addClass(nestedClass);
                    }
                }
            }

            // Handle top level methods
            if (compilationUnit.getMethods().size() > 0 || compilationUnit.getComments().size() > 0 || comments.size() > 0 ||
                    compilationUnit.getImports().size() > 0 || compilationUnit.getStatements().size() > 0){
                handleTopLevelMethods(model, filename, compilationUnit, imports, fileContent, comments);
            }
        }
    }

    private static void handleTopLevelMethods(UMLModel model, String filename, LangCompilationUnit compilationUnit, List<UMLImport> imports, String fileContent, List<UMLComment> comments) {
        List<LangMethodDeclaration> topLevelMethods = compilationUnit.getMethods();
        UMLClass moduleClass = createModuleClass(compilationUnit, filename, imports, fileContent);

        moduleClass.setActualSignature(moduleClass.getName());
        moduleClass.setVisibility(Visibility.PUBLIC);
        moduleClass.setAbstract(false);
        moduleClass.setInterface(false);
        moduleClass.setFinal(false);
        moduleClass.setStatic(true);
        moduleClass.setAnnotation(false);
        moduleClass.setEnum(false);
        moduleClass.setRecord(false);

        if (!topLevelMethods.isEmpty()) {
            String sourceFolder = UMLAdapterUtil.extractSourceFolder(filename);
            String filepath = UMLAdapterUtil.extractFilePath(filename);
            for (LangMethodDeclaration method : topLevelMethods) {
                UMLOperation operation = createUMLOperation(method, moduleClass.getName(),
                        sourceFolder, filepath, fileContent, comments, convertToVariableDeclarationMap(moduleClass.getFieldDeclarationMap().values()));
                moduleClass.addOperation(operation);
            }
        }
        distributeComments(comments, moduleClass.getLocationInfo(), moduleClass.getComments());
        //if there are still comments left, add them to module
        if(comments.size() > 0)
            moduleClass.getComments().addAll(comments);
        model.addClass(moduleClass);
        if(moduleClass.getContainer().isPresent()) {
            for(UMLClass nestedClass : moduleClass.getContainer().get().getNestedClasses()) {
                model.addClass(nestedClass);
            }
        }
    }

    private static UMLClass createModuleClass(LangCompilationUnit compilationUnit, String filename, List<UMLImport> imports, String fileContent) {
        String moduleName = UMLAdapterUtil.extractModuleName(filename);
        String sourceFolder = UMLAdapterUtil.extractSourceFolder(filename);
        String filepath = UMLAdapterUtil.extractFilePath(filename);

        LocationInfo locationInfo = new LocationInfo(sourceFolder, filepath, compilationUnit,
                LocationInfo.CodeElementType.TYPE_DECLARATION);

        UMLClass moduleClass = new UMLClass(moduleName, "__module__", locationInfo, true, imports);
        moduleClass.setModule(true);
        moduleClass.setStatic(true);
        // Handle module-scope assignments as attributes
        for (LangAssignment moduleLevelAssignment: compilationUnit.getModuleLevelAssignments()){
            processClassLevelAssignmentForAttribute(moduleClass, moduleLevelAssignment, sourceFolder, filepath, fileContent);
        }
        if (compilationUnit.getStatements().size() > 0) {
            ModuleContainer moduleContainer = new ModuleContainer(locationInfo, moduleClass.getName());
            OperationBody opBody = new OperationBody(
                    compilationUnit,
                    sourceFolder,
                    filepath,
                    compilationUnit.getStatements(),
                    moduleContainer,
                    convertToVariableDeclarationMap(moduleClass.getFieldDeclarationMap().values()),
                    fileContent
            );
            moduleContainer.addStatements(opBody.getCompositeStatement().getStatements());
            moduleClass.setContainer(moduleContainer);
        }
        // add compilation unit comments to moduleClass
        for (LangComment compilationUnitLevelComment : compilationUnit.getComments()) {
            UMLComment comment = createUMLComment(compilationUnitLevelComment, compilationUnit, sourceFolder, filepath);
            if (comment != null)
                moduleClass.getComments().add(comment);
        }

        return moduleClass;
    }

    public static UMLClass createUMLClass(LangTypeDeclaration typeDecl, String filename, List<UMLImport> imports, String fileContent, List<UMLComment> comments) {
        String className = typeDecl.getName();
        String moduleName = UMLAdapterUtil.extractModuleName(filename);
        String sourceFolder = UMLAdapterUtil.extractSourceFolder(filename);
        String filepath = UMLAdapterUtil.extractFilePath(filename);

        LocationInfo locationInfo = new LocationInfo(sourceFolder,
                filepath,
                typeDecl,
                LocationInfo.CodeElementType.TYPE_DECLARATION);

        //handle nested type declarations
        LangASTNode current = typeDecl;
        List<String> parentClassNames = new ArrayList<>();
        while(current.getParent() instanceof LangTypeDeclaration parentTypeDecl) {
            parentClassNames.add(0, parentTypeDecl.getName());
            current = current.getParent();
        }
        for(String s : parentClassNames) {
            moduleName = moduleName + "." + s;
        }
        UMLClass umlClass = new UMLClass(moduleName, className, locationInfo, typeDecl.isTopLevel(), imports);

        for (LangAnnotation langAnnotation : typeDecl.getAnnotations()) {
            umlClass.addAnnotation(new UMLAnnotation(
                    typeDecl.getRootCompilationUnit(),
                    sourceFolder,
                    filepath,
                    langAnnotation,
                    fileContent));
        }

        if (!typeDecl.getSuperClassNames().isEmpty()) {
            // Qualify and set the first superclass as the main superclass
            LangSimpleName primarySuperClassRaw = typeDecl.getSuperClassNames().get(0);
            LocationInfo superTypeLocationInfo = new LocationInfo(sourceFolder, filepath, primarySuperClassRaw, LocationInfo.CodeElementType.TYPE);
            UMLType superClassType = UMLType.extractTypeObject(primarySuperClassRaw.getIdentifier(), "[", "]", superTypeLocationInfo);
            umlClass.setSuperclass(superClassType);

            // For additional base classes, also add as generalizations (Python multiple inheritance)
            for (int i = 1; i < typeDecl.getSuperClassNames().size(); i++) {
                LangSimpleName additionalSuperClassRaw = typeDecl.getSuperClassNames().get(i);
                LocationInfo additionalSuperTypeLocationInfo = new LocationInfo(sourceFolder, filepath, additionalSuperClassRaw, LocationInfo.CodeElementType.TYPE);
                UMLType additionalSuperClassType = UMLType.extractTypeObject(additionalSuperClassRaw.getIdentifier(), "[", "]", additionalSuperTypeLocationInfo);
                umlClass.addImplementedInterface(additionalSuperClassType);
            }
        }

      //  storeClassHierarchyInfo(umlClass, typeDecl, model, packageName, imports);

        // Handle class-scope assignments as attributes
        for (LangAssignment classLevelAssignment: typeDecl.getClassLevelAssignments()){
            processClassLevelAssignmentForAttribute(umlClass, classLevelAssignment, sourceFolder, filepath, fileContent);
        }
        for (LangComment classLevelComment: typeDecl.getComments()) {
            UMLComment comment = createUMLComment(classLevelComment, typeDecl.getRootCompilationUnit(), sourceFolder, filepath);
            if (comment != null)
                umlClass.getComments().add(comment);
        }

        // Setters
        umlClass.setActualSignature(typeDecl.getActualSignature());
        umlClass.setVisibility(typeDecl.getVisibility());
        umlClass.setAbstract(typeDecl.isAbstract());
        umlClass.setInterface(typeDecl.isInterface());
        umlClass.setFinal(typeDecl.isFinal());
        umlClass.setStatic(typeDecl.isStatic());
        umlClass.setAnnotation(typeDecl.isAnnotation());
        umlClass.setEnum(typeDecl.isEnum());
        umlClass.setRecord(typeDecl.isRecord());

        for (LangMethodDeclaration methodDecl : typeDecl.getMethods()) {
            UMLOperation umlOperation = createUMLOperation(methodDecl, umlClass.getName(), sourceFolder, filepath, fileContent, comments, convertToVariableDeclarationMap(umlClass.getFieldDeclarationMap().values()));
            umlClass.addOperation(umlOperation);
            if ("__init__".equals(methodDecl.getName()) || "_build".equals(methodDecl.getName())) {
                List<UMLAttribute> attributes = getAttributes(methodDecl, umlClass.getName(), sourceFolder, filepath, umlOperation, fileContent);
                for (UMLAttribute attribute : attributes) {
                    // avoid adding the attribute again, if it has been already created by processing a class-level assignment
                    if(!umlClass.getAttributes().contains(attribute)) {
                        umlClass.addAttribute(attribute);
                    }
                }
            }
        }
        if (typeDecl.getStatements().size() > 0) {
            ModuleContainer moduleContainer = new ModuleContainer(locationInfo, typeDecl.getName());
            OperationBody opBody = new OperationBody(
                    typeDecl.getRootCompilationUnit(),
                    sourceFolder,
                    filepath,
                    typeDecl.getStatements(),
                    moduleContainer,
                    convertToVariableDeclarationMap(umlClass.getFieldDeclarationMap().values()),
                    fileContent
            );
            moduleContainer.addStatements(opBody.getCompositeStatement().getStatements());
            umlClass.setContainer(moduleContainer);
        }
        distributeComments(comments, locationInfo, umlClass.getComments());
        return umlClass;
    }

    public static UMLOperation createUMLOperation(LangMethodDeclaration methodDecl, String className, String sourceFolder, String filePath, String fileContent, List<UMLComment> comments, Map<String, Set<VariableDeclaration>> activeVariableDeclarations) {
        int startSignatureOffset = methodDecl.getStartChar();
        LocationInfo locationInfo = new LocationInfo(sourceFolder, filePath, methodDecl, LocationInfo.CodeElementType.METHOD_DECLARATION);
        LangSupportedEnum language = LangSupportedEnum.fromFileName(filePath);
        String operationName = methodDecl.getName();
        UMLOperation umlOperation = new UMLOperation(operationName, locationInfo, className);

        // Convert to UMLAnnotations
        for (LangAnnotation langAnnotation : methodDecl.getAnnotations()) {
            umlOperation.addAnnotation(new UMLAnnotation(
                    methodDecl.getRootCompilationUnit(),
                    sourceFolder,
                    filePath,
                    langAnnotation,
                    fileContent));
        }

        List<LangSingleVariableDeclaration> params = methodDecl.getParameters();
        List<String> parameterNames = new ArrayList<>();

        int paramOffset = UMLAdapterUtil.getParamOffset(methodDecl, params, language);

        for (int i = paramOffset; i < params.size(); i++) {
            LangSingleVariableDeclaration param = params.get(i);
            UMLType typeObject = UMLType.extractTypeObject("Object");
            LocationInfo paramLocationInfo = new LocationInfo(sourceFolder, filePath, param, LocationInfo.CodeElementType.TYPE);
            if (LangSupportedEnum.PYTHON.equals(language)) {
                if (param.getTypeAnnotation() != null) {
                    typeObject = UMLType.extractTypeObject(param.getTypeAnnotation().getName(), "[", "]", paramLocationInfo);
                }
            } else {
                if (param.getTypeAnnotation() != null) {
                    String typeName = param.getTypeAnnotation().getName();
                    if (typeName != null && !typeName.isEmpty()) {
                        typeObject = UMLType.extractTypeObject(typeName, "[", "]", paramLocationInfo);
                    }
                }
            }

            UMLParameter umlParam = new UMLParameter(param.getLangSimpleName().getIdentifier(), typeObject, "in", param.isVarArgs());
            processVariableDeclarations(param, umlParam, typeObject, sourceFolder, filePath, umlOperation, fileContent);
            umlOperation.addParameter(umlParam);
            parameterNames.add(param.getLangSimpleName().getIdentifier());
        }

        umlOperation.setFinal(methodDecl.isFinal());
        umlOperation.setStatic(methodDecl.isStatic());
        umlOperation.setConstructor(methodDecl.isConstructor());
        umlOperation.setVisibility(methodDecl.getVisibility());
        umlOperation.setAbstract(methodDecl.isAbstract());
        umlOperation.setNative(methodDecl.isNative());
        umlOperation.setSynchronized(methodDecl.isSynchronized());

        processComments(methodDecl, sourceFolder, filePath, umlOperation);

        UMLType returnType;
        LocationInfo returnTypeLocationInfo = new LocationInfo(sourceFolder, filePath, methodDecl, LocationInfo.CodeElementType.TYPE);
        if (LangSupportedEnum.PYTHON.equals(language)){
            returnType = UMLType.extractTypeObject(methodDecl.getReturnTypeAnnotation(), "[", "]", returnTypeLocationInfo);
        } else {
            String resolvedReturnType = methodDecl.getReturnTypeAnnotation();
            if (resolvedReturnType == null || resolvedReturnType.isEmpty()) {
                resolvedReturnType = TypeObjectEnum.VOID.name();
            }
            returnType = UMLType.extractTypeObject(resolvedReturnType, "[", "]", returnTypeLocationInfo);
            if (methodDecl.getReturnTypeAnnotation() == null) {
                methodDecl.setReturnTypeAnnotation(resolvedReturnType);
            }
        }
        String returnTypeString = methodDecl.getReturnTypeAnnotation() != null ? methodDecl.getReturnTypeAnnotation() : "void";
        if (!(TypeObjectEnum.VOID.name().equals(returnTypeString))) {
            UMLParameter returnParam = new UMLParameter("", returnType, "return", false);
            umlOperation.addParameter(returnParam);
        }


        OperationBody opBody = new OperationBody(
                methodDecl.getRootCompilationUnit(),
                sourceFolder,
                filePath,
                methodDecl.getBody(),
                umlOperation,
                activeVariableDeclarations,
                fileContent
        );

        umlOperation.setBody(opBody);
        //logUMLOperation(umlOperation, methodDecl);
        int endSignatureOffset = methodDecl.getBody() != null ?
                methodDecl.getBody().getStartChar() + 1 :
                methodDecl.getStartChar() + methodDecl.getLength();
        String text = fileContent.substring(startSignatureOffset, endSignatureOffset);
        umlOperation.setActualSignature(text);
        distributeComments(comments, locationInfo, umlOperation.getComments());
        return umlOperation;
    }

    private static Map<String, Set<VariableDeclaration>> convertToVariableDeclarationMap(Collection<VariableDeclaration> allVariableDeclarations) {
        Map<String, Set<VariableDeclaration>> variableDeclarationMap = new LinkedHashMap<String, Set<VariableDeclaration>>();
        for(VariableDeclaration declaration : allVariableDeclarations) {
            if(variableDeclarationMap.containsKey(declaration.getVariableName())) {
                variableDeclarationMap.get(declaration.getVariableName()).add(declaration);
            }
            else {
                Set<VariableDeclaration> variableDeclarations = new LinkedHashSet<VariableDeclaration>();
                variableDeclarations.add(declaration);
                variableDeclarationMap.put(declaration.getVariableName(), variableDeclarations);
            }
        }
        return variableDeclarationMap;
    }

    private static List<UMLAttribute> getAttributes(LangMethodDeclaration methodDecl, String className, String sourceFolder, String filePath, UMLOperation umlOperation, String fileContent) {
        List<UMLAttribute> attributes = new ArrayList<>();

        // Only process __init__ method for attribute extraction
        if (!"__init__".equals(methodDecl.getName()) && !"_build".equals(methodDecl.getName())) {
            return attributes;
        }

        LangBlock methodBody = methodDecl.getBody();
        if (methodBody == null) {
            return attributes;
        }

        if (methodBody.getStatements() != null) {
            for (LangASTNode statement : methodBody.getStatements()) {
                // Handle direct assignments
                if (statement instanceof LangAssignment assignment) {
                    processAssignmentForAttribute(methodDecl, assignment, className, attributes, sourceFolder, filePath, umlOperation, fileContent);
                }
                // Handle expression statements that contain assignments
                else if (statement instanceof LangExpressionStatement exprStmt) {
                    if (exprStmt.getExpression() instanceof LangAssignment assignment) {
                        processAssignmentForAttribute(methodDecl, assignment, className, attributes, sourceFolder, filePath, umlOperation, fileContent);
                    }
                }
            }
        }

        return attributes;
    }

    private static void processAssignmentForAttribute(LangMethodDeclaration methodDeclaration, LangAssignment assignment, String className, List<UMLAttribute> attributes,
                                               String sourceFolder, String filePath, UMLOperation umlOperation, String fileContent) {
        LangASTNode leftSide = assignment.getLeftSide();

        if (leftSide instanceof LangFieldAccess langFieldAccess) {
            LangASTNode expression = langFieldAccess.getExpression();

            // Check if it's self.attribute
            if (expression instanceof LangSimpleName simpleName) {
                if ("self".equals(simpleName.getIdentifier())) {
                    String attributeName = langFieldAccess.getName().getIdentifier();

                    // Create VariableDeclaration for the attribute using the new constructor
                    VariableDeclaration variableDeclaration = UMLAdapterVariableProcessor.processAttributeAssignment(
                            assignment,
                            sourceFolder,
                            filePath,
                            attributeName,
                            umlOperation,
                            fileContent
                    );


                    // Create UMLAttribute
                    LocationInfo attributeLocationInfo = new LocationInfo(
                            assignment.getRootCompilationUnit(),
                            sourceFolder,
                            filePath,
                            langFieldAccess,
                            LocationInfo.CodeElementType.FIELD_DECLARATION
                    );
                    UMLAttribute attribute = new UMLAttribute(
                            attributeName,
                            variableDeclaration.getType(),
                            attributeLocationInfo,
                            className
                    );

                    // Set the variable declaration on the attribute
                    attribute.setVariableDeclaration(variableDeclaration);
                    attribute.setVisibility(Visibility.PUBLIC);
                    attribute.setFinal(false);
                    attribute.setStatic(false);

                    attributes.add(attribute);

//                    LOGGER.info("Created attribute: " + attributeName + " with initializer: " +
//                            (variableDeclaration.getInitializer() != null ? "yes" : "no"));
                }
            }
        }
    }

    private static void processClassLevelAssignmentForAttribute(UMLClass typeDeclaration, LangAssignment assignment,
                                               String sourceFolder, String filePath, String fileContent) {
        LangASTNode leftSide = assignment.getLeftSide();

        if (leftSide instanceof LangSimpleName simpleName) {
            String attributeName = simpleName.getIdentifier();

            // Create UMLAttribute
            LocationInfo attributeLocationInfo = new LocationInfo(
                    assignment.getRootCompilationUnit(),
                    sourceFolder,
                    filePath,
                    simpleName,
                    LocationInfo.CodeElementType.FIELD_DECLARATION
            );
            UMLAttribute attribute = new UMLAttribute(
                    attributeName,
                    UMLType.extractTypeObject("Object"),
                    attributeLocationInfo,
                    typeDeclaration.getName()
            );
            // Create VariableDeclaration for the attribute using the new constructor
            VariableDeclaration variableDeclaration = UMLAdapterVariableProcessor.processAttributeAssignment(
                    assignment,
                    sourceFolder,
                    filePath,
                    attributeName,
                    attribute,
                    fileContent
            );

            // Set the variable declaration on the attribute
            attribute.setVariableDeclaration(variableDeclaration);
            attribute.setVisibility(Visibility.PUBLIC);
            attribute.setFinal(false);
            attribute.setStatic(false);

            typeDeclaration.addAttribute(attribute);
        }
    }

    private static void processComments(LangMethodDeclaration methodDecl, String sourceFolder, String filePath, UMLOperation umlOperation){
        List<UMLComment> comments = new ArrayList<>();
        for (LangComment langComment: methodDecl.getComments()) {
            UMLComment comment = createUMLComment(langComment, methodDecl.getRootCompilationUnit(), sourceFolder, filePath);
            if (comment != null)
                comments.add(comment);
        }
        umlOperation.getComments().addAll(comments);
    }

    private static UMLComment createUMLComment(LangComment langComment, LangCompilationUnit cu, String sourceFolder, String filePath) {
        if (langComment.isBlockComment() || langComment.isDocComment()){
            return new UMLComment(langComment.getContent(), new LocationInfo(
                    cu,
                    sourceFolder,
                    filePath,
                    langComment,
                    LocationInfo.CodeElementType.BLOCK_COMMENT
            ));
        }
        else if (langComment.isLineComment()) {
            return new UMLComment(langComment.getContent(), new LocationInfo(
                    cu,
                    sourceFolder,
                    filePath,
                    langComment,
                    LocationInfo.CodeElementType.LINE_COMMENT
            ));
        }
        return null;
    }

    public UMLModel getUMLModel() {
        return umlModel;
    }
}