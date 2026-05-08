package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import gr.uom.java.xmi.UMLNamedExport;

public class UMLNamedExportListDiff {
	private List<UMLNamedExport> removedNamedExports;
	private List<UMLNamedExport> addedNamedExports;
	private Set<Pair<UMLNamedExport, UMLNamedExport>> commonExports;
	private Set<UMLNamedExportDiff> changedExports;
	
	public UMLNamedExportListDiff(List<UMLNamedExport> oldExports, List<UMLNamedExport> newExports) {
		this.commonExports = new LinkedHashSet<>();
		this.changedExports = new LinkedHashSet<UMLNamedExportDiff>();
		this.removedNamedExports = new ArrayList<UMLNamedExport>();
		this.addedNamedExports = new ArrayList<UMLNamedExport>();
		if(oldExports.size() <= newExports.size()) {
			for(UMLNamedExport oldExport : oldExports) {
				int index = newExports.indexOf(oldExport);
				if(index != -1) {
					UMLNamedExport newExport = newExports.get(index);
					if(oldExport.equalSpecifiers(newExport)) {
						Pair<UMLNamedExport, UMLNamedExport> pair = Pair.of(oldExport, newExport);
						commonExports.add(pair);
					}
					else {
						UMLNamedExportDiff diff = new UMLNamedExportDiff(oldExport, newExport);
						changedExports.add(diff);
					}
				}
				else {
					removedNamedExports.add(oldExport);
				}
			}
		}
		else {
			for(UMLNamedExport newExport : newExports) {
				int index = oldExports.indexOf(newExport);
				if(index != -1) {
					UMLNamedExport oldExport = oldExports.get(index);
					if(oldExport.equalSpecifiers(newExport)) {
						Pair<UMLNamedExport, UMLNamedExport> pair = Pair.of(oldExport, newExport);
						commonExports.add(pair);
					}
					else {
						UMLNamedExportDiff diff = new UMLNamedExportDiff(oldExport, newExport);
						changedExports.add(diff);
					}
				}
				else {
					addedNamedExports.add(newExport);
				}
			}
		}
	}

	public List<UMLNamedExport> getRemovedNamedExports() {
		return removedNamedExports;
	}

	public List<UMLNamedExport> getAddedNamedExports() {
		return addedNamedExports;
	}

	public Set<Pair<UMLNamedExport, UMLNamedExport>> getCommonExports() {
		return commonExports;
	}

	public Set<UMLNamedExportDiff> getChangedExports() {
		return changedExports;
	}
}
