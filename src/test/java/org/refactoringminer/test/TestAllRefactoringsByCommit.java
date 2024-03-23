package org.refactoringminer.test;

import net.joshka.junit.json.params.JsonFileSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.utils.RefactoringJsonConverter;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

/**
 * @author  Victor Guerra Veloso victorgvbh@gmail.com
 */
public class TestAllRefactoringsByCommit {
    private static final String REPOS = System.getProperty("user.dir") + "/src/test/resources/oracle/commits";
    private static final String EXPECTED = System.getProperty("user.dir") + "/src/test/resources/oracle/expected.txt";
    private static final Map<String, Integer> expectedTP = new HashMap<>();
    private static final Map<String, Integer> expectedFP = new HashMap<>();
    private static final Map<String, Integer> expectedFN = new HashMap<>();
    private static final Set<String> deletedCommits = Set.of("5b7947034a656c463ca477e198f7728cccc9e4c1", "ebb1c2c364e888d4a0f47abe691cb2bad4eb4e75");

    @BeforeAll
    public static void setUp() {
    	try {
    		BufferedReader reader = new BufferedReader(new FileReader(EXPECTED));
    		String line;
    		while ((line = reader.readLine()) != null) {
    			String[] tokens = line.split(", ");
    			String commitId = tokens[0];
    			int tp = Integer.parseInt(tokens[1]);
    			int fp = Integer.parseInt(tokens[2]);
    			int fn = Integer.parseInt(tokens[3]);
    			expectedTP.put(commitId, tp);
    			expectedFP.put(commitId, fp);
    			expectedFN.put(commitId, fn);
    		}
    		reader.close();
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    }

    @ParameterizedTest
    @JsonFileSource(resources = "/oracle/data.json")
    public void testAllRefactoringsParameterized(@ConvertWith(RefactoringJsonConverter.class) RefactoringPopulator.Root testCase) throws Exception {
    	if(!deletedCommits.contains(testCase.sha1)) {
	        GitHistoryRefactoringMinerImpl detector = new GitHistoryRefactoringMinerImpl();
	        detector.detectAtCommitWithGitHubAPI(testCase.repository, testCase.sha1, new File(REPOS), new RefactoringHandler() {
	            Set<String> foundRefactorings = null;
	
	            @Override
	            public boolean skipCommit(String commitId) {
	                return commitId != testCase.sha1;
	            }
	
	            @Override
	            public void handle(String commitId, List<Refactoring> refactorings) {
	        		foundRefactorings = new HashSet<>();
	        		for (Refactoring found : refactorings) {
	        			foundRefactorings.addAll(normalize(found.toString()));
	        		}
	        		Iterator<RefactoringPopulator.Refactoring> iter = testCase.refactorings.iterator();
	        		int actualTP = 0, actualFP = 0, actualFN = 0;
	        		while(iter.hasNext()){
	        			RefactoringPopulator.Refactoring expectedRefactoring = iter.next();
	        			String description = expectedRefactoring.description;
	        			if(foundRefactorings.contains(description)) {
	        				if(expectedRefactoring.validation.contains("TP"))
	        					actualTP++;
	        				else if(expectedRefactoring.validation.equals("FP")) {
	        					actualFP++;
	        				}
	        				foundRefactorings.remove(description);
	        			}
	        			else if(expectedRefactoring.validation.contains("TP")) {
	        				actualFN++;
	        			}
	        		}
	        		actualFP += foundRefactorings.size();
	        		final int finalActualTP = actualTP;
	        		final int finalActualFP = actualFP;
	        		final int finalActualFN = actualFN;
	        		Assertions.assertAll(
	        		() -> Assertions.assertEquals(expectedTP.get(commitId), finalActualTP, String.format("Should have %s True Positives, but has %s", expectedTP.get(commitId), finalActualTP)),
	        		() -> Assertions.assertEquals(expectedFP.get(commitId), finalActualFP, String.format("Should have %s False Positives, but has %s", expectedFP.get(commitId), finalActualFP)),
	        		() -> Assertions.assertEquals(expectedFN.get(commitId), finalActualFN, String.format("Should have %s False Negatives, but has %s", expectedFN.get(commitId), finalActualFN))
	        		);
	        	}
	        });
    	}
    }

	private List<String> normalize(String refactoring) {
		refactoring = normalizeSingle(refactoring);
		int begin = refactoring.indexOf("from classes [");
		if (begin != -1) {
			int end = refactoring.lastIndexOf(']');
			String types = refactoring.substring(begin + "from classes [".length(), end);
			String[] typesArray = types.split(", ");
			List<String> refactorings = new ArrayList<String>();
			for (String type : typesArray) {
				refactorings.add(refactoring.substring(0, begin) + "from class " + type);
			}
			return refactorings;
		}
		return Collections.singletonList(refactoring);
	}

	/**
	 * Remove generics type information.
	 */
	private static String normalizeSingle(String refactoring) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < refactoring.length(); i++) {
			char c = refactoring.charAt(i);
			if (c == '\t') {
				c = ' ';
			}
			sb.append(c);
		}
		return sb.toString();
	}
}
