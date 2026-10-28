/**
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen,
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html)
 * */

package org.eventb.texteditor.ui.editor.navigation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eventb.texteditor.ui.editor.hover.HoverIndex.Declaration;
import org.eventb.texttools.ComponentNameUtil;
import org.eventb.texttools.Constants;

/**
 * Finds the declaration of a name in the text of a machine or context. This
 * works on the text only, so it also works while the text contains syntax
 * errors.
 */
public final class DeclarationLocator {

	private static final Set<String> KEYWORDS = new HashSet<String>(
			Arrays.asList(Constants.structural_keywords));

	private static final Set<String> EVENT_SECTIONS = new HashSet<String>(
			Arrays.asList(Constants.WHERE, Constants.WHEN, Constants.WITH,
					Constants.THEN, Constants.BEGIN, Constants.END));

	private static final Pattern WORD = Pattern
			.compile("(?<![\\w'@])[\\w']+(?![\\w'])");

	private DeclarationLocator() {
		// utility class
	}

	/**
	 * @return the region of the declaring occurrence of the name, or
	 *         <code>null</code> if it cannot be found
	 */
	public static IRegion find(final String text, final Declaration.Kind kind,
			final String name, final String event) {
		switch (kind) {
		case VARIABLE:
			return findInSection(text, Constants.VARIABLES, name, 0,
					text.length());
		case CONSTANT:
			return findInSection(text, Constants.CONSTANTS, name, 0,
					text.length());
		case CARRIER_SET:
			return findInSection(text, Constants.SETS, name, 0,
					text.length());
		case PARAMETER: {
			final IRegion eventRegion = findEventRegion(text, event);
			if (eventRegion == null) {
				return null;
			}
			return findInSection(text, Constants.ANY, name,
					eventRegion.getOffset(), eventRegion.getOffset()
							+ eventRegion.getLength());
		}
		case EVENT: {
			final Matcher matcher = eventPattern(name).matcher(text);
			return matcher.find() ? new Region(matcher.start(1),
					name.length()) : null;
		}
		case MACHINE:
		case CONTEXT: {
			final ComponentNameUtil.NameInfo info = ComponentNameUtil
					.findComponentName(text);
			return info != null ? new Region(info.offset, info.getLength())
					: new Region(0, 0);
		}
		default:
			return null;
		}
	}

	private static Pattern eventPattern(final String label) {
		return Pattern.compile("(?<![\\w'])" + Constants.EVENT + "\\s+("
				+ Pattern.quote(label) + ")(?![\\w'])");
	}

	/**
	 * @return the text of the event with the given label, from its
	 *         <code>event</code> keyword to the next event (or the end)
	 */
	private static IRegion findEventRegion(final String text,
			final String label) {
		if (label == null) {
			return null;
		}
		final Matcher matcher = eventPattern(label).matcher(text);
		if (!matcher.find()) {
			return null;
		}
		final Matcher next = Pattern.compile(
				"(?<![\\w'])" + Constants.EVENT + "\\s").matcher(text);
		final int end = next.find(matcher.end()) ? next.start() : text
				.length();
		return new Region(matcher.start(), end - matcher.start());
	}

	/**
	 * Searches the name as a whole word in the section that starts with the
	 * given keyword and ends at the next structural keyword.
	 */
	private static IRegion findInSection(final String text,
			final String keyword, final String name, final int from,
			final int to) {
		final Matcher words = WORD.matcher(text);
		words.region(from, to);

		boolean inSection = false;
		while (words.find()) {
			final String word = words.group();
			if (inSection && word.equals(name)) {
				return new Region(words.start(), word.length());
			}
			if (word.equals(keyword)) {
				inSection = true;
			} else if (inSection
					&& (KEYWORDS.contains(word) || EVENT_SECTIONS
							.contains(word))) {
				// the section ended without the name, there may be another
				inSection = false;
			}
		}
		return null;
	}
}
