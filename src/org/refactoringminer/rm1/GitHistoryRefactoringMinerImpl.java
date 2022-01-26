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
import java.util.ArrayList;
import java.util.Collections;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHRepositoryWrapper;
import org.kohsuke.github.GHTree;
import org.kohsuke.github.GHTreeEntry;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedIterable;
import org.refactoringminer.api.Churn;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.GitService;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.util.GitServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

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
		String commitId = currentCommit.getId().getName();
		List<String> filePathsBefore = new ArrayList<String>();
		List<String> filePathsCurrent = new ArrayList<String>();
		Map<String, String> renamedFilesHint = new HashMap<String, String>();
		gitService.fileTreeDiff(repository, currentCommit, filePathsBefore, filePathsCurrent, renamedFilesHint);
		
		Set<String> repositoryDirectoriesBefore = new LinkedHashSet<String>();
		Set<String> repositoryDirectoriesCurrent = new LinkedHashSet<String>();
		Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
		Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
		try (RevWalk walk = new RevWalk(repository)) {
			// If no java files changed, there is no refactoring. Also, if there are
			// only ADD's or only REMOVE's there is no refactoring
			if (!filePathsBefore.isEmpty() && !filePathsCurrent.isEmpty() && currentCommit.getParentCount() > 0) {
				RevCommit parentCommit = currentCommit.getParent(0);
				populateFileContents(repository, parentCommit, filePathsBefore, fileContentsBefore, repositoryDirectoriesBefore);
				populateFileContents(repository, currentCommit, filePathsCurrent, fileContentsCurrent, repositoryDirectoriesCurrent);
				List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = processIdenticalFiles(fileContentsBefore, fileContentsCurrent);
				UMLModel parentUMLModel = createModel(fileContentsBefore, repositoryDirectoriesBefore);
				UMLModel currentUMLModel = createModel(fileContentsCurrent, repositoryDirectoriesCurrent);
				
				UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
				refactoringsAtRevision = modelDiff.getRefactorings();
				refactoringsAtRevision.addAll(moveSourceFolderRefactorings);
				refactoringsAtRevision = filter(refactoringsAtRevision);
			} else {
				//logger.info(String.format("Ignored revision %s with no changes in java files", commitId));
				refactoringsAtRevision = Collections.emptyList();
			}
			handler.handle(commitId, refactoringsAtRevision);
			
			walk.dispose();
		}
		return refactoringsAtRevision;
	}

	public static List<MoveSourceFolderRefactoring> processIdenticalFiles(Map<String, String> fileContentsBefore, Map<String, String> fileContentsCurrent) throws IOException {
		Map<String, String> identicalFiles = new HashMap<String, String>();
		for(String key : fileContentsBefore.keySet()) {
			if(fileContentsCurrent.containsKey(key)) {
				String fileBefore = fileContentsBefore.get(key);
				String fileAfter = fileContentsCurrent.get(key);
				if(fileBefore.equals(fileAfter) || StringDistance.trivialCommentChange(fileBefore, fileAfter)) {
					identicalFiles.put(key, key);
				}
			}
		}
		//second iteration to find renamed/moved files with identical contents
		for(String key1 : fileContentsBefore.keySet()) {
			if(!identicalFiles.containsKey(key1)) {
				for(String key2 : fileContentsCurrent.keySet()) {
					if(!identicalFiles.containsValue(key2)) {
						String fileBefore = fileContentsBefore.get(key1);
						String fileAfter = fileContentsCurrent.get(key2);
						if(fileBefore.equals(fileAfter)) {
							identicalFiles.put(key1, key2);
						}
					}
				}
			}
		}
		//third iteration to find renamed/moved files with trivial comment changes
		for(String key1 : fileContentsBefore.keySet()) {
			if(!identicalFiles.containsKey(key1)) {
				for(String key2 : fileContentsCurrent.keySet()) {
					if(!identicalFiles.containsValue(key2)) {
						String fileBefore = fileContentsBefore.get(key1);
						String fileAfter = fileContentsCurrent.get(key2);
						if(StringDistance.trivialCommentChange(fileBefore, fileAfter)) {
							identicalFiles.put(key1, key2);
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

	public static void populateFileContents(Repository repository, RevCommit commit,
			List<String> filePaths, Map<String, String> fileContents, Set<String> repositoryDirectories) throws Exception {
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

	protected List<Refactoring> detectRefactorings(final RefactoringHandler handler, File projectFolder, String cloneURL, String currentCommitId) {
		List<Refactoring> refactoringsAtRevision = Collections.emptyList();
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
				List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = processIdenticalFiles(fileContentsBefore, fileContentsCurrent); 
				UMLModel parentUMLModel = createModel(fileContentsBefore, repositoryDirectoriesBefore);
				UMLModel currentUMLModel = createModel(fileContentsCurrent, repositoryDirectoriesCurrent);
				// Diff between currentModel e parentModel
				UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
				refactoringsAtRevision = modelDiff.getRefactorings();
				refactoringsAtRevision.addAll(moveSourceFolderRefactorings);
				refactoringsAtRevision = filter(refactoringsAtRevision);
			}
			else {
				logger.warn(String.format("Folder %s not found", currentFolder.getPath()));
			}
		} catch (Exception e) {
			logger.warn(String.format("Ignored revision %s due to error", currentCommitId), e);
			handler.handleException(currentCommitId, e);
		}
		handler.handle(currentCommitId, refactoringsAtRevision);

		return refactoringsAtRevision;
	}

	private void populateFileContents(File projectFolder, List<String> filePaths, Map<String, String> fileContents,	Set<String> repositoryDirectories) throws IOException {
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
		private List<String> filesBefore;
		private List<String> filesCurrent;
		private Map<String, String> renamedFilesHint;

		public ChangedFileInfo() {
			
		}

		public ChangedFileInfo(String parentCommitId, List<String> filesBefore,
				List<String> filesCurrent, Map<String, String> renamedFilesHint) {
			this.filesBefore = filesBefore;
			this.filesCurrent = filesCurrent;
			this.renamedFilesHint = renamedFilesHint;
			this.parentCommitId = parentCommitId;
		}

		public String getParentCommitId() {
			return parentCommitId;
		}

		public List<String> getFilesBefore() {
			return filesBefore;
		}

		public List<String> getFilesCurrent() {
			return filesCurrent;
		}

		public Map<String, String> getRenamedFilesHint() {
			return renamedFilesHint;
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
			String parentCommitId = commit.getParents().get(0).getSHA1();
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

	private GitHub connectToGitHub() {
		if(gitHub == null) {
			try {
				Properties prop = new Properties();
				InputStream input = new FileInputStream("github-oauth.properties");
				prop.load(input);
				String oAuthToken = prop.getProperty("OAuthToken");
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
		return new UMLModelASTReader(fileContents, repositoryDirectories).getUmlModel();
	}

	private static final String systemFileSeparator = Matcher.quoteReplacement(File.separator);

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
	public Churn churnAtCommit(Repository repository, String commitId, RefactoringHandler handler) {
		GitService gitService = new GitServiceImpl();
		RevWalk walk = new RevWalk(repository);
		try {
			RevCommit commit = walk.parseCommit(repository.resolve(commitId));
			if (commit.getParentCount() > 0) {
				walk.parseCommit(commit.getParent(0));
				return gitService.churn(repository, commit);
			}
			else {
				logger.warn(String.format("Ignored revision %s because it has no parent", commitId));
			}
		} catch (MissingObjectException moe) {
			logger.warn(String.format("Ignored revision %s due to missing commit", commitId), moe);
		} catch (Exception e) {
			logger.warn(String.format("Ignored revision %s due to error", commitId), e);
			handler.handleException(commitId, e);
		} finally {
			walk.close();
			walk.dispose();
		}
		return null;
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
			populateWithGitHubAPI(gitURL, currentCommitId, fileContentsBefore, fileContentsCurrent, renamedFilesHint, repositoryDirectoriesBefore, repositoryDirectoriesCurrent);
			List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = processIdenticalFiles(fileContentsBefore, fileContentsCurrent);
			UMLModel currentUMLModel = createModel(fileContentsCurrent, repositoryDirectoriesCurrent);
			UMLModel parentUMLModel = createModel(fileContentsBefore, repositoryDirectoriesBefore);
			//  Diff between currentModel e parentModel
			UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
			refactoringsAtRevision = modelDiff.getRefactorings();
			refactoringsAtRevision.addAll(moveSourceFolderRefactorings);
			refactoringsAtRevision = filter(refactoringsAtRevision);
		}
		catch(RefactoringMinerTimedOutException e) {
			logger.warn(String.format("Ignored revision %s due to timeout", currentCommitId), e);
			handler.handleException(currentCommitId, e);
		}
		catch (Exception e) {
			logger.warn(String.format("Ignored revision %s due to error", currentCommitId), e);
			handler.handleException(currentCommitId, e);
		}
		handler.handle(currentCommitId, refactoringsAtRevision);

		return refactoringsAtRevision;
	}

	private void populateWithGitHubAPI(String cloneURL, String currentCommitId,
			Map<String, String> filesBefore, Map<String, String> filesCurrent, Map<String, String> renamedFilesHint,
			Set<String> repositoryDirectoriesBefore, Set<String> repositoryDirectoriesCurrent) throws IOException, InterruptedException {
		logger.info("Processing {} {} ...", cloneURL, currentCommitId);
		GHRepository repository = getGitHubRepository(cloneURL);
		List<GHCommit.File> commitFiles = new ArrayList<>();
		GHCommit currentCommit = new GHRepositoryWrapper(repository).getCommit(currentCommitId, commitFiles);
		final String parentCommitId = currentCommit.getParents().get(0).getSHA1();
		Set<String> deletedAndRenamedFileParentDirectories = ConcurrentHashMap.newKeySet();
		ExecutorService pool = Executors.newFixedThreadPool(commitFiles.size());
		for (GHCommit.File commitFile : commitFiles) {
			String fileName = commitFile.getFileName();
			if (commitFile.getFileName().endsWith(".java")) {
				if (commitFile.getStatus().equals("modified")) {
					Runnable r = () -> {
						try {
							URL currentRawURL = commitFile.getRawUrl();
							InputStream currentRawFileInputStream = currentRawURL.openStream();
							String currentRawFile = IOUtils.toString(currentRawFileInputStream);
							String rawURLInParentCommit = currentRawURL.toString().replace(currentCommitId, parentCommitId);
							InputStream parentRawFileInputStream = new URL(rawURLInParentCommit).openStream();
							String parentRawFile = IOUtils.toString(parentRawFileInputStream);
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
							String rawFile = IOUtils.toString(rawFileInputStream);
							filesBefore.put(fileName, rawFile);
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
					Runnable r = () -> {
						try {
							String previousFilename = commitFile.getPreviousFilename();
							URL currentRawURL = commitFile.getRawUrl();
							InputStream currentRawFileInputStream = currentRawURL.openStream();
							String currentRawFile = IOUtils.toString(currentRawFileInputStream);
							String rawURLInParentCommit = currentRawURL.toString().replace(currentCommitId, parentCommitId).replace(fileName, previousFilename);
							InputStream parentRawFileInputStream = new URL(rawURLInParentCommit).openStream();
							String parentRawFile = IOUtils.toString(parentRawFileInputStream);
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
}
