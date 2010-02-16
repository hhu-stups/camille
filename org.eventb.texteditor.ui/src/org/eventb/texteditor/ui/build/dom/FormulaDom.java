/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.build.dom;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eventb.emf.core.EventBObject;
import org.eventb.emf.formulas.BFormula;
import org.eventb.emf.formulas.BoundIdentifierExpression;
import org.eventb.emf.formulas.IdentifierExpression;

public class FormulaDom extends AbstractDom {
	private final BFormula formula;
	private final Map<String, IdentifierExpression> freeIdentifiers = new HashMap<String, IdentifierExpression>();
	private final Map<String, BoundIdentifierExpression> boundIdentifiers = new HashMap<String, BoundIdentifierExpression>();

	public FormulaDom(final BFormula formula, final IDom parent) {
		super(Type.Event, parent);
		this.formula = formula;
	}

	public EventBObject getEventBElement() {
		return formula;
	}

	@Override
	protected synchronized Set<String> doGetIdentifiers() {
		final Set<String> result = new HashSet<String>();

		result.addAll(getBoundIdentifiers().keySet());
		result.addAll(getFreeIdentifiers().keySet());

		return result;
	}

	@Override
	protected IdentifierType doGetIdentifierType(final String identifier) {
		if (boundIdentifiers.containsKey(identifier)) {
			return IdentifierType.LocalVariable;
		}

		return null;
	}

	public Map<String, IdentifierExpression> getFreeIdentifiers() {
		return freeIdentifiers;
	}

	public void addFreeIdentifier(final IdentifierExpression expression) {
		freeIdentifiers.put(expression.getName(), expression);
	}

	public Map<String, BoundIdentifierExpression> getBoundIdentifiers() {
		return boundIdentifiers;
	}

	public void addBoundIdentifier(final BoundIdentifierExpression expression) {
		boundIdentifiers.put(expression.getName(), expression);
	}
}
