package gr.uom.java.xmi.decomposition.replacement;

import gr.uom.java.xmi.diff.StringDistance;

public class Replacement {
	private String before;
	private String after;
	private ReplacementType type;
	
	public Replacement(String before, String after, ReplacementType type) {
		this.before = before;
		this.after = after;
		this.type = type;
	}

	public String getBefore() {
		return before;
	}

	public String getAfter() {
		return after;
	}

	public ReplacementType getType() {
		return type;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((after == null) ? 0 : after.hashCode());
		result = prime * result + ((before == null) ? 0 : before.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if(obj instanceof Replacement) {
			Replacement other = (Replacement)obj;
			return this.before.equals(other.before) && this.after.equals(other.after) && this.type.equals(other.type);
		}
		return false;
	}
	
	public String toString() {
		return before + " -> " + after;
	}

	public double normalizedEditDistance() {
		String s1 = getBefore();
		String s2 = getAfter();
		int distance = StringDistance.editDistance(s1, s2);
		double normalized = (double)distance/(double)Math.max(s1.length(), s2.length());
		return normalized;
	}
	
	public boolean involvesVariable() {
		return type.equals(ReplacementType.VARIABLE_NAME) ||
				type.equals(ReplacementType.BOOLEAN_REPLACED_WITH_VARIABLE) ||
				type.equals(ReplacementType.TYPE_LITERAL_REPLACED_WITH_VARIABLE) ||
				type.equals(ReplacementType.ARGUMENT_REPLACED_WITH_VARIABLE) ||
				type.equals(ReplacementType.VARIABLE_REPLACED_WITH_METHOD_INVOCATION) ||
				type.equals(ReplacementType.VARIABLE_REPLACED_WITH_EXPRESSION_OF_METHOD_INVOCATION) ||
				type.equals(ReplacementType.VARIABLE_REPLACED_WITH_ARRAY_ACCESS) ||
				type.equals(ReplacementType.VARIABLE_REPLACED_WITH_PREFIX_EXPRESSION) ||
				type.equals(ReplacementType.VARIABLE_REPLACED_WITH_STRING_LITERAL) ||
				type.equals(ReplacementType.VARIABLE_REPLACED_WITH_NULL_LITERAL) ||
				type.equals(ReplacementType.VARIABLE_REPLACED_WITH_NUMBER_LITERAL);
	}

	public enum ReplacementType {
		TYPE, STRING_LITERAL, NUMBER_LITERAL, ANONYMOUS_CLASS_DECLARATION, INFIX_OPERATOR, VARIABLE_NAME, VARIABLE_DECLARATION,
		MERGE_VARIABLES, SPLIT_VARIABLE, ADD_VARIABLE,
		INVERT_CONDITIONAL,
		BOOLEAN_REPLACED_WITH_VARIABLE,
		BOOLEAN_REPLACED_WITH_ARGUMENT,
		TYPE_LITERAL_REPLACED_WITH_VARIABLE,
		METHOD_INVOCATION,
		METHOD_INVOCATION_EXPRESSION,
		METHOD_INVOCATION_ARGUMENT,
		METHOD_INVOCATION_ARGUMENT_WRAPPED,
		METHOD_INVOCATION_ARGUMENT_CONCATENATED,
		METHOD_INVOCATION_NAME,
		METHOD_INVOCATION_NAME_AND_ARGUMENT,
		METHOD_INVOCATION_NAME_AND_EXPRESSION,
		ARGUMENT_REPLACED_WITH_VARIABLE,
		ARGUMENT_REPLACED_WITH_RETURN_EXPRESSION,
		ARGUMENT_REPLACED_WITH_STATEMENT,
		ARGUMENT_REPLACED_WITH_RIGHT_HAND_SIDE_OF_ASSIGNMENT_EXPRESSION,
		VARIABLE_REPLACED_WITH_METHOD_INVOCATION,
		VARIABLE_REPLACED_WITH_EXPRESSION_OF_METHOD_INVOCATION,
		VARIABLE_REPLACED_WITH_ARRAY_ACCESS,
		VARIABLE_REPLACED_WITH_PREFIX_EXPRESSION,
		VARIABLE_REPLACED_WITH_STRING_LITERAL,
		VARIABLE_REPLACED_WITH_NULL_LITERAL,
		VARIABLE_REPLACED_WITH_NUMBER_LITERAL,
		ARRAY_ACCESS_REPLACED_WITH_METHOD_INVOCATION,
		NULL_LITERAL_REPLACED_WITH_CONDITIONAL_EXPRESSION,
		CLASS_INSTANCE_CREATION,
		CLASS_INSTANCE_CREATION_ARGUMENT,
		CLASS_INSTANCE_CREATION_REPLACED_WITH_METHOD_INVOCATION,
		BUILDER_REPLACED_WITH_CLASS_INSTANCE_CREATION,
		FIELD_ASSIGNMENT_REPLACED_WITH_SETTER_METHOD_INVOCATION,
		ARRAY_CREATION_REPLACED_WITH_DATA_STRUCTURE_CREATION,
		ARRAY_INITIALIZER_REPLACED_WITH_METHOD_INVOCATION_ARGUMENTS,
		EXPRESSION_REPLACED_WITH_TERNARY_ELSE,
		EXPRESSION_REPLACED_WITH_TERNARY_THEN,
		COMPOSITE,
		CONCATENATION, CONDITIONAL;
	}
}
