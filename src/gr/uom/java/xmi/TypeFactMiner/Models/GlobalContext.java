package gr.uom.java.xmi.TypeFactMiner.Models;

import gr.uom.java.xmi.TypeFactMiner.Models.TypeGraphOuterClass.TypeGraph;
import gr.uom.java.xmi.TypeFactMiner.TypFct;
import io.vavr.Tuple;
import org.apache.commons.io.IOUtils;
import org.apache.tinkerpop.gremlin.process.traversal.Text;
import org.apache.tinkerpop.gremlin.process.traversal.TextP;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;
import scala.Tuple2;

import java.io.StringWriter;
import java.util.*;
import java.util.stream.Stream;

import static gr.uom.java.xmi.TypeFactMiner.HierarchyUtil.HierarchyRelation.*;
import static gr.uom.java.xmi.TypeFactMiner.Models.TypeNodeOuterClass.TypeNode.TypeKind.Simple;
import static gr.uom.java.xmi.TypeFactMiner.TypeGraphUtil.*;
import static java.util.stream.Collectors.*;
import static java.util.stream.Collectors.toSet;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.__;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.has;

public class GlobalContext {

    private Set<Information> classInfomration;
    GraphTraversalSource traverser;


    public Map<String, List<String>> getInternalClassHierarchy(){
        return classInfomration.stream()
                .flatMap(x-> x.superTypes.entrySet().stream())
                .collect(toMap(x->x.getKey(), x -> x.getValue().stream().map(t -> pretty(t.getType())).collect(toList()), (a,b)->a));
    }

    public Set<String> getClassesInternal(){
        return classInfomration.stream()
                .flatMap(x-> x.typeDecls.stream())
                .collect(toSet());
    }

    public Set<String> getEnumsInternal(){
        return classInfomration.stream()
                .flatMap(x-> x.enumDecls.stream())
                .collect(toSet());
    }

    public Map<Boolean, List<String>> getAllImports(){
        return classInfomration.stream().flatMap(x->x.imports.entrySet().stream()).collect(groupingBy(x->x.getKey()
                , collectingAndThen(toList(), x->x.stream().flatMap(f->f.getValue().stream()).collect(toList()))));
    }

    public GlobalContext(Repository repository, RevCommit commit, GraphTraversalSource gr){
        classInfomration = getFileContents(repository, commit).stream()
                .map(Information::new).collect(toSet());
        this.traverser = gr;
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


    public static class Information{
        private Set<String> typeDecls = new HashSet<>();
        private Set<String> enumDecls = new HashSet<>();
        private Map<Boolean, List<String>> imports;
        private Map<String,List<TypFct>> superTypes = new HashMap<>();
        private Map<String,List<TypFct>> composes = new HashMap<>();


        Information(String content){
            CompilationUnit cu = getCuFor(content);
            imports = ((List<ImportDeclaration>)cu.imports()).stream()
                    .collect(groupingBy(ImportDeclaration::isOnDemand,
                            collectingAndThen(toList(), x -> x.stream().map(z -> z.getName().getFullyQualifiedName()).collect(toList()))));
            processTypeDeclarations(cu);
        }



        private void processTypeDeclarations(CompilationUnit cu){
            List<AbstractTypeDeclaration> types = cu.types();
            for(AbstractTypeDeclaration t : types){
                if(t instanceof TypeDeclaration){
                    typeDecls.addAll(getTypeDeclIn((TypeDeclaration) t, cu.getPackage()!=null ? cu.getPackage().getName().getFullyQualifiedName() : "", cu));

                }else if(t instanceof EnumDeclaration){
                    enumDecls.add(cu.getPackage().getName().getFullyQualifiedName() + "." + t.getName().toString());
                }
            }

        }

        private Set<String> getTypeDeclIn(TypeDeclaration td, String parent, final CompilationUnit cu){
            Set<String> clss = new HashSet<>();
            String name = parent + "." + td.getName().toString();
            typeDecls.add(name);

            Arrays.stream(td.getTypes()).forEach(x-> getTypeDeclIn(x,name, cu));
            TypFct.EnumVisitor en = new TypFct.EnumVisitor(parent + "." + td.getName().toString());
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

    public Set<String> getAllJavaLangClasses() {
        Set set = traverser.V()
                .has("Kind", "Class")
                .where(has("QName", TextP.startingWith("java.lang")))
                .has("isEnum", false)
                .values("QName")
                .toSet();
        return set;
    }

    public Set<String> getAllJavaClasses() {
        Set set = traverser.V()
                .has("Kind", "Class")
                .has("isEnum", false)
                .values("QName")
                .toSet();
        return set;
    }

    public Set<String> getJavaEnums() {
        Set set = traverser.V()
                .has("Kind", "Class")
                .has("isEnum", true)
                .values("QName")
                .toSet();
        return set;
    }

    public Map<String, List<String>> getSuperTypes() {
        return  traverser.V()
                .has("Kind", "Class")
                .as("c","s")
                .select("c","s")
                    .by("QName")
                    .by(__().out("extends","implements").has("Name", TextP.neq("java.lang.Object")).values("Name").fold())
                .toStream().map(x -> new Tuple2<>((String) x.get("c"), ((List<String>) x.get("s"))))
                .filter(x->!x._2().isEmpty())
                .collect(toMap(x -> x._1(), x -> x._2(),(x,y) -> x));
    }



}
