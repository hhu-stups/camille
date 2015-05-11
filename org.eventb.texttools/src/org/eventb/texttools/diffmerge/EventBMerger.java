package org.eventb.texttools.diffmerge;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.compare.merge.AbstractMerger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eventb.emf.core.context.Context;
import org.eventb.emf.core.machine.Event;
import org.eventb.emf.core.machine.Machine;

public class EventBMerger extends AbstractMerger {
	@Override
	public boolean isMergerFor(Diff target) {
		// System.out.println(target);
		if (target instanceof ReferenceChange) {
			ReferenceChange rtarget = (ReferenceChange) target;
			EReference reference = rtarget.getReference();
			if (reference.getName().equals("extends")
					|| reference.getName().equals("refines")
					|| reference.getName().equals("sees")) {
				return true;
			}
		}
		return false; // currently disabled
	}

	@Override
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
		if (left instanceof Context) {
			Context leftC = (Context) left;
			Context rightC = (Context) right;
			leftC.getExtends().clear();
			leftC.getExtends().addAll(rightC.getExtends());
		}

		if (left instanceof Machine) {
			Machine leftC = (Machine) left;
			Machine rightC = (Machine) right;
			leftC.getRefines().clear();
			leftC.getRefines().addAll(rightC.getRefines());
			leftC.getSees().clear();
			leftC.getSees().addAll(rightC.getSees());
		}

		if (left instanceof Event) {
			Event leftC = (Event) left;
			Event rightC = (Event) right;
			leftC.getRefines().clear();
			leftC.getRefines().addAll(rightC.getRefines());
		}
	}
}
