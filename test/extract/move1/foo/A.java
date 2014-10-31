package foo;

import java.util.List;

public class A {

	private B b = new B();
	
	public String method1(List<Integer> l) {
		int sum = b.getSum(l);
		System.out.println("method1");
		System.out.println("method1");
		System.out.println("method1");
		return "sum : " + sum;
	}

	public String method2(List<Integer> l, C c) {
		int sumSquared = c.getSumSquared(l);
		System.out.println("method2");
		System.out.println("method2");
		System.out.println("method2");
		return "sum : " + sumSquared;
	}

}
