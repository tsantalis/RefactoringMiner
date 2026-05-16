package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import gr.uom.java.xmi.UMLNamedExport;
import gr.uom.java.xmi.decomposition.LeafExpression;

public class UMLNamedExportDiff {
	private UMLNamedExport originalNamedExport;
	private UMLNamedExport nextNamedExport;
	private Set<Pair<LeafExpression, LeafExpression>> commonSpecifiers;
	private List<LeafExpression> removedSpecifiers;
	private List<LeafExpression> addedSpecifiers;

	public UMLNamedExportDiff(UMLNamedExport oldExport, UMLNamedExport newExport) {
		this.originalNamedExport = oldExport;
		this.nextNamedExport = newExport;
		this.commonSpecifiers = new LinkedHashSet<Pair<LeafExpression,LeafExpression>>();
		this.removedSpecifiers = new ArrayList<LeafExpression>();
		this.addedSpecifiers = new ArrayList<LeafExpression>();
		List<LeafExpression> specifiers1 = oldExport.getSpecifiers();
		List<LeafExpression> specifiers2 = newExport.getSpecifiers();
		if(specifiers1.size() <= specifiers2.size()) {
			for(LeafExpression specifier1 : specifiers1) {
				boolean matchFound = false;
				for(LeafExpression specifier2 : specifiers2) {
					if(specifier1.getString().equals(specifier2.getString())) {
						Pair<LeafExpression, LeafExpression> pair = Pair.of(specifier1, specifier2);
						commonSpecifiers.add(pair);
						matchFound = true;
						break;
					}
				}
				if(!matchFound) {
					this.removedSpecifiers.add(specifier1);
				}
			}
		}
		else {
			for(LeafExpression specifier2 : specifiers2) {
				boolean matchFound = false;
				for(LeafExpression specifier1 : specifiers1) {
					if(specifier1.getString().equals(specifier2.getString())) {
						Pair<LeafExpression, LeafExpression> pair = Pair.of(specifier1, specifier2);
						commonSpecifiers.add(pair);
						matchFound = true;
						break;
					}
				}
				if(!matchFound) {
					this.addedSpecifiers.add(specifier2);
				}
			}
		}
	}

	public UMLNamedExport getOriginalNamedExport() {
		return originalNamedExport;
	}

	public UMLNamedExport getNextNamedExport() {
		return nextNamedExport;
	}

	public Set<Pair<LeafExpression, LeafExpression>> getCommonSpecifiers() {
		return commonSpecifiers;
	}

	public List<LeafExpression> getRemovedSpecifiers() {
		return removedSpecifiers;
	}

	public List<LeafExpression> getAddedSpecifiers() {
		return addedSpecifiers;
	}
}
