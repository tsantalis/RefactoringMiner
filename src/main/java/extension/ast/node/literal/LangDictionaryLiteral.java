package extension.ast.node.literal;

import extension.ast.node.LangASTNode;
import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;
import extension.ast.visitor.LangASTVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LangDictionaryLiteral extends LangLiteral {
    private final List<Entry> entries = new ArrayList<>();

    public LangDictionaryLiteral(PositionInfo positionInfo) {
        super(NodeTypeEnum.DICTIONARY_LITERAL, positionInfo);
    }

    public void addEntry(LangASTNode key, LangASTNode value) {
        if (key != null && value != null) {
            entries.add(new Entry(key, value));
            addChild(key);
            addChild(value);
        }
    }

    public List<Entry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    @Override
    public void accept(LangASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "LangDictionaryLiteral{" +
                "entries=" + entries +
                '}';
    }

    public static class Entry {
        private final LangASTNode key;
        private final LangASTNode value;

        public Entry(LangASTNode key, LangASTNode value) {
            this.key = key;
            this.value = value;
        }

        public LangASTNode getKey() {
            return key;
        }

        public LangASTNode getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "Entry{" +
                    "key=" + key +
                    ", value=" + value +
                    '}';
        }
    }
}