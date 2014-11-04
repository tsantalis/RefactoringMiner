package foo;

import java.util.List;

public class A {

	private B b = new B();
	
	public String method1(List<Integer> l) {
		int sum = 0;
		for (Integer i : l) {
			sum += i;
		}
		System.out.println("method1");
		System.out.println("method1");
		System.out.println("method1");
		return "sum : " + sum;
	}

	public String method1B(List<Integer> l) {
		int sum = 0;
		for (Integer i : l) {
			sum += i;
		}
		System.out.println("method1B");
		System.out.println("method1B");
		System.out.println("method1B");
		return "sum : " + sum;
	}

	public String method2(List<Integer> l, C c) {
		int sumSquared = 0;
		for (Integer i : l) {
			sumSquared += i * i;
		}
		System.out.println("method2");
		System.out.println("method2");
		System.out.println("method2");
		return "sum : " + sumSquared;
	}

}
