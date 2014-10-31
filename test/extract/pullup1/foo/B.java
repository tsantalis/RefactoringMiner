package foo;

import java.util.List;

public class B extends A {

	public String method1(List<Integer> l) {
		int sum = getSum(l);
		System.out.println("method1");
		System.out.println("method1");
		System.out.println("method1");
		return "sum : " + sum;
	}

}
