/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.build.ast;

import org.eventb.emf.core.EventBNamedCommentedPredicateElement;
import org.eventb.emf.core.context.util.ContextSwitch;

public class ContextTranslateSwitch extends ContextSwitch<Boolean> {

	private final FormulaTranslator translator;

	public ContextTranslateSwitch(final FormulaTranslator formulaTranslator) {
		translator = formulaTranslator;
	}

	@Override
	public Boolean caseEventBNamedCommentedPredicateElement(final EventBNamedCommentedPredicateElement object) {
		translator.replace(object);
		return true;
	}
}
