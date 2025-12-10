package extension.umladapter;

import extension.ast.node.declaration.LangMethodDeclaration;
import extension.ast.node.declaration.LangSingleVariableDeclaration;
import extension.ast.node.metadata.LangAnnotation;
import extension.ast.node.statement.LangImportStatement;
import extension.ast.node.unit.LangCompilationUnit;
import extension.base.LangSupportedEnum;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.UMLImport;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class UMLAdapterUtil {

    public static String resolveQualifiedTypeName(String name, List<UMLImport> imports, String currentPackage) {
        if (name == null || name.isEmpty()) return name;
        if (name.contains(".")) return name; // already qualified
        // Try exact import matches (from X import Name)
        for (UMLImport imp : imports) {
            if (!imp.isOnDemand()) {
                String impName = imp.getName();
                if (impName != null && impName.endsWith("." + name)) {
                    return impName;
                }
                if (impName != null && impName.equals(name)) {
                    return impName;
                }
            }
        }
        // Try module imports (import module) -> module.Name
        for (UMLImport imp : imports) {
            if (!imp.isOnDemand()) {
                String impName = imp.getName();
                if (impName != null && !impName.contains(".")) {
                    // simple module import
                    return impName + "." + name;
                }
            }
        }
        // Fallback to current package qualification if available
        if (currentPackage != null && !currentPackage.isEmpty()) {
            return currentPackage + "." + name;
        }
        return name;
    }

    public static int getParamOffset(LangMethodDeclaration methodDecl, List<LangSingleVariableDeclaration> params, LangSupportedEnum language) {
        int paramOffset = 0;
        if (!params.isEmpty()) {
            String firstParamName = params.get(0).getLangSimpleName().getIdentifier();
            boolean isPython = LangSupportedEnum.PYTHON.equals(language);

            if (isPython) {
                // Skip 'self' for instance methods
                if ("self".equals(firstParamName) && !methodDecl.isStatic()) {
                    paramOffset = 1;
                } else if ("cls".equals(firstParamName)) {
                    // Try to detect @classmethod to skip 'cls'
                    boolean hasClassMethodDecorator = false;
                    for (LangAnnotation ann : methodDecl.getAnnotations()) {
                        if ("classmethod".equalsIgnoreCase(ann.getName().getIdentifier())) {
                            hasClassMethodDecorator = true;
                            break;
                        }
                    }
                    if (hasClassMethodDecorator) {
                        paramOffset = 1;
                    }
                }
            }
        }
        return paramOffset;
    }

    public static String extractSourceFolder(String filename) {
        Path path = Paths.get(filename);
        Set<String> commonSourceFolders = Set.of("src", "lib", "tests", "");


        // Check for common source folder patterns
        for (int i = 0; i < path.getNameCount() - 1; i++) {
            String segment = path.getName(i).toString();
            if (commonSourceFolders.contains(segment)) {
                return path.subpath(0, i + 1).toString();
            }
        }

        // Fallback to project root if no source folder found
        return path.getNameCount() > 1 ? path.subpath(0, 1).toString() : "";
    }

    // Add this to UMLAdapterUtil.java
    public static String extractPackageName(String filename) {
        String sourceFolder = extractSourceFolder(filename);
        return extractPackageName(filename, sourceFolder);
    }

    public static String extractPackageName(String filename, String sourceFolder) {
        Path path = Paths.get(filename);
        Path sourcePath = Paths.get(sourceFolder);

        try {
            Path relativePath = sourcePath.relativize(path);
            Path parent = relativePath.getParent();

            if (parent == null) return "";

            // Convert path segments to dot notation, validating Python package names
            List<String> packageParts = new ArrayList<>();
            for (int i = 0; i < parent.getNameCount(); i++) {
                String segment = parent.getName(i).toString();
                if (isValidPythonPackageName(segment)) {
                    packageParts.add(segment);
                }
            }

            return String.join(".", packageParts);
        } catch (IllegalArgumentException e) {
            // Path is not relative to source folder
            return "";
        }
    }

    private static boolean isValidPythonPackageName(String name) {
        return name.matches("[a-zA-Z_][a-zA-Z0-9_]*"); //&& !PYTHON_KEYWORDS.contains(name);
    }

    // Returns the path of the file relative to the project root
    public static String extractFilePath(String filename) {
        return filename.replace(File.separatorChar, '/');
    }

    public static List<UMLImport> extractUMLImports(LangCompilationUnit compilationUnit, String filename) {
        List<UMLImport> umlImports = new ArrayList<>();

        // Get source folder and file path for location info
        String sourceFolder = UMLAdapterUtil.extractSourceFolder(filename);
        String filepath = UMLAdapterUtil.extractFilePath(filename);

        for (LangImportStatement importStmt : compilationUnit.getImports()) {
            // Create location info for this import
            LocationInfo locationInfo = new LocationInfo(
                    sourceFolder,
                    filepath,
                    importStmt,
                    LocationInfo.CodeElementType.IMPORT_DECLARATION
            );

            if (importStmt.isFromImport()) {
                // Handle 'from' imports (from X import Y)
                if (importStmt.isWildcardImport()) {
                    // from module import *
                    String importName = importStmt.getModuleName();
                    // isOnDemand is true for wildcard imports
                    // isStatic is false since Python doesn't have static imports like Java
                    UMLImport umlImport = new UMLImport(importName, importStmt.isWildcardImport(), false, locationInfo);
                    umlImports.add(umlImport);
                } else {
                    // from module import specific items
                    // Create separate UMLImport for each imported item
                    for (LangImportStatement.LangImportItem item : importStmt.getImports()) {
                        String fullImportName = importStmt.getModuleName() + "." + item.getName();
                        // isOnDemand is false because we're importing specific items
                        // isStatic is false since Python doesn't have static imports like Java
                        UMLImport umlImport = new UMLImport(fullImportName, false, false, new LocationInfo(
                                sourceFolder,
                                filepath,
                                item,
                                LocationInfo.CodeElementType.IMPORT_DECLARATION));
                        umlImports.add(umlImport);
                    }
                }
            } else {
                // If there are aliases, create separate imports for each
                if (!importStmt.getImports().isEmpty()) {
                    for (LangImportStatement.LangImportItem item : importStmt.getImports()) {
                        UMLImport umlImport = new UMLImport(item.getName(), false, false, new LocationInfo(
                                sourceFolder,
                                filepath,
                                item,
                                LocationInfo.CodeElementType.IMPORT_DECLARATION));
                        umlImports.add(umlImport);
                    }
                }
            }
        }

        return umlImports;
    }

    public static String extractModuleName(String filename) {
        String packageName = UMLAdapterUtil.extractPackageName(filename);

        String baseFileName = Paths.get(filename).getFileName().toString();
        if (baseFileName.endsWith(".py")) {
            baseFileName = baseFileName.substring(0, baseFileName.length() - 3);
        }

        return packageName.isEmpty() ?
                baseFileName/* + "_module"*/ :
                packageName + "." + baseFileName/* + "_module"*/;
    }

}
