package gr.uom.java.xmi.TypeFactMiner;

import com.t2r.common.models.refactorings.JarInfoOuterClass.JarInfo;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.apache.tinkerpop.gremlin.process.traversal.TextP;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;

import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static com.t2r.common.utilities.GitUtil.filePathDiffAtCommit;
import static com.t2r.common.utilities.GitUtil.getFilesAddedRemovedRenamedModified;
import static com.t2r.common.utilities.PrettyPrinter.pretty;
import static gr.uom.java.xmi.TypeFactMiner.GlobalContext.FileStatus.*;
import static java.util.stream.Collectors.*;
import static java.util.stream.Stream.concat;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.__;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.has;

public class GlobalContext {

    protected RevCommit baseCommit;
    protected Map<String, List<Information>> classInformation;
    protected Set<JarInfo> requiredJars;
    protected Path pathToJars;
    protected GraphTraversalSource traverser;


    public Stream<Information> getInformationFor(Set<String> typeNames) {
        return classInformation.entrySet().stream()
                .filter(x -> typeNames.stream().anyMatch(t -> t.contains(x.getKey())))
                .flatMap(x -> x.getValue().stream());
    }


    public List<String> typesUsesType(Set<String> classNames, String qualifiedName) {
        return getInformationFor(classNames)
                .filter(f -> f.getB4After().equals(After) || f.getB4After().equals(Unchanged))
                .flatMap(c -> classNames.stream()
                        .flatMap(x -> c.typeUsesType(qualifiedName, x).stream()))
                .collect(toList());
    }


    public List<String> packageUsesType(Set<String> classNames, String qualifiedName) {
        List<String> involvedPackages = getAllInternalPackages().filter(x -> classNames.stream().anyMatch(c -> c.contains(x)))
                .collect(toList());
        return involvedPackages.stream()
                .filter(x -> classInformation.get(x).stream()
                        .filter(f -> f.getB4After().equals(After) || f.getB4After().equals(Unchanged))
                        .flatMap(c -> c.typeUsesType(qualifiedName).stream()).findFirst().isPresent())
                .collect(toList());
    }

    public boolean projectUsesType(String qualifiedName) {
        return classInformation.entrySet()
                .stream()
                .flatMap(x -> x.getValue().stream())
                .filter(f -> f.getB4After().equals(After) || f.getB4After().equals(Unchanged))
                .anyMatch(c -> c.typeUsesType(qualifiedName).isEmpty());
    }

    public Stream<String> getTypesInPackage(String packageName) {
        return Stream.ofNullable(classInformation.get(packageName)).flatMap(x -> x.stream())
                .filter(f -> f.getB4After().equals(After) || f.getB4After().equals(Unchanged))
                .flatMap(x -> concat(x.typeDecls.stream(), x.enumDecls.stream()));
    }

    public Stream<String> getTypesInternal() {
        return classInformation.values().stream().flatMap(x -> x.stream())
                .flatMap(x -> concat(x.typeDecls.stream(), x.enumDecls.stream()));
    }

    public Stream<String> getAllInternalPackages() {
        return classInformation.keySet().stream().filter(x -> !x.isEmpty());
    }


    public Map<String, List<String>> getInternalClassHierarchy() {
        return classInformation.values().stream().flatMap(x -> x.stream())
                .flatMap(x -> x.superTypes.entrySet().stream())
                .collect(toMap(x -> x.getKey(), x -> x.getValue().stream().map(t -> pretty(t.getType())).collect(toList()), (a, b) -> a));
    }


    public Map<String, List<TypFct>> getInternalCompositionMap() {
        return classInformation.values().stream().flatMap(x -> x.stream())
                .flatMap(x -> x.composes.entrySet().stream())
                .collect(toMap(x -> x.getKey(), x -> x.getValue(), (x, y) -> Stream.concat(x.stream(), y.stream()).collect(toList())));
    }

    public List<gr.uom.java.xmi.TypeFactMiner.TypFct> getSuperTypeOf(String className) {
        return classInformation.values().stream().flatMap(x -> x.stream())
                .flatMap(x -> x.superTypes.entrySet().stream())
                .filter(x -> x.getKey().equals(className))
                .map(x -> x.getValue())
                .findFirst().orElse(new ArrayList<>());
    }

    public Set<String> getClassesInternal() {
        return classInformation.values().stream().flatMap(x -> x.stream())
                .flatMap(x -> x.typeDecls.stream())
                .collect(toSet());
    }

    public Set<String> getEnumsInternal() {
        return classInformation.values().stream().flatMap(Collection::stream)
                .flatMap(x -> x.enumDecls.stream())
                .collect(toSet());
    }

    public Map<Boolean, List<String>> getAllImports(Set<String> classes) {
        return classInformation.values().stream().flatMap(Collection::stream)
                .filter(x -> x.getTypeDecls().stream().anyMatch(classes::contains))
                .flatMap(x -> x.imports.entrySet().stream()).collect(groupingBy(Entry::getKey
                        , collectingAndThen(toList(), x -> x.stream().flatMap(f -> f.getValue().stream()).collect(toList()))));
    }

    public Map<Boolean, List<String>> getAllImports() {
        return classInformation.values().stream().flatMap(Collection::stream)
                .flatMap(x -> x.imports.entrySet().stream()).collect(groupingBy(Entry::getKey
                        , collectingAndThen(toList(), x -> x.stream().flatMap(f -> f.getValue().stream()).collect(toList()))));
    }

//    public GlobalContext(GlobalContext gc, RevCommit against, Git g, BiFunction<Tuple2<String,String>, FileStatus, Information> fn) throws Exception {
//        this.baseCommit = against;
//        this.traverser = gc.traverser;
//        this.requiredJars = gc.requiredJars;
//        this.pathToJars = gc.getPathToJars();
//        var files = getFilesAddedRemovedRenamedModified1(g, against, gc.baseCommit);
//        this.classInformation = concat(concat(files._1().entrySet().stream()
//                        .map(x -> gc.classInformation.values().stream().flatMap(c -> c.stream())
//                                .filter(z -> z.getPath().contains(x.getKey().toString())).findFirst()
//                                .map(z -> z.updateB4After(Before))
//                                .orElseGet(() -> fn.apply(Tuple.of(x.getKey().toString(),x.getValue()), Before)))
//                , files._2().entrySet().stream().map(x -> fn.apply(Tuple.of(x.getKey().toString(),x.getValue()), After)))
//                , files._3().entrySet().stream()
//                        .map(x -> gc.classInformation.values().stream().flatMap(c -> c.stream())
//                                .filter(z -> z.getPath().contains(x.getKey().toString())).findFirst()
//                                .map(z -> z.updateB4After(Unchanged))
//                                .orElseGet(() -> fn.apply(Tuple.of(x.getKey().toString(),x.getValue()), Unchanged))))
//                .collect(groupingBy(x -> x.packageName, toList()));
//    }

    public GlobalContext(Git g, RevCommit commit, GraphTraversalSource gr, Set<JarInfo> jars, Path pathToDependencies
            , BiFunction<Tuple2<String,String>, FileStatus, Information> fn) throws Exception {
        this.baseCommit = commit;
        this.traverser = gr;
        this.requiredJars = jars;
        this.pathToJars = pathToDependencies;
        var files = getFilesAddedRemovedRenamedModified(g.getRepository(), commit,filePathDiffAtCommit(g, commit));
        this.classInformation = concat(concat(files._1().entrySet().stream().map(x -> fn.apply(Tuple.of(x.getKey().toString(), x.getValue()), Before))
                , files._2().entrySet().stream().map(x -> fn.apply(Tuple.of(x.getKey().toString(), x.getValue()),After )))
                , files._3().entrySet().stream().map(x -> fn.apply(Tuple.of(x.getKey().toString(), x.getValue()), Unchanged)))
                .collect(groupingBy(x -> x.packageName, toList()));
    }



    public Set<JarInfo> getRequiredJars() {
        return requiredJars;
    }

    public Path getPathToJars() {
        return pathToJars;
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

    public Stream<String> getAllJavaLangClasses() {
        return traverser.V()
                .has("Kind", "Class")
                .where(has("QName", TextP.startingWith("java.lang")))
                .has("isEnum", false)
                .values("QName")
                .toStream().map(x -> (String) x);

    }

    public Stream<String> getAllJavaClasses() {
        return traverser.V()
                .has("Kind", "Class")
                .has("isEnum", false)
                .values("QName")
                .toStream().map(x -> (String) x);
    }

    public Optional<String> getAllJavaClasses(String qName) {
        return traverser.V()
                .has("Kind", "Class")
                .has("isEnum", false)
                .has("QName", qName)
                .value()
                .toStream().findFirst().map(x -> (String) x);
    }

    public Stream<String> getAllJavaTypes() {
        return traverser.V()
                .has("Kind", "Class")
                .values("QName")
                .toStream().map(x -> (String) x);
    }

    public Optional<String> javaClassesContainsNameLike(String qName) {
        return traverser.V()
                .has("Kind", "Class")
                .has("QName", TextP.containing(qName))
                .values("QName")
                .toStream().findFirst().map(x -> (String) x);
    }

    public Optional<String> qNameContainsJavaPackage(String qName) {
        return traverser.V()
                .has("Kind", "Package")
                .values("Name")
                .toStream()
                .map(x -> (String) x)
                .filter(qName::contains)
                .findFirst();
    }


    public Stream<String> getJavaEnums() {
        return traverser.V()
                .has("Kind", "Class")
                .has("isEnum", true)
                .values("QName")
                .toStream().map(x -> (String) x);
    }

    public Optional<String> getJavaEnums(String qName) {
        return traverser.V()
                .has("Kind", "Class")
                .has("isEnum", true)
                .has("QName", qName)
                .value()
                .toStream().map(x -> (String) x).findFirst();
    }

    public Stream<Tuple2<String, List<String>>> getSuperTypes() {
        return traverser.V()
                .has("Kind", "Class")
                .as("c", "s")
                .select("c", "s")
                .by("QName")
                .by(__().out("extends", "implements").has("Name", TextP.neq("java.lang.Object")).values("Name").fold())
                .toStream().map(x -> Tuple.of((String) x.get("c"), ((List<String>) x.get("s"))))
                .filter(x -> !x._2().isEmpty());

    }

    public Map<String, List<String>> getJdkComposition() {
        return traverser.V()
                .has("Kind", "Class")
                .as("c", "s")
                .select("c", "s").by("QName")
                .by(__().out("Declares").has("Kind", "Field").values("ReturnType").fold())
                .toStream().map(x -> new scala.Tuple2<>((String) x.get("c"), ((List<String>) x.get("s"))))
                .filter(x -> !x._2().isEmpty())
                .collect(toMap(x -> x._1(), x -> x._2(), (x, y) -> x));
    }

    public enum FileStatus {
        Before, After, Unchanged;
    }


}
