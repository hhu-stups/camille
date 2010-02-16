/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.build.ast;

import org.eventb.emf.core.EventBCommentedExpressionElement;
import org.eventb.emf.core.EventBNamedCommentedPredicateElement;
import org.eventb.emf.core.machine.Action;
import org.eventb.emf.core.machine.util.MachineSwitch;

public class MachineTranslateSwitch extends MachineSwitch<Boolean> {

	private final FormulaTranslator translator;

	public MachineTranslateSwitch(final FormulaTranslator formulaTranslator) {
		translator = formulaTranslator;
	}

	@Override
	public Boolean caseEventBNamedCommentedPredicateElement(final EventBNamedCommentedPredicateElement object) {
		translator.replace(object);
		return true;
	}

	@Override
	public Boolean caseEventBCommentedExpressionElement(final EventBCommentedExpressionElement object) {
		translator.replace(object);
		return true;
	}

	@Override
	public Boolean caseAction(final Action object) {
		translator.replace(object);
		return true;
	}
}
