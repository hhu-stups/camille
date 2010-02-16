/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.outline;

import org.eclipse.emf.common.util.EList;
import org.eclipse.jface.viewers.StyledString;
import org.eventb.emf.core.EventBNamed;
import org.eventb.emf.core.machine.Event;
import org.eventb.emf.core.machine.Invariant;
import org.eventb.emf.core.machine.Machine;
import org.eventb.emf.core.machine.Variant;
import org.eventb.emf.core.machine.util.MachineSwitch;

public class MachineLabelSwitch extends MachineSwitch<StyledString> {

	@Override
	public StyledString caseEventBNamed(final EventBNamed object) {
		/*
		 * Default method for all EventBNamedElements that are not handled
		 * seperately in their own methods.
		 */
		return LabelHelper.getStyledName(object.getName());
	}

	@Override
	public StyledString caseMachine(final Machine object) {
		final StyledString result = LabelHelper.getStyledName(object.getName());

		final EList<String> refinesNames = object.getRefinesNames();
		if (refinesNames.size() > 0) {
			LabelHelper.appendAttrDelim(result);
			result.append("refines ", LabelHelper.ATTRIBUTE_STYLER);
			result.append(LabelHelper.joinEList(refinesNames),
					LabelHelper.ATTRIBUTE_STYLER);
		}

		final EList<String> seesNames = object.getSeesNames();
		if (seesNames.size() > 0) {
			LabelHelper.appendAttrDelim(result);
			result.append("sees ", LabelHelper.ATTRIBUTE_STYLER);
			result.append(LabelHelper.joinEList(seesNames),
					LabelHelper.ATTRIBUTE_STYLER);
		}

		return result;
	}

	@Override
	public StyledString caseInvariant(final Invariant object) {
		final StyledString result = LabelHelper.getStyledName(object.getName());

		if (object.isTheorem()) {
			LabelHelper.appendAttrDelim(result);
			result.append("theorem", LabelHelper.ATTRIBUTE_STYLER);
		}

		return result;
	}

	@Override
	public StyledString caseVariant(final Variant object) {
		return LabelHelper.getStyledName("Variant");
	}

	@Override
	public StyledString caseEvent(final Event object) {
		final StyledString result = LabelHelper.getStyledName(object.getName());

		if (object.getParameters().size() > 0) {
			final String paramString = LabelHelper.joinEList(object
					.getParameters());
			result.append(" (" + paramString + ")");
		}

		final EList<String> refinesNames = object.getRefinesNames();
		if (object.isExtended()) {
			LabelHelper.appendAttrDelim(result);
			result.append("extends ", LabelHelper.ATTRIBUTE_STYLER);

			if (refinesNames.size() > 0) {
				result
						.append(refinesNames.get(0),
								LabelHelper.ATTRIBUTE_STYLER);
			} else {
				result.append("?", LabelHelper.ATTRIBUTE_STYLER);
			}
		} else {
			if (refinesNames.size() > 0) {
				LabelHelper.appendAttrDelim(result);
				result.append("refines ", LabelHelper.ATTRIBUTE_STYLER);
				result.append(LabelHelper.joinEList(refinesNames),
						LabelHelper.ATTRIBUTE_STYLER);
			}
		}

		return result;
	}
}
