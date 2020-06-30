/*******************************************************************************
 * Copyright (c) 2020 German Aerospace Center (DLR), Simulation and Software Technology, Germany.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.dlr.sc.virsat.project.editingDomain;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.ecore.resource.Resource;
import org.junit.After;
import org.junit.Before;


import org.junit.Test;

import de.dlr.sc.virsat.model.dvlm.structural.StructuralElementInstance;
import de.dlr.sc.virsat.model.dvlm.structural.StructuralFactory;
import de.dlr.sc.virsat.project.editingDomain.VirSatTransactionalEditingDomain.IResourceEventListener;
import de.dlr.sc.virsat.project.test.AProjectTestCase;

/**
 * This class is intended to test the ResourceListener as implemented in the editing domain
 * and the according notifications.
 */
public class VirSatTransactionalEditingDomainResourceEventListenerTest extends AProjectTestCase {

	@Before
	public void setUp() throws CoreException {
		super.setUp();
		addEditingDomainAndRepository();
		listener = new TestResourceEventListener();
	}

	@Override
	protected void addEditingDomainAndRepository() {
		addEditingDomainAndRepository(true);
	}
	
	@After
	@Override
	public void tearDown() throws CoreException {
		// Always remove the listener again
		VirSatTransactionalEditingDomain.removeResourceEventListener(listener);
		super.tearDown();
	}
	
	TestResourceEventListener listener;
	
	/**
	 * A listener to check if the WorkspaceResourceChangeListener of the TransactionalEditingDomain
	 * is issuing the correct notifications as expected.
	 */
	class TestResourceEventListener implements IResourceEventListener {
		
		int calledResourceEventCount = 0;
		int calledResourceEventType = 0;
		Set<Resource> calledResourceEventResources;
		
		@Override
		public void resourceEvent(Set<Resource> resources, int event) {
			calledResourceEventCount++;
			calledResourceEventType = event;
			calledResourceEventResources = new HashSet<>(resources);
		}
	};
	
	@Test
	public void testHandleExternalyAddedResource() throws CoreException {
	
		// --------------------------------------------
		// Add a new resource externally
		// Expected result is a reload on all resources
		
		editingDomain.saveAll();
		VirSatTransactionalEditingDomain.waitForFiringOfAccumulatedResourceChangeEvents();
		VirSatTransactionalEditingDomain.addResourceEventListener(listener);
		
		// Create a new resource for a SEI in the resourceSet this should issue a notification on the added resources
		StructuralElementInstance sei = StructuralFactory.eINSTANCE.createStructuralElementInstance();
		IFile seiFile = projectCommons.getStructuralElementInstanceFile(sei);
		Resource seiResource = editingDomain.getResourceSet().safeGetResource(seiFile, true);
		VirSatTransactionalEditingDomain.waitForFiringOfAccumulatedResourceChangeEvents();
		assertNotNull("Got a resource for the SEI", seiResource);
		
		assertEquals("Called listener correct amount of times", 1, listener.calledResourceEventCount);
		assertEquals("Called listener with correct type", VirSatTransactionalEditingDomain.EVENT_RELOAD, listener.calledResourceEventType);
		
		seiResource = editingDomain.getResourceSet().getStructuralElementInstanceResource(sei);
		Resource rmResource = editingDomain.getResourceSet().getRoleManagementResource();
		Resource umResource = editingDomain.getResourceSet().getUnitManagementResource();
		Resource repoResource = editingDomain.getResourceSet().getRepositoryResource();
		
		assertThat("List contains correct resources", listener.calledResourceEventResources, containsInAnyOrder(seiResource, rmResource, umResource, repoResource));
	}

	@Test
	public void testHandleExternalAndInternalModelChanges() throws IOException, CoreException, InterruptedException {
		
		// -------------------------------------------------------------
		// Here we create a SEI, and we issue a name change
		// This should only trigger a notification on the SEI's resource
		editingDomain.saveAll();
		VirSatTransactionalEditingDomain.waitForFiringOfAccumulatedResourceChangeEvents();
		
		// Create a new resource for a SEI in the resourceSet this should issue a notification on the added resources
		StructuralElementInstance sei = StructuralFactory.eINSTANCE.createStructuralElementInstance();
		IFile seiFile = projectCommons.getStructuralElementInstanceFile(sei);
		Resource seiResource = editingDomain.getResourceSet().safeGetResource(seiFile, true);
		executeAsCommand(() -> seiResource.getContents().add(sei));
		editingDomain.saveAll();
		VirSatTransactionalEditingDomain.waitForFiringOfAccumulatedResourceChangeEvents();
		VirSatTransactionalEditingDomain.addResourceEventListener(listener);

		assertEquals("SEI is still the same as in the potentially relaoded reosurce", sei, seiResource.getContents().get(0));
		
		// Now change the name which expects as change on the resource containing the SEI
		executeAsCommand(() -> sei.setName("NameNumberOne"));
		VirSatTransactionalEditingDomain.waitForFiringOfAccumulatedResourceChangeEvents();
		
		assertEquals("Called listener correct amount of times", 1, listener.calledResourceEventCount);
		assertEquals("Called listener with correct type", VirSatTransactionalEditingDomain.EVENT_CHANGED, listener.calledResourceEventType);
		assertThat("List contains correct resources", listener.calledResourceEventResources, containsInAnyOrder(seiResource));
		
		// Now saving the resource which triggers a change notification again.
		editingDomain.saveAll();
		VirSatTransactionalEditingDomain.waitForFiringOfAccumulatedResourceChangeEvents();
		assertEquals("Called listener correct amount of times", 2, listener.calledResourceEventCount);
		assertEquals("Called listener with correct type", VirSatTransactionalEditingDomain.EVENT_CHANGED, listener.calledResourceEventType);
		assertThat("List contains correct resources", listener.calledResourceEventResources, containsInAnyOrder(seiResource));
		
		
		// ----------------------------------------------
		// Now we are changing the name externally
		// This should trigger a full reload of the model
		Path seiFilePath = new File(seiFile.getRawLocation().toOSString()).toPath();
		String content = Files.readAllLines(seiFilePath, StandardCharsets.UTF_8).toString();
		content = content.replace("NameNumberOne", "NameNumberTwo");
		Files.write(seiFilePath, content.getBytes(StandardCharsets.UTF_8));
		testProject.refreshLocal(IResource.DEPTH_INFINITE, null);
		VirSatTransactionalEditingDomain.waitForFiringOfAccumulatedResourceChangeEvents();
		
		//CHECKSTYLE:OFF
		assertEquals("Called listener correct amount of times", 3, listener.calledResourceEventCount);
		assertEquals("Called listener with correct type", VirSatTransactionalEditingDomain.EVENT_RELOAD, listener.calledResourceEventType);
		Resource seiResourceReload = editingDomain.getResourceSet().getStructuralElementInstanceResource(sei);
		//CHECKSTYLE:ON
		
		Resource rmResourceReload = editingDomain.getResourceSet().getRoleManagementResource();
		Resource umResourceReload = editingDomain.getResourceSet().getUnitManagementResource();
		Resource repoResourceReload = editingDomain.getResourceSet().getRepositoryResource();
		
		assertThat("List contains correct resources", listener.calledResourceEventResources, containsInAnyOrder(seiResourceReload, rmResourceReload, umResourceReload, repoResourceReload));
	
	}
}
