/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.outline;

import org.eclipse.swt.graphics.Image;
import org.eventb.emf.core.EventBObject;
import org.eventb.emf.core.context.Axiom;
import org.eventb.emf.core.context.CarrierSet;
import org.eventb.emf.core.context.Constant;
import org.eventb.emf.core.context.Context;
import org.eventb.emf.core.context.util.ContextSwitch;
import org.eventb.texteditor.ui.Images;

public class ContextImageSwitch extends ContextSwitch<Image> {

	@Override
	public Image caseEventBObject(final EventBObject object) {
		// TODO return generic image?
		return null;
	}

	@Override
	public Image caseContext(final Context object) {
		return Images.getImage(Images.IMG_CONTEXT);
	}

	@Override
	public Image caseConstant(final Constant object) {
		return Images.getImage(Images.IMG_CONSTANT);
	}

	@Override
	public Image caseCarrierSet(final CarrierSet object) {
		return Images.getImage(Images.IMG_CARRIER_SET);
	}

	@Override
	public Image caseAxiom(final Axiom object) {
		return Images.getImage(Images.IMG_AXIOM);
	}
}
