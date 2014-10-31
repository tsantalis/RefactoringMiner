package foo;

import java.util.List;

public class B extends A {

	@Override
	protected int getSum(List<Integer> l) {
		int sum = 0;
		for (Integer i : l) {
			sum += i * i;
		}
		return sum;
	}

}
