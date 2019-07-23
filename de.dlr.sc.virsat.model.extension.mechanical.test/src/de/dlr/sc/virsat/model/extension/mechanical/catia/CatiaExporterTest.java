/*******************************************************************************
 * Copyright (c) 2008-2019 German Aerospace Center (DLR), Simulation and Software Technology, Germany.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.dlr.sc.virsat.model.extension.mechanical.catia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

import de.dlr.sc.virsat.concept.unittest.util.test.AConceptTestCase;
import de.dlr.sc.virsat.model.dvlm.concepts.Concept;
import de.dlr.sc.virsat.model.extension.ps.model.ConfigurationTree;
import de.dlr.sc.virsat.model.extension.ps.model.ElementDefinition;
import de.dlr.sc.virsat.model.extension.visualisation.model.Visualisation;

/**
 * This class tests the Catia exporter.
 *
 */

public class CatiaExporterTest extends AConceptTestCase {

	private Concept conceptPS;
	private Concept conceptVis;
	
	@Before
	public void setUp() {
		conceptPS = loadConceptFromPlugin("de.dlr.sc.virsat.model.extension.ps");
		conceptVis = loadConceptFromPlugin("de.dlr.sc.virsat.model.extension.visualisation");
	}
	
	@Test
	public void testTransformConfigurationTree() {
		ConfigurationTree ct = new ConfigurationTree(conceptPS);
		
		CatiaExporter catiaExporter = new CatiaExporter();
		JSONObject jsonRoot = catiaExporter.transform(ct);
		
		JSONArray jsonParts = (JSONArray) jsonRoot.get(CatiaProperties.PARTS);		
		JSONObject jsonProductRoot = (JSONObject) jsonRoot.get(CatiaProperties.PRODUCTS);
		
		assertTrue(jsonParts.isEmpty());
		assertNull(jsonProductRoot);
	}
	
	@Test
	public void testTransformParts() {
		ElementDefinition ed = new ElementDefinition(conceptPS);
		List<ElementDefinition> eds = new ArrayList<>();
		eds.add(ed);
		
		CatiaExporter catiaExporter = new CatiaExporter();
		JSONArray jsonParts = catiaExporter.transformParts(eds);
		
		assertTrue(jsonParts.isEmpty());
		
		Visualisation visualisation = new Visualisation(conceptVis);
		ed.add(visualisation);
		
		jsonParts = catiaExporter.transformParts(eds);
		
		assertEquals(1, jsonParts.size());
		
		JSONObject jsonPart = (JSONObject) jsonParts.get(0);
		
		assertEquals(ed.getName(), jsonPart.get(CatiaProperties.NAME));
		assertEquals(ed.getUuid(), jsonPart.get(CatiaProperties.UUID));
	}
	
	@Test
	public void testTransformElement() {
		ElementDefinition ed = new ElementDefinition(conceptPS);
		
		CatiaExporter catiaExporter = new CatiaExporter();
		JSONObject jsonElement = catiaExporter.transformElement(ed);
		
		assertEquals(ed.getName(), jsonElement.get(CatiaProperties.NAME));
		assertEquals(ed.getUuid(), jsonElement.get(CatiaProperties.UUID));
	}
}
