package org.eventb.texttools.diffmerge;

import org.eclipse.emf.compare.AttributeChange;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.compare.merge.AbstractMerger;
import org.eclipse.emf.compare.merge.AttributeChangeMerger;
import org.eclipse.emf.compare.merge.ReferenceChangeMerger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eventb.emf.core.EventBObject;
import org.eventb.texttools.TextPositionUtil;

public class EventBMerger extends AbstractMerger {
	private AttributeChangeMerger am = new AttributeChangeMerger();
	// replacement of emfcompare version (solves a bug with multivalued references
	private MyReferenceChangeMerger rm = new MyReferenceChangeMerger();
	
	@Override
	public boolean isMergerFor(Diff target) {
		EStructuralFeature thing;
		if (target instanceof ReferenceChange) {
			ReferenceChange rc = (ReferenceChange) target;
			thing = rc.getReference();
		} else if (target instanceof AttributeChange) {
			AttributeChange ac = (AttributeChange) target;
			thing = ac.getAttribute();
		} else {
			return false;
		}
		String namespaceURI = thing.getEContainingClass().getEPackage().getNsURI();
		return namespaceURI.startsWith("http://emf.eventb.org/models/core/");

	}

	//@Override
	protected void accept(final Diff diff, boolean rightToLeft) {
		// Maybe we do not need this method for Camille?
		throw new UnsupportedOperationException();
	}

	@Override
	protected void reject(final Diff diff, boolean rightToLeft) {
		// do we always merge right to left in Camille?
		assert (rightToLeft);

		if (!isMergerFor(diff)) {
			super.reject(diff, rightToLeft);
			return;
		}

		EObject left = diff.getMatch().getLeft();
		EObject right = diff.getMatch().getRight();

		if (left == null) {
			super.reject(diff, rightToLeft);
			return;
		}
//TODO: find insertion index to get more precise annotations when multiplicity is > 1
		if (left instanceof EventBObject)
			TextPositionUtil.annotatePosition((EventBObject)left, TextPositionUtil.getTextRange((EventBObject)right));

		if (rm.isMergerFor(diff)) {
			ReferenceChange d = (ReferenceChange) diff;
			//System.out.println("REF CHANGE:"+d.getReference());
			//System.out.println("REF VALUE:"+ d.getValue());
			rm.copyRightToLeft(diff,null);
			Object l = left.eGet(d.getReference());
			Object r = right.eGet(d.getReference());
			if (l instanceof EventBObject && r instanceof EventBObject)
				TextPositionUtil.annotatePosition((EventBObject)l, TextPositionUtil.getTextRange((EventBObject)r));
			return;
		}

		if (am.isMergerFor(diff)) {
			AttributeChange d = (AttributeChange) diff;
			//System.out.println("ATTR CHANGE:"+d.getAttribute());
			//System.out.println("ATTR VALUE:"+ d.getValue());
			am.copyRightToLeft(diff,null);
			Object l = left.eGet(d.getAttribute());
			Object r = right.eGet(d.getAttribute());
			if (l instanceof EventBObject && r instanceof EventBObject)
				TextPositionUtil.annotatePosition((EventBObject)l, TextPositionUtil.getTextRange((EventBObject)r));
			return;
		}

		System.out.println("DIFF:"+diff.getClass()+"  LEFT:"+left+ "  -  RIGHT:"+right);
		
	}
}
