/**
 * Copyright (c) 2010
 * University of Southampton and others.
 * All rights reserved. This program and the accompanying materials  are made
 * available under the terms of the Eclipse Public License v1.0 which accompanies this 
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 *
 * $Id$
 */

package org.eventb.texttools.diff;

import org.eclipse.emf.compare.diff.engine.check.AttributesCheck;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil.CrossReferencer;
import org.eventb.emf.core.CorePackage;

public class EventBAttributesCheck extends AttributesCheck {

	public EventBAttributesCheck(CrossReferencer referencer) {
		super(referencer);
	}

	/**
	 * Determines if we should ignore an attribute for diff detection.
	 * <p>
	 * We do not ignore transient nor derived attributes by default because often these are the user visible/editable attribute. We ignore the reference attribute since name is the
	 * changeable part. We also ignore the attributes of Annotations and their maps
	 * </p>
	 * 
	 * FIXME: Make this extensible via an extension point so that extenders can decide what should be ignored.
	 * 
	 * @param attribute
	 *            Attribute to determine whether it should be ignored.
	 * @return <code>True</code> if attribute has to be ignored, <code>False</code> otherwise.
	 */
	@Override
	protected boolean shouldBeIgnored(EAttribute attribute) {
		boolean ignore = false;
		EObject container = attribute.eContainer();
		// remove default ignore transient and derived since some of these are our user visible attributes
		//		ignore = ignore || attribute.isTransient();
		//		ignore = ignore || attribute.isDerived();

		//ignore contents of string 2 string map entries (e.g. in RodinInternalDetails)
		// FIXME: make this more specific to RodinInternalDetails
		ignore = ignore || (container instanceof ENamedElement && "StringToStringMapEntry".equals(((ENamedElement) container).getName()));

		//ignore contents of Annotations
		// FIXME: make this more specific to RodinInternalDetails
		ignore = ignore || container.equals(CorePackage.eINSTANCE.getAnnotation());
		//ignore reference (instead, the derived attribute 'name' will be shown)
		ignore = ignore || attribute.equals(CorePackage.eINSTANCE.getEventBElement_Reference());
		//ignore generated and local generated attributes as these must be preserved
		ignore = ignore || attribute.equals(CorePackage.eINSTANCE.getEventBElement_Generated());	//ADDED
		ignore = ignore || attribute.equals(CorePackage.eINSTANCE.getEventBElement_LocalGenerated());	//ADDED
		//ignore attributes of Abstract Extension
		ignore = ignore || container.equals(CorePackage.eINSTANCE.getAbstractExtension());
		//ignore attributes of Extension
		ignore = ignore || container.equals(CorePackage.eINSTANCE.getExtension());	//ADDED

		
		return ignore;

	}
}
