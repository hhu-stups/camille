package org.eventb.texttools.merge;

import java.util.List;

import org.eclipse.emf.compare.EMFComparePlugin;
import org.eclipse.emf.compare.FactoryException;
import org.eclipse.emf.compare.diff.metamodel.ReferenceOrderChange;
import org.eclipse.emf.compare.util.EFactory;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eventb.emf.core.context.Context;
import org.eventb.emf.core.machine.Event;
import org.eventb.emf.core.machine.Machine;
import org.eventb.texttools.Constants;

public class ReferenceOrderChangeMerger extends DefaultMerger {
	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.diff.merge.DefaultMerger#applyInOrigin()
	 */
	@Override
	public void applyInOrigin() {
		final ReferenceOrderChange theDiff = (ReferenceOrderChange) this.diff;
		final EObject element = theDiff.getLeftElement();
		final List<EObject> leftTarget = theDiff.getLeftTarget();

		// (mj) START
		if (leftTarget.size() == 0) {

			// Handle Event Refinement changed
			if (element instanceof Event
					&& theDiff.getReference().getName().equals(
							Constants.REFINES)) {
				Event leftEvent = (Event) element;
				Event rightEvent = (Event) theDiff.getRightElement();
				leftEvent.getRefines().clear();
				leftEvent.getRefines().addAll(
						EcoreUtil.copyAll(rightEvent.getRefines()));
			} else if (element instanceof Machine) {
				Machine leftMachine = (Machine) element;
				Machine rightMachine = (Machine) theDiff.getRightElement();
				if (theDiff.getReference().getName().equals(Constants.REFINES)) {
					leftMachine.getRefines().clear();
					leftMachine.getRefines().addAll(
							EcoreUtil.copyAll(rightMachine.getRefines()));
				} else if (theDiff.getReference().getName().equals(
						Constants.SEES)) {
					leftMachine.getSees().clear();
					leftMachine.getSees().addAll(
							EcoreUtil.copyAll(rightMachine.getSees()));
				}
			} else if (element instanceof Context
					&& theDiff.getReference().getName().equals(
							Constants.EXTENDS)) {
				Context leftContext = (Context) element;
				Context rightContext = (Context) theDiff.getRightElement();
				leftContext.getExtends().clear();
				leftContext.getExtends().addAll(
						EcoreUtil.copyAll(rightContext.getExtends()));
			}

			super.applyInOrigin();
			return;
		}
		// (mj) END

		try {
			EFactory
					.eSet(element, theDiff.getReference().getName(), leftTarget);
		} catch (final FactoryException e) {
			EMFComparePlugin.log(e, true);
		}
		super.applyInOrigin();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.diff.merge.DefaultMerger#undoInTarget()
	 */
	@Override
	public void undoInTarget() {
		final ReferenceOrderChange theDiff = (ReferenceOrderChange) this.diff;
		final EObject element = theDiff.getRightElement();
		final List<EObject> rightTarget = theDiff.getRightTarget();
		try {
			EFactory.eSet(element, theDiff.getReference().getName(),
					rightTarget);
		} catch (final FactoryException e) {
			EMFComparePlugin.log(e, true);
		}
		super.undoInTarget();
	}

}