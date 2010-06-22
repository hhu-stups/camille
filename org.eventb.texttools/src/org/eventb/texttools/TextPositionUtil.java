/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texttools;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.EObject;
import org.eventb.emf.core.Annotation;
import org.eventb.emf.core.EventBObject;
import org.eventb.emf.core.impl.CoreFactoryImpl;
import org.eventb.texttools.model.texttools.TextRange;
import org.eventb.texttools.model.texttools.TexttoolsFactory;

public class TextPositionUtil {
	/**
	 * Source key for the {@link EAnnotation} containing {@link TextRange}.
	 */
	public static final String ANNOTATION_TEXTRANGE = "http://emf.eventb.org/models/core/texttools/TextRange";

	/**
	 * <p>
	 * Extracts a {@link TextRange} object from the annotations of the given
	 * {@link EModelElement}. The <code>contents</code> attribute of the
	 * annotation is search for an object of type {@link TextRange} and the
	 * first one found is returned.
	 * </p>
	 * <p>
	 * If no matching object can be found <code>null</code> is returned.
	 * </p>
	 * 
	 * @param annotation
	 * @return
	 */
	public static TextRange getTextRange(final EventBObject element) {
		if (element != null) {
			final Annotation annotation = element
					.getAnnotation(ANNOTATION_TEXTRANGE);

			if (annotation != null) {
				final EList<EObject> contents = annotation.getContents();

				for (final EObject object : contents) {
					if (object instanceof TextRange) {
						return (TextRange) object;
					}
				}
			}
		}

		return null;
	}

	public static void annotatePosition(final EventBObject element,
			final TextRange range) {
		if (range != null) {
			final Annotation annotation = CoreFactoryImpl.eINSTANCE
					.createAnnotation();
			annotation.setEventBObject(element);
			annotation.setSource(TextPositionUtil.ANNOTATION_TEXTRANGE);
			annotation.getContents().add(range);
			element.getAnnotations().add(annotation);
		}
	}

	public static void annotatePosition(final EventBObject element,
			final int startPos, final int length) {
		final TextRange range = TexttoolsFactory.eINSTANCE.createTextRange();
		range.setOffset(startPos);
		range.setLength(length);

		annotatePosition(element, range);
	}

	/**
	 * <p>
	 * Gets the position annotation of the given {@link EModelElement} and if
	 * it's not <code>null</code> the given {@link TextRange} is attached to it
	 * under the given key.
	 * </p>
	 * <p>
	 * This can be used to store position information about subelements, i.e.,
	 * strings. First annotate the EventB element normally. Then create a
	 * {@link TextRange} object for each string for which you want to store the
	 * positions. Then pass the parent EventB element, the new {@link TextRange}
	 * to this method and an appropriate {@link String} as key to this method.
	 * </p>
	 * 
	 * @see
	 * @param emfElement
	 *            Parent element which already has a position annotation to
	 *            which the given annotation is to be attached.
	 * @param range
	 *            The annotation which will be attached to the parent's position
	 *            annotation.
	 * @param key
	 *            A key to store the position under. It will be needed to
	 *            retrieve the position later. So it's helpful to use the string
	 *            itself.
	 */
	public static void addInternalPosition(final EventBObject emfElement,
			final String key, final TextRange range) {
		Assert.isNotNull(emfElement);
		Assert.isNotNull(range);
		Assert.isNotNull(key);

		final TextRange parentRange = getTextRange(emfElement);

		if (parentRange != null) {
			getSubRangeMap(parentRange).put(key, range);
		}
	}

	/**
	 * Replace the {@link TextRange} that is associated with the given
	 * <code>oldKey</code>. Several cases are possible:
	 * <ul>
	 * <li><code>newKey == null</code>: The old {@link TextRange} is just
	 * removed.</li>
	 * <li><code>newKey == oldKey</code>: The {@link TextRange} is simply
	 * replaced.</li>
	 * <li><code>newKey != null && newKey != oldKey</code>: The
	 * {@link TextRange} under key <code>oldKey</code> is removed and the
	 * <code>newRange</code> is added under <code>newKey</code>.</li>
	 * </ul>
	 * 
	 * @param emfElement
	 * @param oldKey
	 * @param newKey
	 * @param newRange
	 */
	public static void replaceInternalPosition(final EventBObject emfElement,
			final String oldKey, final String newKey, final TextRange newRange) {
		Assert.isNotNull(emfElement);
		Assert.isNotNull(oldKey);

		final TextRange parentRange = getTextRange(emfElement);

		if (parentRange != null) {
			final Map<String, TextRange> subRangeMap = getSubRangeMap(parentRange);
			subRangeMap.remove(oldKey);

			if (newKey != null && newRange != null) {
				subRangeMap.put(newKey, newRange);
			}
		}
	}

	/**
	 * Extracts a {@link TextRange} for a substring from the given
	 * {@link EModelElement}'s annotations. The given key is used to find the
	 * correct {@link TextRange} for the substring.
	 * 
	 * @see #addInternalPosition(EModelElement, String, TextRange)
	 * @param emfElement
	 *            Parent element with a position annotation.
	 * @param key
	 *            Key which has been used to store the position information.
	 * @return The relevant {@link TextRange} if available. Returns
	 *         <code>null</code> if the {@link EModelElement} has no position
	 *         annotation or if this annotation didn't contain matching key.
	 */
	public static TextRange getInternalPosition(final EventBObject emfElement,
			final String key) {
		Assert.isNotNull(emfElement);
		Assert.isNotNull(key);

		final TextRange parentRange = getTextRange(emfElement);

		if (parentRange != null) {
			return getSubRangeMap(parentRange).get(key);
		}

		return null;
	}

	private static Map<String, TextRange> getSubRangeMap(
			final TextRange parentRange) {
		Map<String, TextRange> result = parentRange.getSubTextRanges();

		if (result == null) {
			result = new HashMap<String, TextRange>();
			parentRange.setSubTextRanges(result);
		}

		return result;
	}

	/**
	 * Creates a new {@link TextRange} instance initialised with the value of
	 * the given {@link EventBObject}. So the returned object may be changed
	 * without changing the the position of the given emf object.
	 * 
	 * @param element
	 *            {@link EventBObject} with original position
	 * @return New {@link TextRange} instance or <code>null</code> if no
	 *         original text position could be found.
	 */
	public static TextRange createTextRange(final EventBObject element) {
		final TextRange origRange = getTextRange(element);

		if (origRange != null) {
			final TextRange result = TexttoolsFactory.eINSTANCE
					.createTextRange();
			result.setLength(origRange.getLength());
			result.setOffset(origRange.getOffset());
			return result;
		} else {
			return null;
		}
	}

	/**
	 * Pushes the offset back by the given length and corrects the total length
	 * of this {@link TextRange}.
	 * 
	 * @param range
	 * @param length
	 */
	public static void correctStartOffset(final TextRange range,
			final int length) {
		if (range != null) {
			range.setOffset(range.getOffset() + length + 1);
			range.setLength(range.getLength() - length - 1);
		}
	}
}
