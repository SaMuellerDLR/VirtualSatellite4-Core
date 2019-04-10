/*******************************************************************************
 * Copyright (c) 2008-2019 German Aerospace Center (DLR), Simulation and Software Technology, Germany.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.dlr.sc.virsat.graphiti.ui.diagram.feature;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.features.impl.AbstractAddShapeFeature;

import de.dlr.sc.virsat.graphiti.util.DiagramHelper;

/**
 * VirSat extension of the Graphiti add shape feature.
 * Adds rights management to the feature.
 * @author muel_s8
 *
 */

public abstract class VirSatAddShapeFeature extends AbstractAddShapeFeature {
	
	/**
	 * Standard constructor.
	 * @param fp the feature provider
	 */
	
	public VirSatAddShapeFeature(IFeatureProvider fp) {
		super(fp);
	}

	@Override
	public boolean canAdd(IAddContext context) {
		return DiagramHelper.hasDiagramWritePermission(context.getTargetContainer());
	}
}
