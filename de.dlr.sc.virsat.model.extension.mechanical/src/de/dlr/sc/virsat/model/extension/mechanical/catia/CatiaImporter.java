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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.common.command.UnexecutableCommand;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.edit.domain.EditingDomain;

import com.github.cliftonlabs.json_simple.JsonObject;

import de.dlr.sc.virsat.model.concept.types.structural.BeanStructuralElementInstance;
import de.dlr.sc.virsat.model.concept.types.structural.IBeanStructuralElementInstance;
import de.dlr.sc.virsat.model.dvlm.Repository;
import de.dlr.sc.virsat.model.dvlm.concepts.Concept;
import de.dlr.sc.virsat.model.dvlm.concepts.util.ActiveConceptHelper;
import de.dlr.sc.virsat.model.dvlm.structural.StructuralElementInstance;
import de.dlr.sc.virsat.model.extension.mechanical.catia.util.CatiaHelper;
import de.dlr.sc.virsat.model.extension.visualisation.Activator;
import de.dlr.sc.virsat.model.extension.visualisation.model.Visualisation;
import de.dlr.sc.virsat.project.resources.VirSatResourceSet;
import de.dlr.sc.virsat.project.structure.VirSatProjectCommons;

/**
 * This class imports a JSON representation of system model
 *
 */
public class CatiaImporter {

	private EditingDomain editingDomain;

	/**
	 * Main method that imports a JSON representation of our system and updates the
	 * model accordingly
	 * 
	 * @param editingDomain
	 *            the editing domain of the current project
	 * @param jsonObject
	 *            the JSON Object
	 * @param mapJsonUuidToSEI
	 *            the mapping of JSON element IDs to the existing trees
	 * 
	 * @return the emf command to execute the import
	 */
	public Command transform(EditingDomain editingDomain, JsonObject jsonObject,
			Map<String, StructuralElementInstance> mapJsonUuidToSEI) {

		CompoundCommand importCommand = new CompoundCommand();
		this.editingDomain = editingDomain;

		boolean commandCreationWorked = true;

		// Import parts
		for (JsonObject part : CatiaHelper.getListOfAllJSONParts(jsonObject)) {
			commandCreationWorked &= updateSeiFromPart(importCommand, mapJsonUuidToSEI.get(part.getString(CatiaProperties.UUID)), part);
		}

		// Import products
		for (JsonObject product : CatiaHelper.getListOfAllJSONProducts(jsonObject)) {
			commandCreationWorked &= updateSeiFromProduct(importCommand, mapJsonUuidToSEI.get(product.getString(CatiaProperties.UUID)),
					product);
		}

		if (commandCreationWorked) {
			return importCommand;
		} else {
			return UnexecutableCommand.INSTANCE;
		}

	}

	/**
	 * Maps from the IDs of JSON elements to existing model elements in the Virtual
	 * Satellite model 
	 * 
	 * @param jsonContent
	 *            the JSON content to be imported
	 * @param existingTree
	 *            a tree element the imported JSON should be mapped to
	 * @return a map from UUID in the JSON file to their existing tree elements in the model
	 */
	public Map<String, StructuralElementInstance> mapJsonUuidToSEI(JsonObject jsonContent,
			IBeanStructuralElementInstance existingTree) {

		Map<String, StructuralElementInstance> mapExisitingElementToUUID = new HashMap<String, StructuralElementInstance>();
		Map<String, IBeanStructuralElementInstance> mapSEIsToUuid = createMapOfTreeSEIsToUuid(existingTree);

		for (JsonObject object : CatiaHelper.getListOfAllJSONElements(jsonContent)) {
			String uuid = object.getString(CatiaProperties.UUID);
			IBeanStructuralElementInstance mappedElement = mapSEIsToUuid.get(uuid);
			if (mapSEIsToUuid.containsKey(uuid) && mappedElement != null) {
				mapExisitingElementToUUID.put(uuid, mappedElement.getStructuralElementInstance());
			}

		}

		return mapExisitingElementToUUID;

	}

	/**
	 * Method to get all unmapped JSON elements that do not have a representation in
	 * the existing trees
	 * 
	 * @param jsonRoot
	 *            the JSON root element to look for unmapped elements in
	 * @param mapJSONtoSEI
	 *            the Map of JSONObject IDs to SEIs in the model created by method
	 *            {@link #mapJSONtoSEI(JsonObject, IBeanStructuralElementInstance)}
	 * @return a list of unmapped elements
	 */
	public List<JsonObject> getUnmappedJSONObjects(JsonObject jsonRoot,
			Map<String, StructuralElementInstance> mapJSONtoSEI) {

		List<JsonObject> unmappedElements = new ArrayList<>();

		for (JsonObject object : CatiaHelper.getListOfAllJSONElements(jsonRoot)) {
			String uuid = object.getString(CatiaProperties.UUID);
			if (!mapJSONtoSEI.containsKey(uuid)) {
				unmappedElements.add(object);
			}
		}

		return unmappedElements;

	}

	/**
	 * Update an element in the Virtual Satellite model from a corresponding
	 * imported JSON part
	 * 
	 * @param importCommand
	 *            the compound command that handles the import
	 * @param sei
	 *            the structural element instance to be updated
	 * @param part
	 *            the JSON object part that is imported and corresponds to the SEI
	 * 
	 * @return returns if all required properties for the import could be found
	 */
	protected boolean updateSeiFromPart(CompoundCommand importCommand, StructuralElementInstance sei, JsonObject part) {
		BeanStructuralElementInstance beanSEI = new BeanStructuralElementInstance(sei);
		Visualisation visualisation = getVisualisation(beanSEI, importCommand);

		String name;
		
		double sizeX;
		double sizeY;
		double sizeZ;
		double radius;

		long color;
		String shape;
		String stlFile = null;
		try {
			name = part.getString(CatiaProperties.NAME);
			sizeX = part.getDouble(CatiaProperties.PART_LENGTH_X);
			sizeY = part.getDouble(CatiaProperties.PART_LENGTH_Y);
			sizeZ = part.getDouble(CatiaProperties.PART_LENGTH_Z);
			radius = part.getDouble(CatiaProperties.PART_RADIUS);

			color = part.getLong(CatiaProperties.PART_COLOR);
			shape = part.getString(CatiaProperties.PART_SHAPE);

		} catch (NullPointerException e) {
			Activator.getDefault().getLog().log(new Status(Status.ERROR, Activator.getPluginId(),
					"CatiaImport: Failed to perform import! Could not load all required properties", e));
			return false;
		}

		if (part.containsKey(CatiaProperties.PART_STL_PATH.getKey())) {
			stlFile = part.getString(CatiaProperties.PART_STL_PATH);
		}

		importCommand.append(beanSEI.setName(editingDomain, name));
		
		importCommand.append(visualisation.getSizeXBean().setValueAsBaseUnit(editingDomain, sizeX));
		importCommand.append(visualisation.getSizeYBean().setValueAsBaseUnit(editingDomain, sizeY));
		importCommand.append(visualisation.getSizeZBean().setValueAsBaseUnit(editingDomain, sizeZ));
		
		importCommand.append(visualisation.getRadiusBean().setValueAsBaseUnit(editingDomain, radius));

		importCommand.append(visualisation.setShape(editingDomain, shape));
		importCommand.append(visualisation.setColor(editingDomain, color));

		if (shape.equals(Visualisation.SHAPE_GEOMETRY_NAME) && stlFile != null) {
			importCommand.append(visualisation.setGeometryFile(editingDomain, 
					copyAndGetPlatformResource(stlFile, beanSEI)));
		}

		return true;

	}

	/**
	 * Update an element in the Virtual Satellite model from a corresponding
	 * imported JSON product
	 * 
	 * @param importCommand
	 *            the compound command that handles the import
	 * @param sei
	 *            the structural element instance to be updated
	 * @param product
	 *            the JSON object product that is imported and corresponds to the
	 *            SEI
	 * 
	 * @return returns if all required properties for the import could be found
	 */
	protected boolean updateSeiFromProduct(CompoundCommand importCommand, StructuralElementInstance sei,
			JsonObject product) {
		
		if (!hasVisualisationProductProperties(product)) {
			return true;
		}
		
		BeanStructuralElementInstance beanSEI = new BeanStructuralElementInstance(sei);
		Visualisation visualisation = getVisualisation(beanSEI, importCommand);

		String name;
		
		double posX;
		double posY;
		double posZ;

		double rotX;
		double rotY;
		double rotZ;

		String shape;
		String stlFile = null;

		try {
			name = product.getString(CatiaProperties.NAME);
			
			posX = product.getDouble(CatiaProperties.PRODUCT_POS_X);
			posY = product.getDouble(CatiaProperties.PRODUCT_POS_Y);
			posZ = product.getDouble(CatiaProperties.PRODUCT_POS_Z);

			rotX = product.getDouble(CatiaProperties.PRODUCT_ROT_X);
			rotY = product.getDouble(CatiaProperties.PRODUCT_ROT_Y);
			rotZ = product.getDouble(CatiaProperties.PRODUCT_ROT_Z);

			shape = product.getString(CatiaProperties.PRODUCT_SHAPE);

		} catch (NullPointerException e) {
			Activator.getDefault().getLog().log(new Status(Status.ERROR, Activator.getPluginId(),
					"CatiaImport: Failed to perform import! Could not load all required properties", e));
			return false;
		}

		if (product.containsKey(CatiaProperties.PART_STL_PATH.getKey())) {
			stlFile = product.getString(CatiaProperties.PART_STL_PATH);
		}

		importCommand.append(beanSEI.setName(editingDomain, name));
		
		importCommand.append(visualisation.getPositionXBean().setValueAsBaseUnit(editingDomain, posX));
		importCommand.append(visualisation.getPositionYBean().setValueAsBaseUnit(editingDomain, posY));
		importCommand.append(visualisation.getPositionZBean().setValueAsBaseUnit(editingDomain, posZ));

		importCommand.append(visualisation.getRotationXBean().setValueAsBaseUnit(editingDomain, rotX));
		importCommand.append(visualisation.getRotationYBean().setValueAsBaseUnit(editingDomain, rotY));
		importCommand.append(visualisation.getRotationZBean().setValueAsBaseUnit(editingDomain, rotZ));

		importCommand.append(visualisation.setShape(editingDomain, shape));

		if (shape.equals(Visualisation.SHAPE_GEOMETRY_NAME) && stlFile != null) {
			importCommand
					.append(visualisation.setGeometryFile(editingDomain, copyAndGetPlatformResource(stlFile, beanSEI)));
		}

		return true;

	}

	/**
	 * Returns whether the part contains any visualisation properties
	 * @param product the JSON object of the product
	 * @return true if the product has any visualisation properties
	 */
	private boolean hasVisualisationProductProperties(JsonObject product) {
		return product.containsKey(CatiaProperties.PRODUCT_POS_X.getKey())
				|| product.containsKey(CatiaProperties.PRODUCT_POS_Y.getKey())
				|| product.containsKey(CatiaProperties.PRODUCT_POS_Z.getKey())
				|| product.containsKey(CatiaProperties.PRODUCT_ROT_X.getKey())
				|| product.containsKey(CatiaProperties.PRODUCT_ROT_Y.getKey())
				|| product.containsKey(CatiaProperties.PRODUCT_ROT_Z.getKey())
				|| product.containsKey(CatiaProperties.PRODUCT_SHAPE.getKey());
	}

	/**
	 * Copy a given stl resource to the workspace and return its URI
	 * 
	 * @param stlPath
	 *            the stl resource's path
	 * @param seiBean
	 *            a structural element instance bean
	 * @return the URI
	 */
	private URI copyAndGetPlatformResource(String stlPath, BeanStructuralElementInstance seiBean) {

		URI stlURI = null;
		// Copy file to workspace
		Path catiaSTLPath = Paths.get(stlPath);
		
		Path fileName = catiaSTLPath.getFileName();
		if (fileName == null) {
			throw new IllegalArgumentException("Invalid path to STL file. Can't extract internal directory: " + stlPath);
		}
		
		String stlName = fileName.toString();
		
		try {
			String workspacePath = ResourcesPlugin.getWorkspace().getRoot().getRawLocation().toOSString();
			String documentPath = VirSatProjectCommons.getDocumentFolder(seiBean.getStructuralElementInstance())
					.getFullPath().toOSString();

			Path localPath = Paths.get(documentPath, stlName);
			Path workspace = Paths.get(workspacePath, localPath.toString());

			Files.copy(catiaSTLPath, workspace, StandardCopyOption.REPLACE_EXISTING);
			stlURI = URI.createPlatformResourceURI(localPath.toString(), false);

		} catch (IOException e) {
			e.printStackTrace();
		}

		return stlURI;

	}

	/**
	 * Create a map of all UUIDs to their structural element instances in a tree and their super instances
	 * 
	 * @param existingTree
	 *            the existing tree element in the Virtual Satellite model
	 * @return a map that maps the UUID to all SEIs
	 */
	private Map<String, IBeanStructuralElementInstance> createMapOfTreeSEIsToUuid(
			IBeanStructuralElementInstance existingTree) {

		Map<String, IBeanStructuralElementInstance> mapSEIsToUuid = new HashMap<String, IBeanStructuralElementInstance>();

		for (IBeanStructuralElementInstance sei : existingTree.getDeepChildren(IBeanStructuralElementInstance.class)) {
			mapSEIsToUuid.put(sei.getUuid(), sei);

			// Also add relevant elements in other trees (the super elements) to the map
			for (IBeanStructuralElementInstance superSei : sei.getAllSuperSeis(IBeanStructuralElementInstance.class)) {
				mapSEIsToUuid.put(superSei.getUuid(), superSei);
			}
		}

		return mapSEIsToUuid;

	}

	/**
	 * Get the visualisation object of a SEI or if not exisiting create it
	 * 
	 * @param seiBean
	 *            the containing structural element instance bean
	 * @param importCommand
	 *            the compound command for the import
	 * @return the visualisation object
	 */
	private Visualisation getVisualisation(BeanStructuralElementInstance seiBean, CompoundCommand importCommand) {
		Visualisation visualisation = seiBean.getFirst(Visualisation.class);
		if (visualisation == null) {
			visualisation = createNewVisualisation(seiBean, importCommand);
		}
		return visualisation;
	}

	/**
	 * Create a new visualisation element from an exisitng SEI. The category is not
	 * yet added into the SEI
	 * 
	 * @param container
	 *            any structural element instance
	 * @param importCommand
	 *            the compound command for the import
	 * @return a new visualisation element
	 */
	private Visualisation createNewVisualisation(BeanStructuralElementInstance container,
			CompoundCommand importCommand) {

		Repository repository = VirSatResourceSet.getVirSatResourceSet(container.getStructuralElementInstance())
				.getRepository();
		ActiveConceptHelper activeConceptHelper = new ActiveConceptHelper(repository);
		Concept visConcept = activeConceptHelper.getConcept(Activator.getPluginId());
		Visualisation visualisation = new Visualisation(visConcept);

		importCommand.append(container.add(editingDomain, visualisation));

		return visualisation;
	}

}
