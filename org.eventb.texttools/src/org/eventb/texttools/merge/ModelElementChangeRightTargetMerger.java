package org.eventb.texttools.merge;

import java.util.Iterator;

import org.eclipse.emf.compare.EMFComparePlugin;
import org.eclipse.emf.compare.FactoryException;
import org.eclipse.emf.compare.diff.merge.DefaultMerger;
import org.eclipse.emf.compare.diff.metamodel.DiffElement;
import org.eclipse.emf.compare.diff.metamodel.ModelElementChangeRightTarget;
import org.eclipse.emf.compare.diff.metamodel.ReferenceChangeRightTarget;
import org.eclipse.emf.compare.diff.metamodel.ReferenceOrderChange;
import org.eclipse.emf.compare.util.EFactory;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eventb.emf.core.context.Context;
import org.eventb.emf.core.machine.Event;
import org.eventb.emf.core.machine.Machine;

public class ModelElementChangeRightTargetMerger extends DefaultMerger {

	@Override
	public void applyInOrigin() {
		final ModelElementChangeRightTarget theDiff = (ModelElementChangeRightTarget) this.diff;
		final EObject origin = theDiff.getLeftParent();
		final EObject element = theDiff.getRightElement();

		// Scenarios where we need to handle special:
		// Machine sees Context

		// Machine refines Machine
		if (origin instanceof Machine && element instanceof Machine) {
			MergeUtil.copyMachineRef((Machine) origin, null, (Machine) element);
			// Context extends Context
		} else if (origin instanceof Context && element instanceof Context) {
			MergeUtil.copyContextRef((Context) origin, null, (Context) element);
			// New Event added
		} else if (origin instanceof Machine && element instanceof Event) {
			Event newOne = (Event) EcoreUtil.copy(element);
			final EReference ref = element.eContainmentFeature();
			if (ref != null) {
				try {
					EFactory.eAdd(origin, ref.getName(), newOne);
					setXMIID(newOne, getXMIID(element));
				} catch (final FactoryException e) {
					EMFComparePlugin.log(e, true);
				}
			} else {
				origin.eResource().getContents().add(newOne);
			}
		} else {
			originalApplyInOrigin();
		}
	}

	private void originalApplyInOrigin() {
		final ModelElementChangeRightTarget theDiff = (ModelElementChangeRightTarget) this.diff;
		final EObject origin = theDiff.getLeftParent();
		final EObject element = theDiff.getRightElement();
		final EObject newOne = copy(element);
		final EReference ref = element.eContainmentFeature();
		if (ref != null) {
			try {
				EFactory.eAdd(origin, ref.getName(), newOne);
				setXMIID(newOne, getXMIID(element));
			} catch (final FactoryException e) {
				EMFComparePlugin.log(e, true);
			}
		} else {
			origin.eResource().getContents().add(newOne);
		}
		// we should now have a look for AddReferencesLinks needing this object
		final Iterator<EObject> siblings = getDiffModel().eAllContents();
		while (siblings.hasNext()) {
			final DiffElement op = (DiffElement) siblings.next();
			if (op instanceof ReferenceChangeRightTarget) {
				final ReferenceChangeRightTarget link = (ReferenceChangeRightTarget) op;
				// now if I'm in the target References I should put my copy in
				// the origin
				if (link.getLeftTarget() != null
						&& link.getLeftTarget().equals(element)) {
					link.setRightTarget(newOne);
				}
			} else if (op instanceof ReferenceOrderChange) {
				final ReferenceOrderChange link = (ReferenceOrderChange) op;
				if (link.getReference().equals(ref)) {
					// FIXME respect ordering!
					link.getLeftTarget().add(newOne);
				}
			}
		}
		super.applyInOrigin();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.diff.merge.api.AbstractMerger#undoInTarget()
	 */
	@Override
	public void undoInTarget() {
		final ModelElementChangeRightTarget theDiff = (ModelElementChangeRightTarget) this.diff;
		final EObject element = theDiff.getRightElement();
		final EObject parent = theDiff.getRightElement().eContainer();
		EcoreUtil.remove(element);
		// now removes all the dangling references
		removeDanglingReferences(parent);
		super.undoInTarget();
	}

}
