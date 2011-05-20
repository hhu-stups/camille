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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.compare.FactoryException;
import org.eclipse.emf.compare.diff.engine.check.ReferencesCheck;
import org.eclipse.emf.compare.diff.metamodel.DiffGroup;
import org.eclipse.emf.compare.diff.metamodel.ReferenceChangeLeftTarget;
import org.eclipse.emf.compare.diff.metamodel.ReferenceChangeRightTarget;
import org.eclipse.emf.compare.match.metamodel.Match2Elements;
import org.eclipse.emf.compare.match.metamodel.UnmatchElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.util.EcoreUtil.CrossReferencer;
import org.eventb.emf.core.CorePackage;

public class EventBReferencesCheck extends ReferencesCheck {

	public EventBReferencesCheck(CrossReferencer referencer) {
		super(referencer);
	}

	/**
	 * Determines if we should ignore a reference for diff detection.
	 * This method is intended to be used to filter matched changes.
	 * Containments are dealt with as new element additions so these are ignored.
	 * Everything else is deferred to shouldBeIgnoredAllowContainments().
	 * 
	 * @param reference
	 *            Reference to determine whether it should be ignored.
	 * @return <code>True</code> if reference has to be ignored, <code>False</code> otherwise.
	 */
	@Override
	protected boolean shouldBeIgnored(EReference reference) {
		return reference.isContainment() ? true : shouldBeIgnoredAllowContainment(reference);
	}

	/**
	 * Determines if we should ignore a reference for diff detection.
	 * This method is intended to be used to filter matched or unmatched changes.
	 * (unmatched elements are additions to Containments)
	 * are dealt with as new element additions so these are ignored.
	 * Everything else is deferred to shouldBeIgnoredAllowContainments().
	 * 
	 * <p>
	 * Apart from containment which is dealt with elsewhere, we copy the default which is to ignore references marked either
	 * <ul>
	 * <li>Container</li>
	 * <li>Transient</li>
	 * <li>Derived</li>
	 * </ul>
	 * </p>
	 * <p>
	 * We also ignore the references, refines, sees and extends because any differences will be shown in the derived attributes 'names' lists. We also ignore the containment,
	 * annotations, because it is only used to hold transient tool information. We also ignore anything contained by EcorePackage.eINSTANCE.getEGenericType() or
	 * CorePackage.eINSTANCE.getAnnotation()
	 * 
	 * </p>
	 * 
	 * FIXME: Make this extensible via an extension point so that extenders can decide what should be ignored.
	 * 
	 * @param reference
	 *            Reference to determine whether it should be ignored.
	 * @return <code>True</code> if reference has to be ignored, <code>False</code> otherwise.
	 */

	protected boolean shouldBeIgnoredAllowContainment(EReference reference) {
		boolean ignore = false;
		ignore = ignore || reference.isDerived();
		ignore = ignore || reference.isTransient();
		ignore = ignore || reference.isContainer();
		ignore = ignore || reference.eContainer().equals(EcorePackage.eINSTANCE.getEGenericType());
		String name = reference.getName();
		ignore = ignore || "refines".equals(name);
		ignore = ignore || "sees".equals(name);
		ignore = ignore || "extends".equals(name);
		ignore = ignore || "annotations".equals(name);
		ignore = ignore || reference.eContainer().equals(CorePackage.eINSTANCE.getAnnotation());

		return ignore;
	}

	@Override
	protected void checkReferenceOrderChange(DiffGroup root, EReference reference, EObject leftElement, EObject rightElement,
			List<ReferenceChangeLeftTarget> addedReferences, List<ReferenceChangeRightTarget> removedReferences) throws FactoryException {
		if (shouldBeIgnored(reference)) {
			return;
		}
		super.checkReferenceOrderChange(root, reference, leftElement, rightElement, addedReferences, removedReferences);
	}

	@Override
	protected void checkContainmentReferenceOrderChange(DiffGroup root, Match2Elements mapping, EReference reference) throws FactoryException {
		if (shouldBeIgnoredAllowContainment(reference)) {
			return;
		}
		super.checkContainmentReferenceOrderChange(root, mapping, reference);
	}

	/**
	 * The standard difference checkers take no account of shouldBeIgnored references for unmatched elements.
	 * This additional method can be used to filter out new elements (that will have no match) from the differences.
	 * It returns a new list of UnmatchElement without any elements that are contained in a reference that should Be Ignored (i.e.
	 * for which shouldBeIgnored(containingFeature) returns true.
	 * 
	 * @param unmatched
	 *            : List<UnmatchElement>
	 * @return List<UnmatchElement>
	 */
	public List<UnmatchElement> filterUnmatchedElements(List<UnmatchElement> unmatched) {
		final List<UnmatchElement> filteredUnmatched = new ArrayList<UnmatchElement>(unmatched.size());
		for (UnmatchElement element : unmatched) {
			if (element.getElement().eContainmentFeature() == null || !shouldBeIgnoredAllowContainment(element.getElement().eContainmentFeature())) {
				filteredUnmatched.add(element);
			}
		}
		return filteredUnmatched;
	}

	/**
	 * The standard difference checkers take no account of shouldBeIgnored references for unmatched elements.
	 * This additional method can be used to filter out new elements (that will have no match) from the differences.
	 * It returns a new list of UnmatchElement without any elements that are contained in a reference that should Be Ignored (i.e.
	 * for which shouldBeIgnored(containingFeature) returns true.
	 * 
	 * @param unmatched
	 *            : Map<UnmatchElement, Boolean>
	 * @return Map<UnmatchElement, Boolean>
	 */
	public Map<UnmatchElement, Boolean> filterUnmatchedElements(Map<UnmatchElement, Boolean> unmatched) {
		final Map<UnmatchElement, Boolean> filteredUnmatched = new HashMap<UnmatchElement, Boolean>(unmatched.size());
		for (final Map.Entry<UnmatchElement, Boolean> element : unmatched.entrySet()) {
			if (!shouldBeIgnoredAllowContainment(element.getKey().getElement().eContainmentFeature())) {
				filteredUnmatched.put(element.getKey(), element.getValue());
			}
		}
		return filteredUnmatched;
	}

}
