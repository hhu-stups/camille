package org.eventb.texttools.merge;

import java.util.Iterator;

import org.eclipse.emf.compare.EMFComparePlugin;
import org.eclipse.emf.compare.FactoryException;
import org.eclipse.emf.compare.diff.merge.DefaultMerger;
import org.eclipse.emf.compare.diff.merge.service.MergeService;
import org.eclipse.emf.compare.diff.metamodel.DiffElement;
import org.eclipse.emf.compare.diff.metamodel.ReferenceChangeLeftTarget;
import org.eclipse.emf.compare.diff.metamodel.ReferenceOrderChange;
import org.eclipse.emf.compare.diff.metamodel.ResourceDependencyChange;
import org.eclipse.emf.compare.util.EFactory;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;

/**
 * Merger for an {@link ReferenceChangeLeftTarget} operation.<br/>
 * <p>
 * Are considered for this merger :
 * <ul>
 * <li>AddReferenceValue</li>
 * <li>RemoteRemoveReferenceValue</li>
 * </ul>
 * </p>
 * 
 * @author <a href="mailto:laurent.goubet@obeo.fr">Laurent Goubet</a>
 */
public class ReferenceChangeLeftTargetMerger extends DefaultMerger {
	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.diff.merge.DefaultMerger#applyInOrigin()
	 */
	@Override
	public void applyInOrigin() {
		final ReferenceChangeLeftTarget theDiff = (ReferenceChangeLeftTarget) this.diff;
		final EObject element = theDiff.getLeftElement();
		final EObject leftTarget = theDiff.getRightTarget();
		try {
			if (leftTarget != null) {
				EFactory.eRemove(element, theDiff.getReference().getName(),
						leftTarget);
			} else {
				// (mj) START
				EFactory.eRemove(element, theDiff.getReference().getName(),
						theDiff.getLeftTarget());
				// (mj) END
			}

		} catch (final FactoryException e) {
			EMFComparePlugin.log(e, true);
		}
		// we should now have a look for AddReferencesLinks needing this object
		final Iterator<EObject> siblings = getDiffModel().eAllContents();
		while (siblings.hasNext()) {
			final DiffElement op = (DiffElement) siblings.next();
			if (op instanceof ReferenceChangeLeftTarget) {
				final ReferenceChangeLeftTarget link = (ReferenceChangeLeftTarget) op;
				// now if I'm in the target References I should put my copy in
				// the origin
				if (link.getReference().equals(
						theDiff.getReference().getEOpposite())
						&& link.getRightTarget().equals(element)) {
					removeFromContainer(link);
				}
			} else if (op instanceof ResourceDependencyChange) {
				final ResourceDependencyChange link = (ResourceDependencyChange) op;
				final Resource res = link.getRoots().get(0).eResource();
				if (res == leftTarget.eResource()) {
					EcoreUtil.remove(link);
					res.unload();
				}
			}
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
		final ReferenceChangeLeftTarget theDiff = (ReferenceChangeLeftTarget) this.diff;
		final EObject element = theDiff.getRightElement();
		final EObject leftTarget = theDiff.getRightTarget();
		final EObject rightTarget = theDiff.getLeftTarget();
		// FIXME respect ordering!
		final EObject copiedValue = MergeService.getCopier(diff)
				.copyReferenceValue(theDiff.getReference(), element,
						leftTarget, rightTarget);

		// we should now have a look for AddReferencesLinks needing this object
		final Iterator<EObject> siblings = getDiffModel().eAllContents();
		while (siblings.hasNext()) {
			final DiffElement op = (DiffElement) siblings.next();
			if (op instanceof ReferenceChangeLeftTarget) {
				final ReferenceChangeLeftTarget link = (ReferenceChangeLeftTarget) op;
				// now if I'm in the target References I should put my copy in
				// the origin
				if (link.getReference().equals(
						theDiff.getReference().getEOpposite())
						&& link.getLeftTarget().equals(element)) {
					removeFromContainer(link);
				}
			} else if (op instanceof ReferenceOrderChange) {
				final ReferenceOrderChange link = (ReferenceOrderChange) op;
				if (link.getReference().equals(theDiff.getReference())) {
					// FIXME respect ordering!
					link.getRightTarget().add(copiedValue);
				}
			}
		}
		super.undoInTarget();
	}
}
