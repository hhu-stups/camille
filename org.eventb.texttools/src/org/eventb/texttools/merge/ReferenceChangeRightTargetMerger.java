/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texttools.merge;

import java.util.Iterator;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.compare.EMFComparePlugin;
import org.eclipse.emf.compare.FactoryException;
import org.eclipse.emf.compare.diff.merge.DefaultMerger;
import org.eclipse.emf.compare.diff.merge.service.MergeService;
import org.eclipse.emf.compare.diff.metamodel.DiffElement;
import org.eclipse.emf.compare.diff.metamodel.ReferenceChangeRightTarget;
import org.eclipse.emf.compare.util.EFactory;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eventb.emf.core.EventBElement;
import org.eventb.emf.core.EventBNamedCommentedElement;
import org.eventb.emf.core.context.Context;
import org.eventb.emf.core.machine.Event;
import org.eventb.emf.core.machine.Machine;

public class ReferenceChangeRightTargetMerger extends DefaultMerger {
	@Override
	public void applyInOrigin() {
		final ReferenceChangeRightTarget theDiff = (ReferenceChangeRightTarget) diff;
		final EObject element = theDiff.getLeftElement();
		final EReference reference = theDiff.getReference();

		final EObject leftTarget = theDiff.getLeftTarget();
		EObject rightTarget = theDiff.getRightTarget();


		if (leftTarget instanceof Machine || rightTarget instanceof Machine) {
			// only case: refines attribute of a machine
			copyMachineRef((Machine) element, (Machine) leftTarget,
					(Machine) rightTarget);
		} else if (leftTarget instanceof Context
				|| rightTarget instanceof Context) {
			copyContextRef(element, (Context) leftTarget, (Context) rightTarget);
		} else if (leftTarget instanceof Event || rightTarget instanceof Event) {
			copyEventRef((Event) element, (Event) leftTarget,
					(Event) rightTarget);
		} else {
			/*
			 * Default implementation copied from
			 * ReferenceChangeRightTargetMerger (in internal package of
			 * compare), leave as is...
			 */
			MergeService.getCopier(diff).copyReferenceValue(reference, element,
					rightTarget, leftTarget);

			// We'll now look through this reference's eOpposite as they are
			// already
			// taken care of
			final Iterator<EObject> siblings = getDiffModel().eAllContents();
			while (siblings.hasNext()) {
				final DiffElement op = (DiffElement) siblings.next();
				if (op instanceof ReferenceChangeRightTarget) {
					final ReferenceChangeRightTarget link = (ReferenceChangeRightTarget) op;
					// If this is my eOpposite, delete it from the DiffModel
					// (merged
					// along with this one)
					if (link.getReference().equals(reference.getEOpposite())
							&& link.getLeftTarget().equals(element)) {
						removeFromContainer(link);
					}
				}
			}
		}

		super.applyInOrigin();
	}

	private void printInfo(String label, EObject target, EObject parent) {
		System.out.println("====== " + label + " ========");
		if (!(target instanceof EventBNamedCommentedElement)) {
			System.out.println("  Target not analysed: " + target);

		} else {
			EventBNamedCommentedElement eventTarget = (EventBNamedCommentedElement) target;
			System.out.println("  Target: " + eventTarget.doGetName()
					+ " (proxy: " + eventTarget.eIsProxy() + ")");
			if (target instanceof Event) {
				System.out.println("  Refines: "
						+ ((Event) target).getRefinesNames());
			}
			System.out.println("  Resource: " + eventTarget.eResource());
		}

		if (!(parent instanceof EventBNamedCommentedElement)) {
			System.out.println("  Element not analysed: " + parent);

		} else {
			EventBNamedCommentedElement eventElement = (EventBNamedCommentedElement) parent;
			System.out.println("  Element: " + eventElement.doGetName()
					+ " (proxy: " + eventElement.eIsProxy() + ")");
			if (parent instanceof Event) {
				System.out.println("  Refines: "
						+ ((Event) parent).getRefinesNames());
			}
			System.out.println("  Resource: " + eventElement.eResource());
		}
	}

	private void copyMachineRef(final Machine leftParent,
			final Machine leftTarget, final Machine rightTarget) {
		if (leftTarget != null && leftTarget.eIsProxy()) {
			// simply rename in proxy
			leftTarget.setName(rightTarget.getName());
		} else {
			replaceInList(leftParent.getRefines(), leftTarget, rightTarget);
		}
	}

	private void copyEventRef(final Event leftParent, final Event leftTarget,
			final Event rightTarget) {
		if (leftTarget != null && leftTarget.eIsProxy()) {
			// simply rename in proxy
			leftTarget.setName(rightTarget.getName());
		} else {
			replaceInList(leftParent.getRefines(), leftTarget, rightTarget);
		}
	}

	private void copyContextRef(final EObject leftParent,
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

	@Override
	public void undoInTarget() {
		final ReferenceChangeRightTarget theDiff = (ReferenceChangeRightTarget) diff;
		final EObject element = theDiff.getRightElement();
		final EObject rightTarget = theDiff.getRightTarget();

		/*
		 * Default implementation copied from ReferenceChangeRightTargetMerger
		 * (in internal package of compare), leave as is...
		 */
		try {
			EFactory.eRemove(element, theDiff.getReference().getName(),
					rightTarget);
		} catch (final FactoryException e) {
			EMFComparePlugin.log(e, true);
		}

		// we should now have a look for AddReferencesLinks needing this object
		final Iterator<EObject> siblings = getDiffModel().eAllContents();
		while (siblings.hasNext()) {
			final DiffElement op = (DiffElement) siblings.next();
			if (op instanceof ReferenceChangeRightTarget) {
				final ReferenceChangeRightTarget link = (ReferenceChangeRightTarget) op;
				// now if I'm in the target References I should put my copy in
				// the origin
				if (link.getReference().equals(
						theDiff.getReference().getEOpposite())
						&& link.getRightTarget().equals(element)) {
					removeFromContainer(link);
				}
			}
		}

		super.undoInTarget();
	}

}
