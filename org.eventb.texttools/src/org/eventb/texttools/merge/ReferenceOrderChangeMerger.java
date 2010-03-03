package org.eventb.texttools.merge;

import java.util.List;

import org.eclipse.emf.compare.EMFComparePlugin;
import org.eclipse.emf.compare.FactoryException;
import org.eclipse.emf.compare.diff.merge.DefaultMerger;
import org.eclipse.emf.compare.diff.metamodel.ReferenceOrderChange;
import org.eclipse.emf.compare.util.EFactory;
import org.eclipse.emf.ecore.EObject;

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