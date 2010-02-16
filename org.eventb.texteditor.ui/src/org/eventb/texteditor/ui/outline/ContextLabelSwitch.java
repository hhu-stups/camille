/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.outline;

import org.eclipse.emf.common.util.EList;
import org.eclipse.jface.viewers.StyledString;
import org.eventb.emf.core.EventBNamed;
import org.eventb.emf.core.context.Axiom;
import org.eventb.emf.core.context.Context;
import org.eventb.emf.core.context.util.ContextSwitch;

public class ContextLabelSwitch extends ContextSwitch<StyledString> {

	@Override
	public StyledString caseEventBNamed(final EventBNamed object) {
		/*
		 * Default method for all EventBNamedElements that are not handled
		 * seperately in their own methods.
		 */
		return LabelHelper.getStyledName(object.getName());
	}

	@Override
	public StyledString caseContext(final Context object) {
		final StyledString result = LabelHelper.getStyledName(object.getName());

		final EList<String> extendsNames = object.getExtendsNames();
		if (extendsNames.size() > 0) {
			LabelHelper.appendAttrDelim(result);
			result.append("extends ", LabelHelper.ATTRIBUTE_STYLER);
			result.append(LabelHelper.joinEList(extendsNames),
					LabelHelper.ATTRIBUTE_STYLER);
		}

		return result;
	}

	@Override
	public StyledString caseAxiom(final Axiom object) {
		final StyledString result = LabelHelper.getStyledName(object.getName());

		if (object.isTheorem()) {
			LabelHelper.appendAttrDelim(result);
			result.append("theorem", LabelHelper.ATTRIBUTE_STYLER);
		}

		return result;
	}
}
