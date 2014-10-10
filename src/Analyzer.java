import gr.uom.java.xmi.diff.Refactoring;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joda.time.DateTime;

import ca.ualberta.cs.data.CommitRefactoring;


public class Analyzer {

	public static final String TEST_PATTERN = "Mock";

	private List<CommitRefactoring> commits;
	
	private Hashtable<String, List<String>> refactors;
	
	private Hashtable<String, Hashtable<String, Integer>> timeline;
	
	private Hashtable<String, Hashtable<String, Integer>> timelineTestRefactoring;
	
	private Set<String> uniqueAuthors;
	
	private Hashtable<String, Integer> typeDistribution;
	
	private Hashtable<String, Integer> typeDistributionTest;
	
	private List<Refactoring> refactoringTestFiles;
	
	public Analyzer(){
		commits = loadCommitRefactorings();
		refactors = new Hashtable<String, List<String>> ();
		uniqueAuthors = new HashSet<String>();
		timeline = new Hashtable<String, Hashtable<String, Integer>>();
		typeDistribution = new Hashtable<String, Integer>();
		refactoringTestFiles = new ArrayList<Refactoring>();
		timelineTestRefactoring = new  Hashtable<String, Hashtable<String, Integer>>();
		typeDistributionTest = new Hashtable<String, Integer>();
	}
	
	public void analysis(){
		Map<String, Integer> countMap = new LinkedHashMap<String, Integer>();
		for(CommitRefactoring commit : commits){
			List<Refactoring> refac= commit.getRefactorings();
			
			if(!refac.isEmpty()){
				Iterator<Refactoring> it = refac.iterator();
				String author = commit.getAuthor();
			
				while(it.hasNext()){
					Refactoring r = it.next();
					if(r.toString().contains(TEST_PATTERN)){
						if(countMap.containsKey(author))
							countMap.put(author, countMap.get(author)+1);
						else
							countMap.put(author, 1);
						//refactoringTestFiles.add(r);
					}
					String rname = r.getName();
					List<String> t= refactors.get(author);
					
					if(t==null){
						t = new ArrayList<String>();
						t.add(rname);
						refactors.put(author, t);
					}
					else{
						refactors.remove(author);
						t.add(rname);
						refactors.put(author, t);
					}
				}
			}
		}
		System.out.println(countMap);
		System.out.println("Total Test Refactorign = "+refactoringTestFiles.size());
		
		// Summarizing
		Iterator<String> authors = refactors.keySet().iterator();
		
		int total = 0;
		while(authors.hasNext()){
			String who = authors.next();
			//System.out.println(who + ":" + refactors.get(who).size() + ":" + refactors.get(who));
			total += refactors.get(who).size();
		}
		System.out.println("Total Refactorings = " + total);
		
		// Getting Unique Committers
		for (CommitRefactoring cr : commits){
			uniqueAuthors.add(cr.getAuthor());
		}
		//System.out.println(uniqueAuthors);
		//System.out.println(uniqueAuthors.size());	
		
		
		//Timeline
		for(CommitRefactoring commit : commits){
			List<Refactoring> refac= commit.getRefactorings();
			
			if(!refac.isEmpty()){
				Iterator<Refactoring> it = refac.iterator();
				String date = commit.getDate().toString().substring(0,10);
			
				while(it.hasNext()){
					Refactoring r = it.next();
					String rname = r.getName();
					// Separating test files
					if(r.toString().contains(TEST_PATTERN)){
						Hashtable <String, Integer> t= timelineTestRefactoring.get(date);
						
						if(t==null){
							t = new Hashtable <String, Integer>();
							t.put(rname, 1);		
							timelineTestRefactoring.put(date, t);
						}
						else{
							Integer countType = t.get(rname);
							if(countType == null){
								countType = 0;
							}
							t.remove(rname);
							t.put(rname, countType+1);
						}	
					}
					else{
						Hashtable <String, Integer> t= timeline.get(date);
						
						if(t==null){
							t = new Hashtable <String, Integer>();
							t.put(rname, 1);		
							timeline.put(date, t);
						}
						else{
							Integer countType = t.get(rname);
							if(countType == null){
								countType = 0;
							}
							t.remove(rname);
							t.put(rname, countType+1);
						}
					}
					
					
					
				}
			}
		}
		
		Iterator<String> dates = timeline.keySet().iterator();
		while(dates.hasNext()){
			String dateT = dates.next();
			Hashtable<String, Integer> typesDate = timeline.get(dateT);
			Iterator<String> val = typesDate.keySet().iterator();
			int totalDate=0;
			while(val.hasNext()){
				totalDate += typesDate.get(val.next());
			}
			System.out.println(dateT+", "+totalDate);
			//System.out.println(typesDate);
		}
		
		System.out.println("############ ------ #########");
		
		Iterator<String> datesTest = timelineTestRefactoring.keySet().iterator();
		while(datesTest.hasNext()){
			String dateT = datesTest.next();
			Hashtable<String, Integer> typesDate = timelineTestRefactoring.get(dateT);
			Iterator<String> val = typesDate.keySet().iterator();
			int totalDate=0;
			while(val.hasNext()){
				totalDate += typesDate.get(val.next());
			}
			System.out.println(dateT+", "+totalDate);
			//System.out.println(typesDate);
		}
		
		// Refactoring type distribution
		for(CommitRefactoring commit : commits){
			List<Refactoring> refac= commit.getRefactorings();
			
			if(!refac.isEmpty()){
				Iterator<Refactoring> it = refac.iterator();
				String author = commit.getAuthor();
			
				while(it.hasNext()){
					Refactoring r = it.next();
					String rname = r.getName();
					
					if(r.toString().contains(TEST_PATTERN)){
						
						Integer count= typeDistributionTest.get(rname);
						
						if(count==null){
							count = 1;
							typeDistributionTest.put(rname, count);
						}
						else{
							typeDistributionTest.remove(rname);
							typeDistributionTest.put(rname, count+1);
						}
						
					}
					else{
						Integer count= typeDistribution.get(rname);
						
						if(count==null){
							count = 1;
							typeDistribution.put(rname, count);
						}
						else{
							typeDistribution.remove(rname);
							typeDistribution.put(rname, count+1);
						}
					}
					
				}
			}
		}
		System.out.println(typeDistribution);
		System.out.println(typeDistributionTest);
		
	}
	
	public static void main(String... args){
		Analyzer a = new Analyzer();
		a.analysis();
		//WindowGenerator wg = new WindowGenerator(a.commits);
		
		//JUnit release dates
		/*DateTime releaseDate = new DateTime(2001, 1, 18, 0, 0, 0, 0); //3.5
		createWindow(wg, releaseDate);
		releaseDate = new DateTime(2001, 4, 8, 0, 0, 0, 0); //3.6
		createWindow(wg, releaseDate);
		releaseDate = new DateTime(2001, 5, 21, 0, 0, 0, 0); //3.7
		createWindow(wg, releaseDate);
		releaseDate = new DateTime(2002, 8, 24, 0, 0, 0, 0); //3.8
		createWindow(wg, releaseDate);
		releaseDate = new DateTime(2002, 9, 4, 0, 0, 0, 0); //3.8.1
		createWindow(wg, releaseDate);
		releaseDate = new DateTime(2006, 2, 16, 0, 0, 0, 0); //4.0
		createWindow(wg, releaseDate);
		releaseDate = new DateTime(2006, 3, 3, 0, 0, 0, 0); //3.8.2
		createWindow(wg, releaseDate);
		releaseDate = new DateTime(2006, 5, 3, 0, 0, 0, 0); //4.1
		createWindow(wg, releaseDate);
		releaseDate = new DateTime(2006, 11, 16, 0, 0, 0, 0); //4.2
		createWindow(wg, releaseDate);
		releaseDate = new DateTime(2007, 3, 29, 0, 0, 0, 0); //4.3.1
		createWindow(wg, releaseDate);
		releaseDate = new DateTime(2007, 11, 7, 0, 0, 0, 0); //4.4
		createWindow(wg, releaseDate);
		releaseDate = new DateTime(2008, 8, 19, 0, 0, 0, 0); //4.5
		createWindow(wg, releaseDate);
		releaseDate = new DateTime(2009, 4, 14, 0, 0, 0, 0); //4.6
		createWindow(wg, releaseDate);
		releaseDate = new DateTime(2009, 8, 4, 0, 0, 0, 0); //4.7
		createWindow(wg, releaseDate);
		releaseDate = new DateTime(2009, 12, 1, 0, 0, 0, 0); //4.8
		createWindow(wg, releaseDate);
		releaseDate = new DateTime(2011, 8, 22, 0, 0, 0, 0); //4.9
		createWindow(wg, releaseDate);
		releaseDate = new DateTime(2011, 9, 29, 0, 0, 0, 0); //4.10
		createWindow(wg, releaseDate);*/
		
		//Jakarta Http client
		/*DateTime releaseDate = new DateTime(2007, 7, 14, 0, 0, 0, 0); // 4.0 Alpha1
		createWindow(wg, releaseDate);
		releaseDate = new DateTime(2009, 8, 11, 0, 0, 0, 0); // 4.0
		createWindow(wg, releaseDate);
		releaseDate = new DateTime(2009, 12, 9, 0, 0, 0, 0); // 4.01
		createWindow(wg, releaseDate);
		releaseDate = new DateTime(2010, 9, 18, 0, 0, 0, 0); // 4.03
		createWindow(wg, releaseDate);
		releaseDate = new DateTime(2011, 1, 21, 0, 0, 0, 0); // 4.1
		createWindow(wg, releaseDate);
		releaseDate = new DateTime(2011, 3, 19, 0, 0, 0, 0); // 4.11
		createWindow(wg, releaseDate);
		releaseDate = new DateTime(2011, 8, 4, 0, 0, 0, 0); // 4.12
		createWindow(wg, releaseDate);
		releaseDate = new DateTime(2012, 2, 6, 0, 0, 0, 0); // 4.13
		createWindow(wg, releaseDate);*/
		
		//Jakarta Http core
		/*DateTime releaseDate = new DateTime(2006, 4, 15, 0, 0, 0, 0); // 4.0 Alpha1
		createWindow(wg, releaseDate);
		releaseDate = new DateTime(2008, 1, 20, 0, 0, 0, 0); // 4.0 Beta1
		createWindow(wg, releaseDate);
		releaseDate = new DateTime(2009, 2, 24, 0, 0, 0, 0); // 4.0
		createWindow(wg, releaseDate);
		releaseDate = new DateTime(2009, 6, 21, 0, 0, 0, 0); // 4.01
		createWindow(wg, releaseDate);
		releaseDate = new DateTime(2009, 9, 10, 0, 0, 0, 0); // 4.1 Alpha
		createWindow(wg, releaseDate);
		releaseDate = new DateTime(2010, 11, 16, 0, 0, 0, 0); // 4.1
		createWindow(wg, releaseDate);
		releaseDate = new DateTime(2011, 5, 17, 0, 0, 0, 0); // 4.11
		createWindow(wg, releaseDate);
		releaseDate = new DateTime(2011, 7, 14, 0, 0, 0, 0); // 4.12
		createWindow(wg, releaseDate);
		//releaseDate = new DateTime(2011, 7, 30, 0, 0, 0, 0); // 4.13
		//createWindow(wg, releaseDate);
		releaseDate = new DateTime(2011, 12, 19, 0, 0, 0, 0); // 4.14
		createWindow(wg, releaseDate);
		//releaseDate = new DateTime(2012, 1, 28, 0, 0, 0, 0); // 4.2 Beta 1
		//createWindow(wg, releaseDate);
		 */		
		
		//System.out.println(wg.getLeftMean() + "\t" + wg.getRightMean());
		//a.analysis();
	}

	private static void createWindow(WindowGenerator wg, DateTime releaseDate) {
		Window w = wg.getWindow(releaseDate, 40);
		w.getBalance();
		wg.addWindow(w);
	}
	
	// Save and Load
	private List<CommitRefactoring> loadCommitRefactorings() {
		List<CommitRefactoring> data = null;
		try {
			//FileInputStream fin = new FileInputStream("C:\\httpcore\\repository-refactorings.ser");
			FileInputStream fin = new FileInputStream("C:\\refac-http-client.ser");
			ObjectInputStream ois = new ObjectInputStream(fin);
			data = (List<CommitRefactoring>)ois.readObject();
			ois.close();
		}
		catch(ClassNotFoundException cnfe) { cnfe.printStackTrace(); }
		catch(IOException ioe) { ioe.printStackTrace(); }
		return data;
	}
	
	public void print() {
		for(CommitRefactoring commit : commits) {
			List<Refactoring> refac= commit.getRefactorings();
			if(!refac.isEmpty()) {
				String parent = commit.getParentList().get(0);
				String revision = commit.getRevision();
				String author = commit.getAuthor();
				String date = commit.getDate().toString().substring(0, 10);
				
				System.out.println(parent + "\t" + revision + "\t" + date + "\t" + author);
				for(Refactoring ref : refac) {
					System.out.println(ref);
				}
			}
		}
	}
}
