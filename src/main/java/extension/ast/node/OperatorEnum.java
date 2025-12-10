package extension.ast.node;

/**
 * Represents operators supported in the language AST.
 * Organized by categories for better structure.
 */
public enum OperatorEnum {
    // Arithmetic operators
    PLUS("+", OperatorCategory.ARITHMETIC),
    MINUS("-", OperatorCategory.ARITHMETIC),
    MULTIPLY("*", OperatorCategory.ARITHMETIC),
    DIVIDE("/", OperatorCategory.ARITHMETIC),
    FLOOR_DIVIDE("//", OperatorCategory.ARITHMETIC),
    MODULUS("%", OperatorCategory.ARITHMETIC),
    POWER("**", OperatorCategory.ARITHMETIC),
    MATMUL("@", OperatorCategory.ARITHMETIC),


    // Augmented assignment operators
    PLUS_ASSIGN("+=", OperatorCategory.ASSIGNMENT),
    MINUS_ASSIGN("-=", OperatorCategory.ASSIGNMENT),
    MULTIPLY_ASSIGN("*=", OperatorCategory.ASSIGNMENT),
    DIVIDE_ASSIGN("/=", OperatorCategory.ASSIGNMENT),
    FLOOR_DIVIDE_ASSIGN("//=", OperatorCategory.ASSIGNMENT),
    MODULUS_ASSIGN("%=", OperatorCategory.ASSIGNMENT),
    POWER_ASSIGN("**=", OperatorCategory.ASSIGNMENT),
    BITWISE_AND_ASSIGN("&=", OperatorCategory.ASSIGNMENT),
    BITWISE_OR_ASSIGN("|=", OperatorCategory.ASSIGNMENT),
    BITWISE_XOR_ASSIGN("^=", OperatorCategory.ASSIGNMENT),
    LEFT_SHIFT_ASSIGN("<<=", OperatorCategory.ASSIGNMENT),
    RIGHT_SHIFT_ASSIGN(">>=", OperatorCategory.ASSIGNMENT),
    INCREMENT("++", OperatorCategory.ASSIGNMENT),
    DECREMENT("--", OperatorCategory.ASSIGNMENT),

    // Comparison operators
    GREATER_THAN(">", OperatorCategory.COMPARISON),
    LESS_THAN("<", OperatorCategory.COMPARISON),
    GREATER_EQUAL(">=", OperatorCategory.COMPARISON),
    LESS_EQUAL("<=", OperatorCategory.COMPARISON),
    EQUAL("==", OperatorCategory.COMPARISON),
    NOT_EQUAL("!=", OperatorCategory.COMPARISON),
    NOT_EQUAL_ALT("<>", OperatorCategory.COMPARISON),

    // Logical operators
    AND("and", OperatorCategory.LOGICAL),
    OR("or", OperatorCategory.LOGICAL),
    NOT("not", OperatorCategory.LOGICAL),

    // Bitwise operators
    BITWISE_AND("&", OperatorCategory.BITWISE),
    BITWISE_OR("|", OperatorCategory.BITWISE),
    BITWISE_XOR("^", OperatorCategory.BITWISE),
    BITWISE_NOT("~", OperatorCategory.BITWISE),
    LEFT_SHIFT("<<", OperatorCategory.BITWISE),
    RIGHT_SHIFT(">>", OperatorCategory.BITWISE),

    // Membership operators
    IN("in", OperatorCategory.MEMBERSHIP),
    NOT_IN("notin", OperatorCategory.MEMBERSHIP),

    // Identity operators
    IS("is", OperatorCategory.IDENTITY),
    IS_NOT("isnot", OperatorCategory.IDENTITY);

    private final String symbol;
    private final OperatorCategory category;

    OperatorEnum(String symbol, OperatorCategory category) {
        this.symbol = symbol;
        this.category = category;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getJavaSymbol() {
        if (symbol.equals(AND.symbol)) {
            return "&&";
        }
        else if (symbol.equals(OR.symbol)) {
            return "||";
        }
        return symbol;
    }

    public OperatorCategory getCategory() {
        return category;
    }

    /**
     * Find operator enum by its symbol representation.
     */
    public static OperatorEnum fromSymbol(String symbol) {
        for (OperatorEnum op : values()) {
            if (op.symbol.equals(symbol)) {
                return op;
            }
        }
        throw new IllegalArgumentException("Unknown operator: " + symbol);
    }

    /**
     * Check if a string is a valid operator symbol.
     */
    public static boolean isOperator(String symbol) {
        for (OperatorEnum op : values()) {
            if (op.symbol.equals(symbol)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Categorizes operators by type.
     */
    public enum OperatorCategory {
        ARITHMETIC,
        COMPARISON,
        LOGICAL,
        BITWISE,
        MEMBERSHIP,
        IDENTITY,
        ASSIGNMENT;
    }
}