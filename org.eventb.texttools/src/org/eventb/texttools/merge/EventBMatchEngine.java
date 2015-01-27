package org.eventb.texttools.merge;

import org.eclipse.emf.compare.match.DefaultMatchEngine;
import org.eclipse.emf.compare.match.IComparisonFactory;
import org.eclipse.emf.compare.match.eobject.IEObjectMatcher;
import org.eclipse.emf.compare.match.resource.IResourceMatcher;

public class EventBMatchEngine extends DefaultMatchEngine {

	public EventBMatchEngine(IEObjectMatcher matcher,
			IComparisonFactory comparisonFactory) {
		super(matcher, comparisonFactory);
	}

	@Override
	protected IResourceMatcher createResourceMatcher() {
		return new EventBResourceMatcher();
	}

}
