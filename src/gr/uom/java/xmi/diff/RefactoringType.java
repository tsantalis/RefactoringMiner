package gr.uom.java.xmi.diff;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum RefactoringType {

	EXTRACT_OPERATION("Extract Method", "Extract Method (.+) extracted from (.+) in class (.+)", 2),
	RENAME_CLASS("Rename Class", "Rename Class (.+) renamed to (.+)"),
	MOVE_ATTRIBUTE("Move Attribute", "Move Attribute (.+) from class (.+) to class (.+)"),
	RENAME_METHOD("Rename Method", "Rename Method (.+) renamed to (.+) in class (.+)"),
	INLINE_OPERATION("Inline Method", "Inline Method (.+) inlined to (.+) in class (.+)", 2),
	MOVE_OPERATION("Move Method", "Move Method (.+) from class (.+) to (.+) from class (.+)"),
	PULL_UP_OPERATION("Pull Up Method", "Pull Up Method (.+) from class (.+) to (.+) from class (.+)", 1, 2),
	MOVE_CLASS("Move Class", "Move Class (.+) moved to (.+)"),
	MOVE_RENAME_CLASS("Move And Rename Class", ".+"),
	MOVE_CLASS_FOLDER("Move Class Folder", ".+"),
	PULL_UP_ATTRIBUTE("Pull Up Attribute", "Pull Up Attribute (.+) from class (.+) to class (.+)", 2),
	PUSH_DOWN_ATTRIBUTE("Push Down Attribute", "Push Down Attribute (.+) from class (.+) to class (.+)", 3),
	PUSH_DOWN_OPERATION("Push Down Method", "Push Down Method (.+) from class (.+) to (.+) from class (.+)", 3, 4),
	EXTRACT_INTERFACE("Extract Interface", "Extract Interface (.+) from classes \\[(.+)\\]", 2),
	EXTRACT_SUPERCLASS("Extract Superclass", "Extract Superclass (.+) from classes \\[(.+)\\]", 2),
	MERGE_OPERATION("Merge Method", ".+"),
	EXTRACT_AND_MOVE_OPERATION("Extract And Move Method", ".+"),
	CONVERT_ANONYMOUS_CLASS_TO_TYPE("Convert Anonymous Class to Type", ".+"),
	INTRODUCE_POLYMORPHISM("Introduce Polymorphism", ".+"),
	RENAME_PACKAGE("Rename Package", "Rename Package (.+) to (.+)");

	private String displayName;
	private Pattern regex;
	private int[] aggregateGroups;

	private RefactoringType(String displayName, String regex, int ... aggregateGroups) {
		this.displayName = displayName;
		this.regex = Pattern.compile(regex);
		this.aggregateGroups = aggregateGroups;
	}

	public String getDisplayName() {
		return this.displayName;
	}

    public String abbreviation() {
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

    public String getGroup(String refactoringDescription, int group) {
        Matcher m = regex.matcher(refactoringDescription);
        if (m.matches()) {
            return m.group(group);
        } else {
            throw new RuntimeException("Pattern not matched: " + refactoringDescription);
        }
    }
}
