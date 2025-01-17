package gui.graph;

public enum EdgeType {
    /**
     * file&folder level edges *
     */
    CONTAIN(true, "contain"), // physical relation
    IMPORT(false, "import"), EXTEND(false, "extend"), IMPLEMENT(false, "implement"),
    /**
     * inside-file edges *
     */
    // define field/terminal/constructor/inner type/constant
    DEFINE(true, "define"),
    /**
     * across-node edges *
     */
    // inter-field/terminal edges
    ACCESS(false, "access_field"), //  READ("reads field"),
    //  WRITE("writes field"),
    // call method
    CALL(false, "call_method"), // declare/initialize object
    //  DECLARE(false, "declare_object"),
    PARAM(false, "parameter_type"), TYPE(false, "field_type"), INITIALIZE(false, "initialize"), RETURN(false,
          "return_type");

    Boolean isStructural;
    String label;

    EdgeType(Boolean isStructural, String label) {
        this.isStructural = isStructural;
        this.label = label;
    }

    public String asString() {
        return this.label;
    }

    public Boolean isStructural() {
        return this.isStructural;
    }
}
