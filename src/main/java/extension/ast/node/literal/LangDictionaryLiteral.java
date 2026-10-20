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

    public void addEntry(LangASTNode key, LangASTNode value, PositionInfo positionInfo) {
        if (key != null && value != null) {
            entries.add(new Entry(key, value, positionInfo));
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

    //an entry is located at its key-value pair, i.e., 'darwin': ['open']
    public static class Entry {
        private final LangASTNode key;
        private final LangASTNode value;
        private final PositionInfo positionInfo;

        public Entry(LangASTNode key, LangASTNode value, PositionInfo positionInfo) {
            this.key = key;
            this.value = value;
            this.positionInfo = positionInfo;
        }

        public int getStartChar() {
            return positionInfo.getStartChar();
        }

        //the end is exclusive, as in LangASTNode
        public int getEndChar() {
            return positionInfo.getEndChar() + 1;
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