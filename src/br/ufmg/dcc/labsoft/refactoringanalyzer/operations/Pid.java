package br.ufmg.dcc.labsoft.refactoringanalyzer.operations;

import java.lang.management.ManagementFactory;

public class Pid {

	private final String pid;

	public Pid() {
		this.pid = ManagementFactory.getRuntimeMXBean().getName();
	}
	
	@Override
	public String toString() {
		return this.pid;
	}
	
	public String getMachine() {
		return this.pid.substring(this.pid.indexOf('@') + 1);
	}
	
}
