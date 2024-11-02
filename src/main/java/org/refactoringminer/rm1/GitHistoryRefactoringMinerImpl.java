package org.refactoringminer.rm1;

import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLModelASTReader;
import gr.uom.java.xmi.diff.MoveSourceFolderRefactoring;
import gr.uom.java.xmi.diff.MovedClassToAnotherSourceFolder;
import gr.uom.java.xmi.diff.RenamePattern;
import gr.uom.java.xmi.diff.StringDistance;
import gr.uom.java.xmi.diff.UMLModelDiff;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestCommitDetail;
import org.kohsuke.github.GHPullRequestFileDetail;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHRepositoryWrapper;
import org.kohsuke.github.GHTree;
import org.kohsuke.github.GHTreeEntry;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedIterable;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.GitService;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.astDiff.matchers.ProjectASTDiffer;
import org.refactoringminer.util.GitServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;

public class GitHistoryRefactoringMinerImpl implements GitHistoryRefactoringMiner {

	private final static Logger logger = LoggerFactory.getLogger(GitHistoryRefactoringMinerImpl.class);
	private Set<RefactoringType> refactoringTypesToConsider = null;
	private GitHub gitHub;
	
	public GitHistoryRefactoringMinerImpl() {
		this.setRefactoringTypesToConsider(RefactoringType.ALL);
	}

	public void setRefactoringTypesToConsider(RefactoringType ... types) {
		this.refactoringTypesToConsider = new HashSet<RefactoringType>();
		for (RefactoringType type : types) {
			this.refactoringTypesToConsider.add(type);
		}
	}
	
	private void detect(GitService gitService, Repository repository, final RefactoringHandler handler, Iterator<RevCommit> i) {
		int commitsCount = 0;
		int errorCommitsCount = 0;
		int refactoringsCount = 0;

		File metadataFolder = repository.getDirectory();
		File projectFolder = metadataFolder.getParentFile();
		String projectName = projectFolder.getName();
		
		long time = System.currentTimeMillis();
		while (i.hasNext()) {
			RevCommit currentCommit = i.next();
			try {
				List<Refactoring> refactoringsAtRevision = detectRefactorings(gitService, repository, handler, currentCommit);
				refactoringsCount += refactoringsAtRevision.size();
				
			} catch (Exception e) {
				logger.warn(String.format("Ignored revision %s due to error", currentCommit.getId().getName()), e);
				handler.handleException(currentCommit.getId().getName(),e);
				errorCommitsCount++;
			}

			commitsCount++;
			long time2 = System.currentTimeMillis();
			if ((time2 - time) > 20000) {
				time = time2;
				logger.info(String.format("Processing %s [Commits: %d, Errors: %d, Refactorings: %d]", projectName, commitsCount, errorCommitsCount, refactoringsCount));
			}
		}

		handler.onFinish(refactoringsCount, commitsCount, errorCommitsCount);
		logger.info(String.format("Analyzed %s [Commits: %d, Errors: %d, Refactorings: %d]", projectName, commitsCount, errorCommitsCount, refactoringsCount));
	}

	protected List<Refactoring> detectRefactorings(GitService gitService, Repository repository, final RefactoringHandler handler, RevCommit currentCommit) throws Exception {
		List<Refactoring> refactoringsAtRevision;
		UMLModelDiff modelDiff;
		String commitId = currentCommit.getId().getName();
		Set<String> filePathsBefore = new LinkedHashSet<String>();
		Set<String> filePathsCurrent = new LinkedHashSet<String>();
		Map<String, String> renamedFilesHint = new HashMap<String, String>();
		gitService.fileTreeDiff(repository, currentCommit, filePathsBefore, filePathsCurrent, renamedFilesHint);
		
		Set<String> repositoryDirectoriesBefore = new LinkedHashSet<String>();
		Set<String> repositoryDirectoriesCurrent = new LinkedHashSet<String>();
		Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
		Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
		// If no java files changed, there is no refactoring. Also, if there are
		// only ADD's or only REMOVE's there is no refactoring
		if (!filePathsBefore.isEmpty() && !filePathsCurrent.isEmpty() && currentCommit.getParentCount() > 0) {
			RevCommit parentCommit = currentCommit.getParent(0);
			populateFileContents(repository, parentCommit, filePathsBefore, fileContentsBefore, repositoryDirectoriesBefore);
			populateFileContents(repository, currentCommit, filePathsCurrent, fileContentsCurrent, repositoryDirectoriesCurrent);
			List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = processIdenticalFiles(fileContentsBefore, fileContentsCurrent, renamedFilesHint, false);
			UMLModel parentUMLModel = createModel(fileContentsBefore, repositoryDirectoriesBefore);
			UMLModel currentUMLModel = createModel(fileContentsCurrent, repositoryDirectoriesCurrent);
			
			modelDiff = parentUMLModel.diff(currentUMLModel);
			refactoringsAtRevision = modelDiff.getRefactorings();
			refactoringsAtRevision.addAll(moveSourceFolderRefactorings);
			refactoringsAtRevision = filter(refactoringsAtRevision);
		} else {
			//logger.info(String.format("Ignored revision %s with no changes in java files", commitId));
			modelDiff = new UMLModelDiff(createModel(Collections.emptyMap(), Collections.emptySet()), createModel(Collections.emptyMap(), Collections.emptySet()));
			refactoringsAtRevision = Collections.emptyList();
		}
		handler.handle(commitId, refactoringsAtRevision);
		handler.handleModelDiff(commitId, refactoringsAtRevision, modelDiff);
		return refactoringsAtRevision;
	}

	public static List<MoveSourceFolderRefactoring> processIdenticalFiles(Map<String, String> fileContentsBefore, Map<String, String> fileContentsCurrent,
			Map<String, String> renamedFilesHint, boolean astDiff) throws IOException {
		Map<String, String> identicalFiles = new HashMap<String, String>();
		Map<Pair<String, String>, Integer> consistentSourceFolderChanges = new HashMap<>();
		Map<String, String> nonIdenticalFiles = new HashMap<String, String>();
		for(String key : fileContentsBefore.keySet()) {
			//take advantage of renamed file hints, if available
			if(renamedFilesHint.containsKey(key)) {
				String renamedFile = renamedFilesHint.get(key);
				String fileBefore = fileContentsBefore.get(key);
				String fileAfter = fileContentsCurrent.get(renamedFile);
				if(matchCondition(fileBefore, fileAfter, astDiff)) {
					identicalFiles.put(key, renamedFile);
					if(key.contains("/") && renamedFile.contains("/")) {
						String prefix1 = key.substring(0, key.indexOf("/"));
						String prefix2 = renamedFile.substring(0, renamedFile.indexOf("/"));
						Pair<String, String> p = Pair.of(prefix1, prefix2);
						if(consistentSourceFolderChanges.containsKey(p)) {
							consistentSourceFolderChanges.put(p, consistentSourceFolderChanges.get(p) + 1);
						}
						else {
							consistentSourceFolderChanges.put(p, 1);
						}
					}
				}
				else {
					nonIdenticalFiles.put(key, renamedFile);
				}
			}
			if(fileContentsCurrent.containsKey(key)) {
				String fileBefore = fileContentsBefore.get(key);
				String fileAfter = fileContentsCurrent.get(key);
				if(matchCondition(fileBefore, fileAfter, astDiff)) {
					identicalFiles.put(key, key);
				}
				else {
					nonIdenticalFiles.put(key, key);
				}
			}
		}
		fileContentsBefore.keySet().removeAll(identicalFiles.keySet());
		fileContentsCurrent.keySet().removeAll(identicalFiles.values());
		//second iteration to find renamed/moved files with identical contents
		for(String key1 : fileContentsBefore.keySet()) {
			if(!identicalFiles.containsKey(key1) && !nonIdenticalFiles.containsKey(key1) && key1.contains("/")) {
				String prefix1 = key1.substring(0, key1.indexOf("/"));
				String fileBefore = fileContentsBefore.get(key1);
				boolean matchWithConsistentSourceFolderChangeFound = false;
				List<String> matches = new ArrayList<String>();
				for(String key2 : fileContentsCurrent.keySet()) {
					if(!identicalFiles.containsValue(key2) && !nonIdenticalFiles.containsValue(key2) && key2.contains("/")) {
						String prefix2 = key2.substring(0, key2.indexOf("/"));
						String fileAfter = fileContentsCurrent.get(key2);
						if(matchCondition(fileBefore, fileAfter, astDiff)) {
							if(consistentSourceFolderChanges.containsKey(Pair.of(prefix1, prefix2))) {
								identicalFiles.put(key1, key2);
								matchWithConsistentSourceFolderChangeFound = true;
								break;
							}
							else {
								matches.add(key2);
							}
						}
					}
				}
				if(!matchWithConsistentSourceFolderChangeFound) {
					if(matches.size() == 1) {
						identicalFiles.put(key1, matches.get(0));
					}
					else if(matches.size() > 1) {
						int minEditDistance = key1.length();
						String bestMatch = null;
						for(int i=0; i< matches.size(); i++) {
							String key2 = matches.get(i);
							int editDistance = StringDistance.editDistance(key1, key2);
							if(editDistance < minEditDistance) {
								minEditDistance = editDistance;
								bestMatch = key2;
							}
						}
						if(bestMatch != null) {
							identicalFiles.put(key1, bestMatch);
						}
					}
				}
			}
		}
		fileContentsBefore.keySet().removeAll(identicalFiles.keySet());
		fileContentsCurrent.keySet().removeAll(identicalFiles.values());
		
		List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = new ArrayList<MoveSourceFolderRefactoring>();
		for(String key : identicalFiles.keySet()) {
			String originalPath = key;
			String movedPath = identicalFiles.get(key);
			String originalPathPrefix = "";
			if(originalPath.contains("/")) {
				originalPathPrefix = originalPath.substring(0, originalPath.lastIndexOf('/'));
			}
			String movedPathPrefix = "";
			if(movedPath.contains("/")) {
				movedPathPrefix = movedPath.substring(0, movedPath.lastIndexOf('/'));
			}
			if(!originalPathPrefix.equals(movedPathPrefix) && !key.endsWith("package-info.java")) {
				MovedClassToAnotherSourceFolder refactoring = new MovedClassToAnotherSourceFolder(null, null, originalPathPrefix, movedPathPrefix);
				RenamePattern renamePattern = refactoring.getRenamePattern();
				boolean foundInMatchingMoveSourceFolderRefactoring = false;
				for(MoveSourceFolderRefactoring moveSourceFolderRefactoring : moveSourceFolderRefactorings) {
					if(moveSourceFolderRefactoring.getPattern().equals(renamePattern)) {
						moveSourceFolderRefactoring.putIdenticalFilePaths(originalPath, movedPath);
						foundInMatchingMoveSourceFolderRefactoring = true;
						break;
					}
				}
				if(!foundInMatchingMoveSourceFolderRefactoring) {
					MoveSourceFolderRefactoring moveSourceFolderRefactoring = new MoveSourceFolderRefactoring(renamePattern);
					moveSourceFolderRefactoring.putIdenticalFilePaths(originalPath, movedPath);
					moveSourceFolderRefactorings.add(moveSourceFolderRefactoring);
				}
			}
		}
		return moveSourceFolderRefactorings;
	}

	private static boolean matchCondition(String fileBefore, String fileAfter, boolean astDiff) throws IOException {
		if(astDiff) {
			return fileBefore.equals(fileAfter);
		}
		return fileBefore.equals(fileAfter) || StringDistance.trivialCommentChange(fileBefore, fileAfter);
	}

	public static void populateFileContents(Repository repository, RevCommit commit,
			Set<String> filePaths, Map<String, String> fileContents, Set<String> repositoryDirectories) throws Exception {
		logger.info("Processing {} {} ...", repository.getDirectory().getParent().toString(), commit.getName());
		RevTree parentTree = commit.getTree();
		try (TreeWalk treeWalk = new TreeWalk(repository)) {
			treeWalk.addTree(parentTree);
			treeWalk.setRecursive(true);
			while (treeWalk.next()) {
				String pathString = treeWalk.getPathString();
				if(filePaths.contains(pathString)) {
					ObjectId objectId = treeWalk.getObjectId(0);
					ObjectLoader loader = repository.open(objectId);
					StringWriter writer = new StringWriter();
					IOUtils.copy(loader.openStream(), writer);
					fileContents.put(pathString, writer.toString());
				}
				if(pathString.endsWith(".java") && pathString.contains("/")) {
					String directory = pathString.substring(0, pathString.lastIndexOf("/"));
					repositoryDirectories.add(directory);
					//include sub-directories
					String subDirectory = new String(directory);
					while(subDirectory.contains("/")) {
						subDirectory = subDirectory.substring(0, subDirectory.lastIndexOf("/"));
						repositoryDirectories.add(subDirectory);
					}
				}
			}
		}
	}

	public static void populateFileContentsAndSave(Repository repository, RevCommit commit,
			Set<String> filePaths, Map<String, String> fileContents, Set<String> repositoryDirectories, File rootFolder) throws Exception {
		String cloneURL = repository.getConfig().getString("remote", "origin", "url");
		String repoName = cloneURL.substring(cloneURL.lastIndexOf('/') + 1, cloneURL.lastIndexOf('.'));
		logger.info("Processing {} {} ...", repository.getDirectory().getParent().toString(), commit.getName());
		RevTree parentTree = commit.getTree();
		try (TreeWalk treeWalk = new TreeWalk(repository)) {
			treeWalk.addTree(parentTree);
			treeWalk.setRecursive(true);
			while (treeWalk.next()) {
				String pathString = treeWalk.getPathString();
				if(filePaths.contains(pathString)) {
					ObjectId objectId = treeWalk.getObjectId(0);
					ObjectLoader loader = repository.open(objectId);
					StringWriter writer = new StringWriter();
					IOUtils.copy(loader.openStream(), writer);
					String fileContent = writer.toString();
					fileContents.put(pathString, fileContent);
					File currentFilePath = new File(rootFolder, repoName + "-" + commit.getId().getName() + "/" + pathString);
					FileUtils.writeStringToFile(currentFilePath, fileContent);
				}
				if(pathString.endsWith(".java") && pathString.contains("/")) {
					String directory = pathString.substring(0, pathString.lastIndexOf("/"));
					repositoryDirectories.add(directory);
					//include sub-directories
					String subDirectory = new String(directory);
					while(subDirectory.contains("/")) {
						subDirectory = subDirectory.substring(0, subDirectory.lastIndexOf("/"));
						repositoryDirectories.add(subDirectory);
					}
				}
			}
		}
	}

	protected List<Refactoring> detectRefactorings(final RefactoringHandler handler, File projectFolder, String cloneURL, String currentCommitId) {
		List<Refactoring> refactoringsAtRevision = Collections.emptyList();
		UMLModelDiff modelDiff;
		try {
			ChangedFileInfo changedFileInfo = populateWithGitHubAPI(projectFolder, cloneURL, currentCommitId);
			String parentCommitId = changedFileInfo.getParentCommitId();
			List<String> filesBefore = changedFileInfo.getFilesBefore();
			List<String> filesCurrent = changedFileInfo.getFilesCurrent();
			Map<String, String> renamedFilesHint = changedFileInfo.getRenamedFilesHint();
			File currentFolder = new File(projectFolder.getParentFile(), projectFolder.getName() + "-" + currentCommitId);
			File parentFolder = new File(projectFolder.getParentFile(), projectFolder.getName() + "-" + parentCommitId);
			if (!currentFolder.exists()) {	
				downloadAndExtractZipFile(projectFolder, cloneURL, currentCommitId);
			}
			if (!parentFolder.exists()) {	
				downloadAndExtractZipFile(projectFolder, cloneURL, parentCommitId);
			}
			Set<String> repositoryDirectoriesBefore = new LinkedHashSet<String>();
			Set<String> repositoryDirectoriesCurrent = new LinkedHashSet<String>();
			Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
			Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
			if (currentFolder.exists() && parentFolder.exists()) {
				populateFileContents(currentFolder, filesCurrent, fileContentsCurrent, repositoryDirectoriesCurrent);
				populateFileContents(parentFolder, filesBefore, fileContentsBefore, repositoryDirectoriesBefore);
				List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = processIdenticalFiles(fileContentsBefore, fileContentsCurrent, renamedFilesHint, false); 
				UMLModel parentUMLModel = createModel(fileContentsBefore, repositoryDirectoriesBefore);
				UMLModel currentUMLModel = createModel(fileContentsCurrent, repositoryDirectoriesCurrent);
				modelDiff = parentUMLModel.diff(currentUMLModel);
				refactoringsAtRevision = modelDiff.getRefactorings();
				refactoringsAtRevision.addAll(moveSourceFolderRefactorings);
				refactoringsAtRevision = filter(refactoringsAtRevision);
			}
			else {
				modelDiff = new UMLModelDiff(createModel(Collections.emptyMap(), Collections.emptySet()), createModel(Collections.emptyMap(), Collections.emptySet()));
				logger.warn(String.format("Folder %s not found", currentFolder.getPath()));
			}
			handler.handle(currentCommitId, refactoringsAtRevision);
			handler.handleModelDiff(currentCommitId, refactoringsAtRevision, modelDiff);
		} catch (Exception e) {
			logger.warn(String.format("Ignored revision %s due to error", currentCommitId), e);
			handler.handleException(currentCommitId, e);
		}

		return refactoringsAtRevision;
	}

	private static void populateFileContents(File projectFolder, List<String> filePaths, Map<String, String> fileContents,	Set<String> repositoryDirectories) throws IOException {
		for(String path : filePaths) {
			String fullPath = projectFolder + File.separator + path.replaceAll("/", systemFileSeparator);
			String contents = FileUtils.readFileToString(new File(fullPath));
			fileContents.put(path, contents);
			String directory = new String(path);
			while(directory.contains("/")) {
				directory = directory.substring(0, directory.lastIndexOf("/"));
				repositoryDirectories.add(directory);
			}
		}
	}

	private void downloadAndExtractZipFile(File projectFolder, String cloneURL, String commitId)
			throws IOException {
		String downloadLink = extractDownloadLink(cloneURL, commitId);
		File destinationFile = new File(projectFolder.getParentFile(), projectFolder.getName() + "-" + commitId + ".zip");
		logger.info(String.format("Downloading archive %s", downloadLink));
		FileUtils.copyURLToFile(new URL(downloadLink), destinationFile);
		logger.info(String.format("Unzipping archive %s", downloadLink));
		java.util.zip.ZipFile zipFile = new ZipFile(destinationFile);
		try {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				File entryDestination = new File(projectFolder.getParentFile(),  entry.getName());
				if (entry.isDirectory()) {
					entryDestination.mkdirs();
				} else {
					entryDestination.getParentFile().mkdirs();
					InputStream in = zipFile.getInputStream(entry);
					OutputStream out = new FileOutputStream(entryDestination);
					IOUtils.copy(in, out);
					IOUtils.closeQuietly(in);
					out.close();
				}
			}
		} finally {
			zipFile.close();
		}
	}

	public static class ChangedFileInfo {
		private String parentCommitId;
		private String currentCommitId;
		private List<String> filesBefore;
		private List<String> filesCurrent;
		private Map<String, String> renamedFilesHint;
		private Set<String> repositoryDirectoriesBefore;
		private Set<String> repositoryDirectoriesCurrent;
		private long commitTime;
		private long authoredTime;
		private String commitAuthorName;

		public ChangedFileInfo() {
			
		}

		public ChangedFileInfo(String parentCommitId, List<String> filesBefore,
				List<String> filesCurrent, Map<String, String> renamedFilesHint) {
			this.filesBefore = filesBefore;
			this.filesCurrent = filesCurrent;
			this.renamedFilesHint = renamedFilesHint;
			this.parentCommitId = parentCommitId;
		}

		public ChangedFileInfo(String parentCommitId, String currentCommitId,
				List<String> filesBefore, List<String> filesCurrent,
				Set<String> repositoryDirectoriesBefore, Set<String> repositoryDirectoriesCurrent, Map<String, String> renamedFilesHint) {
			this.filesBefore = filesBefore;
			this.filesCurrent = filesCurrent;
			this.renamedFilesHint = renamedFilesHint;
			this.repositoryDirectoriesBefore = repositoryDirectoriesBefore;
			this.repositoryDirectoriesCurrent = repositoryDirectoriesCurrent;
			this.parentCommitId = parentCommitId;
			this.currentCommitId = currentCommitId;
		}

		public ChangedFileInfo(String parentCommitId, String currentCommitId,
				List<String> filesBefore, List<String> filesCurrent,
				Set<String> repositoryDirectoriesBefore, Set<String> repositoryDirectoriesCurrent, Map<String, String> renamedFilesHint,
				long commitTime, long authoredTime, String commitAuthorName) {
			this.filesBefore = filesBefore;
			this.filesCurrent = filesCurrent;
			this.renamedFilesHint = renamedFilesHint;
			this.repositoryDirectoriesBefore = repositoryDirectoriesBefore;
			this.repositoryDirectoriesCurrent = repositoryDirectoriesCurrent;
			this.parentCommitId = parentCommitId;
			this.currentCommitId = currentCommitId;
			this.commitTime = commitTime;
			this.authoredTime = authoredTime;
			this.commitAuthorName = commitAuthorName;
		}

		public String getParentCommitId() {
			return parentCommitId;
		}

		public String getCurrentCommitId() {
			return currentCommitId;
		}

		public List<String> getFilesBefore() {
			return filesBefore;
		}

		public List<String> getFilesCurrent() {
			return filesCurrent;
		}

		public Set<String> getRepositoryDirectoriesBefore() {
			return repositoryDirectoriesBefore;
		}

		public Set<String> getRepositoryDirectoriesCurrent() {
			return repositoryDirectoriesCurrent;
		}

		public Map<String, String> getRenamedFilesHint() {
			return renamedFilesHint;
		}

		public long getCommitTime() {
			return commitTime;
		}

		public long getAuthoredTime() {
			return authoredTime;
		}

		public String getCommitAuthorName() {
			return commitAuthorName;
		}
	}

	private ChangedFileInfo populateWithGitHubAPI(File projectFolder, String cloneURL, String currentCommitId) throws IOException {
		logger.info("Processing {} {} ...", cloneURL, currentCommitId);
		String jsonFilePath = projectFolder.getName() + "-" + currentCommitId + ".json";
		File jsonFile = new File(projectFolder.getParent(), jsonFilePath);
		if(jsonFile.exists()) {
			final ObjectMapper mapper = new ObjectMapper();
			ChangedFileInfo changedFileInfo = mapper.readValue(jsonFile, ChangedFileInfo.class);
			return changedFileInfo;
		}
		else {
			GHRepository repository = getGitHubRepository(cloneURL);
			List<GHCommit.File> commitFiles = new ArrayList<>();
			GHCommit commit = new GHRepositoryWrapper(repository).getCommit(currentCommitId, commitFiles);
			//if parents.size() == 0 then currentCommit is the initial commit of the repository, but then all files will have an ADDED status
			String parentCommitId = commit.getParents().size() > 0 ? commit.getParents().get(0).getSHA1() : null;
			List<String> filesBefore = new ArrayList<String>();
			List<String> filesCurrent = new ArrayList<String>();
			Map<String, String> renamedFilesHint = new HashMap<String, String>();
			for (GHCommit.File commitFile : commitFiles) {
				if (commitFile.getFileName().endsWith(".java")) {
					if (commitFile.getStatus().equals("modified")) {
						filesBefore.add(commitFile.getFileName());
						filesCurrent.add(commitFile.getFileName());
					}
					else if (commitFile.getStatus().equals("added")) {
						filesCurrent.add(commitFile.getFileName());
					}
					else if (commitFile.getStatus().equals("removed")) {
						filesBefore.add(commitFile.getFileName());
					}
					else if (commitFile.getStatus().equals("renamed")) {
						filesBefore.add(commitFile.getPreviousFilename());
						filesCurrent.add(commitFile.getFileName());
						renamedFilesHint.put(commitFile.getPreviousFilename(), commitFile.getFileName());
					}
				}
			}
			ChangedFileInfo changedFileInfo = new ChangedFileInfo(parentCommitId, filesBefore, filesCurrent, renamedFilesHint);
			final ObjectMapper mapper = new ObjectMapper();
			mapper.writeValue(jsonFile, changedFileInfo);
			return changedFileInfo;
		}
	}

	@Override
	public GitHub connectToGitHub(String oAuthToken) {
		if(gitHub == null) {
			try {
				gitHub = GitHub.connectUsingOAuth(oAuthToken);
				if(gitHub.isCredentialValid()) {
					logger.info("Connected to GitHub with OAuth token");
				} else {
					connectToGitHub();
				}
			} catch(IOException ioe) {
				ioe.printStackTrace();
			}
		}
		return gitHub;
	}

	private GitHub connectToGitHub() {
		if(gitHub == null) {
			try {
				String oAuthToken = System.getenv("OAuthToken");
				if (oAuthToken == null || oAuthToken.isEmpty()) {
					Properties prop = new Properties();
					InputStream input = new FileInputStream("github-oauth.properties");
					prop.load(input);
					oAuthToken = prop.getProperty("OAuthToken");
				}
				if (oAuthToken != null) {
					gitHub = GitHub.connectUsingOAuth(oAuthToken);
					if(gitHub.isCredentialValid()) {
						logger.info("Connected to GitHub with OAuth token");
					}
				}
				else {
					gitHub = GitHub.connect();
				}
			} catch(FileNotFoundException e) {
				logger.warn("File github-oauth.properties was not found in RefactoringMiner's execution directory", e);
			} catch(IOException ioe) {
				ioe.printStackTrace();
			}
		}
		return gitHub;
	}

	protected List<Refactoring> filter(List<Refactoring> refactoringsAtRevision) {
		if (this.refactoringTypesToConsider == null) {
			return refactoringsAtRevision;
		}
		List<Refactoring> filteredList = new ArrayList<Refactoring>();
		for (Refactoring ref : refactoringsAtRevision) {
			if (this.refactoringTypesToConsider.contains(ref.getRefactoringType())) {
				filteredList.add(ref);
			}
		}
		return filteredList;
	}
	
	@Override
	public void detectAll(Repository repository, String branch, final RefactoringHandler handler) throws Exception {
		GitService gitService = new GitServiceImpl() {
			@Override
			public boolean isCommitAnalyzed(String sha1) {
				return handler.skipCommit(sha1);
			}
		};
		RevWalk walk = gitService.createAllRevsWalk(repository, branch);
		try {
			detect(gitService, repository, handler, walk.iterator());
		} finally {
			walk.dispose();
		}
	}

	@Override
	public void fetchAndDetectNew(Repository repository, final RefactoringHandler handler) throws Exception {
		GitService gitService = new GitServiceImpl() {
			@Override
			public boolean isCommitAnalyzed(String sha1) {
				return handler.skipCommit(sha1);
			}
		};
		RevWalk walk = gitService.fetchAndCreateNewRevsWalk(repository);
		try {
			detect(gitService, repository, handler, walk.iterator());
		} finally {
			walk.dispose();
		}
	}

	public static UMLModel createModel(Map<String, String> fileContents, Set<String> repositoryDirectories) throws Exception {
		return new UMLModelASTReader(fileContents, repositoryDirectories, false).getUmlModel();
	}

	public static UMLModel createModelForASTDiff(Map<String, String> fileContents, Set<String> repositoryDirectories) throws Exception {
		return new UMLModelASTReader(fileContents, repositoryDirectories, true).getUmlModel();
	}

	private static final String systemFileSeparator = Matcher.quoteReplacement(File.separator);

	private static List<String> getJavaFilePaths(File folder) throws IOException {
		Stream<Path> walk = Files.walk(Paths.get(folder.toURI()));
		List<String> paths = walk.map(x -> x.toString())
				.filter(f -> f.endsWith(".java"))
				.map(x -> x.substring(folder.getPath().length()+1).replaceAll(systemFileSeparator, "/"))
				.collect(Collectors.toList());
		walk.close();
		return paths;
	}

	private static Set<String> populateDirectories(Map<String, String> fileContents) {
		Set<String> repositoryDirectories = new LinkedHashSet<>();
		for(String path : fileContents.keySet()) {
			String directory = new String(path);
			while(directory.contains("/")) {
				directory = directory.substring(0, directory.lastIndexOf("/"));
				repositoryDirectories.add(directory);
			}
		}
		return repositoryDirectories;
	}

	@Override
	public void detectAtFileContents(Map<String, String> fileContentsBefore, Map<String, String> fileContentsAfter, RefactoringHandler handler) {
		List<Refactoring> refactorings = Collections.emptyList();
		Set<String> repositoryDirectoriesBefore = populateDirectories(fileContentsBefore);
		Set<String> repositoryDirectoriesCurrent = populateDirectories(fileContentsAfter);
		String rootDirBefore = repositoryDirectoriesBefore.stream()
                .sorted(Comparator.comparingInt(String::length))
                .findFirst()
                .orElse("");
		String rootDirCurrent = repositoryDirectoriesCurrent.stream()
                .sorted(Comparator.comparingInt(String::length))
                .findFirst()
                .orElse("");
		String id = rootDirBefore + " -> " + rootDirCurrent;
		try {
			List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = processIdenticalFiles(fileContentsBefore, fileContentsAfter, Collections.emptyMap(), false); 
			UMLModel parentUMLModel = createModel(fileContentsBefore, repositoryDirectoriesBefore);
			UMLModel currentUMLModel = createModel(fileContentsAfter, repositoryDirectoriesCurrent);
			UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
			refactorings = modelDiff.getRefactorings();
			refactorings.addAll(moveSourceFolderRefactorings);
			refactorings = filter(refactorings);
			handler.handleModelDiff(id, refactorings, modelDiff);
		} catch (Exception e) {
			logger.warn(String.format("Ignored revision %s due to error", id), e);
			handler.handleException(id, e);
		}
		handler.handle(id, refactorings);
	}

	@Override
	public void detectAtDirectories(Path previousPath, Path nextPath, RefactoringHandler handler) {
		File previousFile = previousPath.toFile();
		File nextFile = nextPath.toFile();
		detectAtDirectories(previousFile, nextFile, handler);
	}

	@Override
	public void detectAtDirectories(File previousFile, File nextFile, RefactoringHandler handler) {
		if(previousFile.exists() && nextFile.exists()) {
			List<Refactoring> refactorings = Collections.emptyList();
			String id = previousFile.getName() + " -> " + nextFile.getName();
			try {
				if(previousFile.isDirectory() && nextFile.isDirectory()) {
					Set<String> repositoryDirectoriesBefore = new LinkedHashSet<String>();
					Set<String> repositoryDirectoriesCurrent = new LinkedHashSet<String>();
					Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
					Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
					populateFileContents(nextFile, getJavaFilePaths(nextFile), fileContentsCurrent, repositoryDirectoriesCurrent);
					populateFileContents(previousFile, getJavaFilePaths(previousFile), fileContentsBefore, repositoryDirectoriesBefore);
					List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = processIdenticalFiles(fileContentsBefore, fileContentsCurrent, Collections.emptyMap(), false); 
					UMLModel parentUMLModel = createModel(fileContentsBefore, repositoryDirectoriesBefore);
					UMLModel currentUMLModel = createModel(fileContentsCurrent, repositoryDirectoriesCurrent);
					UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
					refactorings = modelDiff.getRefactorings();
					refactorings.addAll(moveSourceFolderRefactorings);
					refactorings = filter(refactorings);
					handler.handleModelDiff(id, refactorings, modelDiff);
				}
				else if(previousFile.isFile() && nextFile.isFile()) {
					String previousFileName = previousFile.getName();
					String nextFileName = nextFile.getName();
					if(previousFileName.endsWith(".java") && nextFileName.endsWith(".java")) {
						Set<String> repositoryDirectoriesBefore = new LinkedHashSet<String>();
						Set<String> repositoryDirectoriesCurrent = new LinkedHashSet<String>();
						Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
						Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
						populateFileContents(nextFile.getParentFile(), List.of(nextFileName), fileContentsCurrent, repositoryDirectoriesCurrent);
						populateFileContents(previousFile.getParentFile(), List.of(previousFileName), fileContentsBefore, repositoryDirectoriesBefore);
						List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = processIdenticalFiles(fileContentsBefore, fileContentsCurrent, Collections.emptyMap(), false); 
						UMLModel parentUMLModel = createModelForASTDiff(fileContentsBefore, repositoryDirectoriesBefore);
						UMLModel currentUMLModel = createModelForASTDiff(fileContentsCurrent, repositoryDirectoriesCurrent);
						UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
						refactorings = modelDiff.getRefactorings();
						refactorings.addAll(moveSourceFolderRefactorings);
						refactorings = filter(refactorings);
						handler.handleModelDiff(id, refactorings, modelDiff);
					}
				}
			}
			catch (Exception e) {
				logger.warn(String.format("Ignored revision %s due to error", id), e);
				handler.handleException(id, e);
			}
			handler.handle(id, refactorings);
		}
	}

	@Override
	public void detectAtCommit(Repository repository, String commitId, RefactoringHandler handler) {
		String cloneURL = repository.getConfig().getString("remote", "origin", "url");
		File metadataFolder = repository.getDirectory();
		File projectFolder = metadataFolder.getParentFile();
		GitService gitService = new GitServiceImpl();
		RevWalk walk = new RevWalk(repository);
		try {
			RevCommit commit = walk.parseCommit(repository.resolve(commitId));
			if (commit.getParentCount() > 0) {
				walk.parseCommit(commit.getParent(0));
				this.detectRefactorings(gitService, repository, handler, commit);
			}
			else {
				logger.warn(String.format("Ignored revision %s because it has no parent", commitId));
			}
		} catch (MissingObjectException moe) {
			this.detectRefactorings(handler, projectFolder, cloneURL, commitId);
		} catch (RefactoringMinerTimedOutException e) {
			logger.warn(String.format("Ignored revision %s due to timeout", commitId), e);
		} catch (Exception e) {
			logger.warn(String.format("Ignored revision %s due to error", commitId), e);
			handler.handleException(commitId, e);
		} finally {
			walk.close();
			walk.dispose();
		}
	}

	public void detectAtCommit(Repository repository, String commitId, RefactoringHandler handler, int timeout) {
		ExecutorService service = Executors.newSingleThreadExecutor();
		Future<?> f = null;
		try {
			Runnable r = () -> detectAtCommit(repository, commitId, handler);
			f = service.submit(r);
			f.get(timeout, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			f.cancel(true);
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			service.shutdown();
		}
	}

	@Override
	public String getConfigId() {
	    return "RM1";
	}

	@Override
	public void detectBetweenTags(Repository repository, String startTag, String endTag, RefactoringHandler handler)
			throws Exception {
		GitService gitService = new GitServiceImpl() {
			@Override
			public boolean isCommitAnalyzed(String sha1) {
				return handler.skipCommit(sha1);
			}
		};
		
		Iterable<RevCommit> walk = gitService.createRevsWalkBetweenTags(repository, startTag, endTag);
		detect(gitService, repository, handler, walk.iterator());
	}

	@Override
	public void detectBetweenCommits(Repository repository, String startCommitId, String endCommitId,
			RefactoringHandler handler) throws Exception {
		GitService gitService = new GitServiceImpl() {
			@Override
			public boolean isCommitAnalyzed(String sha1) {
				return handler.skipCommit(sha1);
			}
		};
		
		Iterable<RevCommit> walk = gitService.createRevsWalkBetweenCommits(repository, startCommitId, endCommitId);
		detect(gitService, repository, handler, walk.iterator());
	}

	@Override
	public void detectAtCommit(String gitURL, String commitId, RefactoringHandler handler, int timeout) {
		ExecutorService service = Executors.newSingleThreadExecutor();
		Future<?> f = null;
		try {
			Runnable r = () -> detectRefactorings(handler, gitURL, commitId);
			f = service.submit(r);
			f.get(timeout, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			f.cancel(true);
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			service.shutdown();
		}
	}

	protected List<Refactoring> detectRefactorings(final RefactoringHandler handler, String gitURL, String currentCommitId) {
		List<Refactoring> refactoringsAtRevision = Collections.emptyList();
		try {
			Set<String> repositoryDirectoriesBefore = ConcurrentHashMap.newKeySet();
			Set<String> repositoryDirectoriesCurrent = ConcurrentHashMap.newKeySet();
			Map<String, String> fileContentsBefore = new ConcurrentHashMap<String, String>();
			Map<String, String> fileContentsCurrent = new ConcurrentHashMap<String, String>();
			Map<String, String> renamedFilesHint = new ConcurrentHashMap<String, String>();
			List<String> commitFileNames = new ArrayList<>();
			populateWithGitHubAPI(gitURL, currentCommitId, commitFileNames, fileContentsBefore, fileContentsCurrent, renamedFilesHint, repositoryDirectoriesBefore, repositoryDirectoriesCurrent);
			Map<String, String> filesBefore = new LinkedHashMap<String, String>();
			Map<String, String> filesCurrent = new LinkedHashMap<String, String>();
			for(String fileName : commitFileNames) {
				if(fileContentsBefore.containsKey(fileName)) {
					filesBefore.put(fileName, fileContentsBefore.get(fileName));
				}
				if(fileContentsCurrent.containsKey(fileName)) {
					filesCurrent.put(fileName, fileContentsCurrent.get(fileName));
				}
			}
			fileContentsBefore = filesBefore;
			fileContentsCurrent = filesCurrent;
			List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = processIdenticalFiles(fileContentsBefore, fileContentsCurrent, renamedFilesHint, false);
			UMLModel currentUMLModel = createModel(fileContentsCurrent, repositoryDirectoriesCurrent);
			UMLModel parentUMLModel = createModel(fileContentsBefore, repositoryDirectoriesBefore);
			UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
			refactoringsAtRevision = modelDiff.getRefactorings();
			refactoringsAtRevision.addAll(moveSourceFolderRefactorings);
			refactoringsAtRevision = filter(refactoringsAtRevision);
			handler.handle(currentCommitId, refactoringsAtRevision);
			handler.handleModelDiff(currentCommitId, refactoringsAtRevision, modelDiff);
		}
		catch(RefactoringMinerTimedOutException e) {
			logger.warn(String.format("Ignored revision %s due to timeout", currentCommitId), e);
			handler.handleException(currentCommitId, e);
		}
		catch (Exception e) {
			logger.warn(String.format("Ignored revision %s due to error", currentCommitId), e);
			handler.handleException(currentCommitId, e);
		}

		return refactoringsAtRevision;
	}

	private void populateWithGitHubAPI(String cloneURL, String currentCommitId, List<String> commitFileNames,
			Map<String, String> filesBefore, Map<String, String> filesCurrent, Map<String, String> renamedFilesHint,
			Set<String> repositoryDirectoriesBefore, Set<String> repositoryDirectoriesCurrent) throws IOException, InterruptedException {
		GHRepository repository = getGitHubRepository(cloneURL);
		final String commitId = repository.queryCommits().from(currentCommitId).list().iterator().next().getSHA1();
		logger.info("Processing {} {} ...", cloneURL, commitId);
		List<GHCommit.File> commitFiles = new ArrayList<>();
		GHCommit currentCommit = new GHRepositoryWrapper(repository).getCommit(commitId, commitFiles);
		//if parents.size() == 0 then currentCommit is the initial commit of the repository, but then all files will have an ADDED status
		final String parentCommitId = currentCommit.getParents().size() > 0 ? currentCommit.getParents().get(0).getSHA1() : null;
		Set<String> deletedAndRenamedFileParentDirectories = ConcurrentHashMap.newKeySet();
		ExecutorService pool = Executors.newFixedThreadPool(commitFiles.size());
		for (GHCommit.File commitFile : commitFiles) {
			String fileName = commitFile.getFileName();
			if (commitFile.getFileName().endsWith(".java")) {
				commitFileNames.add(fileName);
				if (commitFile.getStatus().equals("modified")) {
					Runnable r = () -> {
						try {
							String currentRawFile = null;
							String parentRawFile = null;
							if(repository.isPrivate()) {
								InputStream currentRawFileInputStream = repository.getFileContent(fileName, currentCommitId).read();
								currentRawFile = IOUtils.toString(currentRawFileInputStream);
								InputStream parentRawFileInputStream = repository.getFileContent(fileName, parentCommitId).read();
								parentRawFile = IOUtils.toString(parentRawFileInputStream);
							}
							else {
								URL currentRawURL = commitFile.getRawUrl();
								InputStream currentRawFileInputStream = currentRawURL.openStream();
								currentRawFile = IOUtils.toString(currentRawFileInputStream);
								String rawURLInParentCommit = currentRawURL.toString().replace(commitId, parentCommitId);
								InputStream parentRawFileInputStream = new URL(rawURLInParentCommit).openStream();
								parentRawFile = IOUtils.toString(parentRawFileInputStream);
							}
							filesBefore.put(fileName, parentRawFile);
							filesCurrent.put(fileName, currentRawFile);
						}
						catch(IOException e) {
							e.printStackTrace();
						}
					};
					pool.submit(r);
				}
				else if (commitFile.getStatus().equals("added")) {
					Runnable r = () -> {
						try {
							String currentRawFile = null;
							if(repository.isPrivate()) {
								InputStream currentRawFileInputStream = repository.getFileContent(fileName, currentCommitId).read();
								currentRawFile = IOUtils.toString(currentRawFileInputStream);
							}
							else {
								URL currentRawURL = commitFile.getRawUrl();
								InputStream currentRawFileInputStream = currentRawURL.openStream();
								currentRawFile = IOUtils.toString(currentRawFileInputStream);
							}
							filesCurrent.put(fileName, currentRawFile);
						}
						catch(IOException e) {
							e.printStackTrace();
						}
					};
					pool.submit(r);
				}
				else if (commitFile.getStatus().equals("removed")) {
					Runnable r = () -> {
						try {
							String parentRawFile = null;
							if(repository.isPrivate()) {
								InputStream parentRawFileInputStream = repository.getFileContent(fileName, parentCommitId).read();
								parentRawFile = IOUtils.toString(parentRawFileInputStream);
							}
							else {
								URL rawURL = commitFile.getRawUrl();
								InputStream rawFileInputStream = rawURL.openStream();
								parentRawFile = IOUtils.toString(rawFileInputStream);
							}
							filesBefore.put(fileName, parentRawFile);
							if(fileName.contains("/")) {
								deletedAndRenamedFileParentDirectories.add(fileName.substring(0, fileName.lastIndexOf("/")));
							}
						}
						catch(IOException e) {
							e.printStackTrace();
						}
					};
					pool.submit(r);
				}
				else if (commitFile.getStatus().equals("renamed")) {
					commitFileNames.add(commitFile.getPreviousFilename());
					Runnable r = () -> {
						try {
							String previousFilename = commitFile.getPreviousFilename();
							String currentRawFile = null;
							String parentRawFile = null;
							if(repository.isPrivate()) {
								InputStream currentRawFileInputStream = repository.getFileContent(fileName, currentCommitId).read();
								currentRawFile = IOUtils.toString(currentRawFileInputStream);
								InputStream parentRawFileInputStream = repository.getFileContent(previousFilename, parentCommitId).read();
								parentRawFile = IOUtils.toString(parentRawFileInputStream);
							}
							else {
								URL currentRawURL = commitFile.getRawUrl();
								InputStream currentRawFileInputStream = currentRawURL.openStream();
								currentRawFile = IOUtils.toString(currentRawFileInputStream);
								String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
								String encodedPreviousFilename = URLEncoder.encode(previousFilename, StandardCharsets.UTF_8);
								String rawURLInParentCommit = currentRawURL.toString().replace(commitId, parentCommitId).replace(encodedFileName, encodedPreviousFilename);
								InputStream parentRawFileInputStream = new URL(rawURLInParentCommit).openStream();
								parentRawFile = IOUtils.toString(parentRawFileInputStream);
							}
							filesBefore.put(previousFilename, parentRawFile);
							filesCurrent.put(fileName, currentRawFile);
							renamedFilesHint.put(previousFilename, fileName);
							if(previousFilename.contains("/")) {
								deletedAndRenamedFileParentDirectories.add(previousFilename.substring(0, previousFilename.lastIndexOf("/")));
							}
						}
						catch(IOException e) {
							e.printStackTrace();
						}
					};
					pool.submit(r);
				}
			}
		}
		pool.shutdown();
		pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
		repositoryDirectories(currentCommit.getTree(), "", repositoryDirectoriesCurrent, deletedAndRenamedFileParentDirectories);
		repositoryDirectoriesCurrent.addAll(deletedAndRenamedFileParentDirectories);
		//allRepositoryDirectories(currentCommit.getTree(), "", repositoryDirectoriesCurrent);
		//GHCommit parentCommit = repository.getCommit(parentCommitId);
		//allRepositoryDirectories(parentCommit.getTree(), "", repositoryDirectoriesBefore);
	}

	public void detectAtCommitWithGitHubAPI(String cloneURL, String commitId, File rootFolder, RefactoringHandler m) {
		try {
			List<Refactoring> refactoringsAtRevision = Collections.emptyList();
			Set<String> repositoryDirectoriesBefore = ConcurrentHashMap.newKeySet();
			Set<String> repositoryDirectoriesCurrent = ConcurrentHashMap.newKeySet();
			Map<String, String> fileContentsBefore = new ConcurrentHashMap<String, String>();
			Map<String, String> fileContentsCurrent = new ConcurrentHashMap<String, String>();
			Map<String, String> renamedFilesHint = new ConcurrentHashMap<String, String>();
			ChangedFileInfo info = populateWithGitHubAPIAndSaveFiles(cloneURL, commitId, 
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
			List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = processIdenticalFiles(fileContentsBefore, fileContentsCurrent, renamedFilesHint, false);
			UMLModel currentUMLModel = createModel(fileContentsCurrent, repositoryDirectoriesCurrent);
			UMLModel parentUMLModel = createModel(fileContentsBefore, repositoryDirectoriesBefore);
			UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
			refactoringsAtRevision = modelDiff.getRefactorings();
			refactoringsAtRevision.addAll(moveSourceFolderRefactorings);
			m.handle(commitId, refactoringsAtRevision);
			m.handleModelDiff(commitId, refactoringsAtRevision, modelDiff);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public UMLModelDiff detectAtCommitWithGitHubAPI(String cloneURL, String commitId, File rootFolder) {
		try {
			List<Refactoring> refactoringsAtRevision = Collections.emptyList();
			Set<String> repositoryDirectoriesBefore = ConcurrentHashMap.newKeySet();
			Set<String> repositoryDirectoriesCurrent = ConcurrentHashMap.newKeySet();
			Map<String, String> fileContentsBefore = new ConcurrentHashMap<String, String>();
			Map<String, String> fileContentsCurrent = new ConcurrentHashMap<String, String>();
			Map<String, String> renamedFilesHint = new ConcurrentHashMap<String, String>();
			ChangedFileInfo info = populateWithGitHubAPIAndSaveFiles(cloneURL, commitId, 
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
			List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = processIdenticalFiles(fileContentsBefore, fileContentsCurrent, renamedFilesHint, false);
			UMLModel currentUMLModel = createModel(fileContentsCurrent, repositoryDirectoriesCurrent);
			UMLModel parentUMLModel = createModel(fileContentsBefore, repositoryDirectoriesBefore);
			UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
			refactoringsAtRevision = modelDiff.getRefactorings();
			refactoringsAtRevision.addAll(moveSourceFolderRefactorings);
			return modelDiff;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static ChangedFileInfo populateWithLocalRepositoryAndSaveFiles(Repository repository, String currentCommitId,
			Map<String, String> filesBefore, Map<String, String> filesCurrent, Map<String, String> renamedFilesHint,
			Set<String> repositoryDirectoriesBefore, Set<String> repositoryDirectoriesCurrent, File rootFolder) throws IOException {
		String cloneURL = repository.getConfig().getString("remote", "origin", "url");
		logger.info("Processing {} {} ...", cloneURL, currentCommitId);
		String repoName = cloneURL.substring(cloneURL.lastIndexOf('/') + 1, cloneURL.lastIndexOf('.'));
		String jsonFilePath = repoName + "-" + currentCommitId + ".json";
		File jsonFile = new File(rootFolder, jsonFilePath);
		if(jsonFile.exists()) {
			final ObjectMapper mapper = new ObjectMapper();
			ChangedFileInfo changedFileInfo = mapper.readValue(jsonFile, ChangedFileInfo.class);
			String parentCommitId = changedFileInfo.getParentCommitId();
			String commitId = changedFileInfo.getCurrentCommitId();
			repositoryDirectoriesBefore.addAll(changedFileInfo.getRepositoryDirectoriesBefore());
			repositoryDirectoriesCurrent.addAll(changedFileInfo.getRepositoryDirectoriesCurrent());
			renamedFilesHint.putAll(changedFileInfo.getRenamedFilesHint());
			for(String filePathBefore : changedFileInfo.getFilesBefore()) {
				String fullPath = rootFolder + File.separator + repoName + "-" + parentCommitId + File.separator + filePathBefore.replaceAll("/", systemFileSeparator);
				String contents = FileUtils.readFileToString(new File(fullPath));
				filesBefore.put(filePathBefore, contents);
			}
			for(String filePathCurrent : changedFileInfo.getFilesCurrent()) {
				String fullPath = rootFolder + File.separator + repoName + "-" + commitId + File.separator + filePathCurrent.replaceAll("/", systemFileSeparator);
				String contents = FileUtils.readFileToString(new File(fullPath));
				filesCurrent.put(filePathCurrent, contents);
			}
			return changedFileInfo;
		}
		RevWalk walk = new RevWalk(repository);
		try {
			RevCommit currentCommit = walk.parseCommit(repository.resolve(currentCommitId));
			if (currentCommit.getParentCount() > 0) {
				walk.parseCommit(currentCommit.getParent(0));
				Set<String> filePathsBefore = new LinkedHashSet<String>();
				Set<String> filePathsCurrent = new LinkedHashSet<String>();
				new GitServiceImpl().fileTreeDiff(repository, currentCommit, filePathsBefore, filePathsCurrent, renamedFilesHint);
				
				RevCommit parentCommit = currentCommit.getParent(0);
				populateFileContentsAndSave(repository, parentCommit, filePathsBefore, filesBefore, repositoryDirectoriesBefore, rootFolder);
				populateFileContentsAndSave(repository, currentCommit, filePathsCurrent, filesCurrent, repositoryDirectoriesCurrent, rootFolder);
				
				String parentCommitId = parentCommit.getId().getName();
				ChangedFileInfo changedFileInfo = new ChangedFileInfo(parentCommitId, currentCommitId, 
						new ArrayList<>(filePathsBefore), new ArrayList<>(filePathsCurrent), repositoryDirectoriesBefore, repositoryDirectoriesCurrent, renamedFilesHint);
				final ObjectMapper mapper = new ObjectMapper();
				mapper.writeValue(jsonFile, changedFileInfo);
				walk.close();
				return changedFileInfo;
			}
			else if(currentCommit.getParentCount() == 0) {
				//initial commit of the repository
				Set<String> filePathsBefore = new LinkedHashSet<String>();
				Set<String> filePathsCurrent = new LinkedHashSet<String>();
				new GitServiceImpl().fileTreeDiff(repository, currentCommit, filePathsBefore, filePathsCurrent, renamedFilesHint);
				populateFileContentsAndSave(repository, currentCommit, filePathsCurrent, filesCurrent, repositoryDirectoriesCurrent, rootFolder);
				
				String parentCommitId = "0";
				ChangedFileInfo changedFileInfo = new ChangedFileInfo(parentCommitId, currentCommitId, 
						new ArrayList<>(filePathsBefore), new ArrayList<>(filePathsCurrent), repositoryDirectoriesBefore, repositoryDirectoriesCurrent, renamedFilesHint);
				final ObjectMapper mapper = new ObjectMapper();
				mapper.writeValue(jsonFile, changedFileInfo);
				walk.close();
				return changedFileInfo;
			}
		} catch (Exception e) {
			logger.warn(String.format("Ignored revision %s due to error", currentCommitId), e);
		}
		return null;
	}

	public ChangedFileInfo populateWithGitHubAPIAndSaveFiles(String cloneURL, String currentCommitId,
			Map<String, String> filesBefore, Map<String, String> filesCurrent, Map<String, String> renamedFilesHint,
			Set<String> repositoryDirectoriesBefore, Set<String> repositoryDirectoriesCurrent, File rootFolder) throws IOException, InterruptedException {
		logger.info("Processing {} {} ...", cloneURL, currentCommitId);
		String repoName = cloneURL.substring(cloneURL.lastIndexOf('/') + 1, cloneURL.lastIndexOf('.'));
		String jsonFilePath = repoName + "-" + currentCommitId + ".json";
		File jsonFile = new File(rootFolder, jsonFilePath);
		if(jsonFile.exists()) {
			final ObjectMapper mapper = new ObjectMapper();
			ChangedFileInfo changedFileInfo = mapper.readValue(jsonFile, ChangedFileInfo.class);
			String parentCommitId = changedFileInfo.getParentCommitId();
			String commitId = changedFileInfo.getCurrentCommitId();
			repositoryDirectoriesBefore.addAll(changedFileInfo.getRepositoryDirectoriesBefore());
			repositoryDirectoriesCurrent.addAll(changedFileInfo.getRepositoryDirectoriesCurrent());
			renamedFilesHint.putAll(changedFileInfo.getRenamedFilesHint());
			for(String filePathBefore : changedFileInfo.getFilesBefore()) {
				String fullPath = rootFolder + File.separator + repoName + "-" + parentCommitId + File.separator + filePathBefore.replaceAll("/", systemFileSeparator);
				String contents = FileUtils.readFileToString(new File(fullPath));
				filesBefore.put(filePathBefore, contents);
			}
			for(String filePathCurrent : changedFileInfo.getFilesCurrent()) {
				String fullPath = rootFolder + File.separator + repoName + "-" + commitId + File.separator + filePathCurrent.replaceAll("/", systemFileSeparator);
				String contents = FileUtils.readFileToString(new File(fullPath));
				filesCurrent.put(filePathCurrent, contents);
			}
			return changedFileInfo;
		}
		GHRepository repository = getGitHubRepository(cloneURL);
		final String commitId = repository.queryCommits().from(currentCommitId).list().iterator().next().getSHA1();
		List<GHCommit.File> commitFiles = new ArrayList<>();
		GHCommit currentCommit = new GHRepositoryWrapper(repository).getCommit(commitId, commitFiles);
		//if parents.size() == 0 then currentCommit is the initial commit of the repository, but then all files will have an ADDED status
		final String parentCommitId = currentCommit.getParents().size() > 0 ? currentCommit.getParents().get(0).getSHA1() : null;
		Set<String> deletedAndRenamedFileParentDirectories = ConcurrentHashMap.newKeySet();
		List<String> commitFileNames = new ArrayList<>();
		ExecutorService pool = Executors.newFixedThreadPool(commitFiles.size());
		for (GHCommit.File commitFile : commitFiles) {
			String fileName = commitFile.getFileName();
			if (commitFile.getFileName().endsWith(".java")) {
				commitFileNames.add(fileName);
				if (commitFile.getStatus().equals("modified")) {
					Runnable r = () -> {
						try {
							String currentRawFile = null;
							String parentRawFile = null;
							if(repository.isPrivate()) {
								InputStream currentRawFileInputStream = repository.getFileContent(fileName, currentCommitId).read();
								currentRawFile = IOUtils.toString(currentRawFileInputStream);
								InputStream parentRawFileInputStream = repository.getFileContent(fileName, parentCommitId).read();
								parentRawFile = IOUtils.toString(parentRawFileInputStream);
							}
							else {
								URL currentRawURL = commitFile.getRawUrl();
								InputStream currentRawFileInputStream = currentRawURL.openStream();
								currentRawFile = IOUtils.toString(currentRawFileInputStream);
								String rawURLInParentCommit = currentRawURL.toString().replace(commitId, parentCommitId);
								InputStream parentRawFileInputStream = new URL(rawURLInParentCommit).openStream();
								parentRawFile = IOUtils.toString(parentRawFileInputStream);
							}
							filesBefore.put(fileName, parentRawFile);
							filesCurrent.put(fileName, currentRawFile);
							File parentFilePath = new File(rootFolder, repoName + "-" + parentCommitId + "/" + fileName);
							FileUtils.writeStringToFile(parentFilePath, parentRawFile);
							File currentFilePath = new File(rootFolder, repoName + "-" + currentCommitId + "/" + fileName);
							FileUtils.writeStringToFile(currentFilePath, currentRawFile);
						}
						catch(IOException e) {
							e.printStackTrace();
						}
					};
					pool.submit(r);
				}
				else if (commitFile.getStatus().equals("added")) {
					Runnable r = () -> {
						try {
							String currentRawFile = null;
							if(repository.isPrivate()) {
								InputStream currentRawFileInputStream = repository.getFileContent(fileName, currentCommitId).read();
								currentRawFile = IOUtils.toString(currentRawFileInputStream);
							}
							else {
								URL currentRawURL = commitFile.getRawUrl();
								InputStream currentRawFileInputStream = currentRawURL.openStream();
								currentRawFile = IOUtils.toString(currentRawFileInputStream);
							}
							filesCurrent.put(fileName, currentRawFile);
							File currentFilePath = new File(rootFolder, repoName + "-" + currentCommitId + "/" + fileName);
							FileUtils.writeStringToFile(currentFilePath, currentRawFile);
						}
						catch(IOException e) {
							e.printStackTrace();
						}
					};
					pool.submit(r);
				}
				else if (commitFile.getStatus().equals("removed")) {
					Runnable r = () -> {
						try {
							String parentRawFile = null;
							if(repository.isPrivate()) {
								InputStream parentRawFileInputStream = repository.getFileContent(fileName, parentCommitId).read();
								parentRawFile = IOUtils.toString(parentRawFileInputStream);
							}
							else {
								URL rawURL = commitFile.getRawUrl();
								InputStream rawFileInputStream = rawURL.openStream();
								parentRawFile = IOUtils.toString(rawFileInputStream);
							}
							filesBefore.put(fileName, parentRawFile);
							if(fileName.contains("/")) {
								deletedAndRenamedFileParentDirectories.add(fileName.substring(0, fileName.lastIndexOf("/")));
							}
							File parentFilePath = new File(rootFolder, repoName + "-" + parentCommitId + "/" + fileName);
							FileUtils.writeStringToFile(parentFilePath, parentRawFile);
						}
						catch(IOException e) {
							e.printStackTrace();
						}
					};
					pool.submit(r);
				}
				else if (commitFile.getStatus().equals("renamed")) {
					commitFileNames.add(commitFile.getPreviousFilename());
					Runnable r = () -> {
						try {
							String previousFilename = commitFile.getPreviousFilename();
							String currentRawFile = null;
							String parentRawFile = null;
							if(repository.isPrivate()) {
								InputStream currentRawFileInputStream = repository.getFileContent(fileName, currentCommitId).read();
								currentRawFile = IOUtils.toString(currentRawFileInputStream);
								InputStream parentRawFileInputStream = repository.getFileContent(previousFilename, parentCommitId).read();
								parentRawFile = IOUtils.toString(parentRawFileInputStream);
							}
							else {
								URL currentRawURL = commitFile.getRawUrl();
								InputStream currentRawFileInputStream = currentRawURL.openStream();
								currentRawFile = IOUtils.toString(currentRawFileInputStream);
								String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
								String encodedPreviousFilename = URLEncoder.encode(previousFilename, StandardCharsets.UTF_8);
								String rawURLInParentCommit = currentRawURL.toString().replace(commitId, parentCommitId).replace(encodedFileName, encodedPreviousFilename);
								InputStream parentRawFileInputStream = new URL(rawURLInParentCommit).openStream();
								parentRawFile = IOUtils.toString(parentRawFileInputStream);
							}
							filesBefore.put(previousFilename, parentRawFile);
							filesCurrent.put(fileName, currentRawFile);
							renamedFilesHint.put(previousFilename, fileName);
							if(previousFilename.contains("/")) {
								deletedAndRenamedFileParentDirectories.add(previousFilename.substring(0, previousFilename.lastIndexOf("/")));
							}
							File parentFilePath = new File(rootFolder, repoName + "-" + parentCommitId + "/" + previousFilename);
							FileUtils.writeStringToFile(parentFilePath, parentRawFile);
							File currentFilePath = new File(rootFolder, repoName + "-" + currentCommitId + "/" + fileName);
							FileUtils.writeStringToFile(currentFilePath, currentRawFile);
						}
						catch(IOException e) {
							e.printStackTrace();
						}
					};
					pool.submit(r);
				}
			}
		}
		pool.shutdown();
		pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
		List<String> orderedFilesBefore = new ArrayList<>();
		List<String> orderedFilesCurrent = new ArrayList<>();
		for(String fileName : commitFileNames) {
			if(filesBefore.containsKey(fileName)) {
				orderedFilesBefore.add(fileName);
			}
			if(filesCurrent.containsKey(fileName)) {
				orderedFilesCurrent.add(fileName);
			}
		}
		repositoryDirectories(currentCommit.getTree(), "", repositoryDirectoriesCurrent, new LinkedHashSet<>(orderedFilesCurrent));
		//repositoryDirectoriesCurrent.addAll(deletedAndRenamedFileParentDirectories);
		//allRepositoryDirectories(currentCommit.getTree(), "", repositoryDirectoriesCurrent);
		if(parentCommitId != null) {
			GHCommit parentCommit = repository.getCommit(parentCommitId);
			repositoryDirectories(parentCommit.getTree(), "", repositoryDirectoriesBefore, new LinkedHashSet<>(orderedFilesBefore));
		}
		ChangedFileInfo changedFileInfo = new ChangedFileInfo(parentCommitId, commitId, orderedFilesBefore, orderedFilesCurrent, repositoryDirectoriesBefore, repositoryDirectoriesCurrent, renamedFilesHint);
		final ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(jsonFile, changedFileInfo);
		return changedFileInfo;
	}

	private void repositoryDirectories(GHTree tree, String pathFromRoot, Set<String> repositoryDirectories, Set<String> targetPaths) throws IOException {
		for(GHTreeEntry entry : tree.getTree()) {
			String path = null;
			if(pathFromRoot.equals("")) {
				path = entry.getPath();
			}
			else {
				path = pathFromRoot + "/" + entry.getPath();
			}
			if(atLeastOneStartsWith(targetPaths, path)) {
				if(targetPaths.contains(path)) {
					repositoryDirectories.add(path);
				}
				else {
					repositoryDirectories.add(path);
					GHTree asTree = entry.asTree();
					if(asTree != null) {
						repositoryDirectories(asTree, path, repositoryDirectories, targetPaths);
					}
				}
			}
		}
	}

	private boolean atLeastOneStartsWith(Set<String> targetPaths, String path) {
		for(String targetPath : targetPaths) {
			if(path.endsWith("/") && targetPath.startsWith(path)) {
				return true;
			}
			else if(!path.endsWith("/") && targetPath.startsWith(path + "/")) {
				return true;
			}
		}
		return false;
	}
	/*
	private void allRepositoryDirectories(GHTree tree, String pathFromRoot, Set<String> repositoryDirectories) throws IOException {
		for(GHTreeEntry entry : tree.getTree()) {
			String path = null;
			if(pathFromRoot.equals("")) {
				path = entry.getPath();
			}
			else {
				path = pathFromRoot + "/" + entry.getPath();
			}
			GHTree asTree = entry.asTree();
			if(asTree != null) {
				allRepositoryDirectories(asTree, path, repositoryDirectories);
			}
			else if(path.endsWith(".java")) {
				repositoryDirectories.add(path.substring(0, path.lastIndexOf("/")));
			}
		}
	}
	*/

	@Override
	public void detectAtPullRequest(String cloneURL, int pullRequestId, RefactoringHandler handler, int timeout) throws IOException {
		GHRepository repository = getGitHubRepository(cloneURL);
		GHPullRequest pullRequest = repository.getPullRequest(pullRequestId);
		PagedIterable<GHPullRequestCommitDetail> commits = pullRequest.listCommits();
		for(GHPullRequestCommitDetail commit : commits) {
			detectAtCommit(cloneURL, commit.getSha(), handler, timeout);
		}
	}

	public GHRepository getGitHubRepository(String cloneURL) throws IOException {
		GitHub gitHub = connectToGitHub();
		String repoName = extractRepositoryName(cloneURL);
		return gitHub.getRepository(repoName);
	}

	private static final String GITHUB_URL = "https://github.com/";
	private static final String BITBUCKET_URL = "https://bitbucket.org/";

	private static String extractRepositoryName(String cloneURL) {
		int hostLength = 0;
		if(cloneURL.startsWith(GITHUB_URL)) {
			hostLength = GITHUB_URL.length();
		}
		else if(cloneURL.startsWith(BITBUCKET_URL)) {
			hostLength = BITBUCKET_URL.length();
		}
		int indexOfDotGit = cloneURL.length();
		if(cloneURL.endsWith(".git")) {
			indexOfDotGit = cloneURL.indexOf(".git");
		}
		else if(cloneURL.endsWith("/")) {
			indexOfDotGit = cloneURL.length() - 1;
		}
		String repoName = cloneURL.substring(hostLength, indexOfDotGit);
		return repoName;
	}

	public static String extractCommitURL(String cloneURL, String commitId) {
		int indexOfDotGit = cloneURL.length();
		if(cloneURL.endsWith(".git")) {
			indexOfDotGit = cloneURL.indexOf(".git");
		}
		else if(cloneURL.endsWith("/")) {
			indexOfDotGit = cloneURL.length() - 1;
		}
		String commitResource = "/";
		if(cloneURL.startsWith(GITHUB_URL)) {
			commitResource = "/commit/";
		}
		else if(cloneURL.startsWith(BITBUCKET_URL)) {
			commitResource = "/commits/";
		}
		String commitURL = cloneURL.substring(0, indexOfDotGit) + commitResource + commitId;
		return commitURL;
	}

	private static String extractDownloadLink(String cloneURL, String commitId) {
		int indexOfDotGit = cloneURL.length();
		if(cloneURL.endsWith(".git")) {
			indexOfDotGit = cloneURL.indexOf(".git");
		}
		else if(cloneURL.endsWith("/")) {
			indexOfDotGit = cloneURL.length() - 1;
		}
		String downloadResource = "/";
		if(cloneURL.startsWith(GITHUB_URL)) {
			downloadResource = "/archive/";
		}
		else if(cloneURL.startsWith(BITBUCKET_URL)) {
			downloadResource = "/get/";
		}
		String downloadLink = cloneURL.substring(0, indexOfDotGit) + downloadResource + commitId + ".zip";
		return downloadLink;
	}

	@Override
	public ProjectASTDiff diffAtCommit(Repository repository, String commitId) {
		String cloneURL = repository.getConfig().getString("remote", "origin", "url");
		File metadataFolder = repository.getDirectory();
		File projectFolder = metadataFolder.getParentFile();
		GitService gitService = new GitServiceImpl();
		RevWalk walk = new RevWalk(repository);
		try {
			RevCommit currentCommit = walk.parseCommit(repository.resolve(commitId));
			if (currentCommit.getParentCount() > 0) {
				walk.parseCommit(currentCommit.getParent(0));
				Set<String> filePathsBefore = new LinkedHashSet<String>();
				Set<String> filePathsCurrent = new LinkedHashSet<String>();
				Map<String, String> renamedFilesHint = new HashMap<String, String>();
				gitService.fileTreeDiff(repository, currentCommit, filePathsBefore, filePathsCurrent, renamedFilesHint);
				
				Set<String> repositoryDirectoriesBefore = new LinkedHashSet<String>();
				Set<String> repositoryDirectoriesCurrent = new LinkedHashSet<String>();
				Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
				Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
				// If no java files changed, there is no refactoring. Also, if there are
				// only ADD's or only REMOVE's there is no refactoring
				if (!filePathsBefore.isEmpty() && !filePathsCurrent.isEmpty() && currentCommit.getParentCount() > 0) {
					RevCommit parentCommit = currentCommit.getParent(0);
					populateFileContents(repository, parentCommit, filePathsBefore, fileContentsBefore, repositoryDirectoriesBefore);
					populateFileContents(repository, currentCommit, filePathsCurrent, fileContentsCurrent, repositoryDirectoriesCurrent);
					List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = processIdenticalFiles(fileContentsBefore, fileContentsCurrent, renamedFilesHint, true);
					UMLModel parentUMLModel = createModelForASTDiff(fileContentsBefore, repositoryDirectoriesBefore);
					UMLModel currentUMLModel = createModelForASTDiff(fileContentsCurrent, repositoryDirectoriesCurrent);
					UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
					ProjectASTDiffer differ = new ProjectASTDiffer(modelDiff, fileContentsBefore, fileContentsCurrent);
					return differ.getProjectASTDiff();
				}
				else if (currentCommit.getParentCount() == 0) {
					//initial commit of the repository
					populateFileContents(repository, currentCommit, filePathsCurrent, fileContentsCurrent, repositoryDirectoriesCurrent);
					List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = processIdenticalFiles(fileContentsBefore, fileContentsCurrent, renamedFilesHint, true);
					UMLModel parentUMLModel = createModelForASTDiff(fileContentsBefore, repositoryDirectoriesBefore);
					UMLModel currentUMLModel = createModelForASTDiff(fileContentsCurrent, repositoryDirectoriesCurrent);
					UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
					ProjectASTDiffer differ = new ProjectASTDiffer(modelDiff, fileContentsBefore, fileContentsCurrent);
					return differ.getProjectASTDiff();
				}
			}
			else {
				logger.warn(String.format("Ignored revision %s because it has no parent", commitId));
			}
		} catch (MissingObjectException moe) {
			try {
				ChangedFileInfo changedFileInfo = populateWithGitHubAPI(projectFolder, cloneURL, commitId);
				String parentCommitId = changedFileInfo.getParentCommitId();
				List<String> filesBefore = changedFileInfo.getFilesBefore();
				List<String> filesCurrent = changedFileInfo.getFilesCurrent();
				Map<String, String> renamedFilesHint = changedFileInfo.getRenamedFilesHint();
				File currentFolder = new File(projectFolder.getParentFile(), projectFolder.getName() + "-" + commitId);
				File parentFolder = new File(projectFolder.getParentFile(), projectFolder.getName() + "-" + parentCommitId);
				if (!currentFolder.exists()) {	
					downloadAndExtractZipFile(projectFolder, cloneURL, commitId);
				}
				if (!parentFolder.exists()) {	
					downloadAndExtractZipFile(projectFolder, cloneURL, parentCommitId);
				}
				Set<String> repositoryDirectoriesBefore = new LinkedHashSet<String>();
				Set<String> repositoryDirectoriesCurrent = new LinkedHashSet<String>();
				Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
				Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
				if (currentFolder.exists() && parentFolder.exists()) {
					populateFileContents(currentFolder, filesCurrent, fileContentsCurrent, repositoryDirectoriesCurrent);
					populateFileContents(parentFolder, filesBefore, fileContentsBefore, repositoryDirectoriesBefore);
					List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = processIdenticalFiles(fileContentsBefore, fileContentsCurrent, renamedFilesHint, true); 
					UMLModel parentUMLModel = createModelForASTDiff(fileContentsBefore, repositoryDirectoriesBefore);
					UMLModel currentUMLModel = createModelForASTDiff(fileContentsCurrent, repositoryDirectoriesCurrent);
					UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
					ProjectASTDiffer differ = new ProjectASTDiffer(modelDiff, fileContentsBefore, fileContentsCurrent);
					return differ.getProjectASTDiff();
				}
			} catch (Exception e) {
				logger.warn(String.format("Ignored revision %s due to error", commitId), e);
			}
		} catch (RefactoringMinerTimedOutException e) {
			logger.warn(String.format("Ignored revision %s due to timeout", commitId), e);
		} catch (Exception e) {
			logger.warn(String.format("Ignored revision %s due to error", commitId), e);
		} finally {
			walk.close();
			walk.dispose();
		}
		return null;
	}

	public ProjectASTDiff diffAtCommitWithGitHubAPI(String cloneURL, String commitId, File rootFolder) {
		try {
			Set<String> repositoryDirectoriesBefore = ConcurrentHashMap.newKeySet();
			Set<String> repositoryDirectoriesCurrent = ConcurrentHashMap.newKeySet();
			Map<String, String> fileContentsBefore = new ConcurrentHashMap<String, String>();
			Map<String, String> fileContentsCurrent = new ConcurrentHashMap<String, String>();
			Map<String, String> renamedFilesHint = new ConcurrentHashMap<String, String>();
			ChangedFileInfo info = populateWithGitHubAPIAndSaveFiles(cloneURL, commitId, 
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
			List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = processIdenticalFiles(fileContentsBefore, fileContentsCurrent, renamedFilesHint, true);
			UMLModel currentUMLModel = createModelForASTDiff(fileContentsCurrent, repositoryDirectoriesCurrent);
			UMLModel parentUMLModel = createModelForASTDiff(fileContentsBefore, repositoryDirectoriesBefore);
			UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
			ProjectASTDiffer differ = new ProjectASTDiffer(modelDiff, fileContentsBefore, fileContentsCurrent);
			return differ.getProjectASTDiff();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public ProjectASTDiff diffAtPullRequest(String cloneURL, int pullRequestId, int timeout) throws Exception {
		GHRepository repository = getGitHubRepository(cloneURL);
		GHPullRequest pullRequest = repository.getPullRequest(pullRequestId);
		PagedIterable<GHPullRequestFileDetail> files = pullRequest.listFiles();
		int changedFiles = pullRequest.getChangedFiles();
		Map<String, String> filesBefore = new ConcurrentHashMap<String, String>();
		Map<String, String> filesCurrent = new ConcurrentHashMap<String, String>();
		Map<String, String> renamedFilesHint = new ConcurrentHashMap<String, String>();
		Set<String> repositoryDirectoriesBefore = ConcurrentHashMap.newKeySet();
		Set<String> repositoryDirectoriesCurrent = ConcurrentHashMap.newKeySet();
		Set<String> deletedAndRenamedFileParentDirectories = ConcurrentHashMap.newKeySet();
		List<String> commitFileNames = new ArrayList<>();
		ExecutorService pool = Executors.newFixedThreadPool(changedFiles);
		for(GHPullRequestFileDetail commitFile : files) {
			String fileName = commitFile.getFilename();
			if (commitFile.getFilename().endsWith(".java")) {
				commitFileNames.add(fileName);
				if (commitFile.getStatus().equals("modified")) {
					Runnable r = () -> {
						try {
							URL currentRawURL = commitFile.getRawUrl();
							InputStream currentRawFileInputStream = currentRawURL.openStream();
							String currentRawFile = IOUtils.toString(currentRawFileInputStream);
							List<String> patchLineList = createPatchLines(commitFile);
							com.github.difflib.patch.Patch<String> patch = UnifiedDiffUtils.parseUnifiedDiff(patchLineList);
							List<String> parentRawFileLines = DiffUtils.unpatch(Arrays.asList(currentRawFile.split("\\n")), patch);
							String parentRawFile = String.join("\n", parentRawFileLines);
							filesBefore.put(fileName, parentRawFile);
							filesCurrent.put(fileName, currentRawFile);
							String directory = new String(fileName);
							while(directory.contains("/")) {
								directory = directory.substring(0, directory.lastIndexOf("/"));
								repositoryDirectoriesBefore.add(directory);
								repositoryDirectoriesCurrent.add(directory);
							}
						}
						catch(IOException e) {
							e.printStackTrace();
						}
					};
					pool.submit(r);
				}
				else if (commitFile.getStatus().equals("added")) {
					Runnable r = () -> {
						try {
							URL currentRawURL = commitFile.getRawUrl();
							InputStream currentRawFileInputStream = currentRawURL.openStream();
							String currentRawFile = IOUtils.toString(currentRawFileInputStream);
							filesCurrent.put(fileName, currentRawFile);
						}
						catch(IOException e) {
							e.printStackTrace();
						}
					};
					pool.submit(r);
				}
				else if (commitFile.getStatus().equals("removed")) {
					Runnable r = () -> {
						try {
							URL rawURL = commitFile.getRawUrl();
							InputStream rawFileInputStream = rawURL.openStream();
							String parentRawFile = IOUtils.toString(rawFileInputStream);
							filesBefore.put(fileName, parentRawFile);
							if(fileName.contains("/")) {
								deletedAndRenamedFileParentDirectories.add(fileName.substring(0, fileName.lastIndexOf("/")));
							}
						}
						catch(IOException e) {
							e.printStackTrace();
						}
					};
					pool.submit(r);
				}
				else if (commitFile.getStatus().equals("renamed")) {
					commitFileNames.add(commitFile.getPreviousFilename());
					Runnable r = () -> {
						try {
							String previousFilename = commitFile.getPreviousFilename();
							URL currentRawURL = commitFile.getRawUrl();
							InputStream currentRawFileInputStream = currentRawURL.openStream();
							String currentRawFile = IOUtils.toString(currentRawFileInputStream);
							List<String> patchLineList = createPatchLines(commitFile);
							com.github.difflib.patch.Patch<String> patch = UnifiedDiffUtils.parseUnifiedDiff(patchLineList);
							List<String> parentRawFileLines = DiffUtils.unpatch(Arrays.asList(currentRawFile.split("\\n")), patch);
							String parentRawFile = String.join("\n", parentRawFileLines);
							filesBefore.put(previousFilename, parentRawFile);
							filesCurrent.put(fileName, currentRawFile);
							renamedFilesHint.put(previousFilename, fileName);
							if(previousFilename.contains("/")) {
								deletedAndRenamedFileParentDirectories.add(previousFilename.substring(0, previousFilename.lastIndexOf("/")));
							}
							String directory = new String(fileName);
							while(directory.contains("/")) {
								directory = directory.substring(0, directory.lastIndexOf("/"));
								repositoryDirectoriesCurrent.add(directory);
							}
							directory = new String(previousFilename);
							while(directory.contains("/")) {
								directory = directory.substring(0, directory.lastIndexOf("/"));
								repositoryDirectoriesBefore.add(directory);
							}
						}
						catch(IOException e) {
							e.printStackTrace();
						}
					};
					pool.submit(r);
				}
			}
		}
		pool.shutdown();
		pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
		
		Set<ProjectASTDiff> diffs = new HashSet<>();
		ExecutorService service = Executors.newSingleThreadExecutor();
		Future<?> f = null;
		try {
			Runnable r = () -> {
				try {
					Map<String, String> filesContentsBefore = new LinkedHashMap<String, String>();
					Map<String, String> filesContentsCurrent = new LinkedHashMap<String, String>();
					for(String fileName : commitFileNames) {
						if(filesBefore.containsKey(fileName)) {
							filesContentsBefore.put(fileName, filesBefore.get(fileName));
						}
						if(filesCurrent.containsKey(fileName)) {
							filesContentsCurrent.put(fileName, filesCurrent.get(fileName));
						}
					}
					List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = processIdenticalFiles(filesContentsBefore, filesContentsCurrent, renamedFilesHint, true);
					UMLModel currentUMLModel = createModelForASTDiff(filesContentsCurrent, repositoryDirectoriesCurrent);
					UMLModel parentUMLModel = createModelForASTDiff(filesContentsBefore, repositoryDirectoriesBefore);
					UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
					ProjectASTDiffer differ = new ProjectASTDiffer(modelDiff, filesBefore, filesCurrent);
					diffs.add(differ.getProjectASTDiff());
				}
				catch(RefactoringMinerTimedOutException e) {
					logger.warn(String.format("Ignored PR %s due to timeout", pullRequestId), e);
				}
				catch (Exception e) {
					logger.warn(String.format("Ignored PR %s due to error", pullRequestId), e);
				}
			};
			f = service.submit(r);
			f.get(timeout, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			f.cancel(true);
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			service.shutdown();
		}
		return diffs.iterator().next();
	}

	private List<String> createPatchLines(GHPullRequestFileDetail commitFile) {
		String[] patchLines = commitFile.getPatch().split("\\n");
		List<String> patchLineList = new ArrayList<String>();
		patchLineList.add("+++");
		for(String line : patchLines) {
			if(line.contains("@@")) {
				String s = line.substring(0, line.lastIndexOf("@@")+2);
				patchLineList.add(s);
			}
			else {
				patchLineList.add(line);
			}
		}
		return patchLineList;
	}

	@Override
	public ProjectASTDiff diffAtCommit(String gitURL, String commitId, int timeout) {
		Set<ProjectASTDiff> diffs = new HashSet<>();
		ExecutorService service = Executors.newSingleThreadExecutor();
		Future<?> f = null;
		try {
			Runnable r = () -> {
				try {
					Set<String> repositoryDirectoriesBefore = ConcurrentHashMap.newKeySet();
					Set<String> repositoryDirectoriesCurrent = ConcurrentHashMap.newKeySet();
					Map<String, String> fileContentsBefore = new ConcurrentHashMap<String, String>();
					Map<String, String> fileContentsCurrent = new ConcurrentHashMap<String, String>();
					Map<String, String> renamedFilesHint = new ConcurrentHashMap<String, String>();
					List<String> commitFileNames = new ArrayList<>();
					populateWithGitHubAPI(gitURL, commitId, commitFileNames, fileContentsBefore, fileContentsCurrent, renamedFilesHint, repositoryDirectoriesBefore, repositoryDirectoriesCurrent);
					Map<String, String> filesBefore = new LinkedHashMap<String, String>();
					Map<String, String> filesCurrent = new LinkedHashMap<String, String>();
					for(String fileName : commitFileNames) {
						if(fileContentsBefore.containsKey(fileName)) {
							filesBefore.put(fileName, fileContentsBefore.get(fileName));
						}
						if(fileContentsCurrent.containsKey(fileName)) {
							filesCurrent.put(fileName, fileContentsCurrent.get(fileName));
						}
					}
					fileContentsBefore = filesBefore;
					fileContentsCurrent = filesCurrent;
					List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = processIdenticalFiles(fileContentsBefore, fileContentsCurrent, renamedFilesHint, true);
					UMLModel currentUMLModel = createModelForASTDiff(fileContentsCurrent, repositoryDirectoriesCurrent);
					UMLModel parentUMLModel = createModelForASTDiff(fileContentsBefore, repositoryDirectoriesBefore);
					UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
					ProjectASTDiffer differ = new ProjectASTDiffer(modelDiff, fileContentsBefore, fileContentsCurrent);
					diffs.add(differ.getProjectASTDiff());
				}
				catch(RefactoringMinerTimedOutException e) {
					logger.warn(String.format("Ignored revision %s due to timeout", commitId), e);
				}
				catch (Exception e) {
					logger.warn(String.format("Ignored revision %s due to error", commitId), e);
				}
			};
			f = service.submit(r);
			f.get(timeout, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			f.cancel(true);
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			service.shutdown();
		}
		return diffs.iterator().next();
	}

	@Override
	public ProjectASTDiff diffAtDirectories(Path previousPath, Path nextPath) {
		File previousFile = previousPath.toFile();
		File nextFile = nextPath.toFile();
		return diffAtDirectories(previousFile, nextFile);
	}

	@Override
	public ProjectASTDiff diffAtDirectories(File previousFile, File nextFile) {
		if(previousFile.exists() && nextFile.exists()) {
			String id = previousFile.getName() + " -> " + nextFile.getName();
			try {
				if(previousFile.isDirectory() && nextFile.isDirectory()) {
					Set<String> repositoryDirectoriesBefore = new LinkedHashSet<String>();
					Set<String> repositoryDirectoriesCurrent = new LinkedHashSet<String>();
					Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
					Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
					populateFileContents(nextFile, getJavaFilePaths(nextFile), fileContentsCurrent, repositoryDirectoriesCurrent);
					populateFileContents(previousFile, getJavaFilePaths(previousFile), fileContentsBefore, repositoryDirectoriesBefore);
					List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = processIdenticalFiles(fileContentsBefore, fileContentsCurrent, Collections.emptyMap(), true); 
					UMLModel parentUMLModel = createModelForASTDiff(fileContentsBefore, repositoryDirectoriesBefore);
					UMLModel currentUMLModel = createModelForASTDiff(fileContentsCurrent, repositoryDirectoriesCurrent);
					UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
					ProjectASTDiffer differ = new ProjectASTDiffer(modelDiff, fileContentsBefore, fileContentsCurrent);
					return differ.getProjectASTDiff();
				}
				else if(previousFile.isFile() && nextFile.isFile()) {
					String previousFileName = previousFile.getName();
					String nextFileName = nextFile.getName();
					if(previousFileName.endsWith(".java") && nextFileName.endsWith(".java")) {
						Set<String> repositoryDirectoriesBefore = new LinkedHashSet<String>();
						Set<String> repositoryDirectoriesCurrent = new LinkedHashSet<String>();
						Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
						Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
						populateFileContents(nextFile.getParentFile(), List.of(nextFileName), fileContentsCurrent, repositoryDirectoriesCurrent);
						populateFileContents(previousFile.getParentFile(), List.of(previousFileName), fileContentsBefore, repositoryDirectoriesBefore);
						List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = processIdenticalFiles(fileContentsBefore, fileContentsCurrent, Collections.emptyMap(), true); 
						UMLModel parentUMLModel = createModelForASTDiff(fileContentsBefore, repositoryDirectoriesBefore);
						UMLModel currentUMLModel = createModelForASTDiff(fileContentsCurrent, repositoryDirectoriesCurrent);
						UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
						ProjectASTDiffer differ = new ProjectASTDiffer(modelDiff, fileContentsBefore, fileContentsCurrent);
						return differ.getProjectASTDiff();
					}
				}
			}
			catch (Exception e) {
				logger.warn(String.format("Ignored revision %s due to error", id), e);
			}
		}
		return null;
	}
}
