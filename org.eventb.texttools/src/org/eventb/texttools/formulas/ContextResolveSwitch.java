/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texttools.formulas;

import java.util.ArrayList;
import java.util.List;

import org.eventb.emf.core.EventBNamedCommentedPredicateElement;
import org.eventb.emf.core.EventBObject;
import org.eventb.emf.core.context.util.ContextSwitch;

public class ContextResolveSwitch extends ContextSwitch<Boolean> {

	private final List<FormulaParseException> exceptions = new ArrayList<FormulaParseException>();

	@Override
	public Boolean caseEventBObject(final EventBObject object) {
		return true;
	}

	@Override
	public Boolean caseEventBNamedCommentedPredicateElement(
			final EventBNamedCommentedPredicateElement object) {
		try {
			FormulaResolver.resolve(object);
		} catch (final FormulaParseException e) {
			handleError(e);
		}

		// no need to traverse any children
		return false;
	}

	private void handleError(final FormulaParseException e) {
		exceptions.add(e);
	}

	public List<FormulaParseException> getExceptions() {
		return exceptions;
	}
}
