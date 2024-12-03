package org.refactoringminer.perforce;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.refactoringminer.api.PerforceHistoryRefactoringMiner;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.astDiff.matchers.ProjectASTDiffer;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;

import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.MoveSourceFolderRefactoring;
import gr.uom.java.xmi.diff.UMLModelDiff;

/**
 * Main class to provide operations to support Perforce in RefactoringMiner
 * @author Davood Mazinanian
 */
public class PerforceHistoryRefactoringMinerImpl implements PerforceHistoryRefactoringMiner {
	private final static Logger logger = LoggerFactory.getLogger(PerforceHistoryRefactoringMinerImpl.class);
	private Set<RefactoringType> refactoringTypesToConsider = null;
	private IOptionsServer perforceServer;

	public PerforceHistoryRefactoringMinerImpl() {
		this.setRefactoringTypesToConsider(RefactoringType.ALL);
	}

	public void setRefactoringTypesToConsider(RefactoringType ... types) {
		this.refactoringTypesToConsider = new HashSet<RefactoringType>();
		for (RefactoringType type : types) {
			this.refactoringTypesToConsider.add(type);
		}
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
	public IOptionsServer connectToPerforceServer(String serverUrl, String userName, String password) throws Exception {
		PerforceConnectionDetails connection = PerforceUtils.getPerforceCredentialsObject(serverUrl, userName, password);
        // Get perforce server object
        IOptionsServer server = ServerFactory.getOptionsServer(connection.perforceServerUri(), null, null);
        if (server == null) {
        	logger.warn("Error connecting to Perforce server");
            throw new ConnectionException("Error connecting to Perforce server");
        }
        server.connect();
        // Log in to the server
        server.setUserName(connection.userName());
        server.login(connection.password());
        logger.info("Connected to Perforce server with credentials");
        return server;
	}

	@Override
	public ProjectASTDiff diffAtChangeList(String serverUrl, String userName, String password, int changeListNumber) {
		try {
			// Connect to Perforce server.
	        this.perforceServer = connectToPerforceServer(serverUrl, userName, password);
	        ASTDiffInput astDiffInput = populateContentsForPerforceCl(changeListNumber);
	        GitHistoryRefactoringMinerImpl.processIdenticalFiles(
	        		astDiffInput.getFileContentsBefore(), 
	        		astDiffInput.getFileContentsAfter(), 
	        		Collections.emptyMap(), true);
	        UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModelForASTDiff(
	                astDiffInput.getFileContentsBefore(),
	                astDiffInput.getDirectoriesBefore());
	        UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModelForASTDiff(
	                astDiffInput.getFileContentsAfter(),
	                astDiffInput.getDirectoriesAfter());
	        UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
	        ProjectASTDiffer differ = new ProjectASTDiffer(modelDiff, astDiffInput.getFileContentsBefore(), astDiffInput.getFileContentsAfter());
	        return differ.getProjectASTDiff();
		} catch (RefactoringMinerTimedOutException e) {
			logger.warn(String.format("Ignored changeList %s due to timeout", changeListNumber), e);
		} catch (Exception e) {
			logger.warn(String.format("Ignored changeList %s due to error", changeListNumber), e);
		}
		return null;
    }

    private ASTDiffInput populateContentsForPerforceCl(int clNumber) throws Exception {
        // Create changelist object for changelist
        IChangelist changelist = perforceServer.getChangelist(clNumber);
        if (changelist == null) {
        	logger.warn("ChangeList %d not found".formatted(clNumber));
            throw new Exception("null returned when getting the changelist %d".formatted(clNumber));
        }
        ASTDiffInput astDiffInput = new ASTDiffInput();
        /*
         * Get the files from changelist. Force refreshing the files from the server instead of getting the local copy
         * if it exists.
         */
        boolean refreshFilesFromServer = true;
        List<IFileSpec> fileList = changelist.getFiles(refreshFilesFromServer);
        // Iterate over each file from the changelist
        for (IFileSpec fileSpec : fileList) {
            // Only VALID status should be considered. See docs for FileSpecOpStatus.
            if (fileSpec.getOpStatus() != FileSpecOpStatus.VALID) {
                // Just logging and moving to the next file
            	logger.warn(fileSpec.getStatusMessage());
                continue;
            }
            // Path to the file in the Depot.
            String depotPath = fileSpec.getDepotPathString();
            /*
             * Remove the heading double slashes (the start of the Depot path in Perforce). This is used as the unique
             * key in the maps used to generate the ASTDiff. This is also important for the ASTDiff to work properly.
             */
            String sanitizedPath = depotPath.replace("//", "");
            // Only consider Java files - of course.
            if (!isJavaFile(depotPath)) {
                continue;
            }
            // Get the contents of the file as String - we use the APIs of AST Diff which work with file contents.
            String fileContents = PerforceUtils.getFileContents(fileSpec);
            switch (fileSpec.getAction()) {
                case DELETE:
                    // File was deleted in this CL
                    astDiffInput.addFileContentsBefore(sanitizedPath, fileContents);
                    break;
                case ADD:
                    // File was added in this CL
                    // Falls through
                case BRANCH:
                    // File was added from another stream in this CL (e.g., a merge operation)
                    astDiffInput.addFileContentsAfter(sanitizedPath, fileContents);
                    break;
                case EDIT:
                    // File was edited in the current stream in this CL
                    // Falls through
                case INTEGRATE:
                    // File is being integrated from another stream in this CL (an edit from another stream)
                    // We need to get the current and previous version of the file for EDIT and INTEGRATE
                    int fileRevision = fileSpec.getEndRevision();
                    /*
                     * If the file is not the first version (it shouldn't be if we are in this branch of the code), try
                     * to get the previous version of it. Otherwise, consider the file as added.
                     */
                    if (fileRevision > 1) {
                        int previousRevisionNumber = fileRevision - 1;
                        String previousRevisionContents = PerforceUtils.getFileContentsAtRevision(depotPath,
                                previousRevisionNumber,
                                perforceServer);
                        astDiffInput.addFileContentsBefore(sanitizedPath, previousRevisionContents);
                    }
                    // Put the current version's contents either case
                    astDiffInput.addFileContentsAfter(sanitizedPath, fileContents);
                    break;
            }
        }
        // Populate directory info the way UML Diff needs it
        populateDirectories(astDiffInput);
        return astDiffInput;
    }

    private static void populateDirectories(ASTDiffInput astDiffInput) {
        populateDirectories(astDiffInput.getFileContentsBefore().keySet(), astDiffInput.getDirectoriesBefore());
        populateDirectories(astDiffInput.getFileContentsAfter().keySet(), astDiffInput.getDirectoriesAfter());
    }

    /**
     * Populate depotDirectories given the list of paths of the files.
     */
    private static void populateDirectories(Set<String> filePaths, Set<String> depotDirectories) {
        for (String directory : filePaths) {
            while (directory.contains("/")) {
                directory = directory.substring(0, directory.lastIndexOf("/"));
                depotDirectories.add(directory);
            }
        }
    }

    private static boolean isJavaFile(String path) {
        return path.toLowerCase().endsWith(".java");
    }

	@Override
	public void detectAtChangeList(String serverUrl, String userName, String password, int changeListNumber,
			RefactoringHandler handler) {
		try {
			// Connect to Perforce server.
	        this.perforceServer = connectToPerforceServer(serverUrl, userName, password);
	        ASTDiffInput astDiffInput = populateContentsForPerforceCl(changeListNumber);
	        List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = GitHistoryRefactoringMinerImpl.processIdenticalFiles(
	        		astDiffInput.getFileContentsBefore(), 
	        		astDiffInput.getFileContentsAfter(), 
	        		Collections.emptyMap(), false);
	        UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(
	                astDiffInput.getFileContentsBefore(),
	                astDiffInput.getDirectoriesBefore());
	        UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(
	                astDiffInput.getFileContentsAfter(),
	                astDiffInput.getDirectoriesAfter());
	        UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
	        List<Refactoring> refactoringsAtRevision = modelDiff.getRefactorings();
			refactoringsAtRevision.addAll(moveSourceFolderRefactorings);
			refactoringsAtRevision = filter(refactoringsAtRevision);
			handler.handle(String.valueOf(changeListNumber), refactoringsAtRevision);
			handler.handleModelDiff(String.valueOf(changeListNumber), refactoringsAtRevision, modelDiff);
		} catch (RefactoringMinerTimedOutException e) {
			logger.warn(String.format("Ignored changeList %s due to timeout", changeListNumber), e);
		} catch (Exception e) {
			logger.warn(String.format("Ignored changeList %s due to error", changeListNumber), e);
			handler.handleException(String.valueOf(changeListNumber), e);
		}
	}

	@Override
	public void detectAtChangeList(String serverUrl, String userName, String password, int changeListNumber,
			RefactoringHandler handler, int timeout) {
		ExecutorService service = Executors.newSingleThreadExecutor();
		Future<?> f = null;
		try {
			Runnable r = () -> detectAtChangeList(serverUrl, userName, password, changeListNumber, handler);
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
}
