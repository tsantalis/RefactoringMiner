package extension.ast.node;

public enum TypeObjectEnum {
    OBJECT("Object"),
    INT("int"),
    BOOLEAN("boolean"),
    STRING("String"),
    DOUBLE("double"),
    FLOAT("float"),
    LONG("long"),
    CHAR("char"),
    BYTE("byte"),
    SHORT("short"),
    VOID("void");

    private final String name;

    public String getName() {
        return name;
    }

    TypeObjectEnum(String name) {
        this.name = name;
    }

    /**
     * Returns the type name formatted with angle brackets for UML representation
     * @return Type name wrapped in angle brackets
     */
    public String getFormatTypeWithAngleBrackets
    () {
        return "<" + name + ">";
    }

    public static TypeObjectEnum fromType(String typeName) {
        String n = typeName.trim();
        if (n.startsWith("<") && n.endsWith(">") && n.length() > 2) {
            n = n.substring(1, n.length() - 1).trim();
        }

        int lastDot = n.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < n.length() - 1) {
            n = n.substring(lastDot + 1);
        }

        return switch (n) {
            case "int", "Integer", "integer" -> INT;
            case "boolean", "Boolean" -> BOOLEAN;
            case "String", "string" -> STRING;
            case "double", "Double" -> DOUBLE;
            case "float", "Float" -> FLOAT;
            case "long", "Long" -> LONG;
            case "char", "Char", "character", "Character" -> CHAR;
            case "byte", "Byte" -> BYTE;
            case "short", "Short" -> SHORT;
            case "Object", "object" -> OBJECT;
            default -> null;
        };
    }


}
