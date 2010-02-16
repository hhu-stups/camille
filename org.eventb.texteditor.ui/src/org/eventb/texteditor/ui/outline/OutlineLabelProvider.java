/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.outline;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eventb.emf.core.EventBObject;
import org.eventb.emf.core.context.ContextPackage;
import org.eventb.emf.core.machine.MachinePackage;

public class OutlineLabelProvider extends LabelProvider implements
		IStyledLabelProvider {

	private MachineLabelSwitch machineLabelSwitch;
	private ContextLabelSwitch contextLabelSwitch;
	private MachineImageSwitch machineImageSwitch;
	private ContextImageSwitch contextImageSwitch;

	@Override
	public String getText(final Object element) {
		return getStyledText(element).getString();
	}

	public StyledString getStyledText(final Object element) {
		if (element instanceof EventBObject) {
			final EventBObject emfObject = (EventBObject) element;
			final String packageNsURI = emfObject.eClass().getEPackage()
					.getNsURI();

			if (packageNsURI.equals(MachinePackage.eNS_URI)) {
				return getTextsForMachine(emfObject);
			} else if (packageNsURI.equals(ContextPackage.eNS_URI)) {
				return getTextsForContext(emfObject);
			}
		}

		// we can only handle EventBObjects here, use default otherwise
		return new StyledString(super.getText(element));
	}

	@Override
	public Image getImage(final Object element) {
		if (element instanceof EventBObject) {
			final EventBObject emfObject = (EventBObject) element;
			final String packageNsURI = emfObject.eClass().getEPackage()
					.getNsURI();

			if (packageNsURI.equals(MachinePackage.eNS_URI)) {
				return getImageForMachine(emfObject);
			} else if (packageNsURI.equals(ContextPackage.eNS_URI)) {
				return getImageForContext(emfObject);
			}
		}

		return super.getImage(element);
	}

	private Image getImageForMachine(final EventBObject emfObject) {
		if (machineImageSwitch == null) {
			machineImageSwitch = new MachineImageSwitch();
		}

		return machineImageSwitch.doSwitch(emfObject);
	}

	private Image getImageForContext(final EventBObject emfObject) {
		if (contextImageSwitch == null) {
			contextImageSwitch = new ContextImageSwitch();
		}

		return contextImageSwitch.doSwitch(emfObject);
	}

	private StyledString getTextsForContext(final EventBObject emfObject) {
		if (contextLabelSwitch == null) {
			contextLabelSwitch = new ContextLabelSwitch();
		}

		return contextLabelSwitch.doSwitch(emfObject);
	}

	private StyledString getTextsForMachine(final EventBObject emfObject) {
		if (machineLabelSwitch == null) {
			machineLabelSwitch = new MachineLabelSwitch();
		}

		return machineLabelSwitch.doSwitch(emfObject);
	}
}
