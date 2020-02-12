package gr.uom.java.xmi.TypeFactMiner;

import org.eclipse.jdt.core.dom.*;

import java.util.*;
import java.util.stream.Stream;

import static gr.uom.java.xmi.TypeFactMiner.TypeGraphUtil.getTypeGraph;
import static gr.uom.java.xmi.TypeFactMiner.TypeGraphUtil.getTypeGraphStripParam;
import static java.util.stream.Collectors.*;
import static java.util.stream.Stream.concat;

public class Information {
    protected String packageName;
    protected Set<String> typeDecls = new HashSet<>();
    protected Set<String> enumDecls = new HashSet<>();

    public String getPackageName() {
        return packageName;
    }

    public Set<String> getTypeDecls() {
        return typeDecls;
    }

    public Set<String> getEnumDecls() {
        return enumDecls;
    }

    public Map<Boolean, List<String>> getImports() {
        return imports;
    }

    protected Map<Boolean, List<String>> imports;
    protected Map<String, List<TypFct>> superTypes = new HashMap<>();
    protected Map<String, List<TypFct>> composes = new HashMap<>();
    protected GlobalContext.FileStatus b4After;
    private final String path;
    protected Map<String, Set<String>> usesTypes;


    public boolean containsType(String typeName) {
        return concat(typeDecls.stream(), enumDecls.stream())
                .anyMatch(x -> x.equals(typeName));
    }


    public Information updateB4After(GlobalContext.FileStatus f){
        b4After = f;
        return this;
    }

    public Information(String content, GlobalContext.FileStatus b4After, String path) {
        this.b4After = b4After;
        this.path = path;
        CompilationUnit cu = GlobalContext.getCuFor(content);
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

    public void processTypeDeclarations(CompilationUnit cu) {
        List<AbstractTypeDeclaration> types = cu.types();
        for (AbstractTypeDeclaration t : types) {
            if (t instanceof TypeDeclaration) {
                try {
                    getTypeDeclIn((TypeDeclaration) t, cu.getPackage() != null ? cu.getPackage().getName().getFullyQualifiedName() : "", cu);
                } catch (Exception e) {
                    e.printStackTrace();
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

    public void getTypeDeclIn(TypeDeclaration td, String parent, final CompilationUnit cu) {
        String name = parent + "." + td.getName().toString();
        typeDecls.add(name);

        Arrays.stream(td.getTypes()).forEach(x -> getTypeDeclIn(x, name, cu));
        Visitors.EnumVisitor en = new Visitors.EnumVisitor(parent + "." + td.getName().toString());
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

        Visitors.UsedTypes ut = new Visitors.UsedTypes();
        td.accept(ut);
        usesTypes.put(name, ut.typesUsed);


    }

    public GlobalContext.FileStatus getB4After() {
        return b4After;
    }

    public String getPath() {
        return path;
    }
}
