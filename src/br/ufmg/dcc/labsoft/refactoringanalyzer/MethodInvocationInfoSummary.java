package br.ufmg.dcc.labsoft.refactoringanalyzer;

import gr.uom.java.xmi.MethodInvocationInfo;
import gr.uom.java.xmi.MethodInvocationInfo.MethodInfo;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.ExtractAndMoveOperationRefactoring;
import gr.uom.java.xmi.diff.ExtractOperationRefactoring;
import gr.uom.java.xmi.diff.Refactoring;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class MethodInvocationInfoSummary {

	private Map<String, MethodInfoSummary> map = new HashMap<>();
	
	public static class MethodInfoSummary {
		private int countDup = 0;
		private int countInit = 0;
		private int countCurrent = 0;
		private int countMax = 0;
		private LinkedList<String> revExtracted = null;
		private boolean dead = true;
		private boolean extracted = false;
		private boolean moved = false;
	}

	public void analyzeCurrent(UMLModel model) {
		MethodInvocationInfo mii = model.getMethodInvocationInfo();
		for (MethodInfo methodInfo : mii.getMethodInfoCollection()) {
			if (!methodInfo.isExternal()) {
				MethodInfoSummary info = this.getOrCreate(methodInfo.getBindingKey());
				final int count = methodInfo.getCount();
				info.dead = false;
				info.countCurrent = count;
				info.countMax = Math.max(info.countMax, count);
			}
		}
	}
	
	public void analyzeRevision(Revision rev, UMLModel model, List<Refactoring> refactorings) {
		MethodInvocationInfo mii = model.getMethodInvocationInfo();
		for (MethodInfo methodInfo : mii.getMethodInfoCollection()) {
			if (!methodInfo.isExternal()) {
				MethodInfoSummary info = this.getOrCreate(methodInfo.getBindingKey());
				final int count = methodInfo.getCount();
				info.countMax = Math.max(info.countMax, count);
			}
		}
		for (Refactoring ref : refactorings) {
			this.analyzeRefactoring(rev, model, ref);
		}
	}

	private void analyzeRefactoring(Revision rev, UMLModel model, Refactoring refactoring) {
		String bindingKey;
		if (refactoring instanceof ExtractOperationRefactoring) {
			ExtractOperationRefactoring emr = (ExtractOperationRefactoring) refactoring;
			bindingKey = emr.getExtractedOperation().getXmiID();
		}
		else if (refactoring instanceof ExtractAndMoveOperationRefactoring) {
			ExtractAndMoveOperationRefactoring emr = (ExtractAndMoveOperationRefactoring) refactoring;
			bindingKey = emr.getExtractedOperation().getXmiID();
		} else {
			return;
		}
		if (bindingKey == null) {
			System.out.println(String.format("WARN refactoring sem binding: %s %s", rev.getId(), refactoring));
			return;
		}
		MethodInfoSummary info = this.getOrCreate(bindingKey);
		if (info.revExtracted != null && !info.revExtracted.getLast().equals(rev.getId())) {
			info.revExtracted.add(rev.getId());
			return;
		}
		if (info.revExtracted == null) {
			info.revExtracted = new LinkedList<>();
			info.revExtracted.add(rev.getId());
			info.extracted = true;
			info.moved = refactoring instanceof ExtractAndMoveOperationRefactoring;
			MethodInvocationInfo mii = model.getMethodInvocationInfo();
			MethodInfo methodInfo = mii.getMethodInfo(bindingKey);
			if (methodInfo != null) {
				info.countInit = methodInfo.getCount();
			} else {
				System.out.println(String.format("WARN refactoring com countInit=0: %s %s", rev.getId(), refactoring));
			}
		}
		info.countDup += 1;
	}

	private MethodInfoSummary getOrCreate(String bindingKey) {
		assert bindingKey != null;
	    MethodInfoSummary methodInfo = this.map.get(bindingKey);
	    if (methodInfo == null) {
	    	methodInfo = new MethodInfoSummary();
	    	this.map.put(bindingKey, methodInfo);
	    }
	    return methodInfo;
    }

	public void print(PrintStream out) {
	    for (Entry<String, MethodInfoSummary> e : this.map.entrySet()) {
	    	String key = e.getKey();
	    	MethodInfoSummary info = e.getValue();
	    	if (info.extracted) {
	    		out.println(String.format("%s\t%b\t%b\t%b\t%d\t%d\t%d\t%d\t%s", key, info.extracted, info.moved, info.dead, info.countDup, info.countInit, info.countCurrent, info.countMax, info.revExtracted));
	    	}
	    }
    }

}
