package org.eventb.texttools.merge;

import java.util.Iterator;
import java.util.List;

import org.eclipse.emf.compare.MatchResource;
import org.eclipse.emf.compare.match.resource.StrategyResourceMatcher;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eventb.emf.core.EventBNamed;
import org.eventb.emf.core.machine.Event;
import org.eventb.emf.core.machine.Variant;

import com.google.common.collect.Lists;

public class EventBResourceMatcher extends StrategyResourceMatcher {
	@Override
	public Iterable<MatchResource> createMappings(
			Iterator<? extends Resource> leftResources,
			Iterator<? extends Resource> rightResources,
			Iterator<? extends Resource> originResources) {
		final List<MatchResource> mappings = Lists.newArrayList();

		final List<? extends Resource> leftCopy = Lists
				.newArrayList(leftResources);
		final List<? extends Resource> rightCopy = Lists
				.newArrayList(rightResources);
		final List<? extends Resource> originCopy = Lists
				.newArrayList(originResources);

		// only two way merge in camille?!
		assert (originCopy.isEmpty());

		for (Resource left : leftCopy) {
			Resource right = findMatch(left, rightCopy);
			leftCopy.remove(left);
			rightCopy.remove(right);
			mappings.add(createMatchResource(left, right, null));
		}

		// further matches (including the left-only / right-only ones are found
		// by the default strategies
		Iterable<MatchResource> superMappings = super.createMappings(
				leftCopy.iterator(), rightCopy.iterator(),
				originCopy.iterator());

		for (MatchResource matchResource : superMappings) {
			mappings.add(matchResource);
		}

		return mappings;
	}

	protected Resource findMatch(Resource reference,
			List<? extends Resource> rightCopy) {
		for (Resource candidate : rightCopy) {
			if (matching(reference, candidate)) {
				rightCopy.remove(candidate);
				return candidate;
			}
		}
		return null;
	}

	// this logic was previously found in isSimiliar of EventBMatchEngine
	private boolean matching(Resource reference, Resource candidate) {
		/*
		 * Only one variant may exist in a model, so two variants are a match
		 */
		// FIXME: type of reference / resource
		// if (areVariants(reference, candidate)) {
		// return true;
		// }

		/*
		 * Improve matching for event b named objects with same name
		 */
		if (reference instanceof EventBNamed
				&& candidate instanceof EventBNamed) {
			Event r = (Event) reference;
			Event c = (Event) reference;

			if (r.getName().equals(c.getName())) {
				return true;
			}
		}

		// false => the default behaviour of StrategyResourceMatcher will be
		// tried afterwards
		return false;
	}

	private boolean areSameType(final EObject obj1, final EObject obj2) {
		return obj1 != null && obj2 != null
				&& obj1.eClass().equals(obj2.eClass());
	}

	/**
	 * Test if both given {@link EObject}s are of type {@link Variant}, includes
	 * <code>null</code> check.
	 * 
	 * @param obj1
	 * @param obj2
	 * @return
	 */
	private boolean areVariants(final EObject obj1, final EObject obj2) {
		return areSameType(obj1, obj2) && obj2 instanceof Variant;
	}
}
