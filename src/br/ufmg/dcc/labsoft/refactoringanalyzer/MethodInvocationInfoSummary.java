package br.ufmg.dcc.labsoft.refactoringanalyzer;

import gr.uom.java.xmi.MethodInvocationInfo;
import gr.uom.java.xmi.MethodInvocationInfo.MethodInfo;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.diff.ExtractAndMoveOperationRefactoring;
import gr.uom.java.xmi.diff.ExtractOperationRefactoring;
import gr.uom.java.xmi.diff.Refactoring;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.Database;
import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.ExtractMethodInfo;
import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.ProjectGit;

public class MethodInvocationInfoSummary {

	Logger logger = LoggerFactory.getLogger(MethodInvocationInfoSummary.class);
	
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
		public String visibility;
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
	
	public void analyzeRevision(String commitId, UMLModel model, List<Refactoring> refactorings) {
		MethodInvocationInfo mii = model.getMethodInvocationInfo();
		for (MethodInfo methodInfo : mii.getMethodInfoCollection()) {
			if (!methodInfo.isExternal()) {
				MethodInfoSummary info = this.getOrCreate(methodInfo.getBindingKey());
				final int count = methodInfo.getCount();
				info.countMax = Math.max(info.countMax, count);
			}
		}
		for (Refactoring ref : refactorings) {
			this.analyzeRefactoring(commitId, model, ref);
		}
	}

	private void analyzeRefactoring(String commitId, UMLModel model, Refactoring refactoring) {
		UMLOperation op;
		if (refactoring instanceof ExtractOperationRefactoring) {
			ExtractOperationRefactoring emr = (ExtractOperationRefactoring) refactoring;
			op = emr.getExtractedFromOperation();
		}
		else if (refactoring instanceof ExtractAndMoveOperationRefactoring) {
			ExtractAndMoveOperationRefactoring emr = (ExtractAndMoveOperationRefactoring) refactoring;
			op = emr.getExtractedOperation();
		} else {
			return;
		}
		String bindingKey = op.getXmiID();
		if (bindingKey == null) {
			this.logger.warn(String.format("WARN refactoring SEM binding: %s", refactoring));
			return;
		} else {
			//this.logger.warn(String.format("WARN refactoring COM binding: %s", refactoring));
		}
		MethodInfoSummary info = this.getOrCreate(bindingKey);
		if (info.revExtracted != null && !info.revExtracted.getLast().equals(commitId)) {
			info.revExtracted.add(commitId);
			return;
		}
		if (info.revExtracted == null) {
			info.revExtracted = new LinkedList<>();
			info.revExtracted.add(commitId);
			info.extracted = true;
			info.visibility = op.getVisibility();
			
			info.moved = refactoring instanceof ExtractAndMoveOperationRefactoring;
			MethodInvocationInfo mii = model.getMethodInvocationInfo();
			MethodInfo methodInfo = mii.getMethodInfo(bindingKey);
			if (methodInfo != null) {
				info.countInit = methodInfo.getCount();
			} else {
				this.logger.warn(String.format("WARN refactoring com countInit=0: %s %s", commitId, refactoring));
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
	    	//if (info.extracted) {
	    		out.println(String.format("%s\t%b\t%b\t%b\t%d\t%d\t%d\t%d\t%s\t%s", key, info.extracted, info.moved, info.dead, info.countDup, info.countInit, info.countCurrent, info.countMax, info.revExtracted, info.visibility));
	    	//}
	    }
    }

	public void insertAtDb(Database db, ProjectGit project) {
		for (Entry<String, MethodInfoSummary> e : this.map.entrySet()) {
			String key = e.getKey();
			MethodInfoSummary info = e.getValue();
			//if (info.extracted) {
			//out.println(String.format("%s\t%b\t%b\t%b\t%d\t%d\t%d\t%d\t%s\t%s", key, info.extracted, info.moved, info.dead, info.countDup, info.countInit, info.countCurrent, info.countMax, info.revExtracted, info.visibility));
			//}
			
			ExtractMethodInfo emi = new ExtractMethodInfo();
			emi.setProject(project);
			emi.setMethod(key);
			emi.setCountDup(info.countDup);
			emi.setCountInit(info.countInit);
			emi.setCountCurrent(info.countCurrent);
			emi.setCountMax(info.countMax);
			emi.setRevExtracted(info.revExtracted == null ? null : info.revExtracted.getFirst());
			emi.setDead(info.dead);
			emi.setExtracted(info.extracted);
			emi.setMoved(info.moved);
			emi.setVisibility(info.visibility);
			
			db.insert(emi);
		}
	}

}
