package org.eventb.texttools.diffmerge;

import org.eclipse.emf.compare.AttributeChange;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.compare.merge.AbstractMerger;
import org.eclipse.emf.compare.merge.AttributeChangeMerger;
import org.eclipse.emf.compare.merge.ReferenceChangeMerger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eventb.emf.core.EventBObject;
import org.eventb.texttools.TextPositionUtil;

public class EventBMerger extends AbstractMerger {
	private AttributeChangeMerger am = new AttributeChangeMerger();
	private ReferenceChangeMerger rm = new ReferenceChangeMerger();
	
	@Override
	public boolean isMergerFor(Diff target) {
		//FIXME: could return true in all cases???
		if (target instanceof ReferenceChange) {
			ReferenceChange rtarget = (ReferenceChange) target;
			EReference reference = rtarget.getReference();
			if (reference.getName().equals("extends")
					|| reference.getName().equals("refines")
					|| reference.getName().equals("sees")) {
				return true;
			}
			//FIXME: find a way to avoid naming all core MM attributes
			//but test seems useless ... => always returns true?
			if (reference.getName().contains("extension")) {
				System.out.println("REFERENCE:"+reference.getName());
				return false;
			}
			return true;
		} else if (target instanceof AttributeChange)
			return true; 
		 System.out.println("TARGET:"+target);
		return false;
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
