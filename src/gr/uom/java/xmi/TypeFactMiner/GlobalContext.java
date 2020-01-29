package gr.uom.java.xmi.TypeFactMiner;

import com.t2r.common.models.refactorings.CommitInfoOuterClass.CommitInfo.JarInfo;
import com.t2r.common.utilities.GitUtil;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.apache.commons.io.IOUtils;
import org.apache.tinkerpop.gremlin.process.traversal.TextP;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.StringWriter;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static com.t2r.common.utilities.PrettyPrinter.pretty;
import static gr.uom.java.xmi.TypeFactMiner.TypeGraphUtil.getTypeGraph;
import static gr.uom.java.xmi.TypeFactMiner.TypeGraphUtil.getTypeGraphStripParam;
import static java.util.stream.Collectors.*;
import static java.util.stream.Stream.concat;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.__;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.has;

public class GlobalContext {

    private Map<String,List<Information>> classInfomration;
    private Set<JarInfo> requiredJars;
    private Path pathToJars;
    GraphTraversalSource traverser;


    public  Stream<String> getTypesInPackage(String packageName){
        return Stream.ofNullable(classInfomration.get(packageName)).flatMap(x -> x.stream())
                .flatMap(x-> concat(x.typeDecls.stream(), x.enumDecls.stream()));
    }

    public  Stream<String> getTypesInternal(){
        return classInfomration.values().stream().flatMap(x -> x.stream())
                .flatMap(x-> concat(x.typeDecls.stream(), x.enumDecls.stream()));
    }

    public Stream<String> getAllInternalPackages(){
        return classInfomration.keySet().stream().filter(x -> !x.isEmpty());
    }


    public Map<String, List<String>> getInternalClassHierarchy(){
        return classInfomration.values().stream().flatMap(x -> x.stream())
                .flatMap(x-> x.superTypes.entrySet().stream())
                .collect(toMap(x->x.getKey(), x -> x.getValue().stream().map(t -> pretty(t.getType())).collect(toList()), (a, b)->a));
    }


    public Map<String, List<String>> getInternalCompositionMap(){
        return classInfomration.values().stream().flatMap(x -> x.stream())
                .flatMap(x -> x.composes.entrySet().stream())
                .collect(toMap(x -> x.getKey(), x -> x.getValue().stream().map(t -> pretty(t.getType())).collect(toList()), (a, b)->a));
    }

    public List<gr.uom.java.xmi.TypeFactMiner.TypFct> getSuperTypeOf(String className){
        return classInfomration.values().stream().flatMap(x -> x.stream())
                .flatMap(x-> x.superTypes.entrySet().stream())
                .filter(x->x.getKey().equals(className))
                .map(x -> x.getValue())
                .findFirst().orElse(new ArrayList<>());
    }

    public Set<String> getClassesInternal(){
        return classInfomration.values().stream().flatMap(x -> x.stream())
                .flatMap(x-> x.typeDecls.stream())
                .collect(toSet());
    }

    public Set<String> getEnumsInternal(){
        return classInfomration.values().stream().flatMap(x -> x.stream())
                .flatMap(x-> x.enumDecls.stream())
                .collect(toSet());
    }

    public Map<Boolean, List<String>> getAllImports(){
        return classInfomration.values().stream().flatMap(x -> x.stream())
                .flatMap(x->x.imports.entrySet().stream()).collect(groupingBy(x->x.getKey()
                , collectingAndThen(toList(), x->x.stream().flatMap(f->f.getValue().stream()).collect(toList()))));
    }

    public  GlobalContext( Git g, RevCommit commit, GraphTraversalSource gr, Set<JarInfo> jars, Path pathToDependencies){
        this.traverser = gr;
        this.requiredJars = jars;
        this.pathToJars = pathToDependencies;
        this.classInfomration = concat(GitUtil.getFilesAddedRenamedModified(g, commit.getId().getName()).values().stream()
                , getFileContents(g.getRepository(), commit.getParent(0)).stream())
                .map(Information::new).collect(groupingBy(x -> x.packageName,toList()));



    }

    public static Set<String> getFileContents(Repository repository, RevCommit commit) {
        RevTree parentTree = commit.getTree();
        Set<String> fileContents = new HashSet<>();
        try (TreeWalk treeWalk = new TreeWalk(repository)) {
            treeWalk.addTree(parentTree);
            treeWalk.setRecursive(true);
            while (treeWalk.next()) {
                String pathString = treeWalk.getPathString();
                if(pathString.endsWith(".java")) {
                    ObjectId objectId = treeWalk.getObjectId(0);
                    ObjectLoader loader = repository.open(objectId);
                    StringWriter writer = new StringWriter();
                    IOUtils.copy(loader.openStream(), writer);
                    fileContents.add(writer.toString());
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return fileContents;
    }

    public Set<JarInfo> getRequiredJars() {
        return requiredJars;
    }

    public Path getPathToJars() {
        return pathToJars;
    }


    public static class Information{
        private String packageName;
        private Set<String> typeDecls = new HashSet<>();
        private Set<String> enumDecls = new HashSet<>();
        private Map<Boolean, List<String>> imports;
        private Map<String,List<gr.uom.java.xmi.TypeFactMiner.TypFct>> superTypes = new HashMap<>();
        private Map<String,List<gr.uom.java.xmi.TypeFactMiner.TypFct>> composes = new HashMap<>();


        Information(String content) {
            CompilationUnit cu = getCuFor(content);
            packageName = cu.getPackage() != null ? cu.getPackage().getName().getFullyQualifiedName() : "";
            imports = ((List<ImportDeclaration>) cu.imports()).stream()
                        .collect(groupingBy(ImportDeclaration::isOnDemand,
                                collectingAndThen(toList(), x -> x.stream().map(z -> z.getName().getFullyQualifiedName()).collect(toList()))));
            try {
                processTypeDeclarations(cu);
            }catch (Exception e){
                e.printStackTrace();
            }
        }


        private void processTypeDeclarations(CompilationUnit cu){
            List<AbstractTypeDeclaration> types = cu.types();
            for(AbstractTypeDeclaration t : types){
                if(t instanceof TypeDeclaration){
                    try {
                        typeDecls.addAll(getTypeDeclIn((TypeDeclaration) t, cu.getPackage() != null ? cu.getPackage().getName().getFullyQualifiedName() : "", cu));
                    }catch (Exception e){
                        System.out.println(e.getStackTrace());
                    }
                }else if(t instanceof EnumDeclaration){
                    try {
                        enumDecls.add(cu.getPackage() != null ? cu.getPackage().getName().getFullyQualifiedName()
                                + "." + t.getName().toString() : "");
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }

        }

        private Set<String> getTypeDeclIn(TypeDeclaration td, String parent, final CompilationUnit cu){
            Set<String> clss = new HashSet<>();
            String name = parent + "." + td.getName().toString();
            typeDecls.add(name);

            Arrays.stream(td.getTypes()).forEach(x-> getTypeDeclIn(x,name, cu));
            gr.uom.java.xmi.TypeFactMiner.TypFct.EnumVisitor en = new gr.uom.java.xmi.TypeFactMiner.TypFct.EnumVisitor(parent + "." + td.getName().toString());
            td.accept(en);
            enumDecls.addAll(en.getEnums());

            if(td.getSuperclassType() != null) {
                ArrayList<TypFct> l = new ArrayList<>();
                l.add(getTypeGraphStripParam((td.getSuperclassType()), cu));
                superTypes.put(name,l );
            }

            if(td.superInterfaceTypes()!=null && !(td.superInterfaceTypes().isEmpty())){
                List<Type> superInterfaces = td.superInterfaceTypes();
                if(superTypes.containsKey(name))
                    superTypes.get(name).addAll(superInterfaces.stream().map(x-> getTypeGraphStripParam(x,cu)).collect(toList()));
                else
                    superTypes.put(name,superInterfaces.stream().map(x-> getTypeGraphStripParam(x,cu)).collect(toList()));
            }
            if(td.getFields()!= null){
                composes.put(name, Arrays.stream(td.getFields()).map(x-> getTypeGraph(x.getType(), cu)).collect(toList()));
            }
            return clss;
        }
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

    public Stream<String> getAllJavaLangClasses() {
        return  traverser.V()
                .has("Kind", "Class")
                .where(has("QName", TextP.startingWith("java.lang")))
                .has("isEnum", false)
                .values("QName")
                .toStream().map(x->(String)x);

    }

    public Stream<String> getAllJavaClasses() {
        return  traverser.V()
                .has("Kind", "Class")
                .has("isEnum", false)
                .values("QName")
                .toStream().map(x->(String)x);
    }

    public Optional<String> getAllJavaClasses(String qName) {
        return  traverser.V()
                .has("Kind", "Class")
                .has("isEnum", false)
                .has("QName", qName)
                .value()
                .toStream().findFirst().map(x->(String)x);
    }

    public Stream<String> getAllJavaTypes() {
        return  traverser.V()
                .has("Kind", "Class")
                .values("QName")
                .toStream().map(x->(String)x);
    }

    public Optional<String> javaClassesContainsNameLike(String qName) {
        return  traverser.V()
                .has("Kind", "Class")
                .has("QName", TextP.containing(qName))
                .values("QName")
                .toStream().findFirst().map(x->(String)x);
    }

    public Stream<String> getJavaEnums() {
        return traverser.V()
                .has("Kind", "Class")
                .has("isEnum", true)
                .values("QName")
                .toStream().map(x->(String)x);
    }

    public Optional<String> getJavaEnums(String qName) {
        return traverser.V()
                .has("Kind", "Class")
                .has("isEnum", true)
                .has("QName", qName)
                .value()
                .toStream().map(x->(String)x).findFirst();
    }

    public Stream<Tuple2<String, List<String>>> getSuperTypes() {
        return  traverser.V()
                .has("Kind", "Class")
                .as("c","s")
                .select("c","s")
                    .by("QName")
                    .by(__().out("extends","implements").has("Name", TextP.neq("java.lang.Object")).values("Name").fold())
                .toStream().map(x -> Tuple.of((String) x.get("c"), ((List<String>) x.get("s"))))
                .filter(x->!x._2().isEmpty());

    }

    public Map<String, List<String>> getJdkComposition() {
        return  traverser.V()
                .has("Kind", "Class")
                .as("c","s")
                .select("c","s").by("QName")
                .by(__().out("Declares").has("Kind", "Field").values("ReturnType").fold())
                .toStream().map(x -> new scala.Tuple2<>((String) x.get("c"), ((List<String>) x.get("s"))))
                .filter(x->!x._2().isEmpty())
                .collect(toMap(x -> x._1(), x -> x._2(),(x,y) -> x));
    }





}
