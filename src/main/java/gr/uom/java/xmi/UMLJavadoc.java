package gr.uom.java.xmi;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class UMLJavadoc extends UMLAbstractDocumentation {
	private List<UMLTagElement> tags;

	public UMLJavadoc(String text, LocationInfo locationInfo) {
		super(text, locationInfo);
		this.tags = new ArrayList<UMLTagElement>();
	}

	public boolean isEmpty() {
		return tags.size() == 0;
	}

	public void addTag(UMLTagElement tag) {
		tags.add(tag);
	}

	public List<UMLTagElement> getTags() {
		return tags;
	}

	public boolean contains(String s) {
		for(UMLTagElement tag : tags) {
			if(tag.contains(s)) {
				return true;
			}
		}
		return false;
	}

	public boolean equalText(UMLJavadoc other) {
		return this.tags.equals(other.tags);
	}

	@Override
	public String getText() {
		StringBuilder sb = new StringBuilder();
		Scanner scanner = new Scanner(this.text);
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			line = line.trim();
			if(line.startsWith("/*")) {
				line = line.substring(2);
			}
			if(line.endsWith("*/")) {
				line = line.substring(0, line.length()-2);
			}
			if(line.startsWith("//")) {
				line = line.substring(2);
			}
			if(line.startsWith("*")) {
				line = line.substring(1);
			}
			line = line.trim();
			sb.append(line).append("\n");
		}
		scanner.close();
		return sb.toString();
	}
}
