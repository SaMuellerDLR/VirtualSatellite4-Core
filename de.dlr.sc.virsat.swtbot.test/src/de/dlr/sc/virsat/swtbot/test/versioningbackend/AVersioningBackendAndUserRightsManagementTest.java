/*******************************************************************************
 * Copyright (c) 2020 German Aerospace Center (DLR), Simulation and Software Technology, Germany.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.dlr.sc.virsat.swtbot.test.versioningbackend;

import static org.junit.Assert.fail;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Before;
import org.junit.Test;

import de.dlr.sc.virsat.swtbot.test.ASwtBotTestCase;

public abstract class AVersioningBackendAndUserRightsManagementTest extends ASwtBotTestCase {

	@Before
	public void before() throws Exception {
		super.before();
		
		// Now prepare the versioning-backends
		setUpVersioningBackend();
		
		// Switch back to virtual Satellite perspective
		openCorePerspective();

		// Share the test project with the backend
		shareTestProjectWithVersioningBackend();
	}
	
	@Override
	public void tearDown() throws CoreException, IOException {
		super.tearDown();
		
		// Remove repositories after the test case execution
		tearDownVersioningBackend();
	}
	
	/**
	 * Abstract method to be implemented to setup the versioning-backend for the test cases
	 * @throws IOException
	 */
	protected abstract void setUpVersioningBackend() throws IOException;
	
	/**
	 * Abstract method to be implemented to clean up and tear down the versioning-backend
	 * @throws IOException
	 */
	protected abstract void tearDownVersioningBackend() throws IOException;
	
	/**
	 * Abstract method to be implemented to share the standard test case project with the versioning-backend
	 */
	protected abstract void shareTestProjectWithVersioningBackend();
	
	/**
	 * Method to open the share project dialog
	 */
	protected void openShareProjectDialog() {
		SWTBotTreeItem projectNode = bot.tree().getTreeItem("SWTBotTestProject").select();
		projectNode.contextMenu("Team").menu("Share Project...").click();
	}
	
	public static final String SWTBOT_COMMIT_MESSAGE = "SwtBotTest - Commit Message!";
	
	@Test
	public void testCommitProject() {
	
		openVirtualSatelliteNavigatorView();
		
		// Use the context menu to commit the project and add a message
		// into the commit dialog. The message will be used for testing 
		// later, if the commit has arrived as exepected.
		buildCounter.executeInterlocked(() -> {
			SWTBotTreeItem projectNode = bot.tree().getTreeItem("SWTBotTestProject");
			projectNode.select();
			projectNode.contextMenu("Commit Project to Repository").click();
			bot.text().setText(SWTBOT_COMMIT_MESSAGE);
			bot.button("OK").click();
		});
		
		// Call backend specific assertion of commit results;
		testCommitProjectAssert();
	}	
	
	/**
	 * method to be implemented for backend specific assert code
	 * of the basic Commit test.
	 */
	protected abstract void testCommitProjectAssert();

	@Test
	public void testUpdateProject() {
		fail("Not yet implemented");
	}
}
