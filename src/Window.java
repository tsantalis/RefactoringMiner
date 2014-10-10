import java.util.LinkedHashMap;
import java.util.Map;

import org.joda.time.DateTime;

public class Window {
	private Map<Integer, Integer> window;
	private DateTime date;
	private int leftCount;
	private int rightCount;
	
	public Window(DateTime date) {
		this.date = date;
		this.window = new LinkedHashMap<Integer, Integer>();
	}

	public Map<Integer, Integer> getWindow() {
		return window;
	}

	public void setWindow(Map<Integer, Integer> window) {
		this.window = window;
	}

	public int getLeftCount() {
		return leftCount;
	}

	public int getRightCount() {
		return rightCount;
	}

	public void getBalance() {
		for(Integer key : window.keySet()) {
			if(key <= 0)
				leftCount += window.get(key);
			else
				rightCount += window.get(key);
		}
		System.out.println(date.toString().substring(0, 10) + "\t" + leftCount + ":" + rightCount);
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(Integer key : window.keySet()) {
			sb.append(key + "," + window.get(key)).append("\n");
		}
		return sb.toString();
	}
}
