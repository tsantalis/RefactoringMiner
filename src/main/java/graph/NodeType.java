package graph;

/**
 * Node declarations
 */
public enum NodeType {
    PROJECT("project"), // final root of all nodes
    PACKAGE("package"), COMPILATION_UNIT("compilation_unit"), // logical node to represent file

    // nonterminal (children classes of AbstractTypeDeclaration)
    CLASS("class"), ANONY_CLASS("anonymous class"), //  INNER_CLASS("class"),
    INTERFACE("interface"),

    ENUM("enum"), ANNOTATION("@interface"), // annotation type declaration

    // terminal
    //  CONSTRUCTOR("constructor"),
    FIELD("field"), METHOD("method"), ENUM_CONSTANT("enum_constant"), INITIALIZER_BLOCK("initializer_block"),
  ANNOTATION_MEMBER("annotation_member"),

    HUNK("hunk");

    String label;

    NodeType(String label) {
        this.label = label;
    }

    public String asString() {
        return this.label;
    }
}
