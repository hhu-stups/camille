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

import java.util.List;

import org.eclipse.emf.compare.diff.engine.GenericDiffEngine;
import org.eclipse.emf.compare.diff.metamodel.DiffElement;
import org.eclipse.emf.compare.diff.metamodel.DiffGroup;
import org.eclipse.emf.compare.match.metamodel.UnmatchElement;

public class EventBDiffEngine extends GenericDiffEngine {

	/**
	 * Returns the Event-B implementation of a
	 * {@link org.eclipse.emf.compare.diff.engine.check.AbstractCheck}
	 * responsible for the verification of updates on attribute values.
	 * 
	 * @return The Event-B implementation of a
	 *         {@link org.eclipse.emf.compare.diff.engine.check.AbstractCheck}
	 *         responsible for the verification of updates on attribute values.
	 * @since 1.0
	 */
	@Override
	protected EventBAttributesCheck getAttributesChecker() {
		return new EventBAttributesCheck(matchCrossReferencer);
	}

	/**
	 * Returns the Event-B implementation of a
	 * {@link org.eclipse.emf.compare.diff.engine.check.AbstractCheck}
	 * responsible for the verification of updates on reference values.
	 * 
	 * @return The Event-B implementation of a
	 *         {@link org.eclipse.emf.compare.diff.engine.check.AbstractCheck}
	 *         responsible for the verification of updates on reference values.
	 * @since 1.0
	 */
	@Override
	protected EventBReferencesCheck getReferencesChecker() {
		return new EventBReferencesCheck(matchCrossReferencer);
	}

	/**
	 * This will process the {@link #unmatchedElements unmatched elements} list
	 * and create the appropriate {@link DiffElement}s.
	 * <p>
	 * This is called for two-way comparison.
	 * </p>
	 * <p>
	 * Filters any unmatched elements according to the references checker. Rodin
	 * internal detail Annotation and then defers to the generic diff engine.
	 * </p>
	 * <p>
	 * FIXME: Make this extensible via an extension point so that extenders can
	 * decide what should be ignored.
	 * </p>
	 * 
	 * @param diffRoot
	 *            {@link DiffGroup} under which to create the
	 *            {@link DiffElement}s.
	 * @param unmatched
	 *            The MatchModel's {@link UnmatchElement}s.
	 */
	@Override
	protected void processUnmatchedElements(DiffGroup diffRoot,
			List<UnmatchElement> unmatched) {
		super.processUnmatchedElements(diffRoot, getReferencesChecker()
				.filterUnmatchedElements(unmatched));
	}

}
