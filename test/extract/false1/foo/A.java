package foo;

import java.util.List;

public class A {

	public String method1(List<Integer> l) {
		int sum = 0;
		for (Integer i : l) {
			sum += i;
		}
		int product = this.getProduct(l);
		System.out.println("method1");
		System.out.println("method1");
		System.out.println("method1");
		return "sum : " + sum + " " + product;
	}

	private int getProduct(List<Integer> l) {
		int product = 0;
		for (Integer i : l) {
			product *= i;
		}
		return product;
	}

}
