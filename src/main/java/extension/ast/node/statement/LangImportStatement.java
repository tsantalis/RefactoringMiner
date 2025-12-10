package extension.ast.node.statement;

import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;
import extension.ast.node.expression.LangExpression;
import extension.ast.visitor.LangASTVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LangImportStatement extends LangStatement {
    private String moduleName;                        // The module being imported from
    private List<LangImportItem> imports = new ArrayList<>();  // The items being imported
    private boolean isFromImport = false;             // Whether this is a 'from' import
    private boolean isWildcardImport = false;         // Whether this is a 'from module import *'
    private int relativeLevel = 0;

    public LangImportStatement() {
        super(NodeTypeEnum.IMPORT_STATEMENT);
    }

    public LangImportStatement(String moduleName, int relativeLevel, PositionInfo positionInfo, List<LangImportItem> importItems, boolean isWildcard) {
        super(NodeTypeEnum.IMPORT_STATEMENT, positionInfo);
        this.moduleName = moduleName;
        this.isFromImport = true;
        this.relativeLevel = relativeLevel;
        this.imports = importItems;
        this.isWildcardImport = isWildcard;
    }


    public LangImportStatement(PositionInfo positionInfo) {
        super(NodeTypeEnum.IMPORT_STATEMENT, positionInfo);
    }

    public LangImportStatement(PositionInfo positionInfo, List<LangImportItem> importItems) {
        super(NodeTypeEnum.IMPORT_STATEMENT, positionInfo);
        this.isFromImport = false;
        this.imports = importItems;
    }

    @Override
    public void accept(LangASTVisitor visitor) {
        visitor.visit(this);
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public List<LangImportItem> getImports() {
        return Collections.unmodifiableList(imports);
    }

    public boolean isFromImport() {
        return isFromImport;
    }

    public void setFromImport(boolean fromImport) {
        isFromImport = fromImport;
    }

    public boolean isWildcardImport() {
        return isWildcardImport;
    }

    public void setWildcardImport(boolean wildcardImport) {
        isWildcardImport = wildcardImport;
    }

    public int getRelativeLevel() {
        return relativeLevel;
    }

    public void setRelativeLevel(int relativeLevel) {
        this.relativeLevel = relativeLevel;
    }

    @Override
    public String toString() {
        return "LangImportStatement{" +
                "moduleName='" + moduleName + '\'' +
                ", imports=" + imports +
                ", isFromImport=" + isFromImport +
                ", isWildcardImport=" + isWildcardImport +
                '}';
    }

    /**
     * Represents an individual imported item, which can be a module, class, function, etc.
     */
    public static class LangImportItem extends LangExpression {
        private String name;   // Name of the imported item
        private String alias;  // Optional alias for the imported item

        public LangImportItem(String name, String alias, PositionInfo positionInfo) {
        	super(NodeTypeEnum.IMPORT_ITEM, positionInfo);
            this.name = name;
            this.alias = alias;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAlias() {
            return alias;
        }

        public void setAlias(String alias) {
            this.alias = alias;
        }

        @Override
        public String toString() {
            return "ImportItem{" +
                    "name='" + name + '\'' +
                    ", alias='" + alias + '\'' +
                    '}';
        }

        @Override
        public void accept(LangASTVisitor visitor) {
            visitor.visit(this);
        }
    }
}