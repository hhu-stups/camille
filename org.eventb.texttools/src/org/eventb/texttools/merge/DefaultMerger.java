package org.eventb.texttools.merge;

import org.eclipse.emf.compare.diff.metamodel.DiffElement;
import org.eclipse.emf.compare.diff.metamodel.DiffGroup;
import org.eclipse.emf.compare.diff.metamodel.ModelElementChangeLeftTarget;
import org.eclipse.emf.compare.diff.metamodel.ModelElementChangeRightTarget;
import org.eclipse.emf.compare.diff.metamodel.ReferenceChange;
import org.eclipse.emf.compare.diff.metamodel.ReferenceChangeLeftTarget;
import org.eclipse.emf.compare.diff.metamodel.ReferenceChangeRightTarget;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EGenericType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcorePackage;

public class DefaultMerger extends
		org.eclipse.emf.compare.diff.merge.DefaultMerger {

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.diff.merge.IMerger#applyInOrigin()
	 */
	public void applyInOrigin() {
		handleMutuallyDerivedReferences();
		removeFromContainer(diff);
	}

	/*
	 * This function has been copied from emf-compare version 1.1.0M7 as a
	 * bugfix for https://bugs.eclipse.org/bugs/show_bug.cgi?id=288659
	 */
	private void handleMutuallyDerivedReferences() {
		DiffElement toRemove = null;
		if (diff instanceof ReferenceChange) {
			final EReference reference = ((ReferenceChange) diff)
					.getReference();
			if (reference == EcorePackage.eINSTANCE.getEClass_ESuperTypes()) {
				final EObject referenceType;
				if (diff instanceof ReferenceChangeLeftTarget) {
					referenceType = ((ReferenceChangeLeftTarget) diff)
							.getRightTarget();
				} else {
					referenceType = ((ReferenceChangeRightTarget) diff)
							.getLeftTarget();
				}
				for (final DiffElement siblingDiff : ((DiffGroup) diff
						.eContainer()).getSubDiffElements()) {
					if (siblingDiff instanceof ModelElementChangeLeftTarget) {
						if (((ModelElementChangeLeftTarget) siblingDiff)
								.getLeftElement() instanceof EGenericType
								&& ((EGenericType) ((ModelElementChangeLeftTarget) siblingDiff)
										.getLeftElement()).getEClassifier() == referenceType) {
							toRemove = siblingDiff;
							break;
						}
					} else if (siblingDiff instanceof ModelElementChangeRightTarget) {
						if (((ModelElementChangeRightTarget) siblingDiff)
								.getRightElement() instanceof EGenericType
								&& ((EGenericType) ((ModelElementChangeRightTarget) siblingDiff)
										.getRightElement()).getEClassifier() == referenceType) {
							toRemove = siblingDiff;
							break;
						}
					}
				}
			}
		} else if (diff instanceof ModelElementChangeLeftTarget
				&& ((ModelElementChangeLeftTarget) diff).getLeftElement() instanceof EGenericType) {
			final ModelElementChangeLeftTarget theDiff = (ModelElementChangeLeftTarget) diff;
			final EClassifier referenceType = ((EGenericType) theDiff
					.getLeftElement()).getEClassifier();
			for (final DiffElement siblingDiff : ((DiffGroup) diff.eContainer())
					.getSubDiffElements()) {
				if (siblingDiff instanceof ReferenceChangeLeftTarget
						&& ((ReferenceChangeLeftTarget) siblingDiff)
								.getReference().getFeatureID() == EcorePackage.ECLASS__ESUPER_TYPES) {
					if (((ReferenceChangeLeftTarget) siblingDiff)
							.getRightTarget() == referenceType) {
						toRemove = siblingDiff;
						break;
					}
				}
			}
		} else if (diff instanceof ModelElementChangeRightTarget
				&& ((ModelElementChangeRightTarget) diff).getRightElement() instanceof EGenericType) {
			final ModelElementChangeRightTarget theDiff = (ModelElementChangeRightTarget) diff;
			final EClassifier referenceType = ((EGenericType) theDiff
					.getRightElement()).getEClassifier();
			for (final DiffElement siblingDiff : ((DiffGroup) diff.eContainer())
					.getSubDiffElements()) {
				if (siblingDiff instanceof ReferenceChangeRightTarget
						&& ((ReferenceChangeRightTarget) siblingDiff)
								.getReference().getFeatureID() == EcorePackage.ECLASS__ESUPER_TYPES) {
					if (((ReferenceChangeRightTarget) siblingDiff)
							.getLeftTarget() == referenceType) {
						toRemove = siblingDiff;
						break;
					}
				}
			}
		}
		if (toRemove != null) {
			removeFromContainer(toRemove);
		}
	}

}
