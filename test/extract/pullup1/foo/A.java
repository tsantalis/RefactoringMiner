package foo;

import java.util.List;


public class A {

	public int getSum(List<Integer> l) {
		int sum = 0;
		for (Integer i : l) {
			sum += i;
		}
		return sum;
	}

}
