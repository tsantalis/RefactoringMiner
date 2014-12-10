package gr.uom.java.xmi;

import gr.uom.java.xmi.diff.Refactoring;
import gr.uom.java.xmi.diff.UMLModelDiff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UMLModelSet {

	Map<String, UMLModel> map = new HashMap<>();

	public UMLModel get(String packageRoot) {
		UMLModel model = map.get(packageRoot);
		if (model == null) {
			model = new UMLModel();
			map.put(packageRoot, model);
		}
		return model;
	}

	public UMLModel getFirst() {
		return map.entrySet().iterator().next().getValue();
	}

	public List<Refactoring> detectRefactorings(UMLModelSet umlModelSet) {
		List<Refactoring> all = new ArrayList<>();
		for (String key : map.keySet()) {
			UMLModelDiff diff = map.get(key).diff(umlModelSet.get(key));
			all.addAll(diff.getRefactorings());
		}
		return all;
	}

	public Collection<UMLModel> getUMLModels() {
		return map.values();
	}

}
