package org.refactoringminer.api;

import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.refactoringminer.util.AstUtils;
import org.refactoringminer.utils.RefactoringRelationship;

public enum RefactoringType {

	EXTRACT_OPERATION("Extract Method", "Extract Method (.+) extracted from (.+) in class (.+)", 2),
	RENAME_CLASS("Rename Class", "Rename Class (.+) renamed to (.+)"),
	MOVE_ATTRIBUTE("Move Attribute", "Move Attribute (.+) from class (.+) to (.+) from class (.+)"),
	MOVE_RENAME_ATTRIBUTE("Move And Rename Attribute", "Move And Rename Attribute (.+) renamed to (.+) and moved from class (.+) to class (.+)"),
	REPLACE_ATTRIBUTE("Replace Attribute", "Replace Attribute (.+) from class (.+) with (.+) from class (.+)"),
	RENAME_METHOD("Rename Method", "Rename Method (.+) renamed to (.+) in class (.+)"),
	INLINE_OPERATION("Inline Method", "Inline Method (.+) inlined to (.+) in class (.+)", 2),
	MOVE_OPERATION("Move Method", "Move Method (.+) from class (.+) to (.+) from class (.+)"),
	MOVE_AND_RENAME_OPERATION("Move And Rename Method", "Move And Rename Method (.+) from class (.+) to (.+) from class (.+)"),
	PULL_UP_OPERATION("Pull Up Method", "Pull Up Method (.+) from class (.+) to (.+) from class (.+)", 1, 2),
	MOVE_CLASS("Move Class", "Move Class (.+) moved to (.+)"),
	MOVE_RENAME_CLASS("Move And Rename Class", ".+"),
	MOVE_SOURCE_FOLDER("Move Source Folder", "Move Source Folder (.+) to (.+)"),
	PULL_UP_ATTRIBUTE("Pull Up Attribute", "Pull Up Attribute (.+) from class (.+) to (.+) from class (.+)", 2),
	PUSH_DOWN_ATTRIBUTE("Push Down Attribute", "Push Down Attribute (.+) from class (.+) to (.+) from class (.+)", 3),
	PUSH_DOWN_OPERATION("Push Down Method", "Push Down Method (.+) from class (.+) to (.+) from class (.+)", 3, 4),
	EXTRACT_INTERFACE("Extract Interface", "Extract Interface (.+) from classes \\[(.+)\\]", 2),
	EXTRACT_SUPERCLASS("Extract Superclass", "Extract Superclass (.+) from classes \\[(.+)\\]", 2),
	EXTRACT_SUBCLASS("Extract Subclass", "Extract Subclass (.+) from class (.+)"),
	EXTRACT_CLASS("Extract Class", "Extract Class (.+) from class (.+)"),
	MERGE_OPERATION("Merge Method", ".+"),
	EXTRACT_AND_MOVE_OPERATION("Extract And Move Method", "Extract And Move Method (.+) extracted from (.+) in class (.+) & moved to class (.+)"),
	MOVE_AND_INLINE_OPERATION("Move And Inline Method", "Move And Inline Method (.+) moved from class (.+) to class (.+) & inlined to (.+)"),
	CONVERT_ANONYMOUS_CLASS_TO_TYPE("Convert Anonymous Class to Type", ".+"),
	INTRODUCE_POLYMORPHISM("Introduce Polymorphism", ".+"),
	RENAME_PACKAGE("Change Package", "Change Package (.+) to (.+)"),
	EXTRACT_VARIABLE("Extract Variable", "Extract Variable (.+) in method (.+) from class (.+)"),
	EXTRACT_ATTRIBUTE("Extract Attribute", "Extract Attribute (.+) in class (.+)"),
	INLINE_VARIABLE("Inline Variable", "Inline Variable (.+) in method (.+) from class (.+)"),
	RENAME_VARIABLE("Rename Variable", "Rename Variable (.+) to (.+) in method (.+) from class (.+)"),
	RENAME_PARAMETER("Rename Parameter", "Rename Parameter (.+) to (.+) in method (.+) from class (.+)"),
	RENAME_ATTRIBUTE("Rename Attribute", "Rename Attribute (.+) to (.+) in class (.+)"),
	MERGE_VARIABLE("Merge Variable", "Merge Variable \\[(.+)\\] to (.+) in method (.+) from class (.+)"),
	MERGE_PARAMETER("Merge Parameter", "Merge Parameter \\[(.+)\\] to (.+) in method (.+) from class (.+)"),
	MERGE_ATTRIBUTE("Merge Attribute", "Merge Attribute \\[(.+)\\] to (.+) in class (.+)"),
	SPLIT_VARIABLE("Split Variable", "Split Variable (.+) to \\[(.+)\\] in method (.+) from class (.+)"),
	SPLIT_PARAMETER("Split Parameter", "Split Parameter (.+) to \\[(.+)\\] in method (.+) from class (.+)"),
	SPLIT_ATTRIBUTE("Split Attribute", "Split Attribute (.+) to \\[(.+)\\] in class (.+)"),
	REPLACE_VARIABLE_WITH_ATTRIBUTE("Replace Variable With Attribute", "Replace Variable With Attribute (.+) to (.+) in method (.+) from class (.+)"),
	PARAMETERIZE_VARIABLE("Parameterize Variable", "Parameterize Variable (.+) to (.+) in method (.+) from class (.+)"),
	CHANGE_RETURN_TYPE("Change Return Type", "Change Return Type (.+) to (.+) in method (.+) from class (.+)"),
	CHANGE_VARIABLE_TYPE("Change Variable Type", "Change Variable Type (.+) to (.+) in method (.+) from class (.+)"),
	CHANGE_PARAMETER_TYPE("Change Parameter Type", "Change Parameter Type (.+) to (.+) in method (.+) from class (.+)"),
	CHANGE_ATTRIBUTE_TYPE("Change Attribute Type", "Change Attribute Type (.+) to (.+) in class (.+)"),
	ADD_METHOD_ANNOTATION("Add Method Annotation", "Add Method Annotation (.+) in method (.+) from class (.+)"),
	REMOVE_METHOD_ANNOTATION("Remove Method Annotation", "Remove Method Annotation (.+) in method (.+) from class (.+)"),
	MODIFY_METHOD_ANNOTATION("Modify Method Annotation", "Modify Method Annotation (.+) to (.+) in method (.+) from class (.+)"),
	ADD_ATTRIBUTE_ANNOTATION("Add Attribute Annotation", "Add Attribute Annotation (.+) in attribute (.+) from class (.+)"),
	REMOVE_ATTRIBUTE_ANNOTATION("Remove Attribute Annotation", "Remove Attribute Annotation (.+) in attribute (.+) from class (.+)"),
	MODIFY_ATTRIBUTE_ANNOTATION("Modify Attribute Annotation", "Modify Attribute Annotation (.+) to (.+) in attribute (.+) from class (.+)"),
	ADD_CLASS_ANNOTATION("Add Class Annotation", "Add Class Annotation (.+) in class (.+)"),
	REMOVE_CLASS_ANNOTATION("Remove Class Annotation", "Remove Class Annotation (.+) in class (.+)"),
	MODIFY_CLASS_ANNOTATION("Modify Class Annotation", "Modify Class Annotation (.+) to (.+) in class (.+)"),
	ADD_PARAMETER_ANNOTATION("Add Parameter Annotation", "Add Parameter Annotation (.+) in parameter (.+) in method (.+) from class (.+)"),
	REMOVE_PARAMETER_ANNOTATION("Remove Parameter Annotation", "Remove Parameter Annotation (.+) in parameter (.+) in method (.+) from class (.+)"),
	MODIFY_PARAMETER_ANNOTATION("Modify Parameter Annotation", "Modify Parameter Annotation (.+) to (.+) in parameter (.+) in method (.+) from class (.+)"),
	ADD_PARAMETER("Add Parameter", "Add Parameter (.+) in method (.+) from class (.+)"),
	REMOVE_PARAMETER("Remove Parameter", "Remove Parameter (.+) in method (.+) from class (.+)"),
	REORDER_PARAMETER("Reorder Parameter", "Reorder Parameter \\[(.+)\\] to \\[(.+)\\] in method (.+) from class (.+)"),
	ADD_VARIABLE_ANNOTATION("Add Variable Annotation", "Add Variable Annotation (.+) in variable (.+) in method (.+) from class (.+)"),
	REMOVE_VARIABLE_ANNOTATION("Remove Variable Annotation", "Remove Variable Annotation (.+) in variable (.+) in method (.+) from class (.+)"),
	MODIFY_VARIABLE_ANNOTATION("Modify Variable Annotation", "Modify Variable Annotation (.+) to (.+) in variable (.+) in method (.+) from class (.+)"),;

	private String displayName;
	private Pattern regex;
	private int[] aggregateGroups;
	public static RefactoringType[] ALL = {
		RENAME_CLASS,
		MOVE_CLASS,
		MOVE_SOURCE_FOLDER,
		RENAME_METHOD,
		EXTRACT_OPERATION,
		INLINE_OPERATION,
		MOVE_OPERATION,
		PULL_UP_OPERATION,
		PUSH_DOWN_OPERATION,
		MOVE_ATTRIBUTE,
		MOVE_RENAME_ATTRIBUTE,
		REPLACE_ATTRIBUTE,
		PULL_UP_ATTRIBUTE,
		PUSH_DOWN_ATTRIBUTE,
		EXTRACT_INTERFACE,
		EXTRACT_SUPERCLASS,
		EXTRACT_SUBCLASS,
		EXTRACT_CLASS,
		EXTRACT_AND_MOVE_OPERATION,
		MOVE_RENAME_CLASS,
		RENAME_PACKAGE,
		EXTRACT_VARIABLE,
		INLINE_VARIABLE,
		RENAME_VARIABLE,
		RENAME_PARAMETER,
		RENAME_ATTRIBUTE,
		REPLACE_VARIABLE_WITH_ATTRIBUTE,
		PARAMETERIZE_VARIABLE,
		MERGE_VARIABLE,
		MERGE_PARAMETER,
		MERGE_ATTRIBUTE,
		SPLIT_VARIABLE,
		SPLIT_PARAMETER,
		SPLIT_ATTRIBUTE,
		CHANGE_RETURN_TYPE,
		CHANGE_VARIABLE_TYPE,
		CHANGE_PARAMETER_TYPE,
		CHANGE_ATTRIBUTE_TYPE,
		EXTRACT_ATTRIBUTE,
		MOVE_AND_RENAME_OPERATION,
		MOVE_AND_INLINE_OPERATION,
		ADD_METHOD_ANNOTATION,
		REMOVE_METHOD_ANNOTATION,
		MODIFY_METHOD_ANNOTATION,
		ADD_ATTRIBUTE_ANNOTATION,
		REMOVE_ATTRIBUTE_ANNOTATION,
		MODIFY_ATTRIBUTE_ANNOTATION,
		ADD_CLASS_ANNOTATION,
		REMOVE_CLASS_ANNOTATION,
		MODIFY_CLASS_ANNOTATION,
		ADD_PARAMETER,
		REMOVE_PARAMETER,
		REORDER_PARAMETER,
		ADD_PARAMETER_ANNOTATION,
		REMOVE_PARAMETER_ANNOTATION,
		MODIFY_PARAMETER_ANNOTATION,
		ADD_VARIABLE_ANNOTATION,
		REMOVE_VARIABLE_ANNOTATION,
		MODIFY_VARIABLE_ANNOTATION
	};

	private RefactoringType(String displayName, String regex, int ... aggregateGroups) {
		this.displayName = displayName;
		this.regex = Pattern.compile(regex);
		this.aggregateGroups = aggregateGroups;
	}

	public Pattern getRegex() {
        return regex;
    }

    public String getDisplayName() {
		return this.displayName;
	}

    public String getAbbreviation() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.displayName.length(); i++) {
            char c = this.displayName.charAt(i);
            if (Character.isLetter(c) && Character.isUpperCase(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public String aggregate(String refactoringDescription) {
        Matcher m = regex.matcher(refactoringDescription);
        if (m.matches()) {
            StringBuilder sb = new StringBuilder();
            int current = 0;
            int replace = 0;
            for (int g = 1; g <= m.groupCount(); g++) {
                sb.append(refactoringDescription, current, m.start(g));
                if (aggregateGroups.length > replace && g == aggregateGroups[replace]) {
                    sb.append('*');
                    replace++;
                } else {
                    sb.append(refactoringDescription, m.start(g), m.end(g));
                }
                current = m.end(g);
            }
            sb.append(refactoringDescription, current, refactoringDescription.length());
            return sb.toString();
        } else {
            throw new RuntimeException("Pattern not matched: " + refactoringDescription);
        }
    }

    public static void parse(String refactoringDescription, Collection<RefactoringRelationship> result) {
        RefactoringType refType = extractFromDescription(refactoringDescription);
        Matcher m = refType.regex.matcher(refactoringDescription);
        if (m.matches()) {
            switch (refType) {
            case RENAME_CLASS:
            case MOVE_CLASS:
            case RENAME_PACKAGE: {
                String entityBefore = m.group(1);
                String entityAfter = m.group(2);
                result.add(new RefactoringRelationship(refType, entityBefore, entityAfter));
                return;
            }
            case MOVE_OPERATION:
            case PULL_UP_OPERATION:
            case PUSH_DOWN_OPERATION: {
                String entityBefore = methodKey(m.group(1), m.group(2));
                String entityAfter = methodKey(m.group(3), m.group(4));
                result.add(new RefactoringRelationship(refType, entityBefore, entityAfter));
                return;
            }
            case RENAME_METHOD:
            case INLINE_OPERATION: {
                String entityBefore = methodKey(m.group(1), m.group(3));
                String entityAfter = methodKey(m.group(2), m.group(3));
                result.add(new RefactoringRelationship(refType, entityBefore, entityAfter));
                return;
            }
            case EXTRACT_OPERATION: {
                String entityBefore = methodKey(m.group(2), m.group(3));
                String entityAfter = methodKey(m.group(1), m.group(3));
                result.add(new RefactoringRelationship(refType, entityBefore, entityAfter));
                return;
            }
            case EXTRACT_INTERFACE:
            case EXTRACT_SUPERCLASS: {
                String entityAfter = m.group(1);
                String[] entityBeforeArray = m.group(2).split(" *, *");
                for (String entityBefore : entityBeforeArray) {
                    result.add(new RefactoringRelationship(refType, entityBefore, entityAfter));
                }
                return;
            }
            case MOVE_ATTRIBUTE:
            case PULL_UP_ATTRIBUTE:
            case PUSH_DOWN_ATTRIBUTE: {
                String entityBefore = attributeKey(m.group(1), m.group(2));
                String entityAfter = attributeKey(m.group(1), m.group(3));
                result.add(new RefactoringRelationship(refType, entityBefore, entityAfter));
                return;
            }
            default:
                throw new RuntimeException("Unable do parse: " + refactoringDescription);
            }
        } else {
            throw new RuntimeException("Pattern not matched: " + refactoringDescription);
        }
    }

    private static String methodKey(String methodSignature, String typeKey) {
        return typeKey + "#" + AstUtils.normalizeMethodSignature(methodSignature);
    }

    private static String attributeKey(String attribute, String typeKey) {
        return typeKey + "#" + AstUtils.normalizeAttribute(attribute);
    }

    public List<RefactoringRelationship> parseRefactoring(String refactoringDescription) {
        List<RefactoringRelationship> result;
        Matcher m = regex.matcher(refactoringDescription);
        if (m.matches()) {
            
            for (int g = 1; g <= m.groupCount(); g++) {
                
            }
            return null;
        } else {
            throw new RuntimeException("Pattern not matched: " + refactoringDescription);
        }
    }

    public static RefactoringType extractFromDescription(String refactoringDescription) {
        for (RefactoringType refType : RefactoringType.values()) {
            if (refactoringDescription.startsWith(refType.getDisplayName())) {
                return refType;
            }
        }
        throw new RuntimeException("Unknown refactoring type: " + refactoringDescription);
    }

    public String getGroup(String refactoringDescription, int group) {
        Matcher m = regex.matcher(refactoringDescription);
        if (m.matches()) {
            return m.group(group);
        } else {
            throw new RuntimeException("Pattern not matched: " + refactoringDescription);
        }
    }
    
    public static RefactoringType fromName(String name) {
      String lcName = name.toLowerCase();
      for (RefactoringType rt : RefactoringType.values()) {
        if (lcName.equals(rt.getDisplayName().toLowerCase())) {
          return rt;
        }
      }
      throw new IllegalArgumentException("refactoring type not known " + name);
    }
}
