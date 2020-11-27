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

import org.junit.Test;

import de.dlr.sc.virsat.model.dvlm.json.JAXBUtility;
import de.dlr.sc.virsat.model.extension.tests.model.AConceptTestCase;
import de.dlr.sc.virsat.model.extension.tests.model.TestCategoryAllProperty;

public class TestCategoryAllPropertyTest extends AConceptTestCase {

	@Test
	public void testJsonMarshalling() throws JAXBException, IOException {
		JAXBUtility jaxbUtility = new JAXBUtility(new Class[] { TestCategoryAllProperty.class });
		System.out.println(jaxbUtility);
	}
}
