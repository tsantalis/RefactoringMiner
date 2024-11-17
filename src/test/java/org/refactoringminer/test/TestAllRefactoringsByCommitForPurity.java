package org.refactoringminer.test;

import net.joshka.junit.json.params.JsonFileSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.refactoringminer.api.PurityCheckResult;
import org.refactoringminer.api.PurityChecker;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.utils.RefactoringPurityJsonConverter;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import gr.uom.java.xmi.diff.UMLModelDiff;

import java.util.List;
import java.util.Map;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

/**
 * @author  Victor Guerra Veloso victorgvbh@gmail.com
 */
public class TestAllRefactoringsByCommitForPurity {
    private static final String REPOS = System.getProperty("user.dir") + "/src/test/resources/oracle/commits";
    private static final String EXPECTED = System.getProperty("user.dir") + "/src/test/resources/oracle/expectedPurity.txt";
    private static final Map<String, Integer> expectedTP = new HashMap<>();
    private static final Map<String, Integer> expectedTN = new HashMap<>();
    private static final Map<String, Integer> expectedFP = new HashMap<>();
    private static final Map<String, Integer> expectedFN = new HashMap<>();

    @BeforeAll
    public static void setUp() throws JsonParseException, JsonMappingException, IOException {
    	try {
    		BufferedReader reader = new BufferedReader(new FileReader(EXPECTED));
    		String line;
    		while ((line = reader.readLine()) != null) {
    			String[] tokens = line.split(", ");
    			String commitId = tokens[0];
    			int tp = Integer.parseInt(tokens[1]);
    			int tn = Integer.parseInt(tokens[2]);
    			int fp = Integer.parseInt(tokens[3]);
    			int fn = Integer.parseInt(tokens[4]);
    			expectedTP.put(commitId, tp);
    			expectedTN.put(commitId, tn);
    			expectedFP.put(commitId, fp);
    			expectedFN.put(commitId, fn);
    		}
    		reader.close();
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    }

    private RefactoringPopulator.Purity findPurity(RefactoringPopulator.Root testCase, String refDescription) {
		for(RefactoringPopulator.Refactoring refactoring : testCase.refactorings) {
			if(refactoring.description.equals(refDescription)) {
				return refactoring.purity;
			}
		}
    	return null;
    }

    @ParameterizedTest
    @JsonFileSource(resources = "/oracle/sampleResPurity.json")
    public void testAllRefactoringsParameterized(@ConvertWith(RefactoringPurityJsonConverter.class) RefactoringPopulator.Root testCase) throws Exception {
        GitHistoryRefactoringMinerImpl detector = new GitHistoryRefactoringMinerImpl();
        detector.detectAtCommitWithGitHubAPI(testCase.repository, testCase.sha1, new File(REPOS), new RefactoringHandler() {

            @Override
            public boolean skipCommit(String commitId) {
                return commitId != testCase.sha1;
            }

            @Override
            public void handleModelDiff(String commitId, List<Refactoring> refactorings, UMLModelDiff modelDiff) {
        		int actualTP = 0, actualTN = 0, actualFP = 0, actualFN = 0;
        		for (Refactoring found : refactorings) {
        			PurityCheckResult actual = PurityChecker.check(found, refactorings, modelDiff);
        			String description = found.toString();
					if(actual != null) {
						RefactoringPopulator.Purity p = findPurity(testCase, description);
						if(p != null) {
							if(p.purityValue.equals("1") && actual.isPure()) {
								actualTP++;
							}
							else if(p.purityValue.equals("0") && !actual.isPure()) {
								actualTN++;
							}
							else if(p.purityValue.equals("0") && actual.isPure()) {
								actualFP++;
							}
							else if(p.purityValue.equals("1") && !actual.isPure()) {
								actualFN++;
							}
						}
					}
        		}
        		final int finalActualTP = actualTP;
        		final int finalActualTN = actualTN;
        		final int finalActualFP = actualFP;
        		final int finalActualFN = actualFN;
        		Assertions.assertAll(
        		() -> Assertions.assertEquals(expectedTP.get(commitId), finalActualTP, String.format("Should have %s True Positives, but has %s", expectedTP.get(commitId), finalActualTP)),
        		() -> Assertions.assertEquals(expectedTN.get(commitId), finalActualTN, String.format("Should have %s True Negatives, but has %s", expectedTN.get(commitId), finalActualTN)),
        		() -> Assertions.assertEquals(expectedFP.get(commitId), finalActualFP, String.format("Should have %s False Positives, but has %s", expectedFP.get(commitId), finalActualFP)),
        		() -> Assertions.assertEquals(expectedFN.get(commitId), finalActualFN, String.format("Should have %s False Negatives, but has %s", expectedFN.get(commitId), finalActualFN))
        		);
        	}
        });
    }
}
