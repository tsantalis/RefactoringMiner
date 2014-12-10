package br.ufmg.dcc.labsoft.refactoringanalyzer.operations;

import java.lang.management.ManagementFactory;

public class Main {

	public static void main(String[] args) {
		System.out.println(ManagementFactory.getRuntimeMXBean().getName());
	}

}
