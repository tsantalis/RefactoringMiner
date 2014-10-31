package foo;

import java.util.List;


public class C {

	public int getSumSquared(List<Integer> l) {
		int sumSquared = 0;
		for (Integer i : l) {
			sumSquared += i * i;
		}
		return sumSquared;
	}

}
