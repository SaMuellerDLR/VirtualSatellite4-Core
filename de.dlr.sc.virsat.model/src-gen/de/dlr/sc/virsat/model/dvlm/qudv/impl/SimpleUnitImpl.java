/*******************************************************************************
 * Copyright (c) 2008-2019 German Aerospace Center (DLR), Simulation and Software Technology, Germany.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.dlr.sc.virsat.model.dvlm.qudv.impl;


import de.dlr.sc.virsat.model.dvlm.qudv.QudvPackage;
import de.dlr.sc.virsat.model.dvlm.qudv.SimpleUnit;

import de.dlr.sc.virsat.model.dvlm.util.DVLMUnresolvedReferenceException;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.InternalEObject;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Simple Unit</b></em>'.
 * <!-- end-user-doc -->
 *
 * @generated
 */
public class SimpleUnitImpl extends AUnitImpl implements SimpleUnit {
	/**
	 * *********************************
	 * VirSat Specific Code Generation
	 * *********************************
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected SimpleUnitImpl() {
		super();
		this.uuid = new de.dlr.sc.virsat.model.dvlm.types.impl.VirSatUuid(); 
	}
	
	/**
	 * *********************************
	 * VirSat Specific Code Generation
	 * *********************************
	 * This method handles unresolved references into otehr resources.
	 * When ever a proxy object in the DVLM model is resolved
	 * This method will check the object and place an error to the resource
	 * if it cannot be resolved 
	 * @generated
	 */
	@Override
	public EObject eResolveProxy(InternalEObject proxy) {
		EObject resolvedProxy = super.eResolveProxy(proxy);
		return DVLMUnresolvedReferenceException.checkProxyObject(resolvedProxy, proxy, this.eResource());
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return QudvPackage.Literals.SIMPLE_UNIT;
	}

} //SimpleUnitImpl
