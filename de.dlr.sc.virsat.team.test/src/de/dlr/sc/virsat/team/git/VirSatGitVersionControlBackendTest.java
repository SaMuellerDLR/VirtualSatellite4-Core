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
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Before;

import de.dlr.sc.virsat.team.Activator;
import de.dlr.sc.virsat.team.test.AVirSatVersionControlBackendTest;

public class VirSatGitVersionControlBackendTest extends AVirSatVersionControlBackendTest {
	
	@Before
	public void setUp() throws CoreException {
		try {
			pathRepoRemote = Files.createTempDirectory("VirtualSatelliteGitRemote_");
			File fileGitRemoteRepo = pathRepoRemote.toFile();
			fileGitRemoteRepo.mkdir();
			Git.init().setDirectory(fileGitRemoteRepo).setBare(true).call();

			URI uriToRemoteRepoPath = pathRepoRemote.toUri();
			pathRepoLocal1 = Files.createTempDirectory("VirtualSatelliteGitLocal1_");
			File filePathToProject = pathRepoLocal1.toFile();
			Git.cloneRepository().setURI(uriToRemoteRepoPath.toString()).setDirectory(filePathToProject).call();

			uriToRemoteRepoPath = pathRepoRemote.toUri();
			pathRepoLocal2 = Files.createTempDirectory("VirtualSatelliteGitLocal2_");
			filePathToProject = pathRepoLocal2.toFile();
			Git.cloneRepository().setURI(uriToRemoteRepoPath.toString()).setDirectory(filePathToProject).call();
		} catch (IOException | GitAPIException e) {
			Activator.getDefault().getLog().log(new Status(Status.ERROR, Activator.getPluginId(),
					"Error during temp remote directory creation", e));
		}

		super.setUp();

		backend = new VirSatGitVersionControlBackend(null);
	}
}
