package gr.uom.java.xmi.TypeFactMiner;

import com.t2r.common.models.refactorings.JarInfoOuterClass.JarInfo;
import gr.uom.java.xmi.TypeFactMiner.Visitors.EnumVisitor;
import gr.uom.java.xmi.TypeFactMiner.Visitors.UsedTypes;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.apache.tinkerpop.gremlin.process.traversal.TextP;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;

import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Stream;

import static com.t2r.common.utilities.GitUtil.getFilesAddedRemovedRenamedModified;
import static com.t2r.common.utilities.PrettyPrinter.pretty;
import static gr.uom.java.xmi.TypeFactMiner.GlobalContext.FileStatus.*;
import static gr.uom.java.xmi.TypeFactMiner.TypeGraphUtil.getTypeGraph;
import static gr.uom.java.xmi.TypeFactMiner.TypeGraphUtil.getTypeGraphStripParam;
import static java.util.stream.Collectors.*;
import static java.util.stream.Stream.concat;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.__;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.has;

public class GlobalContext {

    private Map<String, List<Information>> classInformation;
    private Set<JarInfo> requiredJars;
    private Path pathToJars;
    GraphTraversalSource traverser;


    public Stream<Information> getInformationFor(Set<String> typeNames) {
        return classInformation.entrySet().stream()
                .filter(x -> typeNames.stream().anyMatch(t ->t.contains(x.getKey())))
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

    public Map<Boolean, List<String>> getAllImports() {
        return classInformation.values().stream().flatMap(Collection::stream)
                .flatMap(x -> x.imports.entrySet().stream()).collect(groupingBy(Entry::getKey
                        , collectingAndThen(toList(), x -> x.stream().flatMap(f -> f.getValue().stream()).collect(toList()))));
    }

    public GlobalContext(Git g, RevCommit commit, GraphTraversalSource gr, Set<JarInfo> jars, Path pathToDependencies) {
        this.traverser = gr;
        this.requiredJars = jars;
        this.pathToJars = pathToDependencies;
        var files = getFilesAddedRemovedRenamedModified(g, commit, commit.getParent(0));
        this.classInformation = concat(
                concat(files._1().values().stream().map(x -> new Information(x, Before))
                        , files._2().values().stream().map(x -> new Information(x, After)))
                , files._3().values().stream().map(x -> new Information(x, Unchanged)))
                .collect(groupingBy(x -> x.packageName, toList()));
    }

    public Set<JarInfo> getRequiredJars() {
        return requiredJars;
    }

    public Path getPathToJars() {
        return pathToJars;
    }


    public static class Information {
        private String packageName;
        private Set<String> typeDecls = new HashSet<>();
        private Set<String> enumDecls = new HashSet<>();
        private Map<Boolean, List<String>> imports;
        private Map<String, List<TypFct>> superTypes = new HashMap<>();
        private Map<String, List<TypFct>> composes = new HashMap<>();
        private final FileStatus b4After;
        private Map<String, Set<String>> usesTypes;

        public boolean containsType(String typeName) {
            return concat(typeDecls.stream(), enumDecls.stream())
                    .anyMatch(x -> x.equals(typeName));
        }

        Information(String content, FileStatus b4After) {
            this.b4After = b4After;
            CompilationUnit cu = getCuFor(content);
            packageName = cu.getPackage() != null ? cu.getPackage().getName().getFullyQualifiedName() : "";
            imports = ((List<ImportDeclaration>) cu.imports()).stream()
                    .collect(groupingBy(ImportDeclaration::isOnDemand,
                            collectingAndThen(toList(), x -> x.stream().map(z -> z.getName().getFullyQualifiedName()).collect(toList()))));
            usesTypes = new HashMap<>();
            try {
                processTypeDeclarations(cu);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public Optional<String> typeUsesType(String qualifiedName, String className) {
            if (Stream.ofNullable(imports.get(false)).flatMap(x -> x.stream()).anyMatch(qualifiedName::equals))
                return Optional.ofNullable(className);

            if (qualifiedName.contains(packageName)
                    || Stream.ofNullable(imports.get(true)).flatMap(x -> x.stream()).anyMatch(qualifiedName::contains)
                    || Stream.ofNullable(imports.get(false)).flatMap(x -> x.stream()).anyMatch(qualifiedName::contains))
                if (Stream.ofNullable(usesTypes.get(className)).flatMap(Collection::stream)
                        .anyMatch(ut -> qualifiedName.endsWith("." + ut)))
                    return Optional.of(className);

            return Optional.empty();
        }

        public List<String> typeUsesType(String qualifiedName) {
            return typeDecls.stream().flatMap(t -> typeUsesType(qualifiedName, t).stream())
                    .collect(toList());
        }

        private void processTypeDeclarations(CompilationUnit cu) {
            List<AbstractTypeDeclaration> types = cu.types();
            for (AbstractTypeDeclaration t : types) {
                if (t instanceof TypeDeclaration) {
                    try {
                        typeDecls.addAll(getTypeDeclIn((TypeDeclaration) t, cu.getPackage() != null ? cu.getPackage().getName().getFullyQualifiedName() : "", cu));
                    } catch (Exception e) {
                        System.out.println(e.getStackTrace());
                    }
                } else if (t instanceof EnumDeclaration) {
                    try {
                        enumDecls.add(cu.getPackage() != null ? cu.getPackage().getName().getFullyQualifiedName()
                                + "." + t.getName().toString() : "");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

        }

        private Set<String> getTypeDeclIn(TypeDeclaration td, String parent, final CompilationUnit cu) {
            Set<String> clss = new HashSet<>();
            String name = parent + "." + td.getName().toString();
            typeDecls.add(name);

            Arrays.stream(td.getTypes()).forEach(x -> getTypeDeclIn(x, name, cu));
            EnumVisitor en = new EnumVisitor(parent + "." + td.getName().toString());
            td.accept(en);
            enumDecls.addAll(en.getEnums());
            usesTypes.putAll(en.usedTypes);


            if (td.getSuperclassType() != null) {
                ArrayList<TypFct> l = new ArrayList<>();
                l.add(getTypeGraphStripParam((td.getSuperclassType()), cu));
                superTypes.put(name, l);
            }

            if (td.superInterfaceTypes() != null && !(td.superInterfaceTypes().isEmpty())) {
                List<Type> superInterfaces = td.superInterfaceTypes();
                if (superTypes.containsKey(name))
                    superTypes.get(name).addAll(superInterfaces.stream().map(x -> getTypeGraphStripParam(x, cu)).collect(toList()));
                else
                    superTypes.put(name, superInterfaces.stream().map(x -> getTypeGraphStripParam(x, cu)).collect(toList()));
            }
            if (td.getFields() != null) {
                composes.put(name, Arrays.stream(td.getFields()).map(x -> getTypeGraph(x.getType(), cu)).collect(toList()));
            }

            UsedTypes ut = new UsedTypes();
            td.accept(ut);
            usesTypes.put(name, ut.typesUsed);
            return clss;
        }

        public FileStatus getB4After() {
            return b4After;
        }
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
