package gr.uom.java.xmi.TypeFactMiner;

import com.t2r.common.models.ast.TypeGraphOuterClass.TypeGraph;
import com.t2r.common.utilities.Counter;
import gr.uom.java.xmi.TypeFactMiner.GlobalContext.FileStatus;
import gr.uom.java.xmi.TypeFactMiner.TypFct.Context;
import gr.uom.java.xmi.TypeFactMiner.Visitors.EnumVisitor;
import io.vavr.Tuple2;
import org.eclipse.jdt.core.dom.*;

import java.util.*;

import static com.t2r.common.models.refactorings.ElementKindOuterClass.ElementKind.*;
import static com.t2r.common.utilities.Counter.updateKey;
import static gr.uom.java.xmi.TypeFactMiner.TypeGraphUtil.*;
import static java.util.stream.Collectors.toList;

public class CodeStatsInformation extends Information {

    private Tuple2<Counter<TypeGraph>, Context> typeFoundAndContext;
    private Counter<TypFct> typFctFound;
    private Counter<String> visibilityMap;
    private Counter<String> typeKindMap;
    private Counter<String> nameSpaceMap;
    private Counter<TypeGraph> typesFound;

    public Counter<String> getElemKindMap() {
        return elemKindMap  == null ? new Counter<>() : elemKindMap;
    }

    private Counter<String> elemKindMap;

    public Map<String, List<String>> getTd_tp() {
        return td_tp;
    }

    public List<String> getM_tp() {
        return m_tp;
    }

    private Map<String, List<String>> td_tp;
    private List<String> m_tp ;


    public Counter<String> getVisibilityMap() {
        return visibilityMap == null ? new Counter<>() : visibilityMap;
    }

    public Counter<String> getTypeKindMap() {
        return typeKindMap == null ? new Counter<>() : typeKindMap;
    }

    public Counter<String> getNameSpaceMap() {
        return nameSpaceMap  == null ? new Counter<>() : nameSpaceMap;
    }

    public Counter<TypFct> getTypeFctsFound() {
        return typFctFound;
    }


    public CodeStatsInformation(String content, FileStatus b4After, String path, Set<String> classes) {
        super(content, b4After, path);
//        typesFound = new Counter<>();
//        typeFoundAndContext = Tuple.of(typesFound, new Context(getCuFor(content)));
        Context c = new Context(this);
        if(typeDecls.stream().anyMatch(classes::contains))
            typFctFound = typesFound == null ? new Counter<>() : updateKey(x -> getTypeFact(x, c), typesFound);
    }

    @Override
    public void getTypeDeclIn(TypeDeclaration td, String parent, final CompilationUnit cu) {

        if (visibilityMap == null) {
            typesFound = new Counter<>();
            visibilityMap = new Counter<>();
            nameSpaceMap = new Counter<>();
            typeKindMap = new Counter<>();
            typFctFound = new Counter<>();
            td_tp = new HashMap<>();
            m_tp = new ArrayList<>();
            elemKindMap = new Counter<String>();
        }


        String name = parent + "." + td.getName().toString();
        typeDecls.add(name);

        Arrays.stream(td.getTypes()).forEach(x -> getTypeDeclIn(x, name, cu));
        EnumVisitor en = new EnumVisitor(parent + "." + td.getName().toString());
        td.accept(en);
        enumDecls.addAll(en.getEnums());
//        usesTypes.putAll(en.usedTypes);

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

        Visitors.UsedTypes ut = new Visitors.UsedTypes();
        td.accept(ut);

        visibilityMap.addAll(Arrays.stream(td.getFields()).map(x -> getVisibility(td.isInterface(), x.getModifiers())).collect(toList()));
        typeKindMap.addAll(Arrays.stream(td.getFields()).map(t -> getTypeGraph(t.getType()).getRoot().getKind().name()).collect(toList()));
        elemKindMap.add(Field.name(),td.getFields().length);
        typesFound.addAll(Arrays.stream(td.getFields()).map(t -> getTypeGraph(t.getType())).collect(toList()));
        visibilityMap.addAll(Arrays.stream(td.getMethods()).map(x -> getVisibility(td.isInterface(), x.getModifiers())).collect(toList()));
        typeKindMap.addAll(Arrays.stream(td.getMethods())
                .filter(x -> x.getReturnType2() != null)
                .map(t -> getTypeGraph(t.getReturnType2()).getRoot().getKind().name()).collect(toList()));
        typesFound.addAll(Arrays.stream(td.getMethods())
                .filter(x -> x.getReturnType2() != null)
                .map(t -> getTypeGraph(t.getReturnType2())).collect(toList()));
        td_tp.put(name, ((List<TypeParameter>) td.typeParameters())
                .stream().map(x -> x.getName().toString()).collect(toList()));
        elemKindMap.add(Return.name(),td.getMethods().length);
        for (var m : td.getMethods()) {

            m_tp.addAll((List<String>) m.typeParameters().stream()
                    .map(x -> ((TypeParameter) x).getName().toString()).collect(toList()));

            List<SingleVariableDeclaration> params = m.parameters();
            elemKindMap.add(Parameter.name(),params.size());
            visibilityMap.addAll(params.stream().map(x -> getVisibility(td.isInterface(), m.getModifiers())).collect(toList()));
            typeKindMap.addAll(params.stream().map(t -> getTypeGraph(t.getType()).getRoot().getKind().name()).collect(toList()));
            typesFound.addAll(params.stream().map(t -> getTypeGraph(t.getType())).collect(toList()));
            if (m.getBody() != null) {
                m.getBody().accept(new ASTVisitor() {
                    @Override
                    public boolean visit(VariableDeclarationExpression node){
                        typeKindMap.addAll(Collections.nCopies(node.fragments().size(), getTypeGraph(node.getType()).getRoot().getKind().name()));
                        visibilityMap.addAll(Collections.nCopies(node.fragments().size(), "Block"));
                        typesFound.addAll(Collections.nCopies(node.fragments().size(), getTypeGraph(node.getType())));
                        elemKindMap.add(LocalVariable.name());
                        return super.visit(node);
                    }
                    @Override
                    public boolean visit(VariableDeclarationStatement node) {
                        typeKindMap.addAll(Collections.nCopies(node.fragments().size(), getTypeGraph(node.getType()).getRoot().getKind().name()));
                        visibilityMap.addAll(Collections.nCopies(node.fragments().size(), "Block"));
                        typesFound.addAll(Collections.nCopies(node.fragments().size(), getTypeGraph(node.getType())));
                        elemKindMap.add(LocalVariable.name());
                        return super.visit(node);
                    }

                    @Override
                    public boolean visit(SingleVariableDeclaration node) {
                        typeKindMap.add(getTypeGraph(node.getType()).getRoot().getKind().name());
                        visibilityMap.add("Block");
                        typesFound.add(getTypeGraph(node.getType()));
                        elemKindMap.add(LocalVariable.name());
                        return super.visit(node);
                    }
                });
//                System.out.println();
            }
        }


    }

    private String getVisibility(boolean isInterace, int fieldModifiers) {
        if ((fieldModifiers & Modifier.PUBLIC) != 0)
            return "public";
        else if ((fieldModifiers & Modifier.PROTECTED) != 0)
            return "protected";
        else if ((fieldModifiers & Modifier.PRIVATE) != 0)
            return "private";
        else if (isInterace)
            return "public";
        else
            return "package";
    }
}
