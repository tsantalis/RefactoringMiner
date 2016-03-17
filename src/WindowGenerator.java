import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.refactoringminer.api.Refactoring;

import ca.ualberta.cs.data.CommitRefactoring;


public class WindowGenerator {
	private Map<DateTime, Integer> data;
	private List<Window> windows;
	
	public WindowGenerator(List<CommitRefactoring> commits) {
		this.data = new LinkedHashMap<DateTime, Integer>();
		this.windows = new ArrayList<Window>();
		for(CommitRefactoring commit : commits){
			DateTime date = commit.getDate();
			List<Refactoring> refactorings= commit.getRefactorings();
			/*if(data.containsKey(date)) {
				data.put(date, data.get(date) + refactorings.size());
			}
			else {
				data.put(date, refactorings.size());
			}*/
			//alternative for excluding tests
			for(Refactoring ref : refactorings) {
				if(!ref.toString().contains(Analyzer.TEST_PATTERN)) {
					if(data.containsKey(date)) {
						data.put(date, data.get(date) + 1);
					}
					else {
						data.put(date, 1);
					}
				}
			}
			//end alternative
		}
	}
	
	public void addWindow(Window w) {
		windows.add(w);
	}
	
	public double getLeftMean() {
		int sum = 0;
		for(Window w : windows) {
			sum += w.getLeftCount();
		}
		return (double)sum/(double)windows.size();
	}
	
	public double getRightMean() {
		int sum = 0;
		for(Window w : windows) {
			sum += w.getRightCount();
		}
		return (double)sum/(double)windows.size();
	}
	
	public Window getWindow(DateTime date, int size) {
		Map<Integer, Integer> window = new LinkedHashMap<Integer, Integer>();
		DateTime startDate = date.minusDays(size);
		int x = -size;
		for(int i=0; i<=2*size; i++) {
			DateTime currentDate = startDate.plusDays(i);
			int y;
			if(data.containsKey(currentDate))
				y = data.get(currentDate);
			else
				y = 0;
			//System.out.println((x+i) + "," + y);
			window.put(x+i, y);
		}
		Window w = new Window(date);
		w.setWindow(window);
		return w;
	}
}
