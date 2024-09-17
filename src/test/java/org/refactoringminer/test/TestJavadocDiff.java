package org.refactoringminer.test;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl.ChangedFileInfo;

import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLDocElement;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.UMLClassDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;

public class TestJavadocDiff {
	private static final String REPOS = System.getProperty("user.dir") + "/src/test/resources/oracle/commits";
	private static final String EXPECTED_PATH = System.getProperty("user.dir") + "/src/test/resources/mappings/";
	
	@ParameterizedTest
	@CsvSource({
		"https://github.com/apache/cassandra.git, cb56d9fc3c773abbefa2044ce41ddbfb7717e0cb, org.apache.cassandra.repair.messages.RepairOption, parse, cassandra-cb56d9fc3c773abbefa2044ce41ddbfb7717e0cb.txt",
		"https://github.com/apache/flink.git, bac21bf5d77c8e15c608ecbf006d29e7af1dd68a, org.apache.flink.streaming.api.datastream.DataStream, join, flink-bac21bf5d77c8e15c608ecbf006d29e7af1dd68a.txt"
	})
	public void testMethodJavadocMappings(String url, String commitId, String className, String containerName, String testResultFileName) throws Exception {
		final List<String> actual = new ArrayList<>();
		/*
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI(url, commitId, new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getName().equals(containerName) && mapper.getContainer2().getName().equals(containerName)) {
					javadocInfo(mapper, actual);
					break;
				}
			}
		}
		*/
		UMLClassDiff classDiff = generateClassDiff(url, commitId, new File(REPOS), className);
		classDiff.process();
		for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
			if(mapper.getContainer1().getName().equals(containerName) && mapper.getContainer2().getName().equals(containerName)) {
				javadocInfo(mapper, actual);
				break;
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + testResultFileName));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}
	
	@ParameterizedTest
	@CsvSource({
		"https://github.com/hazelcast/hazelcast.git, 30c4ae09745d6062077925a54f27205b7401d8df, com.hazelcast.internal.monitors.HealthMonitor, hazelcast-30c4ae09745d6062077925a54f27205b7401d8df.txt"
	})
	public void testClassJavadocMappings(String url, String commitId, String containerName, String testResultFileName) throws Exception {
		final List<String> actual = new ArrayList<>();
		/*
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI(url, commitId, new File(REPOS));
		List<UMLClassDiff> commonClassDiff = modelDiff.getCommonClassDiffList();
		for(UMLClassDiff classDiff : commonClassDiff) {
			if(classDiff.getOriginalClassName().equals(containerName) && classDiff.getNextClassName().equals(containerName)) {
				javadocInfo(classDiff, actual);
				break;
			}
		}
		*/
		UMLClassDiff classDiff = generateClassDiff(url, commitId, new File(REPOS), containerName);
		javadocInfo(classDiff, actual);
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + testResultFileName));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	private UMLClassDiff generateClassDiff(String cloneURL, String commitId, File rootFolder, String className) throws Exception {
		Set<String> repositoryDirectoriesBefore = ConcurrentHashMap.newKeySet();
		Set<String> repositoryDirectoriesCurrent = ConcurrentHashMap.newKeySet();
		Map<String, String> fileContentsBefore = new ConcurrentHashMap<String, String>();
		Map<String, String> fileContentsCurrent = new ConcurrentHashMap<String, String>();
		Map<String, String> renamedFilesHint = new ConcurrentHashMap<String, String>();
		ChangedFileInfo info = new GitHistoryRefactoringMinerImpl().populateWithGitHubAPIAndSaveFiles(cloneURL, commitId, 
				fileContentsBefore, fileContentsCurrent, renamedFilesHint, repositoryDirectoriesBefore, repositoryDirectoriesCurrent, rootFolder);
		Map<String, String> filesBefore = new LinkedHashMap<String, String>();
		Map<String, String> filesCurrent = new LinkedHashMap<String, String>();
		for(String fileName : info.getFilesBefore()) {
			if(fileContentsBefore.containsKey(fileName)) {
				filesBefore.put(fileName, fileContentsBefore.get(fileName));
			}
		}
		for(String fileName : info.getFilesCurrent()) {
			if(fileContentsCurrent.containsKey(fileName)) {
				filesCurrent.put(fileName, fileContentsCurrent.get(fileName));
			}
		}
		fileContentsBefore = filesBefore;
		fileContentsCurrent = filesCurrent;
		GitHistoryRefactoringMinerImpl.processIdenticalFiles(fileContentsBefore, fileContentsCurrent, renamedFilesHint, false);
		UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, repositoryDirectoriesCurrent);
		UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, repositoryDirectoriesBefore);
		UMLClass leftClass = null;
		for(UMLClass parentClass : parentUMLModel.getClassList()) {
			if(parentClass.getName().equals(className)) {
				leftClass = parentClass;
				break;
			}
		}
		UMLClass rightClass = null;
		for(UMLClass parentClass : currentUMLModel.getClassList()) {
			if(parentClass.getName().equals(className)) {
				rightClass = parentClass;
				break;
			}
		}
		if(rightClass != null && leftClass != null) {
			return new UMLClassDiff(leftClass, rightClass, null);
		}
		return null;
	}

	private void javadocInfo(UMLOperationBodyMapper bodyMapper, final List<String> actual) {
		actual.add(bodyMapper.toString());
		if(bodyMapper.getJavadocDiff().isPresent()) {
			for(Pair<UMLDocElement, UMLDocElement> mapping : bodyMapper.getJavadocDiff().get().getCommonDocElements()) {
				String line = mapping.getLeft().getLocationInfo() + "==" + mapping.getRight().getLocationInfo();
				actual.add(line);
			}
		}
	}

	private void javadocInfo(UMLClassDiff classDiff, final List<String> actual) {
		if(classDiff.getJavadocDiff().isPresent()) {
			actual.add(classDiff.getNextClassName());
			for(Pair<UMLDocElement, UMLDocElement> mapping : classDiff.getJavadocDiff().get().getCommonDocElements()) {
				String line = mapping.getLeft().getLocationInfo() + "==" + mapping.getRight().getLocationInfo();
				actual.add(line);
			}
		}
	}
}
