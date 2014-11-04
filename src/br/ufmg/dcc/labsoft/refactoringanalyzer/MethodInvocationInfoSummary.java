package br.ufmg.dcc.labsoft.refactoringanalyzer;

import gr.uom.java.xmi.MethodInvocationInfo;
import gr.uom.java.xmi.MethodInvocationInfo.MethodInfo;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.ExtractOperationRefactoring;
import gr.uom.java.xmi.diff.Refactoring;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class MethodInvocationInfoSummary {

	private Map<String, MethodInfoSummary> map = new HashMap<>();
	
	public static class MethodInfoSummary {
		private int countInit = 0;
		private int countCurrent = 0;
		private int countMax = 0;
		private int timeInit = Integer.MAX_VALUE;
		private boolean dead = true;
		private boolean extracted = false;
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
	
	public void analyzeRevision(Revision rev, UMLModel model) {
		MethodInvocationInfo mii = model.getMethodInvocationInfo();
		for (MethodInfo methodInfo : mii.getMethodInfoCollection()) {
			if (!methodInfo.isExternal()) {
				MethodInfoSummary info = this.getOrCreate(methodInfo.getBindingKey());
				final int count = methodInfo.getCount();
				final int revTime = rev.getTime();
				if (rev.isCurrent()) {
					info.dead = false;
					info.countCurrent = count;
				}
				info.countMax = Math.max(info.countMax, count);
				if (revTime < info.timeInit) {
					info.countInit = count;
					info.timeInit = revTime;
				}
			}
		}
	}

	public void analyzeRefactoring(Revision rev, Refactoring refactoring) {
		if (refactoring instanceof ExtractOperationRefactoring) {
			ExtractOperationRefactoring emr = (ExtractOperationRefactoring) refactoring;
			String bindingKey = emr.getExtractedOperation().getXmiID();
			if (bindingKey == null) {
				System.out.println("ERRO refactoring sem binding: " + refactoring);
				return;
			}
			MethodInfoSummary info = this.getOrCreate(bindingKey);
			info.extracted = true;
			if (info.countInit == 0) {
				System.out.println(String.format("Refactoring sem count: %s %s", rev.getId(), refactoring));
			}
		}
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
    		out.println(String.format("%s\t%b\t%b\t%d\t%d\t%d", key, info.extracted, info.dead, info.countInit, info.countCurrent, info.countMax));
	    }
    }

}
