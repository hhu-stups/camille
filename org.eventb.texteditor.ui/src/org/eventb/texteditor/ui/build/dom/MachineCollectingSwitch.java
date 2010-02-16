/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.build.dom;

import org.eclipse.core.runtime.Assert;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.resource.Resource;
import org.eventb.emf.core.EventBObject;
import org.eventb.emf.core.context.Context;
import org.eventb.emf.core.machine.Event;
import org.eventb.emf.core.machine.Machine;
import org.eventb.emf.core.machine.Variable;
import org.eventb.emf.core.machine.util.MachineSwitch;

public class MachineCollectingSwitch extends MachineSwitch<Boolean> {
	private final MachineDom machineDom;
	private IDom currentParent = null;
	private final Resource machineResource;

	protected MachineCollectingSwitch(final MachineDom dom,
			final Resource machineResource) {
		machineDom = dom;
		this.machineResource = machineResource;
	}

	protected AbstractDom getDom() {
		return machineDom;
	}

	public IDom getCurrentParentDom() {
		return currentParent;
	}

	@Override
	public Boolean caseEventBObject(final EventBObject object) {
		return true;
	}

	@Override
	public Boolean caseMachine(final Machine object) {
		machineDom.setMachine(object);

		final EList<Machine> refines = object.getRefines();
		for (final Machine machine : refines) {
			final IComponentDom dom = DomBuilder.getReferencedDom(machine,
					machineResource);

			if (dom != null) {
				Assert.isTrue(dom instanceof MachineDom);
				machineDom.addRefinedMachine((MachineDom) dom);
			}

			/*
			 * If no DOM is found the reference must be to a not-existing
			 * machine.
			 */
		}

		final EList<Context> sees = object.getSees();
		for (final Context context : sees) {
			final IComponentDom dom = DomBuilder.getReferencedDom(context,
					machineResource);

			if (dom != null) {
				Assert.isTrue(dom instanceof ContextDom);
				machineDom.addSeenContext((ContextDom) dom);
			}

			/*
			 * If no DOM is found the reference must be to a not-existing
			 * machine.
			 */
		}

		currentParent = machineDom;

		return true;
	}

	@Override
	public Boolean caseVariable(final Variable object) {
		machineDom.addVariable(object);
		return false;
	}

	@Override
	public Boolean caseEvent(final Event object) {
		final EventDom eventDom = new EventDom(object, machineDom);
		eventDom.addAllRefinedEvent(object.getRefines());
		eventDom.addAllParameters(object.getParameters());
		machineDom.addChild(eventDom);

		currentParent = eventDom;

		return true;
	}
}
