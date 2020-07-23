/*******************************************************************************
 * Copyright (c) 2020 German Aerospace Center (DLR), Simulation and Software Technology, Germany.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.dlr.sc.virsat.team.git;

import java.io.File;
import java.util.Collection;
import java.util.Set;

import org.eclipse.core.internal.resources.Resource;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.egit.core.EclipseGitProgressTransformer;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CheckoutCommand.Stage;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.transport.CredentialsProvider;
import de.dlr.sc.virsat.team.IVirSatVersionControlBackend;

@SuppressWarnings("restriction")
public class VirSatGitVersionControlBackend implements IVirSatVersionControlBackend {

	private CredentialsProvider credentialsProvider;
	public static final String BACKEND_REPOSITORY_COMMIT_PULL_MESSAGE = "Backend Local Commit Before Pull: ";
	public static final String BACKEND_REPOSITORY_MERGE_COMMIT_MESSAGE = "Backend Merge Commit";
	
	public VirSatGitVersionControlBackend(CredentialsProvider credentialsProvider) {
		this.credentialsProvider = credentialsProvider;
	}
	
	public static final int PROGRESS_INDEX_COMMIT_STEPS = 2;
	public static final int PROGRESS_INDEX_UPDATE_STEPS = 2;
	public static final int PROGRESS_INDEX_CHECKIN_STEPS = 4;
	public static final int PROGRESS_INDEX_CHECKOUT_STEPS = 4;
	public static final int PROGRESS_INDEX_DO_COMMIT_STEPS = 2;
	
	public static final int GIT_REMOTE_TIMEOUT = 30;
	
	@Override
	public void commit(IProject project, String message, IProgressMonitor monitor) throws Exception {
		SubMonitor pushAndCommitMonitor = SubMonitor.convert(monitor, "Virtual Satellite git push and commit", PROGRESS_INDEX_COMMIT_STEPS);
		
		// Get the repository mapped to the project
		Repository gitRepository = RepositoryMapping.getMapping(project).getRepository();
		// Stage and commit all changes
		doCommit(gitRepository, message, pushAndCommitMonitor.split(1));

		ProgressMonitor gitPushMonitor = new EclipseGitProgressTransformer(pushAndCommitMonitor.split(1));
		
		// Push the commit
		Git.wrap(gitRepository).push()
			.setCredentialsProvider(credentialsProvider)
			.setProgressMonitor(gitPushMonitor)
			.setTimeout(GIT_REMOTE_TIMEOUT)
			.call();
		
		Git.wrap(gitRepository).close();
	}

	/**
	 * Method to add all unstaged files and commit them to the local repository
	 * @param gitRepository the repository on which to perform the commit
	 * @param message a message to be used for the commit
	 * @param pushAndCommitMonitor A SubMonitor for progress Reporting. 
	 * @throws Exception 
	 */
	protected void doCommit(Repository gitRepository, String message, SubMonitor monitor) throws Exception {
		SubMonitor pushAndCommitMonitor = SubMonitor.convert(monitor, "Virtual Satellite git commit", PROGRESS_INDEX_DO_COMMIT_STEPS);
		
		pushAndCommitMonitor.split(1).subTask("Adding new files to index");
		Git.wrap(gitRepository).add()
			.addFilepattern(".")
			.call();		
		
		if (Git.wrap(gitRepository).status().call().hasUncommittedChanges()) {
			pushAndCommitMonitor.split(1).subTask("Commiting files");
			Git.wrap(gitRepository).commit()
				.setAll(true)
				.setMessage(message)
				.call();
		}
	}

	@Override
	public void update(IProject project,  IProgressMonitor monitor) throws Exception {
		SubMonitor commitAndPullMonitor = SubMonitor.convert(monitor, "Virtual Satellite git commit and pull", PROGRESS_INDEX_UPDATE_STEPS);
		
		// Get the repository mapped to the project
		Repository gitRepository = RepositoryMapping.getMapping(project).getRepository();
		// Stage and commit all changes
		doCommit(gitRepository, BACKEND_REPOSITORY_COMMIT_PULL_MESSAGE  + project.getName(), commitAndPullMonitor.split(1));

		commitAndPullMonitor.split(1).subTask("Check if remotes exist");
		
		// Get the remotes for the repository
		String remoteUrl = gitRepository.getConfig().getString("remote", "origin", "url");
		Collection<Ref> refs = Git.lsRemoteRepository()
			.setRemote(remoteUrl)
			.call();
		
		// Only perform a pull of the remote exists
		if (!refs.isEmpty()) {
			
			// Pull from origin and apply the Recursive Merge Strategy. It is the git standard merge strategy.
			// In case there has been another commit on the remote in between, the pull will try to resolve it
			// and store it as a new commit locally.
			ProgressMonitor gitPullMonitor = new EclipseGitProgressTransformer(commitAndPullMonitor.setWorkRemaining(1).split(1));
			Git.wrap(gitRepository).pull()
				.setCredentialsProvider(credentialsProvider)
				.setProgressMonitor(gitPullMonitor)
				.setTimeout(GIT_REMOTE_TIMEOUT)
				.setStrategy(MergeStrategy.RECURSIVE)
				.call();
			
			// In case not all files could be merged they need to be fixed.
			// Therefore lets see if files are in a conflicting state within the repository
			Set<String> conflictingFiles = Git.wrap(gitRepository).status().call().getConflicting();
			
			// If there are any continue from here with resolving the conflicts.
			if (!conflictingFiles.isEmpty()) {
				commitAndPullMonitor.setWorkRemaining(2);
				ProgressMonitor gitCheckoutMonitor = new EclipseGitProgressTransformer(commitAndPullMonitor.split(1));
				
				// Prepare a checkout command to get THEIRS on all conflicting files.
				// This basically means that in case of concept the files in the repository
				// are regarded as being correct. This happens on the cost of loosing local changes.
				CheckoutCommand checkoutCommand = Git.wrap(gitRepository).checkout();
				checkoutCommand.setProgressMonitor(gitCheckoutMonitor);
				
				for (String conflictingFile : conflictingFiles) {
					checkoutCommand.addPath(conflictingFile);
				}
				checkoutCommand.setStage(Stage.THEIRS);
				checkoutCommand.call();
				
				// Finally close the still open merge commit
				doCommit(gitRepository, BACKEND_REPOSITORY_MERGE_COMMIT_MESSAGE, commitAndPullMonitor.split(1));
			}
		} 
		 
		Git.wrap(gitRepository).close();
		
		project.refreshLocal(Resource.DEPTH_INFINITE, commitAndPullMonitor.split(1));
	}

	@Override
	public IProject checkout(IProjectDescription projectDescription, File pathLocalRepository, String remoteUri, IProgressMonitor monitor) throws Exception {
		SubMonitor checkoutMonitor = SubMonitor.convert(monitor, "Virtual Satellite git clone", PROGRESS_INDEX_CHECKOUT_STEPS);
		
		checkoutMonitor.split(1).subTask("Cloning remote Repository");
		// Clone into the location specified by the project description
		Git.cloneRepository()
			.setCredentialsProvider(credentialsProvider)
			.setURI(remoteUri)
			.setDirectory(pathLocalRepository)
			.call();
		
		String projectName = projectDescription.getName();
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		project.create(projectDescription, checkoutMonitor.split(1));
		project.open(checkoutMonitor.split(1));
		
		project.refreshLocal(Resource.DEPTH_INFINITE, checkoutMonitor.split(1));
		
		connect(project, pathLocalRepository, checkoutMonitor.split(1));
		
		RepositoryMapping.getMapping(project).getRepository().close();
		
		return project;
	}

	public static final String INITIAL_COMMIT_MESSAGE = "Initial Commit on Checkin";
	
	@Override
	public void checkin(IProject project, File pathRepoLocal, String remoteUri, IProgressMonitor monitor) throws Exception {
		SubMonitor checkInMonitor = SubMonitor.convert(monitor, "Virtual Satellite git init", PROGRESS_INDEX_CHECKIN_STEPS);
		
		checkInMonitor.split(1).subTask("Cloning remote Repository");
		// Clone into the location specified by the project description
		Repository repo = Git.cloneRepository()
			.setCredentialsProvider(credentialsProvider)
			.setURI(remoteUri)
			.setDirectory(pathRepoLocal)
			.call()
			.getRepository();
		
		// Changing the project to the target location lets eclipse know where we want to move the project
		IProjectDescription projectDescription = ResourcesPlugin.getWorkspace().newProjectDescription(project.getName());
		projectDescription.setLocationURI(pathRepoLocal.toURI().resolve(project.getName()));
		project.move(projectDescription, true, checkInMonitor.split(1));
		
		// Stage and commit all changes
		doCommit(repo, INITIAL_COMMIT_MESSAGE, checkInMonitor.split(1));
		
		connect(project, pathRepoLocal, checkInMonitor.split(1));

		RepositoryMapping.getMapping(project).getRepository().close();
	}

	/**
	 * Internal method to connect a project with a path where the .git file should reside.
	 * This is needed for correct repository to project mapping
	 * @param project the project to be connected
	 * @param pathRepoLocal the path in which the .git file should reside
	 * @param monitor a monitor for watching the progress
	 * @throws Exception May throw an exception in case connection fails
	 */
	protected void connect(IProject project, File pathRepoLocal, IProgressMonitor monitor) throws Exception {
		SubMonitor.convert(monitor, "Connecting Project", 1);
		// Connect Eclipse to the created (existing) Git repository
		// By associating the .git file of the new repository explicit with the project
		File pathRepoLocalGit = new File(pathRepoLocal.toURI().resolve(".git"));
		ConnectProviderOperation connectOperation = new ConnectProviderOperation(project, pathRepoLocalGit);
		connectOperation.execute(null);
	}
	
}
