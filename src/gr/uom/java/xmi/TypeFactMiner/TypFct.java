package gr.uom.java.xmi.TypeFactMiner;

import ca.concordia.jaranalyzer.Models.JarInformation;
import com.t2r.common.models.ast.TypeGraphOuterClass.TypeGraph;
import com.t2r.common.models.ast.TypeNodeOuterClass.TypeNode;
import com.t2r.common.models.ast.TypeNodeOuterClass.TypeNode.TypeKind;
import com.t2r.common.models.refactorings.CommitInfoOuterClass.CommitInfo.JarInfo;
import com.t2r.common.models.refactorings.ExternalDepInfoOuterClass.ExternalDepInfo;
import com.t2r.common.models.refactorings.ExternalDepInfoOuterClass.ExternalDepInfo.TypeNames;
import com.t2r.common.utilities.ProtoUtil;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import static ca.concordia.jaranalyzer.JarAnalyzer.getHierarchyCompositionMap;
import static com.t2r.common.utilities.PrettyPrinter.pretty;
import static java.util.stream.Collectors.*;


public class TypFct {

    private TypeGraph tg;
    private Context context;
    private boolean isResolved;
    private TypeGraph resolvedTypeGraph;

    public TypFct(TypeGraph typeGraph, Context context) {
        this.tg = typeGraph;
        this.context = context;
        this.resolvedTypeGraph = qualify();
        this.isResolved = false;
        if (allTypeNodesMatch(resolvedTypeGraph,t -> t.getName().isEmpty() ||  t.getIsResolved())) {
            this.context = null;
            this.tg = null;
            this.isResolved = true;
        }
    }

    public TypFct(TypeGraph typeGraph, Context context, GlobalContext gc) {
        this.tg = typeGraph;
        this.context = context;
        qualify(gc);
        this.context = null;
        this.tg = null;
        this.isResolved = allTypeNodesMatch(resolvedTypeGraph,t -> t.getName().isEmpty() ||  t.getIsResolved());
    }

    public static boolean allTypeNodesMatch(TypeGraph tg, Predicate<TypeNode> pred) {
        return pred.test(tg.getRoot()) && tg.getEdgesMap().values().stream().allMatch(t -> allTypeNodesMatch(t, pred));
    }

    public static boolean anyTypeNodesMatch(TypeGraph tg, Predicate<TypeNode> pred) {
        return pred.test(tg.getRoot()) || tg.getEdgesMap().values().stream().anyMatch(t -> anyTypeNodesMatch(t, pred));
    }


    public TypeGraph getType() {
        return resolvedTypeGraph;
    }

    public String getTypeStr() {
        return pretty(resolvedTypeGraph);
    }

    public TypeGraph qualify() {
        return transformGraph(tg,
                t -> !t.getName().isEmpty()
                        ? isPrimitiveOrWildCard(t).map(q -> t.toBuilder().setName(q).setIsResolved(true).setIsTypeVariable(false).build())
                        .or(() -> qualifyAsTypeVariable(t.getName()).map(q -> t.toBuilder().setName(q).setIsResolved(true).setIsTypeVariable(true).build()))
                        .or(() -> resolveAndQualify(t.getName()).map(x -> t.toBuilder().setName(x).setIsResolved(true).setIsTypeVariable(false).build()))
                        .orElse(t)
                        : t);
    }

    public void qualify(GlobalContext globalContext) {
        this.resolvedTypeGraph = transformGraph(tg,
                t -> !t.getName().isEmpty()
                        ? isPrimitiveOrWildCard(t).map(q -> t.toBuilder().setName(q).setIsResolved(true).setIsTypeVariable(false).build())
                        .or(() -> qualifyAsTypeVariable(t.getName()).map(q -> t.toBuilder().setName(q).setIsResolved(true).setIsTypeVariable(true).build()))
                        .or(() -> resolveAndQualify(t.getName()).map(x -> t.toBuilder().setName(x).setIsResolved(true).setIsTypeVariable(false).build()))
                        .or(() -> resolveAndQualify(t.getName(), globalContext).map(x -> t.toBuilder().setName(x).setIsResolved(true).setIsTypeVariable(false).build()))
                        .orElse(t)
                        : t);

        if (allTypeNodesMatch(resolvedTypeGraph, t -> t.getName().isEmpty() || t.getIsResolved())) {
            this.isResolved = true;
            this.context = null;
            this.tg = null;
        }
    }

    private Optional<String> resolveAndQualify(String name, GlobalContext gc) {
        return qualifyAsJavaLang(name, gc)
                .or(() -> qualifyFromImports(name, gc.getAllImports(), gc))
                .or(() -> qualifyFromPackage(name, gc))
                .or(() -> qualifyImportOnDemandJava(name, gc))
                .or(() -> qualifyInternal(name, context.getPackageName(), gc))
                .or(() -> qualifyInternalBruteForce(name, gc))
                .or(() -> qualifyExternal(name, gc).map(o -> o._1()));
    }

    private Optional<String> qualifyInternal(String lookup, String packageName, GlobalContext gc) {
        return gc.getClassesInternal().stream()
                .filter(x -> x.contains(packageName) || (context.importDecl.containsKey(true) && context.getImportDecl().get(true).stream().anyMatch(x::contains)))
                .filter(x -> x.endsWith("." + lookup))
                .findFirst()
                .or(() -> gc.getEnumsInternal().stream()
                        .filter(x -> x.contains(packageName) || (context.importDecl.containsKey(true) && context.getImportDecl().get(true).stream().anyMatch(x::contains)))
                        .filter(x -> x.endsWith("." + lookup))
                        .findFirst());
    }

    private Optional<Tuple2<String, JarInfo>> qualifyExternal(String lookup, GlobalContext gc) {
        return gc.getRequiredJars().stream()
                .map(x -> getDependencyInfo(gc.getPathToJars(), x))
                .flatMap(d -> d.toJavaStream())
                .flatMap(d -> d._2().getHierarchicalInfoMap().keySet().stream()
                            .filter(x -> context.importDecl.containsKey(true) && context.getImportDecl().get(true).stream().anyMatch(x::contains))
                            .filter(x -> x.endsWith("." + lookup)).findFirst()
                        .or(() -> d._2().getEnums().getNamesList().stream()
                            .filter(x -> context.importDecl.containsKey(true) && context.getImportDecl().get(true).stream().anyMatch(x::contains))
                            .filter(x -> x.endsWith("." + lookup))
                            .findFirst())
                        .map(l -> Tuple.of(l, d._1()))
                        .stream())
                .findFirst();
    }


    public static Try<Tuple2<JarInfo,ExternalDepInfo>> getDependencyInfo(Path pathToJar, JarInfo jar) {
        ProtoUtil.ReadWriteAt rw = new ProtoUtil.ReadWriteAt(pathToJar);
        String jarName1 = jar.getGroupID() + "-" + jar.getVersion();
        String jarName2 = jar.getArtifactID() + "-" + jar.getVersion();
        return rw.<ExternalDepInfo>read(jarName1, "ExternalDependencyInfo")
                .orElse(() ->
                        Try.of(() -> new JarFile(new File(pathToJar.resolve(jarName1 + ".jar").toAbsolutePath().toString())))
                                .orElse(Try.of(() -> new JarFile(new File(pathToJar.resolve(jarName2 + ".jar").toAbsolutePath().toString()))))
                                .onFailure(e -> System.out.println("Could not create Jar File - " + jarName2 + " or " + jarName1 + "\n" + e.toString()))
                                .map(j -> new JarInformation(j, jar.getGroupID(), jar.getArtifactID(), jar.getVersion()))
                                .map(j -> getHierarchyCompositionMap(j))
                                .map(j -> ExternalDepInfo.newBuilder()
                                        .setEnums(TypeNames.newBuilder().addAllNames(j.entrySet().stream().filter(x -> x.getValue()._3()).map(x -> x.getKey()).collect(toList())))
                                        .putAllHierarchicalInfo(j.entrySet().stream().collect(toMap(x -> x.getKey(), x -> TypeNames.newBuilder().addAllNames(x.getValue()._1()).build())))
                                        .putAllCompositionInfo(j.entrySet().stream().collect(toMap(x -> x.getKey(), x -> TypeNames.newBuilder().addAllNames(x.getValue()._2()).build()))).build())
                                .onSuccess(e -> rw.write(e, jarName1, false))
                                .onFailure(e -> rw.write(ExternalDepInfo.getDefaultInstance(), jarName1, false)))
                .map(e -> Tuple.of(jar,e));
    }

    public static Map<String, List<String>> getExternalHierarchyMap(Path pathToJar, JarInfo jar) {
        Try<Tuple2<JarInfo, ExternalDepInfo>> d = getDependencyInfo(pathToJar, jar);
        if(d.isFailure())
            return new HashMap<>();

        ExternalDepInfo ed = d.get()._2();
        return ed.getHierarchicalInfoMap().entrySet().stream().collect(toMap(x -> x.getKey(), x -> x.getValue().getNamesList()));
    }

    public static Map<String, List<String>> getExternalComposition(Path pathToJar, JarInfo jar) {
        Try<Tuple2<JarInfo, ExternalDepInfo>> d = getDependencyInfo(pathToJar, jar);
        if(d.isFailure())
            return new HashMap<>();

        ExternalDepInfo ed = d.get()._2();
        return ed.getCompositionInfoMap().entrySet().stream().collect(toMap(x -> x.getKey(), x -> x.getValue().getNamesList()));
    }


    public static CompilationUnit getCuFor(String content) {
        ASTParser parser = ASTParser.newParser(AST.JLS11);
        Map<String, String> options = JavaCore.getOptions();
        options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_8);
        options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8);
        options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_8);
        parser.setCompilerOptions(options);
        parser.setResolveBindings(false);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setStatementsRecovery(true);
        parser.setSource(content.toCharArray());
        return (CompilationUnit) parser.createAST(null);
    }


    public static TypeGraph transformGraph(TypeGraph tg, Function<TypeNode, TypeNode> fn) {
        TypeNode transformedRoot = fn.apply(tg.getRoot());
        return TypeGraph.newBuilder()
                .setRoot(transformedRoot)
                .putAllEdges(tg.getEdgesMap().entrySet().stream()
                        .collect(toMap(Map.Entry::getKey, x -> transformGraph(x.getValue(), fn))))
                .build();
    }


    public Optional<String> isPrimitiveOrWildCard(TypeNode lookup) {
        if (lookup.getKind().equals(TypeKind.Primitive) || lookup.getKind().equals(TypeKind.WildCard))
            return Optional.of(lookup.getName());
        return Optional.empty();
    }

    public static Tuple2<Map<String, List<String>>, Set<String>> getTypeDeclAndParam(CompilationUnit cu) {
        List<AbstractTypeDeclaration> types = cu.types();
        Map<String, List<String>> clsMap = new HashMap<>();
        Set<String> enums = new HashSet<>();
        for (AbstractTypeDeclaration t : types) {
            if (t instanceof TypeDeclaration) {
                Tuple2<Map<String, List<String>>, Set<String>> te = getTypeDeclAndParam((TypeDeclaration) t, cu.getPackage() != null ? cu.getPackage().getName().getFullyQualifiedName() : "");
                clsMap.putAll(te._1());
                enums.addAll(te._2());
            } else if (t instanceof EnumDeclaration) {
                enums.add(t.getName().toString());
            }
        }
        return Tuple.of(clsMap, enums);
    }


    public static class EnumVisitor extends ASTVisitor {
        private Set<String> enums = new HashSet<>();
        private final String parentName;

        public EnumVisitor(String parentName) {
            this.parentName = parentName;
        }

        @Override
        public boolean visit(EnumDeclaration node) {
            enums.add(parentName + "." + node.getName().toString());
            return false;
        }

        public Set<String> getEnums() {
            return enums;
        }
    }

    public static Tuple2<Map<String, List<String>>, Set<String>> getTypeDeclAndParam(TypeDeclaration td, String parent) {
        Map<String, List<String>> clsMap = new HashMap<>();
        Set<String> enums = new HashSet<>();
        String name = parent + "." + td.getName().toString();
        clsMap.put(name, ((List<TypeParameter>) td.typeParameters())
                .stream().map(x -> x.getName().toString()).collect(toList()));

        for (TypeDeclaration t : td.getTypes()) {
            Tuple2<Map<String, List<String>>, Set<String>> te = getTypeDeclAndParam(t, name);
            clsMap.putAll(te._1());
            enums.addAll(te._2());
        }

        EnumVisitor en = new EnumVisitor(parent + "." + td.getName().toString());
        td.accept(en);
        en.getEnums().forEach(x -> enums.add(x));
        return Tuple.of(clsMap, enums);
    }


    private Optional<String> resolveAndQualify(String lookup) {
        return qualifyAsTypeVariable(lookup)
                .or(() -> qualifyQualified(lookup))
                .or(() -> qualifyAsClassName(lookup))
                .or(() -> qualifyFromImports(lookup, context.getImportDecl()));
    }

    private Optional<String> qualifyInternalBruteForce(String lookup, GlobalContext gc) {
        return gc.getClassesInternal().stream().filter(x -> x.endsWith("." + lookup))
                .findFirst()
                .or(() -> gc.getEnumsInternal().stream().filter(x -> x.endsWith("." + lookup))
                        .findFirst());

    }

    private Optional<String> qualifyImportOnDemandJava(String lookup, GlobalContext gc) {
        if (context.getImportDecl().containsKey(true))
            return gc.getAllJavaTypes()
                    .filter(x -> context.getImportDecl().get(true).stream().anyMatch(i -> x.startsWith(i)) && x.endsWith("." + lookup))
                    .findFirst();
        return Optional.empty();

    }

    private Optional<String> qualifyAsJavaLang(String lookup, GlobalContext gc) {
        return gc.getAllJavaLangClasses().filter(x -> x.endsWith(lookup)).findFirst()
                .or(() -> gc.getJavaEnums().filter(x -> x.endsWith(lookup)).findFirst());
    }

    private Optional<String> qualifyFromPackage(String lookup, GlobalContext gc) {
            return gc.getTypesInPackage(context.getPackageName()).filter(x -> x.endsWith("." + lookup)).findFirst();
    }

    private Optional<String> qualifyFromImports(String lookup, Map<Boolean, List<String>> importDecl) {
        if (importDecl.containsKey(false))
            return importDecl.get(false).stream().filter(x -> x.endsWith("." + lookup))
                    .findFirst();
        return Optional.empty();
    }

    private Optional<String> qualifyFromImports(String lookup, Map<Boolean, List<String>> importDecl, GlobalContext gc) {
        return qualifyFromImports(lookup, importDecl)
                .or(() -> Stream.ofNullable(importDecl.get(true)).flatMap(x -> x.stream())
                   .flatMap(x -> qualifyFromPackage(x, gc).stream()).findFirst())
                .or(() -> gc.getAllInternalPackages()
                        .filter(x -> Stream.ofNullable(importDecl.get(true)).flatMap(i -> i.stream()).anyMatch(i -> i.contains(x)))
                        .flatMap(x -> gc.getTypesInPackage(x)).filter(x -> x.endsWith("."+lookup)).findFirst());
    }

    private Optional<String> qualifyAsClassName(String lookup) {
        return context.getTd_tp().keySet().stream().filter(x -> x.equals(lookup)).findFirst()
                .or(() -> context.getEnums().stream().filter(x -> x.equals(lookup)).findFirst());
    }

    private Optional<String> qualifyQualified(String lookup) {
        if (lookup.contains(".")) {
            if (lookup.chars().filter(x -> x == '.').count() == 1) {
                return resolveAndQualify(lookup.substring(0, lookup.indexOf(".")))
                        .map(x -> x + "." + lookup.substring(lookup.indexOf(".") + 1));
            }
            return Optional.of(lookup);
        }
        return Optional.empty();
    }


    private Optional<String> qualifyAsTypeVariable(String lookup) {
        if (context.getMethdLevelTypeVars().stream().anyMatch(x -> x.equals(lookup)) || context.getTd_tp().values().stream().anyMatch(x -> x.stream().anyMatch(t -> t.equals(lookup))))
            return Optional.of(lookup);
        return Optional.empty();
    }

    public boolean isResolved() {
        return isResolved;
    }

    public static class Context {
        private final Map<Boolean, List<String>> importDecl;
        private final List<String> methdLevelTypeVars;
        private final Map<String, List<String>> td_tp;
        private final Set<String> enums;
        private final String packageName;

        public Context(CompilationUnit cu, Optional<MethodDeclaration> md) {
            this.importDecl = ((List<ImportDeclaration>) cu.imports()).stream()
                    .collect(groupingBy(ImportDeclaration::isOnDemand,
                            collectingAndThen(toList(), x -> x.stream().map(z -> z.getName().getFullyQualifiedName()).collect(toList()))));
            this.methdLevelTypeVars = md.map(x -> (List<TypeParameter>) x.typeParameters())
                    .map(t -> t.stream().map(x -> x.getName().toString()).collect(toList()))
                    .orElse(new ArrayList<>());
            Tuple2<Map<String, List<String>>, Set<String>> typeDeclAndEnums = getTypeDeclAndParam(cu);
            this.td_tp = typeDeclAndEnums._1();
            this.enums = typeDeclAndEnums._2();
            this.packageName = cu.getPackage() != null ? cu.getPackage().getName().toString() : "";
        }

        public Context(CompilationUnit cu, MethodDeclaration[] md) {
            this.importDecl = ((List<ImportDeclaration>) cu.imports()).stream()
                    .collect(groupingBy(ImportDeclaration::isOnDemand,
                            collectingAndThen(toList(), x -> x.stream().map(z -> z.getName().getFullyQualifiedName()).collect(toList()))));
            this.methdLevelTypeVars = Arrays.stream(md).map(x -> (List<TypeParameter>) x.typeParameters())
                    .flatMap(x -> x.stream())
                    .map(x -> x.getName().toString()).collect(toList());
            Tuple2<Map<String, List<String>>, Set<String>> typeDeclAndEnums = getTypeDeclAndParam(cu);
            this.td_tp = typeDeclAndEnums._1();
            this.enums = typeDeclAndEnums._2();
            this.packageName = cu.getPackage() != null ? cu.getPackage().getName().toString() : "";
        }

        public Map<Boolean, List<String>> getImportDecl() {
            return importDecl;
        }

        public List<String> getMethdLevelTypeVars() {
            return methdLevelTypeVars;
        }

        public Map<String, List<String>> getTd_tp() {
            return td_tp;
        }

        public Set<String> getEnums() {
            return enums;
        }

        public String getPackageName() {
            return packageName;
        }
    }

}

