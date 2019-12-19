package gr.uom.java.xmi.TypeFactMiner;

import gr.uom.java.xmi.TypeFactMiner.Models.GlobalContext;
import gr.uom.java.xmi.TypeFactMiner.Models.TypeGraphOuterClass.TypeGraph;
import gr.uom.java.xmi.TypeFactMiner.Models.TypeNodeOuterClass.TypeNode;
import gr.uom.java.xmi.TypeFactMiner.Models.TypeNodeOuterClass.TypeNode.NameSpace;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

import static gr.uom.java.xmi.TypeFactMiner.Models.TypeNodeOuterClass.TypeNode.*;
import static gr.uom.java.xmi.TypeFactMiner.TypeGraphUtil.pretty;
import static java.util.stream.Collectors.*;


public class TypFct {

    private TypeGraph tg;
    private Context context;
    private boolean isResolved;
    private TypeGraph resolvedTypeGraph;
    private boolean knowsAllNameSpace;

    public TypFct(TypeGraph typeGraph, Context context ) {
        this.tg = typeGraph;
        this.context = context;
        this.resolvedTypeGraph = qualify();
        this.isResolved = false;
        if(allTypeNodesMatch(resolvedTypeGraph, tn -> !tn.hasName() || tn.hasNamespacee())){
            this.context = null;
            this.tg = null;
            this.isResolved = true;
        }
        this.knowsAllNameSpace = !anyTypeNodesMatch(resolvedTypeGraph, tn -> tn.hasName() && tn.hasNamespacee() && tn.getNamespacee().equals(NameSpace.DontKnow));
    }

    public static boolean allTypeNodesMatch(TypeGraph tg, Predicate<TypeNode> pred){
        return pred.test(tg.getRoot()) && tg.getEdgesMap().values().stream().allMatch(t -> allTypeNodesMatch(t, pred));
    }

    public static boolean anyTypeNodesMatch(TypeGraph tg, Predicate<TypeNode> pred){
        return pred.test(tg.getRoot()) && tg.getEdgesMap().values().stream().anyMatch(t -> anyTypeNodesMatch(t, pred));
    }


    public TypeGraph getType(){
        return resolvedTypeGraph;
    }

    public String getTypeStr(){
        return pretty(resolvedTypeGraph);
    }

    public TypeGraph qualify(){
        return transformGraph(tg,
                t -> t.hasName() ? isPrimitiveOrWildCard(t)
                        .or(() ->  resolveAndQualify(t.getName(), false))
                        .map(x -> {
                            TypeNode.Builder bldr = t.toBuilder().setName(x._1()).setNamespacee(x._2());
                            x._3().ifPresent(bldr::setTypeSem);
                            return bldr.build();
                        })
                        .orElse(t)
                        : t);
    }

    public void qualify(GlobalContext clsStruc){
        this.resolvedTypeGraph =  transformGraph(tg,
                t -> t.hasName() ? isPrimitiveOrWildCard(t)
                        .or(() ->  resolveAndQualify(t.getName(), false))
                        .or(() ->  resolveAndQualify(t.getName(), clsStruc))
                        .map(x -> {
                            TypeNode.Builder bldr = t.toBuilder().setName(x._1()).setNamespacee(x._2());
                            x._3().ifPresent(bldr::setTypeSem);
                            return bldr.build();
                        })
                        .orElse(t)
                        : t);
        if(allTypeNodesMatch(resolvedTypeGraph, tn -> !tn.hasName() || (tn.hasName() && tn.hasNamespacee()))){
            this.isResolved = true;
            this.context = null;
            this.tg = null;
        }
    }

    private Optional<Tuple3<String, NameSpace, Optional<TypeSem>>> resolveAndQualify(String name, GlobalContext gc) {
        Set<String> classStructure = gc.getClassesInternal();
        return qualifyAsJavaLang(name, gc)
                .or(() -> qualifyImportOnDemandJava(name, gc))
                .or(() -> qualifyInternal(name,context.getPackageName(),gc))
                .or(() -> qualifyInternalBruteForce(name, gc))
                .or(() -> qualifyFromImports(name,false, gc.getAllImports()));
    }

    private Optional<Tuple3<String, NameSpace, Optional<TypeSem>>> qualifyInternal(String lookup, String packageName,GlobalContext gc){
        return gc.getClassesInternal().stream()
                .filter(x->x.contains(packageName) || (context.importDecl.containsKey(true) && context.getImportDecl().get(true).stream().anyMatch(x::contains)) )
                .filter(x->x.endsWith("." + lookup))
                .findFirst().map(x -> Tuple.of(x, NameSpace.Internal, Optional.of(TypeSem.Object)))
                .or(() -> gc.getEnumsInternal().stream()
                        .filter(x->x.contains(packageName) || (context.importDecl.containsKey(true) && context.getImportDecl().get(true).stream().anyMatch(x::contains)) )
                        .filter(x->x.endsWith("." + lookup))
                        .findFirst().map(x -> Tuple.of(x, NameSpace.Internal, Optional.of(TypeSem.Enum))));
    }

    public static CompilationUnit getCuFor(String content){
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
        return  (CompilationUnit)parser.createAST(null);
    }


    public static TypeGraph transformGraph(TypeGraph tg, Function<TypeNode, TypeNode> fn){
        TypeNode transformedRoot = fn.apply(tg.getRoot());
        return TypeGraph.newBuilder()
                .setRoot(transformedRoot)
                .putAllEdges(tg.getEdgesMap().entrySet().stream()
                        .collect(toMap(Map.Entry::getKey, x -> transformGraph(x.getValue(),fn))))
                .build();
    }


    public Optional<Tuple3<String, NameSpace, Optional<TypeSem>>> isPrimitiveOrWildCard(TypeNode lookup){
        if(lookup.getKind().equals(TypeKind.Primitive))
            return Optional.of(Tuple.of(lookup.getName(), NameSpace.Jdk, Optional.of(TypeSem.PrimitiveType)));
        if(lookup.getKind().equals(TypeKind.WildCard))
            return Optional.of(Tuple.of(lookup.getName(), NameSpace.DontKnow, Optional.of(TypeSem.Object)));
        return Optional.empty();
    }

    public static Tuple2<Map<String, List<String>>,Set<String>> getTypeDeclAndParam(CompilationUnit cu){
        List<AbstractTypeDeclaration> types = cu.types();
        Map<String, List<String>> clsMap = new HashMap<>();
        Set<String> enums = new HashSet<>();
        for(AbstractTypeDeclaration t : types){
            if(t instanceof TypeDeclaration){
                Tuple2<Map<String, List<String>>, Set<String>> te = getTypeDeclAndParam((TypeDeclaration) t, cu.getPackage().getName().getFullyQualifiedName());
                clsMap.putAll(te._1());
                enums.addAll(te._2());
            }else if(t instanceof EnumDeclaration){
                enums.add(t.getName().toString());
            }
        }
        return Tuple.of(clsMap, enums);
    }

    public boolean isKnowsAllNameSpace() {
        return knowsAllNameSpace;
    }

    public void searchNameSpace(GlobalContext gc) {
        if(isResolved)
            this.resolvedTypeGraph = transformGraph(resolvedTypeGraph
                    , t -> t.hasName() && !t.hasNamespacee() ? getNameSpaceFor(t,gc ) : t);
    }

    private TypeNode getNameSpaceFor(TypeNode tn, GlobalContext gc) {
        if(foundAsInternalClass(tn.getName(), gc))
            return tn.toBuilder().setNamespacee(NameSpace.Internal).setTypeSem(TypeSem.Object).build();
        if(foundAsInternalEnum(tn.getName(), gc))
            return tn.toBuilder().setNamespacee(NameSpace.Internal).setTypeSem(TypeSem.Enum).build();
        if(foundAsJdkClass(tn.getName(), gc))
            return tn.toBuilder().setNamespacee(NameSpace.Jdk).setTypeSem(TypeSem.Object).build();
        if(foundAsJdkEnum(tn.getName(), gc))
            return tn.toBuilder().setNamespacee(NameSpace.Jdk).setTypeSem(TypeSem.Enum).build();
        return tn;
    }

    private boolean foundAsInternalClass(String name ,GlobalContext gc){
        return gc.getClassesInternal().stream().anyMatch(c -> c.contains(name));
    }
    private boolean foundAsInternalEnum(String name ,GlobalContext gc){
        return gc.getEnumsInternal().stream().anyMatch(c -> c.contains(name));
    }
    private boolean foundAsJdkClass(String name ,GlobalContext gc){
        return gc.getAllJavaClasses().stream().anyMatch(c -> c.contains(name));
    }
    private boolean foundAsJdkEnum(String name ,GlobalContext gc){
        return gc.getJavaEnums().stream().anyMatch(c -> c.contains(name));
    }


    public static class EnumVisitor extends ASTVisitor{
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

    public static Tuple2<Map<String, List<String>>,Set<String>> getTypeDeclAndParam(TypeDeclaration td, String parent){
        Map<String, List<String>> clsMap = new HashMap<>();
        Set<String> enums = new HashSet<>();
        String name = parent + "." + td.getName().toString();
        clsMap.put(name, ((List<TypeParameter>) td.typeParameters())
                .stream().map(x->x.getName().toString()).collect(toList()));

        for(TypeDeclaration t: td.getTypes()){
            Tuple2<Map<String, List<String>>, Set<String>> te = getTypeDeclAndParam(t, name);
            clsMap.putAll(te._1());
            enums.addAll(te._2());
        }

        EnumVisitor en = new EnumVisitor(parent + "." + td.getName().toString());
        td.accept(en);
        en.getEnums().forEach(x -> enums.add(x));
        return Tuple.of(clsMap, enums);
    }


    private Optional<Tuple3<String, NameSpace, Optional<TypeSem>>> resolveAndQualify(String lookup, boolean isQualifier){
        return  qualifyAsTypeVariable(lookup)
                .or(() -> qualifyQualified(lookup))
                .or(() -> qualifyAsClassName(lookup))
                .or(() -> qualifyFromImports(lookup, isQualifier, context.getImportDecl()));
    }

    private Optional<Tuple3<String, NameSpace, Optional<TypeSem>>> qualifyInternalBruteForce(String lookup, GlobalContext gc) {
        return gc.getClassesInternal().stream().filter(x->x.endsWith("."+lookup))
                    .findFirst().map(x-> Tuple.of(x, NameSpace.Internal, Optional.of(TypeSem.Object)))
                .or(() -> gc.getEnumsInternal().stream().filter(x->x.endsWith("."+lookup))
                        .findFirst().map(x-> Tuple.of(x, NameSpace.Internal, Optional.of(TypeSem.Enum))));

    }

    private Optional<Tuple3<String, NameSpace, Optional<TypeSem>>> qualifyImportOnDemandJava(String lookup, GlobalContext gc) {
        if(context.getImportDecl().containsKey(true))
        return gc.getAllJavaClasses()
                .stream().filter(x-> context.getImportDecl().get(true).stream().anyMatch(i -> x.startsWith(i)))
                .filter(x -> x.endsWith("." + lookup))
                .findFirst().map(t -> Tuple.of(t, NameSpace.Jdk, Optional.of(TypeSem.Object)))
                .or(() -> gc.getJavaEnums()
                        .stream().filter(x-> context.getImportDecl().get(true).stream().anyMatch(i -> x.startsWith(i)))
                        .filter(x -> x.endsWith("." + lookup))
                        .findFirst().map(t -> Tuple.of(t, NameSpace.Jdk, Optional.of(TypeSem.Enum))));
        return Optional.empty();

    }

    private Optional<Tuple3<String, NameSpace, Optional<TypeSem>>> qualifyAsJavaLang(String lookup, GlobalContext gc) {
        return gc.getAllJavaLangClasses().stream().filter(x->x.endsWith(lookup))
                .findFirst().map(t -> Tuple.of(t, NameSpace.Jdk, Optional.of(TypeSem.Object)))
                .or(() -> gc.getJavaEnums().stream().filter(x->x.endsWith(lookup))
                        .findFirst().map(t -> Tuple.of(t, NameSpace.Jdk, Optional.of(TypeSem.Enum))));
    }


    private Optional<Tuple3<String, NameSpace, Optional<TypeSem>>> qualifyFromImports(String lookup, boolean isQualifier, Map<Boolean, List<String>> importDecl) {
        if(importDecl.containsKey(false))
            return importDecl.get(false).stream().filter(x->x.endsWith("." + lookup))
                    .findFirst().map(t -> Tuple.of(t, NameSpace.DontKnow, Optional.empty()));
        if(isQualifier && importDecl.containsKey(true)){
            return importDecl.get(true).stream().filter(x->x.endsWith("." + lookup))
                    .findFirst().map(t -> Tuple.of(t, NameSpace.DontKnow, Optional.empty()));
        }
        return Optional.empty();
    }

    private Optional<Tuple3<String, NameSpace, Optional<TypeSem>>> qualifyAsClassName(String lookup) {
        return context.getTd_tp().keySet().stream().filter(x->x.equals(lookup)).findFirst().map(t -> Tuple.of(t, NameSpace.Internal, Optional.of(TypeSem.Object)))
                .or(() -> context.getEnums().stream().filter(x->x.equals(lookup)).findFirst().map(t -> Tuple.of(t, NameSpace.Internal, Optional.of(TypeSem.Enum))));
    }

    private Optional<Tuple3<String, NameSpace, Optional<TypeSem>>> qualifyQualified(String lookup) {
        if(lookup.contains(".")){
            if(lookup.chars().filter(x -> x == '.').count() == 1){
                return resolveAndQualify(lookup.substring(0,lookup.indexOf(".")), true)
                        .map(s -> s.map1(x -> x + "." + lookup.substring(lookup.indexOf(".")+1)));
            }
            return Optional.of(Tuple.of(lookup, NameSpace.DontKnow, Optional.empty()));
        }
        return Optional.empty();
    }


    private Optional<Tuple3<String, NameSpace, Optional<TypeSem>>> qualifyAsTypeVariable(String lookup) {
        if(context.getMethdLevelTypeVars().stream().anyMatch(x->x.equals(lookup)) || context.getTd_tp().values().stream().anyMatch(x->x.stream().anyMatch(t->t.equals(lookup))))
            return Optional.of(Tuple.of(lookup, NameSpace.TypeVariable, Optional.of(TypeSem.Object)));
        return Optional.empty();
    }

    public boolean isResolved() {
        return isResolved;
    }

    public static class Context{
        private final Map<Boolean, List<String>> importDecl;
        private final List<String> methdLevelTypeVars;
        private final Map<String, List<String>> td_tp;
        private final Set<String> enums;
        private final String packageName;

        public Context(CompilationUnit cu, Optional<MethodDeclaration> md) {
            this.importDecl = ((List<ImportDeclaration>)cu.imports()).stream()
                    .collect(groupingBy(ImportDeclaration::isOnDemand,
                            collectingAndThen(toList(), x -> x.stream().map(z -> z.getName().getFullyQualifiedName()).collect(toList()))));
            this.methdLevelTypeVars = md.map(x -> (List<TypeParameter>) x.typeParameters())
                    .map(t -> t.stream().map(x -> x.getName().toString()).collect(toList()))
                    .orElse(new ArrayList<>());
            Tuple2<Map<String, List<String>>, Set<String>> typeDeclAndEnums = getTypeDeclAndParam(cu);
            this.td_tp = typeDeclAndEnums._1();
            this.enums = typeDeclAndEnums._2();
            this.packageName = cu.getPackage().getName().toString();
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

