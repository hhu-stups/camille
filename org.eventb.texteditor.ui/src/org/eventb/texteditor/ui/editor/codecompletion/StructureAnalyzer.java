/**
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen,
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html)
 * */

package org.eventb.texteditor.ui.editor.codecompletion;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eventb.texttools.Constants;

/**
 * Lightweight, purely text based analysis of the position where code
 * completion was invoked. It works even while the text contains syntax errors
 * (which is the normal case while typing), where no AST is available.
 */
public final class StructureAnalyzer {

	private static final Set<String> KEYWORDS = new HashSet<String>(
			Arrays.asList(Constants.structural_keywords));

	private static final Set<String> REFERENCE_KEYWORDS = new HashSet<String>(
			Arrays.asList(Constants.SEES, Constants.REFINES, Constants.EXTENDS));

	private static final Pattern EVENT_PATTERN = keywordPattern(Constants.EVENT);

	private StructureAnalyzer() {
		// utility class
	}

	private static Pattern keywordPattern(final String keyword) {
		return Pattern.compile("(?<![\\w'])" + Pattern.quote(keyword)
				+ "(?![\\w'])");
	}

	private static boolean isIdentifierPart(final char c) {
		return Character.isJavaIdentifierPart(c) || c == '\'';
	}

	/**
	 * Returns the keyword whose list of names is currently being written,
	 * e.g. <code>sees</code> for <code>sees c0 c|</code>.
	 *
	 * @param text
	 *            document content
	 * @param offset
	 *            start of the word that is being completed
	 * @return one of <code>sees</code>, <code>refines</code>,
	 *         <code>extends</code> or <code>null</code>
	 */
	public static String findReferenceKeyword(final String text,
			final int offset) {
		int i = Math.min(offset, text.length());

		for (int words = 0; words < 50; words++) {
			// skip whitespace and separators between names
			while (i > 0
					&& (Character.isWhitespace(text.charAt(i - 1)) || text
							.charAt(i - 1) == ',')) {
				i--;
			}

			final int end = i;
			while (i > 0 && isIdentifierPart(text.charAt(i - 1))) {
				i--;
			}

			if (end == i) {
				// not an identifier, e.g. an operator
				return null;
			}

			final String word = text.substring(i, end);
			if (REFERENCE_KEYWORDS.contains(word)) {
				return word;
			}
			if (KEYWORDS.contains(word)) {
				return null;
			}
		}

		return null;
	}

	/**
	 * Returns the word directly in front of the given offset (ignoring
	 * whitespace), or <code>null</code>.
	 */
	public static String previousWord(final String text, final int offset) {
		int i = Math.min(offset, text.length());
		while (i > 0 && Character.isWhitespace(text.charAt(i - 1))) {
			i--;
		}
		final int end = i;
		while (i > 0 && isIdentifierPart(text.charAt(i - 1))) {
			i--;
		}
		return end > i ? text.substring(i, end) : null;
	}

	/**
	 * Determines the label prefix for the section the offset is in, e.g.
	 * <code>inv</code> inside <code>invariants</code>.
	 *
	 * @return the prefix or <code>null</code> if labels make no sense here
	 */
	public static String labelPrefixForSection(final String text,
			final int offset) {
		final String section = findCurrentSection(text, offset);
		if (section == null) {
			return null;
		}
		if (Constants.INVARIANTS.equals(section)) {
			return "inv";
		}
		if (Constants.AXIOMS.equals(section)) {
			return "axm";
		}
		if (Constants.WHERE.equals(section) || Constants.WHEN.equals(section)) {
			return "grd";
		}
		if (Constants.THEN.equals(section) || Constants.BEGIN.equals(section)) {
			return "act";
		}
		return null;
	}

	/**
	 * @return the last structural keyword in front of the offset, ignoring
	 *         <code>theorem</code> (which is used inside of sections)
	 */
	static String findCurrentSection(final String text, final int offset) {
		int i = Math.min(offset, text.length());

		while (i > 0) {
			// find end of a word
			while (i > 0 && !isIdentifierPart(text.charAt(i - 1))) {
				i--;
			}
			final int end = i;
			while (i > 0 && isIdentifierPart(text.charAt(i - 1))) {
				i--;
			}

			if (end > i) {
				final String word = text.substring(i, end);
				// words that are part of a label (@inv1) are no keywords
				final boolean isLabel = i > 0 && text.charAt(i - 1) == '@';
				if (!isLabel && KEYWORDS.contains(word)
						&& !Constants.THEOREM.equals(word)) {
					return word;
				}
			}
		}

		return null;
	}

	/**
	 * Computes the next free number for labels like <code>@inv3</code>.
	 * Invariants and axioms are counted in the whole component, guards and
	 * actions only in the current event.
	 */
	public static int nextLabelNumber(final String text, final int offset,
			final String labelPrefix) {
		int scopeStart = 0;
		int scopeEnd = text.length();

		if ("grd".equals(labelPrefix) || "act".equals(labelPrefix)) {
			final Matcher matcher = EVENT_PATTERN.matcher(text);
			while (matcher.find()) {
				if (matcher.start() < offset) {
					scopeStart = matcher.start();
				} else {
					scopeEnd = matcher.start();
					break;
				}
			}
		}

		final Pattern labelPattern = Pattern.compile("@"
				+ Pattern.quote(labelPrefix) + "(\\d+)(?![\\w'])");
		final Matcher matcher = labelPattern.matcher(text.substring(
				scopeStart, scopeEnd));

		int max = 0;
		while (matcher.find()) {
			try {
				max = Math.max(max, Integer.parseInt(matcher.group(1)));
			} catch (final NumberFormatException e) {
				// IGNORE, absurdly large number
			}
		}

		return max + 1;
	}

	/**
	 * @return <code>true</code> if the offset is behind the
	 *         <code>events</code> keyword
	 */
	public static boolean isInEventsSection(final String text, final int offset) {
		final Matcher matcher = keywordPattern(Constants.EVENTS).matcher(text);
		return matcher.find() && matcher.start() < offset;
	}

	/**
	 * Returns the names of the machines refined by this machine (machine
	 * level <code>refines</code> clause in front of <code>events</code>).
	 */
	public static Set<String> findRefinedMachines(final String text) {
		final Set<String> result = new LinkedHashSet<String>();

		final Matcher eventsMatcher = keywordPattern(Constants.EVENTS).matcher(
				text);
		final String header = eventsMatcher.find() ? text.substring(0,
				eventsMatcher.start()) : text;

		final Matcher matcher = Pattern.compile(
				"(?<![\\w'])" + Constants.REFINES + "\\s+([\\w']+)").matcher(
				header);
		while (matcher.find()) {
			result.add(matcher.group(1));
		}

		return result;
	}

	/**
	 * Returns the names of all events declared in the text.
	 */
	public static Set<String> findEventNames(final String text) {
		final Set<String> result = new LinkedHashSet<String>();
		final Matcher matcher = Pattern.compile(
				"(?<![\\w'])" + Constants.EVENT + "\\s+([\\w']+)").matcher(text);
		while (matcher.find()) {
			result.add(matcher.group(1));
		}
		return result;
	}
}
