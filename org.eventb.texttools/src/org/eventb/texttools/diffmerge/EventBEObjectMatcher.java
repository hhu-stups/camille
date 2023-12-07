package org.eventb.texttools.diffmerge;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.common.util.Monitor;
import org.eclipse.emf.compare.CompareFactory;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Match;
import org.eclipse.emf.compare.match.eobject.IEObjectMatcher;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.eventb.emf.core.EventBNamed;
import org.eventb.emf.core.machine.Variant;

public class EventBEObjectMatcher implements IEObjectMatcher {
	@Override
	public void createMatches(Comparison comparison,
			Iterator<? extends EObject> leftEObjects,
			Iterator<? extends EObject> rightEObjects,
			Iterator<? extends EObject> originEObjects, Monitor monitor) {

		final Set<Match> matches = new LinkedHashSet<>();

		final List<EObject> rightCopy = new ArrayList<>();
		rightEObjects.forEachRemaining(rightCopy::add);
		final List<EObject> originCopy = new ArrayList<>();
		originEObjects.forEachRemaining(originCopy::add);

		// only two way merge in camille?!
		assert (originCopy.isEmpty());
		comparison.setThreeWay(false);

		while (leftEObjects.hasNext()) {
			EObject left = leftEObjects.next();
			EObject right = findMatch(left, rightCopy);

			Match match = CompareFactory.eINSTANCE.createMatch();
			match.setLeft(left);
			match.setRight(right);
			matches.add(match);
		}

		// elements in right that did not match any element in left
		for (EObject right : rightCopy) {
			Match match = CompareFactory.eINSTANCE.createMatch();
			match.setRight(right);
			matches.add(match);
		}

		comparison.getMatches().addAll(matches);
	}

	protected EObject findMatch(EObject left, List<EObject> rightCopy) {
		for (EObject candidate : rightCopy) {
			if (matching(left, candidate)) {
				rightCopy.remove(candidate);
				return candidate;
			}
		}
		return null;
	}

	// this logic was previously found in isSimiliar of EventBMatchEngine
	public static boolean matching(EObject left, EObject candidate) {
		/*
		 * If the type differs it can not be a match
		 */
		if (!areSameType(left, candidate)) {
			return false;
		}
		/*
		 * Only one variant may exist in a model, so two variants are a match
		 */
		if (left instanceof Variant) {
			assert candidate instanceof Variant;
			return true;
		}

		/*
		 * Improve matching for event b named objects with same name
		 */
		if (left instanceof EventBNamed) {
			assert candidate instanceof EventBNamed;
			EventBNamed l = (EventBNamed) left;
			EventBNamed c = (EventBNamed) candidate;

			if (l.getName().equals(c.getName())) {
				return true;
			}
		}

		/*
		 * rely on emf identifiers after the event-b specific code
		 */
		String idLeft = getEMFId(left);
		if (idLeft == null) {
			return false;
		}
		String idCandidate = getEMFId(candidate);
		return idLeft.equals(idCandidate);
	}

	private static boolean areSameType(final EObject obj1, final EObject obj2) {
		return obj1 != null && obj2 != null
				&& obj1.eClass().equals(obj2.eClass());
	}

	private static String getEMFId(EObject eObject) {
		final String identifier;
		if (eObject.eIsProxy()) {
			identifier = ((InternalEObject) eObject).eProxyURI().fragment();
		} else {
			String functionalId = EcoreUtil.getID(eObject);
			if (functionalId != null) {
				identifier = functionalId;
			} else {
				final Resource eObjectResource = eObject.eResource();
				if (eObjectResource instanceof XMIResource) {
					identifier = ((XMIResource) eObjectResource).getID(eObject);
				} else {
					identifier = null;
				}
			}
		}
		return identifier;
	}
}
