/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.build.dom;

import org.eclipse.core.runtime.Assert;
import org.eventb.emf.formulas.BFormula;
import org.eventb.emf.formulas.BoundIdentifierExpression;
import org.eventb.emf.formulas.IdentifierExpression;
import org.eventb.emf.formulas.util.FormulasSwitch;

public class FormulaCollectingSwitch extends FormulasSwitch<Boolean> {

	private FormulaDom formulaDom;
	private IDom currentParentDom;

	public void setCurrentParentDom(final IDom currentParentDom) {
		Assert.isNotNull(currentParentDom);
		this.currentParentDom = currentParentDom;
	}

	@Override
	public Boolean caseBFormula(final BFormula object) {
		formulaDom = new FormulaDom(object, currentParentDom);
		currentParentDom.addChild(formulaDom);
		return true;
	}

	@Override
	public Boolean caseIdentifierExpression(final IdentifierExpression object) {
		if (formulaDom == null) {
			caseBFormula(object);
		}

		formulaDom.addFreeIdentifier(object);
		return false;
	}

	@Override
	public Boolean caseBoundIdentifierExpression(
			final BoundIdentifierExpression object) {
		if (formulaDom == null) {
			caseBFormula(object);
		}

		formulaDom.addBoundIdentifier(object);
		return false;
	}
}
