/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.outline;

import org.eventb.emf.core.EventBCommented;
import org.eventb.emf.core.EventBElement;
import org.eventb.emf.core.EventBNamed;
import org.eventb.emf.core.EventBObject;
import org.eventb.emf.core.machine.Variant;
import org.eventb.emf.core.machine.util.MachineSwitch;
import org.eventb.texttools.Constants;
import org.eventb.texttools.TextPositionUtil;
import org.eventb.texttools.model.texttools.TextRange;

public class MachineNavigationProvider extends MachineSwitch<TextRange>
		implements INavigationProvider {

	public TextRange getHighlightRange(final EventBObject element) {
		return doSwitch(element);
	}

	@Override
	public TextRange caseEventBObject(final EventBObject object) {
		/*
		 * Default for all elments that are not explicitely handled. We create a
		 * new copy here so it's save to change the range.
		 */
		return TextPositionUtil.createTextRange(object);
	}

	@Override
	public TextRange caseEventBElement(final EventBElement object) {
		final TextRange range = caseEventBObject(object);

		/*
		 * Correct offset if there is a comment as we don't want to highlight
		 * the comment.
		 */
		if (object instanceof EventBCommented) {
			final String comment = ((EventBCommented) object).getComment();

			if (comment != null && comment.length() > 0) {
				TextPositionUtil.correctStartOffset(range, comment.length());
			}
		}

		return range;
	}

	@Override
	public TextRange caseVariant(final Variant object) {
		final TextRange range = caseEventBElement(object);

		if (range != null) {
			TextPositionUtil.correctStartOffset(range, Constants.VARIANT
					.length());
		}

		return range;
	}

	@Override
	public TextRange caseEventBNamed(final EventBNamed object) {
		TextRange range = TextPositionUtil.getInternalPosition(
				(EventBElement) object, object.getName());

		if (range == null) {
			range = caseEventBElement((EventBElement) object);

			if (range != null) {
				range.setLength(1);
			}
		}

		return range;
	}
}
