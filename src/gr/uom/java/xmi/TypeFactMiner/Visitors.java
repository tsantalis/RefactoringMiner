package gr.uom.java.xmi.TypeFactMiner;

import org.eclipse.jdt.core.dom.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Visitors {

    public static class EnumVisitor extends ASTVisitor {

        private Set<String> enums = new HashSet<>();
        private final String parentName;
        Map<String, Set<String>> usedTypes = new HashMap<>();

        public EnumVisitor(String parentName) {
            this.parentName = parentName;
        }

        @Override
        public boolean visit(EnumDeclaration node) {
            String name = parentName + "." + node.getName().toString();
            enums.add(name);

            UsedTypes ut = new UsedTypes();
            node.accept(ut);
            usedTypes.put(name,ut.typesUsed);
            return false;
        }

        public Set<String> getEnums() {
            return enums;
        }
    }

    public static class UsedTypes extends ASTVisitor {

        Set<String> typesUsed = new HashSet<>();

        @Override
        public boolean visit(SimpleType st) {
            typesUsed.add(st.getName().getFullyQualifiedName());
            return true;
        }

        @Override
        public boolean visit(QualifiedType qt) {
            typesUsed.add(qt.getName().getFullyQualifiedName());
            return true;
        }

        @Override
        public boolean visit(NameQualifiedType nqt) {
            typesUsed.add(nqt.getName().getFullyQualifiedName());
            return true;
        }
    }
}
