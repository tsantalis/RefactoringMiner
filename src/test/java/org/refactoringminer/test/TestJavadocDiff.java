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
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl.ChangedFileInfo;

import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLComment;
import gr.uom.java.xmi.UMLDocElement;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.ExtractOperationRefactoring;
import gr.uom.java.xmi.diff.InlineOperationRefactoring;
import gr.uom.java.xmi.diff.UMLClassDiff;

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

	@ParameterizedTest
	@CsvSource({
		"https://github.com/junit-team/junit4.git, 7a3e99635d7ffcc4d730f27835eeaeb082003199, org.junit.runners.BlockJUnit4ClassRunner, junit4-7a3e99635d7ffcc4d730f27835eeaeb082003199.txt"
	})
	public void testClassCommentMappings(String url, String commitId, String containerName, String testResultFileName) throws Exception {
		final List<String> actual = new ArrayList<>();
		UMLClassDiff classDiff = generateClassDiff(url, commitId, new File(REPOS), containerName);
		commentInfo(classDiff, actual);
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + testResultFileName));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@ParameterizedTest
	@CsvSource({
		"https://github.com/eclipse-jgit/jgit.git, 1b783d037091266b035e1727db6b6ce7a397ef63, org.eclipse.jgit.storage.pack.PackWriter, searchForDeltas, jgit-1b783d037091266b035e1727db6b6ce7a397ef63.txt",
		"https://github.com/hibernate/hibernate-orm.git, 5329bba1ea724eabf5783c71e5127b8f84ad0fcc, org.hibernate.cfg.AnnotationBinder, bindClass, hibernate-orm-5329bba1ea724eabf5783c71e5127b8f84ad0fcc-comments.txt",
		"https://github.com/javaparser/javaparser.git, 2d3f5e219af9d1ba916f1dc21a6169a41a254632, com.github.javaparser.printer.lexicalpreservation.Difference, applyRemovedDiffElement, javaparser-2d3f5e219af9d1ba916f1dc21a6169a41a254632-comments.txt"
	})
	public void testMethodCommentMappings(String url, String commitId, String className, String containerName, String testResultFileName) throws Exception {
		final List<String> actual = new ArrayList<>();
		UMLClassDiff classDiff = generateClassDiff(url, commitId, new File(REPOS), className);
		classDiff.process();
		for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
			if(mapper.getContainer1().getName().equals(containerName) && mapper.getContainer2().getName().equals(containerName)) {
				commentInfo(mapper, actual);
				//break;
			}
		}
		List<String> expected = IOUtils.readLines(new FileReader(EXPECTED_PATH + testResultFileName));
		Assertions.assertTrue(expected.size() == actual.size() && expected.containsAll(actual) && actual.containsAll(expected));
	}

	@ParameterizedTest
	@CsvSource({
		"https://github.com/jOOQ/jOOQ.git, 58a4e74d28073e7c6f15d1f225ac1c2fd9aa4357, org.jooq.tools.Convert.ConvertAll, jOOQ-58a4e74d28073e7c6f15d1f225ac1c2fd9aa4357-comments.txt",
		"https://github.com/thymeleaf/thymeleaf.git, 378ba37750a9cb1b19a6db434dfa59308f721ea6, org.thymeleaf.templateparser.reader.BlockAwareReader, thymeleaf-378ba37750a9cb1b19a6db434dfa59308f721ea6-comments.txt",
		"https://github.com/eclipse-vertx/vert.x.git, 32a8c9086040fd6d6fa11a214570ee4f75a4301f, io.vertx.core.http.impl.HttpServerImpl.ServerHandler, vertx-32a8c9086040fd6d6fa11a214570ee4f75a4301f-comments.txt",
		"https://github.com/eclipse-jgit/jgit.git, 5d8a9f6f3f43ac43c6b1c48cdfad55e545171ea3, org.eclipse.jgit.internal.storage.pack.PackWriter, jgit-5d8a9f6f3f43ac43c6b1c48cdfad55e545171ea3-comments.txt",
		"https://github.com/hibernate/hibernate-orm.git, 025b3cc14180d0459856bc45a6cac7acce3e1265, org.hibernate.cfg.AnnotationBinder, hibernate-orm-025b3cc14180d0459856bc45a6cac7acce3e1265-comments.txt",
		"https://github.com/eclipse-jgit/jgit.git, 8ac65d33ed7a94f77cb066271669feebf9b882fc, org.eclipse.jgit.storage.pack.PackWriter, jgit-8ac65d33ed7a94f77cb066271669feebf9b882fc-comments.txt"
	})
	public void testMethodCommentMultiMappings(String url, String commitId, String className, String testResultFileName) throws Exception {
		final List<String> actual = new ArrayList<>();
		UMLClassDiff classDiff = generateClassDiff(url, commitId, new File(REPOS), className);
		classDiff.process();
		List<Refactoring> refactorings = classDiff.getRefactorings();
		List<UMLOperationBodyMapper> parentMappers = new ArrayList<>();
		for (Refactoring ref : refactorings) {
			if(ref instanceof ExtractOperationRefactoring) {
				ExtractOperationRefactoring ex = (ExtractOperationRefactoring)ref;
				UMLOperationBodyMapper bodyMapper = ex.getBodyMapper();
				if(!bodyMapper.isNested()) {
					if(!parentMappers.contains(bodyMapper.getParentMapper())) {
						parentMappers.add(bodyMapper.getParentMapper());
					}
				}
				commentInfo(bodyMapper, actual);
			}
			else if(ref instanceof InlineOperationRefactoring) {
				InlineOperationRefactoring ex = (InlineOperationRefactoring)ref;
				UMLOperationBodyMapper bodyMapper = ex.getBodyMapper();
				if(!bodyMapper.isNested()) {
					if(!parentMappers.contains(bodyMapper.getParentMapper())) {
						parentMappers.add(bodyMapper.getParentMapper());
					}
				}
				commentInfo(bodyMapper, actual);
			}
		}
		for(UMLOperationBodyMapper parentMapper : parentMappers) {
			commentInfo(parentMapper, actual);
		}
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

	private void commentInfo(UMLOperationBodyMapper bodyMapper, final List<String> actual) {
		actual.add(bodyMapper.toString());
		for(Pair<UMLComment, UMLComment> mapping : bodyMapper.getCommentListDiff().getCommonComments()) {
			String line = mapping.getLeft().getLocationInfo() + "==" + mapping.getRight().getLocationInfo();
			actual.add(line);
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

	private void commentInfo(UMLClassDiff classDiff, final List<String> actual) {
		actual.add(classDiff.getNextClassName());
		for(Pair<UMLComment, UMLComment> mapping : classDiff.getCommentListDiff().getCommonComments()) {
			String line = mapping.getLeft().getLocationInfo() + "==" + mapping.getRight().getLocationInfo();
			actual.add(line);
		}
	}
}
