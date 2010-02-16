/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.outline;

import org.eclipse.swt.graphics.Image;
import org.eventb.emf.core.EventBObject;
import org.eventb.emf.core.machine.Action;
import org.eventb.emf.core.machine.Event;
import org.eventb.emf.core.machine.Guard;
import org.eventb.emf.core.machine.Invariant;
import org.eventb.emf.core.machine.Machine;
import org.eventb.emf.core.machine.Variable;
import org.eventb.emf.core.machine.util.MachineSwitch;
import org.eventb.texteditor.ui.Images;

public class MachineImageSwitch extends MachineSwitch<Image> {

	@Override
	public Image caseEventBObject(final EventBObject object) {
		// TODO return generic image?
		return null;
	}

	@Override
	public Image caseMachine(final Machine object) {
		return Images.getImage(Images.IMG_MACHINE);
	}

	@Override
	public Image caseVariable(final Variable object) {
		return Images.getImage(Images.IMG_VARIABLE);
	}

	@Override
	public Image caseInvariant(final Invariant object) {
		if (object.isTheorem()) {
			return Images.getImage(Images.IMG_INVARIANT);
		} else {
			return Images.getImage(Images.IMG_THEOREM);
		}
	}

	@Override
	public Image caseEvent(final Event object) {
		return Images.getImage(Images.IMG_EVENT);
	}

	@Override
	public Image caseGuard(final Guard object) {
		return Images.getImage(Images.IMG_GUARD);
	}

	@Override
	public Image caseAction(final Action object) {
		return Images.getImage(Images.IMG_ACTION);
	}

	// TODO find images for these elements
	// @Override
	// public Image caseWitness(Witness object) {
	// return Images.getImage(Images.IMG_wi);
	// }
}
