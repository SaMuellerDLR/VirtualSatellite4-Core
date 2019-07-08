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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.transaction.RecordingCommand;
import org.junit.Before;
import org.junit.Test;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;

import de.dlr.sc.virsat.concept.unittest.util.test.AConceptProjectTestCase;
import de.dlr.sc.virsat.model.dvlm.concepts.Concept;
import de.dlr.sc.virsat.model.dvlm.inheritance.InheritanceCopier;
import de.dlr.sc.virsat.model.dvlm.structural.StructuralElementInstance;
import de.dlr.sc.virsat.model.extension.ps.model.AssemblyTree;
import de.dlr.sc.virsat.model.extension.ps.model.ConfigurationTree;
import de.dlr.sc.virsat.model.extension.ps.model.ElementConfiguration;
import de.dlr.sc.virsat.model.extension.ps.model.ElementDefinition;
import de.dlr.sc.virsat.model.extension.ps.model.ElementOccurence;
import de.dlr.sc.virsat.model.extension.ps.model.ProductTree;
import de.dlr.sc.virsat.model.extension.ps.model.ProductTreeDomain;
import de.dlr.sc.virsat.model.extension.visualisation.model.Visualisation;
import de.dlr.sc.virsat.project.structure.VirSatProjectCommons;

/**
 * The CATIA importer test class
 *
 */
public class CatiaImporterTest extends AConceptProjectTestCase {

	private Concept conceptPS;
	private Concept conceptVis;

	private ProductTree productTree;
	private ProductTreeDomain domainAOCS;
	private ElementDefinition elementReactionWheelDefinition;

	private ConfigurationTree configurationTree;
	private ElementConfiguration subSystemAOCS;
	private ElementConfiguration elementConfigurationReactionWheel1;
	private ElementConfiguration elementConfigurationReactionWheel2;

	private AssemblyTree assemblyTree;
	private ElementOccurence aocsSubSystemOccurence;
	private ElementOccurence reactionWheelOccurence1;
	private ElementOccurence reactionWheelOccurence2;

	// Visualisation elements
	private Visualisation reactionWheelVisDefinition;

	private static final int TEST_POS_X_PRODUCT = 1;
	private static final int TEST_POS_Y_PRODUCT = 2;
	private static final int TEST_POS_Z_PRODUCT = 3;

	private static final int TEST_ROT_X_PRODUCT = 4;
	private static final int TEST_ROT_Y_PRODUCT = 5;
	private static final int TEST_ROT_Z_PRODUCT = 6;

	private static final String TEST_SHAPE_PRODUCT = Visualisation.SHAPE_BOX_NAME;

	private static final int TEST_SIZE_X_PART = 7;
	private static final int TEST_SIZE_Y_PART = 8;
	private static final int TEST_SIZE_Z_PART = 9;
	private static final int TEST_RADIUS_PART = 10;

	private static final long TEST_COLOR_PART = 30;
	private static final String TEST_SHAPE_PART = Visualisation.SHAPE_BOX_NAME;

	private static final double EPSILON = 0.001;

	private static final String STL_TEST_FILENAME = "SomeGeometry.stl";

	@Before
	public void setUp() throws CoreException {
		super.setUp();
		conceptPS = loadConceptFromPlugin("de.dlr.sc.virsat.model.extension.ps");
		conceptVis = loadConceptFromPlugin("de.dlr.sc.virsat.model.extension.visualisation");

		addEditingDomainAndRepository();
		editingDomain.getVirSatCommandStack().execute(new RecordingCommand(editingDomain) {

			@Override
			protected void doExecute() {
				repository.getActiveConcepts().add(conceptPS);
				repository.getActiveConcepts().add(conceptVis);
			}
		});
		createTestTreeScenario();
	}


	@Test
	public void testTransform() {

		JsonObject rootObject = createMappedJsonObjectWithProductAndConfiguration();

		// Do the import
		CatiaImporter importer = new CatiaImporter();
		Map<String, StructuralElementInstance> mapping = importer.mapJsonUuidToSEI(rootObject, configurationTree);
		Command importCommand = importer.transform(editingDomain, rootObject, mapping);
		editingDomain.getVirSatCommandStack().execute(importCommand);

		Visualisation visualisationProduct = elementConfigurationReactionWheel1.getFirst(Visualisation.class);
		Visualisation visualisationPart = elementReactionWheelDefinition.getFirst(Visualisation.class);

		assertEquals("Check if product values are imported",
				visualisationProduct.getPositionXBean().getValueToBaseUnit(), TEST_POS_X_PRODUCT, EPSILON);
		assertEquals("Check if product values are imported",
				visualisationProduct.getPositionYBean().getValueToBaseUnit(), TEST_POS_Y_PRODUCT, EPSILON);
		assertEquals("Check if product values are imported",
				visualisationProduct.getPositionZBean().getValueToBaseUnit(), TEST_POS_Z_PRODUCT, EPSILON);

		assertEquals("Check if product values are imported",
				visualisationProduct.getRotationXBean().getValueToBaseUnit(), TEST_ROT_X_PRODUCT, EPSILON);
		assertEquals("Check if product values are imported",
				visualisationProduct.getRotationYBean().getValueToBaseUnit(), TEST_ROT_Y_PRODUCT, EPSILON);
		assertEquals("Check if product values are imported",
				visualisationProduct.getRotationZBean().getValueToBaseUnit(), TEST_ROT_Z_PRODUCT, EPSILON);

		assertEquals("Check if product values are imported", visualisationProduct.getShape(), TEST_SHAPE_PRODUCT);

		assertEquals("Check if part values are imported", visualisationPart.getSizeXBean().getValueToBaseUnit(),
				TEST_SIZE_X_PART, EPSILON);
		assertEquals("Check if part values are imported", visualisationPart.getSizeYBean().getValueToBaseUnit(),
				TEST_SIZE_Y_PART, EPSILON);
		assertEquals("Check if part values are imported", visualisationPart.getSizeZBean().getValueToBaseUnit(),
				TEST_SIZE_Z_PART, EPSILON);
		assertEquals("Check if part values are imported", visualisationPart.getRadiusBean().getValueToBaseUnit(),
				TEST_RADIUS_PART, EPSILON);
		assertEquals("Check if part values are imported", visualisationPart.getShape(), TEST_SHAPE_PART);
		assertEquals("Check if part values are imported", visualisationPart.getColor(), TEST_COLOR_PART);

	}

	@Test
	public void testTransformWithIncompleteJSON() {

		JsonObject rootObject = createMappedJsonObjectWithProductAndConfiguration();

		JsonObject rootProduct = rootObject.getMap(CatiaProperties.PRODUCTS);
		JsonArray childProducts = rootProduct.getCollection(CatiaProperties.PRODUCT_CHILDREN);
		JsonObject firstChild = childProducts.getMap(0);
		firstChild.remove(CatiaProperties.PRODUCT_POS_X.getKey());

		// Do the import
		CatiaImporter importer = new CatiaImporter();
		Map<String, StructuralElementInstance> mapping = importer.mapJsonUuidToSEI(rootObject, configurationTree);
		Command importCommand = importer.transform(editingDomain, rootObject, mapping);
		editingDomain.getVirSatCommandStack().execute(importCommand);

		assertFalse("The command should not be exectuable for incomplete JSONs", importCommand.canExecute());

	}
	
	@Test
	public void testTransformWithIncompleteMapping() {

		JsonObject rootObject = createMappedJsonObjectWithProductAndConfiguration();
		
		// Create unmapped element
		JsonObject rootProduct = rootObject.getMap(CatiaProperties.PRODUCTS);
		JsonArray childProducts = rootProduct.getCollection(CatiaProperties.PRODUCT_CHILDREN);
		JsonObject newUnmappedJsonProduct = new JsonObject();
		newUnmappedJsonProduct.put(CatiaProperties.UUID.getKey(), UUID.randomUUID().toString());
		newUnmappedJsonProduct.put(CatiaProperties.PRODUCT_POS_X.getKey(), TEST_POS_X_PRODUCT);
		newUnmappedJsonProduct.put(CatiaProperties.PRODUCT_POS_Y.getKey(), TEST_POS_Y_PRODUCT);
		newUnmappedJsonProduct.put(CatiaProperties.PRODUCT_POS_Z.getKey(), TEST_POS_Z_PRODUCT);
		newUnmappedJsonProduct.put(CatiaProperties.PRODUCT_ROT_X.getKey(), TEST_ROT_X_PRODUCT);
		newUnmappedJsonProduct.put(CatiaProperties.PRODUCT_ROT_Y.getKey(), TEST_ROT_Y_PRODUCT);
		newUnmappedJsonProduct.put(CatiaProperties.PRODUCT_ROT_Z.getKey(), TEST_ROT_Z_PRODUCT);
		newUnmappedJsonProduct.put(CatiaProperties.PRODUCT_SHAPE.getKey(), TEST_SHAPE_PRODUCT);
		childProducts.add(newUnmappedJsonProduct);


		// Do the import
		CatiaImporter importer = new CatiaImporter();
		Map<String, StructuralElementInstance> mapping = importer.mapJsonUuidToSEI(rootObject, configurationTree);
		Command importCommand = importer.transform(editingDomain, rootObject, mapping);
		editingDomain.getVirSatCommandStack().execute(importCommand);

		Visualisation visualisationProduct = elementConfigurationReactionWheel1.getFirst(Visualisation.class);
		Visualisation visualisationPart = elementReactionWheelDefinition.getFirst(Visualisation.class);

		assertEquals("Check if product values are imported",
				visualisationProduct.getPositionXBean().getValueToBaseUnit(), TEST_POS_X_PRODUCT, EPSILON);
		assertEquals("Check if product values are imported",
				visualisationProduct.getPositionYBean().getValueToBaseUnit(), TEST_POS_Y_PRODUCT, EPSILON);
		assertEquals("Check if product values are imported",
				visualisationProduct.getPositionZBean().getValueToBaseUnit(), TEST_POS_Z_PRODUCT, EPSILON);

		assertEquals("Check if product values are imported",
				visualisationProduct.getRotationXBean().getValueToBaseUnit(), TEST_ROT_X_PRODUCT, EPSILON);
		assertEquals("Check if product values are imported",
				visualisationProduct.getRotationYBean().getValueToBaseUnit(), TEST_ROT_Y_PRODUCT, EPSILON);
		assertEquals("Check if product values are imported",
				visualisationProduct.getRotationZBean().getValueToBaseUnit(), TEST_ROT_Z_PRODUCT, EPSILON);

		assertEquals("Check if product values are imported", visualisationProduct.getShape(), TEST_SHAPE_PRODUCT);

		assertEquals("Check if part values are imported", visualisationPart.getSizeXBean().getValueToBaseUnit(),
				TEST_SIZE_X_PART, EPSILON);
		assertEquals("Check if part values are imported", visualisationPart.getSizeYBean().getValueToBaseUnit(),
				TEST_SIZE_Y_PART, EPSILON);
		assertEquals("Check if part values are imported", visualisationPart.getSizeZBean().getValueToBaseUnit(),
				TEST_SIZE_Z_PART, EPSILON);
		assertEquals("Check if part values are imported", visualisationPart.getRadiusBean().getValueToBaseUnit(),
				TEST_RADIUS_PART, EPSILON);
		assertEquals("Check if part values are imported", visualisationPart.getShape(), TEST_SHAPE_PART);
		assertEquals("Check if part values are imported", visualisationPart.getColor(), TEST_COLOR_PART);

	}

	@Test
	public void testTransformWithoutVisualisation() {

		JsonObject rootObject = createMappedJsonObjectWithProductAndConfiguration();

		// Add some changes to import in a new configuration element without
		// visualisation
		ElementConfiguration elementConfigurationReactionWheel3 = new ElementConfiguration(conceptPS);
		editingDomain.getVirSatCommandStack().execute(new RecordingCommand(editingDomain) {
			@Override
			protected void doExecute() {
				subSystemAOCS.add(elementConfigurationReactionWheel3);
			}
		});

		JsonObject rootProduct = rootObject.getMap(CatiaProperties.PRODUCTS);
		JsonArray childProducts = rootProduct.getCollection(CatiaProperties.PRODUCT_CHILDREN);
		JsonObject jsonProductofNewConfiguration = new JsonObject();
		jsonProductofNewConfiguration.put(CatiaProperties.UUID.getKey(), elementConfigurationReactionWheel3.getUuid());
		jsonProductofNewConfiguration.put(CatiaProperties.PRODUCT_POS_X.getKey(), TEST_POS_X_PRODUCT);
		jsonProductofNewConfiguration.put(CatiaProperties.PRODUCT_POS_Y.getKey(), TEST_POS_Y_PRODUCT);
		jsonProductofNewConfiguration.put(CatiaProperties.PRODUCT_POS_Z.getKey(), TEST_POS_Z_PRODUCT);
		jsonProductofNewConfiguration.put(CatiaProperties.PRODUCT_ROT_X.getKey(), TEST_ROT_X_PRODUCT);
		jsonProductofNewConfiguration.put(CatiaProperties.PRODUCT_ROT_Y.getKey(), TEST_ROT_Y_PRODUCT);
		jsonProductofNewConfiguration.put(CatiaProperties.PRODUCT_ROT_Z.getKey(), TEST_ROT_Z_PRODUCT);
		jsonProductofNewConfiguration.put(CatiaProperties.PRODUCT_SHAPE.getKey(), TEST_SHAPE_PRODUCT);
		childProducts.add(jsonProductofNewConfiguration);

		// Do the import
		CatiaImporter importer = new CatiaImporter();
		Map<String, StructuralElementInstance> mapping = importer.mapJsonUuidToSEI(rootObject, configurationTree);
		Command importCommand = importer.transform(editingDomain, rootObject, mapping);
		editingDomain.getVirSatCommandStack().execute(importCommand);

		// Check if import worked on new element without visualisation
		Visualisation visualisation = elementConfigurationReactionWheel3
				.getFirst(Visualisation.class);
		assertNotNull("Visualisation was created", visualisation);
		
		assertTrue("Check if product values are imported", visualisation
				.getPositionXBean().getValueToBaseUnit() == TEST_POS_X_PRODUCT);
		assertTrue("Check if product values are imported", visualisation
				.getPositionYBean().getValueToBaseUnit() == TEST_POS_Y_PRODUCT);
		assertTrue("Check if product values are imported", visualisation
				.getPositionZBean().getValueToBaseUnit() == TEST_POS_Z_PRODUCT);

		assertTrue("Check if product values are imported", visualisation
				.getRotationXBean().getValueToBaseUnit() == TEST_ROT_X_PRODUCT);
		assertTrue("Check if product values are imported", visualisation
				.getRotationYBean().getValueToBaseUnit() == TEST_ROT_Y_PRODUCT);
		assertTrue("Check if product values are imported", visualisation
				.getRotationZBean().getValueToBaseUnit() == TEST_ROT_Z_PRODUCT);

		assertTrue("Check if product values are imported",
				visualisation.getShape().equals(TEST_SHAPE_PRODUCT));

	}

	@Test
	public void testTransformWithGeometryFile() throws IOException, CoreException {

		JsonObject rootObject = createMappedJsonObjectWithProductAndConfiguration();

		Path externalFolder = Files.createTempDirectory("catiaTest");
		Path externalStl = Paths.get(externalFolder.toString(), STL_TEST_FILENAME);
		List<String> stlContent = Arrays.asList("solid test", "endsolid test");
		Files.write(externalStl, stlContent);

		JsonArray partArray = rootObject.getCollection(CatiaProperties.PARTS);
		JsonObject part = partArray.getMap(0);
		part.put(CatiaProperties.PART_SHAPE.getKey(), Visualisation.SHAPE_GEOMETRY_NAME);
		part.put(CatiaProperties.PART_STL_PATH.getKey(), externalStl.toString());

		// Do the import
		CatiaImporter importer = new CatiaImporter();
		Map<String, StructuralElementInstance> mapping = importer.mapJsonUuidToSEI(rootObject, configurationTree);
		Command importCommand = importer.transform(editingDomain, rootObject, mapping);
		editingDomain.getVirSatCommandStack().execute(importCommand);

		ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, null);

		// Create expected path
		String expectedDocumentsFolder = VirSatProjectCommons
				.getDocumentFolder(elementReactionWheelDefinition.getStructuralElementInstance()).getFullPath()
				.toOSString();
		Path expectedLocalPath = Paths.get(ResourcesPlugin.getWorkspace().getRoot().getRawLocation().toOSString(),
				expectedDocumentsFolder, STL_TEST_FILENAME);

		assertTrue("STL file was copied", expectedLocalPath.toFile().exists());
		assertArrayEquals("STL file is copied correctly", Files.readAllBytes(expectedLocalPath),
				Files.readAllBytes(externalStl));
		assertEquals("URI added correctly",
				URI.createPlatformResourceURI(expectedLocalPath.toString(), false).toFileString(),
				reactionWheelVisDefinition.getGeometryFile().toFileString());
	}
	
	@Test
	public void testTransformWithNewMapping() {
		JsonObject rootObject = createMappedJsonObjectWithProductAndConfiguration();

		// Add some changes to import in a new configuration element without
		// visualisation
		ElementConfiguration elementConfigurationReactionWheel3 = new ElementConfiguration(conceptPS);
		editingDomain.getVirSatCommandStack().execute(new RecordingCommand(editingDomain) {
			@Override
			protected void doExecute() {
				subSystemAOCS.add(elementConfigurationReactionWheel3);
			}
		});

		// Create a new JSON object thats was externally created and does not have a representation
		// in the Virtual Satellite model jet
		JsonObject rootProduct = rootObject.getMap(CatiaProperties.PRODUCTS);
		JsonArray childProducts = rootProduct.getCollection(CatiaProperties.PRODUCT_CHILDREN);
		JsonObject newUnmappedJsonProduct = new JsonObject();
		newUnmappedJsonProduct.put(CatiaProperties.UUID.getKey(), UUID.randomUUID().toString());
		newUnmappedJsonProduct.put(CatiaProperties.PRODUCT_POS_X.getKey(), TEST_POS_X_PRODUCT);
		newUnmappedJsonProduct.put(CatiaProperties.PRODUCT_POS_Y.getKey(), TEST_POS_Y_PRODUCT);
		newUnmappedJsonProduct.put(CatiaProperties.PRODUCT_POS_Z.getKey(), TEST_POS_Z_PRODUCT);
		newUnmappedJsonProduct.put(CatiaProperties.PRODUCT_ROT_X.getKey(), TEST_ROT_X_PRODUCT);
		newUnmappedJsonProduct.put(CatiaProperties.PRODUCT_ROT_Y.getKey(), TEST_ROT_Y_PRODUCT);
		newUnmappedJsonProduct.put(CatiaProperties.PRODUCT_ROT_Z.getKey(), TEST_ROT_Z_PRODUCT);
		newUnmappedJsonProduct.put(CatiaProperties.PRODUCT_SHAPE.getKey(), TEST_SHAPE_PRODUCT);
		childProducts.add(newUnmappedJsonProduct);


		// Check mapping
		CatiaImporter importer = new CatiaImporter();
		Map<String, StructuralElementInstance> mapping = importer.mapJsonUuidToSEI(rootObject, configurationTree);
		
		List<JsonObject> unmappedElements = importer.getUnmappedJSONObjects(rootObject, mapping);
		assertFalse("List of unmapped elements should not be empty", unmappedElements.isEmpty());
		assertEquals(newUnmappedJsonProduct, unmappedElements.get(0));
		
		// Do the handling of unmapped elements
		mapping.put(unmappedElements.get(0).getString(CatiaProperties.UUID), 
				elementConfigurationReactionWheel3.getStructuralElementInstance());
		
		
		// Do the import
		Command importCommand = importer.transform(editingDomain, rootObject, mapping);
		editingDomain.getVirSatCommandStack().execute(importCommand);
		
		
		// Check if import worked on new element without mapping to exisiting element
		Visualisation visualisation = elementConfigurationReactionWheel3
				.getFirst(Visualisation.class);
		assertNotNull("Visualisation was created", visualisation);
		
		assertTrue("Check if product values are imported", visualisation
				.getPositionXBean().getValueToBaseUnit() == TEST_POS_X_PRODUCT);
		assertTrue("Check if product values are imported", visualisation
				.getPositionYBean().getValueToBaseUnit() == TEST_POS_Y_PRODUCT);
		assertTrue("Check if product values are imported", visualisation
				.getPositionZBean().getValueToBaseUnit() == TEST_POS_Z_PRODUCT);

		assertTrue("Check if product values are imported", visualisation
				.getRotationXBean().getValueToBaseUnit() == TEST_ROT_X_PRODUCT);
		assertTrue("Check if product values are imported", visualisation
				.getRotationYBean().getValueToBaseUnit() == TEST_ROT_Y_PRODUCT);
		assertTrue("Check if product values are imported", visualisation
				.getRotationZBean().getValueToBaseUnit() == TEST_ROT_Z_PRODUCT);

		assertTrue("Check if product values are imported",
				visualisation.getShape().equals(TEST_SHAPE_PRODUCT));
		
	}

	@Test
	public void testMapJSONtoSEIWithNoUnmappedJSONObject() {

		JsonObject rootObject = createMappedJsonObjectWithProductAndConfiguration();

		CatiaImporter importer = new CatiaImporter();
		Map<String, StructuralElementInstance> mapping = importer.mapJsonUuidToSEI(rootObject, configurationTree);
		List<JsonObject> unmappedElements = importer.getUnmappedJSONObjects(rootObject, mapping);

		// Check map
		assertEquals("Map does not contain element definition",
				elementReactionWheelDefinition.getStructuralElementInstance(),
				mapping.get(elementReactionWheelDefinition.getUuid()));
		assertEquals("Map does not contain first element configuration",
				elementConfigurationReactionWheel1.getStructuralElementInstance(),
				mapping.get(elementConfigurationReactionWheel1.getUuid()));
		assertEquals("Map does not contain second element configuration",
				elementConfigurationReactionWheel2.getStructuralElementInstance(),
				mapping.get(elementConfigurationReactionWheel2.getUuid()));
		assertEquals("Map does not contain root product", subSystemAOCS.getStructuralElementInstance(),
				mapping.get(subSystemAOCS.getUuid()));

		// Check unmapped elements
		assertTrue("Check that there are no unappable elements in the imported JSON", unmappedElements.isEmpty());

	}

	@Test
	public void testMapJSONtoSEIWithUnmappedJSONPart() {

		JsonObject rootObject = createMappedJsonObjectWithProductAndConfiguration();

		// Create new unmapped element
		JsonObject unmappedJsonObject = new JsonObject();
		unmappedJsonObject.put(CatiaProperties.UUID.getKey(), UUID.randomUUID().toString());

		JsonArray partArray = rootObject.getCollection(CatiaProperties.PARTS);
		partArray.add(unmappedJsonObject);

		CatiaImporter importer = new CatiaImporter();
		Map<String, StructuralElementInstance> mapping = importer.mapJsonUuidToSEI(rootObject, configurationTree);
		List<JsonObject> unmappedElements = importer.getUnmappedJSONObjects(rootObject, mapping);

		// Check map
		assertEquals("Map does not contain element definition",
				elementReactionWheelDefinition.getStructuralElementInstance(),
				mapping.get(elementReactionWheelDefinition.getUuid()));
		assertEquals("Map does not contain first element configuration",
				elementConfigurationReactionWheel1.getStructuralElementInstance(),
				mapping.get(elementConfigurationReactionWheel1.getUuid()));
		assertEquals("Map does not contain second element configuration",
				elementConfigurationReactionWheel2.getStructuralElementInstance(),
				mapping.get(elementConfigurationReactionWheel2.getUuid()));
		assertEquals("Map does not contain root product", subSystemAOCS.getStructuralElementInstance(),
				mapping.get(subSystemAOCS.getUuid()));

		// Check unmapped elements
		assertEquals("Check that there is one unappable element in the imported JSON", 1, unmappedElements.size());
		assertEquals("Expected unmapped part not found", unmappedJsonObject, unmappedElements.get(0));

	}

	@Test
	public void testMapJSONtoSEIWithUnmappedJSONProduct() {

		JsonObject rootObject = createMappedJsonObjectWithProductAndConfiguration();

		// Create new unmapped element
		JsonObject unmappedJsonObject = new JsonObject();
		unmappedJsonObject.put(CatiaProperties.UUID.getKey(), UUID.randomUUID().toString());

		JsonObject rootProduct = rootObject.getMap(CatiaProperties.PRODUCTS);
		JsonArray productArray = rootProduct.getCollection(CatiaProperties.PRODUCT_CHILDREN);
		productArray.add(unmappedJsonObject);

		CatiaImporter importer = new CatiaImporter();
		Map<String, StructuralElementInstance> mapping = importer.mapJsonUuidToSEI(rootObject, configurationTree);
		List<JsonObject> unmappedElements = importer.getUnmappedJSONObjects(rootObject, mapping);

		// Check map
		assertEquals("Map does not contain element definition",
				elementReactionWheelDefinition.getStructuralElementInstance(),
				mapping.get(elementReactionWheelDefinition.getUuid()));
		assertEquals("Map does not contain first element configuration",
				elementConfigurationReactionWheel1.getStructuralElementInstance(),
				mapping.get(elementConfigurationReactionWheel1.getUuid()));
		assertEquals("Map does not contain second element configuration",
				elementConfigurationReactionWheel2.getStructuralElementInstance(),
				mapping.get(elementConfigurationReactionWheel2.getUuid()));
		assertEquals("Map does not contain root product", subSystemAOCS.getStructuralElementInstance(),
				mapping.get(subSystemAOCS.getUuid()));

		// Check unmapped elements
		assertEquals("Check that there is one unappable elements in the imported JSON", 1, unmappedElements.size());
		assertEquals("Expected unmapped product not found", unmappedJsonObject, unmappedElements.get(0));

	}

	/**
	 * Create test scenario with inheritance
	 */
	public void createTestTreeScenario() {

		// Create objects
		productTree = new ProductTree(conceptPS);
		domainAOCS = new ProductTreeDomain(conceptPS);
		elementReactionWheelDefinition = new ElementDefinition(conceptPS);

		configurationTree = new ConfigurationTree(conceptPS);
		subSystemAOCS = new ElementConfiguration(conceptPS);
		elementConfigurationReactionWheel1 = new ElementConfiguration(conceptPS);
		elementConfigurationReactionWheel2 = new ElementConfiguration(conceptPS);

		assemblyTree = new AssemblyTree(conceptPS);
		aocsSubSystemOccurence = new ElementOccurence(conceptPS);
		reactionWheelOccurence1 = new ElementOccurence(conceptPS);
		reactionWheelOccurence2 = new ElementOccurence(conceptPS);

		// Visualisation elements
		reactionWheelVisDefinition = new Visualisation(conceptVis);

		productTree.add(domainAOCS);
		domainAOCS.add(elementReactionWheelDefinition);

		configurationTree.add(subSystemAOCS);
		subSystemAOCS.add(elementConfigurationReactionWheel1);
		subSystemAOCS.add(elementConfigurationReactionWheel2);
		elementConfigurationReactionWheel1.addSuperSei(elementReactionWheelDefinition);
		elementConfigurationReactionWheel2.addSuperSei(elementReactionWheelDefinition);

		assemblyTree.add(aocsSubSystemOccurence);
		aocsSubSystemOccurence.add(reactionWheelOccurence1);
		aocsSubSystemOccurence.add(reactionWheelOccurence2);
		reactionWheelOccurence1.addSuperSei(elementConfigurationReactionWheel1);
		reactionWheelOccurence2.addSuperSei(elementConfigurationReactionWheel1);

		// Add visualisation categories
		elementReactionWheelDefinition.add(reactionWheelVisDefinition);

		// Create tree structure with inheritance
		editingDomain.getVirSatCommandStack().execute(new RecordingCommand(editingDomain) {

			@Override
			protected void doExecute() {

				repository.getRootEntities().add(productTree.getStructuralElementInstance());
				repository.getRootEntities().add(configurationTree.getStructuralElementInstance());
				repository.getRootEntities().add(assemblyTree.getStructuralElementInstance());

				editingDomain.getResourceSet()
						.getAndAddStructuralElementInstanceResource(productTree.getStructuralElementInstance());
				editingDomain.getResourceSet()
						.getAndAddStructuralElementInstanceResource(configurationTree.getStructuralElementInstance());
				editingDomain.getResourceSet()
						.getAndAddStructuralElementInstanceResource(assemblyTree.getStructuralElementInstance());

				new InheritanceCopier().updateAllInOrder(repository, new NullProgressMonitor());
			}
		});

		assertNotNull("Sanitycheck that the inheritance copier worked as expected",
				reactionWheelOccurence1.getFirst(Visualisation.class));

	}

	/**
	 * Create a simple mapped JSON object with parts and products that are mapped to
	 * elements in the test trees
	 * 
	 * @return the JSON root object
	 */
	protected JsonObject createMappedJsonObjectWithProductAndConfiguration() {

		JsonObject jsonObjectReactionWheelDefinition = new JsonObject();
		jsonObjectReactionWheelDefinition.put(CatiaProperties.UUID.getKey(), elementReactionWheelDefinition.getUuid());
		jsonObjectReactionWheelDefinition.put(CatiaProperties.PART_COLOR.getKey(), TEST_COLOR_PART);
		jsonObjectReactionWheelDefinition.put(CatiaProperties.PART_LENGTH_X.getKey(), TEST_SIZE_X_PART);
		jsonObjectReactionWheelDefinition.put(CatiaProperties.PART_LENGTH_Y.getKey(), TEST_SIZE_Y_PART);
		jsonObjectReactionWheelDefinition.put(CatiaProperties.PART_LENGTH_Z.getKey(), TEST_SIZE_Z_PART);
		jsonObjectReactionWheelDefinition.put(CatiaProperties.PART_RADIUS.getKey(), TEST_RADIUS_PART);
		jsonObjectReactionWheelDefinition.put(CatiaProperties.PART_SHAPE.getKey(), TEST_SHAPE_PART);
		JsonArray partArray = new JsonArray();
		partArray.add(jsonObjectReactionWheelDefinition);

		JsonObject jsonObjectReactionWheel1Configuration = new JsonObject();
		jsonObjectReactionWheel1Configuration.put(CatiaProperties.UUID.getKey(),
				elementConfigurationReactionWheel1.getUuid());
		jsonObjectReactionWheel1Configuration.put(CatiaProperties.PRODUCT_POS_X.getKey(), TEST_POS_X_PRODUCT);
		jsonObjectReactionWheel1Configuration.put(CatiaProperties.PRODUCT_POS_Y.getKey(), TEST_POS_Y_PRODUCT);
		jsonObjectReactionWheel1Configuration.put(CatiaProperties.PRODUCT_POS_Z.getKey(), TEST_POS_Z_PRODUCT);
		jsonObjectReactionWheel1Configuration.put(CatiaProperties.PRODUCT_ROT_X.getKey(), TEST_ROT_X_PRODUCT);
		jsonObjectReactionWheel1Configuration.put(CatiaProperties.PRODUCT_ROT_Y.getKey(), TEST_ROT_Y_PRODUCT);
		jsonObjectReactionWheel1Configuration.put(CatiaProperties.PRODUCT_ROT_Z.getKey(), TEST_ROT_Z_PRODUCT);
		jsonObjectReactionWheel1Configuration.put(CatiaProperties.PRODUCT_SHAPE.getKey(), TEST_SHAPE_PRODUCT);
		JsonObject jsonObjectReactionWheel2Configuration = new JsonObject();
		jsonObjectReactionWheel2Configuration.put(CatiaProperties.PRODUCT_POS_X.getKey(), TEST_POS_X_PRODUCT);
		jsonObjectReactionWheel2Configuration.put(CatiaProperties.PRODUCT_POS_Y.getKey(), TEST_POS_Y_PRODUCT);
		jsonObjectReactionWheel2Configuration.put(CatiaProperties.PRODUCT_POS_Z.getKey(), TEST_POS_Z_PRODUCT);
		jsonObjectReactionWheel2Configuration.put(CatiaProperties.PRODUCT_ROT_X.getKey(), TEST_ROT_X_PRODUCT);
		jsonObjectReactionWheel2Configuration.put(CatiaProperties.PRODUCT_ROT_Y.getKey(), TEST_ROT_Y_PRODUCT);
		jsonObjectReactionWheel2Configuration.put(CatiaProperties.PRODUCT_ROT_Z.getKey(), TEST_ROT_Z_PRODUCT);
		jsonObjectReactionWheel2Configuration.put(CatiaProperties.PRODUCT_SHAPE.getKey(), TEST_SHAPE_PRODUCT);
		jsonObjectReactionWheel2Configuration.put(CatiaProperties.UUID.getKey(),
				elementConfigurationReactionWheel2.getUuid());
		JsonArray productArray = new JsonArray();
		productArray.add(jsonObjectReactionWheel1Configuration);
		productArray.add(jsonObjectReactionWheel2Configuration);

		JsonObject rootProduct = new JsonObject();
		rootProduct.put(CatiaProperties.UUID.getKey(), subSystemAOCS.getUuid());
		rootProduct.put(CatiaProperties.PRODUCT_CHILDREN.getKey(), productArray);

		JsonObject rootObject = new JsonObject();
		rootObject.put(CatiaProperties.PARTS.getKey(), partArray);
		rootObject.put(CatiaProperties.PRODUCTS.getKey(), rootProduct);

		return rootObject;
	}



}