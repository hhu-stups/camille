package org.eventb.texttools.diffmerge;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.merge.AbstractMerger;

public class EventBMerger extends AbstractMerger {

	@Override
	public boolean isMergerFor(Diff target) {
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
		// FIXME: ??
		diff.copyRightToLeft();
	}
}
