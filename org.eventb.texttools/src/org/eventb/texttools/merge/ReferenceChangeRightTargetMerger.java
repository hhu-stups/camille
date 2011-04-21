/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texttools.merge;

import java.util.Iterator;
import java.util.List;

import org.eclipse.emf.compare.diff.merge.DefaultMerger;
import org.eclipse.emf.compare.diff.merge.service.MergeService;
import org.eclipse.emf.compare.diff.metamodel.DiffElement;
import org.eclipse.emf.compare.diff.metamodel.ReferenceChangeRightTarget;
import org.eclipse.emf.compare.diff.metamodel.ReferenceOrderChange;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;

public class ReferenceChangeRightTargetMerger extends DefaultMerger {

	@Override
	public void applyInOrigin() {
		final ReferenceChangeRightTarget theDiff = (ReferenceChangeRightTarget) diff;
		final EObject element = theDiff.getLeftElement();
		final EObject leftTarget = theDiff.getLeftTarget();
		final EObject rightTarget = theDiff.getRightTarget();
		System.out.println("ReferenceChangeRightTargetMerger.applyInOrigin");
		System.out.println("  element: " + element);
		System.out.println("  leftTarget: " + leftTarget);
		System.out.println("  rightTarget: " + rightTarget);

		boolean applied = MergeUtil.rodinCopy(element, leftTarget, rightTarget);
		System.out.println("  Applied: " + applied);

		if (!applied)
			originalApplyInOrigin();
	}

	private void originalApplyInOrigin() {
		final ReferenceChangeRightTarget theDiff = (ReferenceChangeRightTarget) this.diff;
		final EObject element = theDiff.getLeftElement();
		// (mj) In the past, we had right and left toggled.
		final EObject rightTarget = theDiff.getRightTarget();
		final EObject leftTarget = theDiff.getLeftTarget();

		// FIXME respect ordering!
		EReference reference = theDiff.getReference();

		// ordering handling:
		int index = -1;
		if (reference.isMany()) {

			EObject rightElement = theDiff.getRightElement();
			Object fightRefValue = rightElement.eGet(reference);
			if (fightRefValue instanceof List) {
				List refRightValueList = (List) fightRefValue;
				index = refRightValueList.indexOf(rightTarget);
			}
		}
		final EObject copiedValue = MergeService.getCopier(diff)
				.copyReferenceValue(reference, element, rightTarget,
						leftTarget, index);

		// We'll now look through this reference's eOpposite as they are already
		// taken care of
		final Iterator<EObject> related = getDiffModel().eAllContents();
		while (related.hasNext()) {
			final DiffElement op = (DiffElement) related.next();
			if (op instanceof ReferenceChangeRightTarget) {
				final ReferenceChangeRightTarget link = (ReferenceChangeRightTarget) op;
				// If this is my eOpposite, delete it from the DiffModel (merged
				// along with this one)
				if (link.getReference().equals(
						theDiff.getReference().getEOpposite())
						&& link.getRightTarget().equals(element)) {
					removeFromContainer(link);
				}
			} else if (op instanceof ReferenceOrderChange) {
				final ReferenceOrderChange link = (ReferenceOrderChange) op;
				if (link.getReference().equals(theDiff.getReference())) {
					// FIXME respect ordering!
					link.getLeftTarget().add(copiedValue);
				}
			}
		}
		super.applyInOrigin();
	}

	@Override
	public void undoInTarget() {
		throw new RuntimeException();
	}

	// /**
	// * {@inheritDoc}
	// *
	// * @see
	// org.eclipse.emf.compare.diff.merge.api.AbstractMerger#undoInTarget()
	// */
	// @Override
	// public void undoInTarget() {
	// final ReferenceChangeRightTarget theDiff = (ReferenceChangeRightTarget)
	// this.diff;
	// final EObject element = theDiff.getRightElement();
	// final EObject rightTarget = theDiff.getLeftTarget();
	// try {
	// EFactory.eRemove(element, theDiff.getReference().getName(),
	// rightTarget);
	// } catch (final FactoryException e) {
	// EMFComparePlugin.log(e, true);
	// }
	// // we should now have a look for AddReferencesLinks needing this object
	// final Iterator<EObject> related = getDiffModel().eAllContents();
	// while (related.hasNext()) {
	// final DiffElement op = (DiffElement) related.next();
	// if (op instanceof ReferenceChangeRightTarget) {
	// final ReferenceChangeRightTarget link = (ReferenceChangeRightTarget) op;
	// // now if I'm in the target References I should put my copy in
	// // the origin
	// if (link.getReference().equals(
	// theDiff.getReference().getEOpposite())
	// && link.getLeftTarget().equals(element)) {
	// removeFromContainer(link);
	// }
	// } else if (op instanceof ResourceDependencyChange) {
	// final ResourceDependencyChange link = (ResourceDependencyChange) op;
	// final Resource res = link.getRoots().get(0).eResource();
	// if (res == rightTarget.eResource()) {
	// EcoreUtil.remove(link);
	// res.unload();
	// }
	// }
	// }
	// super.undoInTarget();
	// }

}
