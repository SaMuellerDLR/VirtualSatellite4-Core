/*******************************************************************************
 * Copyright (c) 2020 German Aerospace Center (DLR), Simulation and Software Technology, Germany.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.dlr.sc.virsat.model.extension.tests.model.json;

import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.eclipse.emf.common.util.URI;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.dlr.sc.virsat.model.dvlm.concepts.Concept;
import de.dlr.sc.virsat.model.dvlm.json.JAXBUtility;
import de.dlr.sc.virsat.model.extension.tests.model.AConceptTestCase;
import de.dlr.sc.virsat.model.extension.tests.model.TestCategoryAllProperty;

public class TestCategoryAllPropertyTest extends AConceptTestCase {

	private TestCategoryAllProperty tcAllProperty;
	private JAXBUtility jaxbUtility;
	private Concept concept;

	//private static final String RESOURCE_WITH_DEFAULTS = "/resources/json/TestCategoryAllProperty_Marshaling_Defaults.json";
	private static final String RESOURCE_WITH_VALUES = "/resources/json/TestCategoryAllProperty_Marshaling.json";

	private static final boolean TEST_BOOL = true;
	private static final int TEST_INT = 1;
	private static final double TEST_FLOAT = 0.0;
	private static final String TEST_STRING = "this is a test";
	private static final String TEST_ENUM = "HIGH";
	private static final String TEST_RESOURCE = "resources/file[1].xls";
	//private static final String TEST_RESOURCE_STRING = "/" + TEST_RESOURCE;

	//private static final double EPSILON = 0.000001;

	@Before
	public void setup() throws JAXBException {
		jaxbUtility = new JAXBUtility(new Class[] { TestCategoryAllProperty.class });

		prepareEditingDomain();
		concept = loadConceptFromPlugin();

		tcAllProperty = new TestCategoryAllProperty(concept);
		JsonTestHelper.setTestCategoryAllPropertyUuids(tcAllProperty);
		JsonTestHelper.createRepositoryWithUnitManagement(concept);
	}
	
	@After
	public void tearDown() throws InterruptedException {
		jaxbUtility = null;
		Runtime.getRuntime().gc();
		final int sleeptime = 1000;
		Thread.sleep(sleeptime);
	}

	/**
	 * Set the new values
	 */
	public void initProperties() {
		tcAllProperty.setTestInt(TEST_INT);
		tcAllProperty.setTestFloat(TEST_FLOAT);
		tcAllProperty.setTestEnum(TEST_ENUM);
		tcAllProperty.setTestResource(URI.createPlatformPluginURI(TEST_RESOURCE, false));
		tcAllProperty.setTestString(TEST_STRING);
		tcAllProperty.setTestBool(TEST_BOOL);
	}

	@Test
	public void testJsonMarshalling() throws JAXBException, IOException {
		initProperties();

		JsonTestHelper.assertMarshall(jaxbUtility, RESOURCE_WITH_VALUES, tcAllProperty);
	}
}
