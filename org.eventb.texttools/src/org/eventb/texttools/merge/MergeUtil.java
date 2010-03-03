package org.eventb.texttools.merge;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eventb.emf.core.EventBElement;
import org.eventb.emf.core.context.Context;
import org.eventb.emf.core.machine.Event;
import org.eventb.emf.core.machine.Machine;

public class MergeUtil {

	private MergeUtil() {
		throw new InstantiationError(
				"MergeUtil is not meant to be instantiated.");
	}

	static void copyMachineRef(final Machine leftParent,
			final Machine leftTarget, final Machine rightTarget) {
		if (leftTarget != null && leftTarget.eIsProxy()) {
			// simply rename in proxy
			leftTarget.setName(rightTarget.getName());
		} else {
			replaceInList(leftParent.getRefines(), leftTarget, rightTarget);
		}
	}

	static void copyEventRef(final Event leftParent, final Event leftTarget,
			final Event rightTarget) {
		if (leftTarget != null && leftTarget.eIsProxy()) {
			// simply rename in proxy
			leftTarget.setName(rightTarget.getName());
		} else {
			replaceInList(leftParent.getRefines(), leftTarget, rightTarget);
		}
	}

	static void copyContextRef(final EObject leftParent,
			final Context leftTarget, final Context rightTarget) {
		if (leftTarget != null && leftTarget.eIsProxy()) {
			// simply rename proxy
			leftTarget.setName(rightTarget.getName());
		} else {
			if (leftParent instanceof Machine) {
				// references is a sees in a machine
				replaceInList(((Machine) leftParent).getSees(), leftTarget,
						rightTarget);
			} else if (leftParent instanceof Context) {
				// references is a extends in context
				replaceInList(((Context) leftParent).getExtends(), leftTarget,
						rightTarget);
			}
		}
	}

	static <T extends EventBElement> void replaceInList(
			final EList<T> refinesList, final T leftTarget, final T rightTarget) {
		EObject newTarget = EcoreUtil.copy(rightTarget);
		if (leftTarget != null) {
			// search old position and replace
			for (int i = 0; i < refinesList.size(); i++) {
				if (refinesList.get(i) == leftTarget) {
					refinesList.set(i, (T) newTarget);
					break;
				}
			}
		} else {
			// just add reference
			// TODO Do we need to care about the position?
			refinesList.add((T) newTarget);
		}
	}

	/**
	 * Makes rightTarget a child of element, potentially displacing rightTarget.
	 * 
	 * @return true if a copy was performed
	 */
	static boolean rodinCopy(EObject element, EObject leftTarget,
			EObject rightTarget) {

		if (leftTarget instanceof Machine || rightTarget instanceof Machine) {
			// only case: refines attribute of a machine
			copyMachineRef((Machine) element, (Machine) leftTarget,
					(Machine) rightTarget);
			return true;
		} else if (leftTarget instanceof Context
				|| rightTarget instanceof Context) {
			copyContextRef(element, (Context) leftTarget, (Context) rightTarget);
			return true;
		} else if (leftTarget instanceof Event || rightTarget instanceof Event) {
			copyEventRef((Event) element, (Event) leftTarget,
					(Event) rightTarget);
			return true;
		}

		return false;
	}

}
