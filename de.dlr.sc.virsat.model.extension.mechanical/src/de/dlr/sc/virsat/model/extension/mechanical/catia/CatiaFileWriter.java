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

//import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.github.cliftonlabs.json_simple.JsonObject;

import de.dlr.sc.virsat.model.concept.types.structural.IBeanStructuralElementInstance;
import de.dlr.sc.virsat.model.extension.visualisation.model.Visualisation;

/**
 * This class creates .json file and copies .stl files
 */

public class CatiaFileWriter {
	
	/**
	 * Exports productRoot to json and copies it to outputJsonFilePath
	 * Also copies all geometry files to the same directory
	 * @param outputJsonFilePath full absolute path to the JSON file to be created
	 * @param productRoot 
	 * @param progressMonitor 
	 * @throws CoreException 
	 * @throws IOException 
	 */
	public void writeFiles(String outputJsonFilePath, IBeanStructuralElementInstance productRoot, IProgressMonitor progressMonitor) throws CoreException, IOException {
		SubMonitor jsonSubMonitor = SubMonitor.convert(progressMonitor, 2);

		CatiaExporter catiaExporter = new CatiaExporter();
		java.nio.file.Path jsonFilePath = Paths.get(outputJsonFilePath);
		java.nio.file.Path outputDirectoryPath = jsonFilePath.getParent();
		
		if (outputDirectoryPath == null) {
			throw new IllegalArgumentException("Invalid path to JSON file. Can't extract output directory: " + outputJsonFilePath);
		}
		
		catiaExporter.setGeometryFilesPath(outputDirectoryPath.toString());
		JsonObject jsonObject = catiaExporter.transform(productRoot);
		jsonSubMonitor.worked(1);

		Files.write(jsonFilePath, Collections.singleton(jsonObject.toJson()));
		jsonSubMonitor.worked(1);
		
		Set<Visualisation> geometryVisualisations = catiaExporter.getGeometryVisualisations();
		SubMonitor fileCopySubMonitor = SubMonitor.convert(progressMonitor, geometryVisualisations.size());
		for (Visualisation visualisation : geometryVisualisations) {
			IFile geometryFile = visualisation.getGeometryFileBean().getFile();
			
			Path source = Paths.get(geometryFile.getRawLocation().toOSString());	
			Path destination = Paths.get(outputDirectoryPath + File.separator + geometryFile.getName());
			
			Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
			
			fileCopySubMonitor.worked(1);
		}
	}
}
