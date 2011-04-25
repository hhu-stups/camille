/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texttools.merge;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.compare.FactoryException;
import org.eclipse.emf.compare.match.MatchOptions;
import org.eclipse.emf.compare.match.engine.IMatchEngine;
import org.eclipse.emf.compare.match.engine.GenericMatchEngine;
import org.eclipse.emf.compare.match.engine.MatchSettings;
import org.eclipse.emf.compare.match.internal.statistic.NameSimilarity;
import org.eclipse.emf.ecore.EObject;
import org.eventb.emf.core.EventBNamed;
import org.eventb.emf.core.EventBNamedCommentedComponentElement;
import org.eventb.emf.core.EventBPredicate;
import org.eventb.emf.core.machine.Event;
import org.eventb.emf.core.machine.Variant;

/**
 * A {@link IMatchEngine} which matches EventB emf models. The special structure
 * of those models is taken into consideration to produce more exact match
 * models.
 */
public class EventBMatchEngine extends GenericMatchEngine {
	public static final String OPTION_DONT_COMPARE_COMPONENTS = "eventb.dont.compare.components";

	private static final Map<String, Object> defaultOptions = new HashMap<String, Object>();

	static {
		defaultOptions.put(OPTION_DONT_COMPARE_COMPONENTS, false);
	}

	public EventBMatchEngine() {
		// needed for Extension Point
	}

	public EventBMatchEngine(final Map<String, Object> options) {
		this.options.putAll(options);
	}

	@Override
	public boolean isSimilar(final EObject obj1, final EObject obj2)
			throws FactoryException {
		/*
		 * Test if we consider components as match by default
		 */
		final Boolean dontCompareComponents = getOption(OPTION_DONT_COMPARE_COMPONENTS);
		if (dontCompareComponents && areSameComponentType(obj1, obj2)) {
			return true;
		}

		/*
		 * Only one variant may exist in a model, so two variants are a match
		 */
		if (areVariants(obj1, obj2)) {
			return true;
		}

		/*
		 * Our models are typed strictly, that means: different type => no match
		 */
		if (!areSameType(obj1, obj2)) {
			return false;
		}

		/*
		 * Improve matching for events with same name
		 */
		if (obj1 instanceof Event) {
			final double similarity = nameSimilarity(obj1, obj2);
			if (similarity == 1) {
				return true;
			}
		}

		// otherwise use default comparison of GenericMatchEngine
		return super.isSimilar(obj1, obj2);
	}

	@Override
	protected double nameSimilarity(final EObject obj1, final EObject obj2) {
		if (!areSameType(obj1, obj2)) {
			return 0d;
		}

		return NameSimilarity
				.nameSimilarityMetric(getName(obj1), getName(obj2));
	}

	@Override
	protected double contentSimilarity(final EObject obj1, final EObject obj2)
			throws FactoryException {
		if (!areSameType(obj1, obj2)) {
			return 0d;
		}

		double result = super.contentSimilarity(obj1, obj2);

		if (obj1 instanceof EventBPredicate) {
			// give predicate equality extra weight
			result = (result + 2 * contentSimilarity((EventBPredicate) obj1,
					(EventBPredicate) obj2)) / 3;
		}

		return result;
	}

	private double contentSimilarity(final EventBPredicate pred1,
			final EventBPredicate pred2) {
		return NameSimilarity.nameSimilarityMetric(pred1.getPredicate(), pred2
				.getPredicate());
	}

	/* 
	 * Overridden to set custom matching options.
	 * (non-Javadoc)
	 * @see org.eclipse.emf.compare.match.engine.GenericMatchEngine#updateSettings(org.eclipse.emf.compare.match.engine.MatchSettings, java.util.Map)
	 */
	@Override
	protected void updateSettings(MatchSettings settings,
			Map<String, Object> optionMap) {
		super.updateSettings(settings, optionMap);
		Map<String, Object> ignoreOptions = new HashMap<String, Object>();
		
		// don't compare by IDs as these can be not unique in current EMF of EventB
		//FIXME: this can be removed if EMF is fixed to support unique IDs
		ignoreOptions.put(MatchOptions.OPTION_IGNORE_XMI_ID, true);
		ignoreOptions.put(MatchOptions.OPTION_IGNORE_ID, true);
		super.updateSettings(settings, ignoreOptions);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected <T> T getOption(final String key) throws ClassCastException {
		if (options.containsKey(key)) {
			return (T) options.get(key);
		} else {
			return (T) defaultOptions.get(key);
		}
	}

	private String getName(final EObject object) {
		/*
		 * Make sure correct names for EventBNamed elements are used.
		 */
		if (object instanceof EventBNamed) {
			return ((EventBNamed) object).getName();
		}

		// fallback to default
		try {
			return NameSimilarity.findName(object);
		} catch (final FactoryException e) {
			return null;
		}
	}

	private boolean areSameType(final EObject obj1, final EObject obj2) {
		return obj1 != null && obj2 != null
				&& obj1.eClass().equals(obj2.eClass());
	}

	/**
	 * Test if both given {@link EObject}s are {@link EventBComponent}s and of
	 * same type of component, includes <code>null</code> check.
	 *
	 * @param obj1
	 * @param obj2
	 * @return
	 */
	private boolean areSameComponentType(final EObject obj1, final EObject obj2) {
		return areSameType(obj1, obj2) && obj1 instanceof EventBNamedCommentedComponentElement;
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
