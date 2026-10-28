/**
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen,
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html)
 * */

package org.eventb.texteditor.ui.editor.folding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eventb.texttools.Constants;

/**
 * Finds the regions of an Event-B text that can be folded: the clauses of a
 * machine or context (<code>variables</code>, <code>invariants</code>,
 * <code>events</code>, ...), single events and multi-line comments.
 * <p>
 * The analysis is purely text based, so folding keeps working while the text
 * contains syntax errors.
 * </p>
 */
public final class FoldingRegionFinder {

	public enum Kind {
		CLAUSE, EVENT, COMMENT
	}

	/**
	 * A foldable region. It always covers complete lines, including the line
	 * delimiter of its last line.
	 */
	public static final class Region {
		public final Kind kind;
		/** Identifies the region across changes, e.g. "event:inc" */
		public final String key;
		public final int offset;
		public final int length;

		Region(final Kind kind, final String key, final int offset,
				final int length) {
			this.kind = kind;
			this.key = key;
			this.offset = offset;
			this.length = length;
		}

		@Override
		public String toString() {
			return key + "[" + offset + "," + length + "]";
		}
	}

	/** Keywords which start a clause of a machine or a context */
	private static final Set<String> CLAUSES = new HashSet<String>(
			Arrays.asList(Constants.REFINES, Constants.SEES, Constants.EXTENDS,
					Constants.VARIABLES, Constants.INVARIANTS,
					Constants.VARIANT, Constants.EVENTS, Constants.SETS,
					Constants.CONSTANTS, Constants.AXIOMS));

	private static final class Word {
		final String text;
		final int offset;

		Word(final String text, final int offset) {
			this.text = text;
			this.offset = offset;
		}

		int end() {
			return offset + text.length();
		}
	}

	private final String text;
	private final int[] lineStarts;
	private final List<Word> words = new ArrayList<Word>();
	private final List<int[]> comments = new ArrayList<int[]>();
	private final List<Region> regions = new ArrayList<Region>();
	private final Map<String, Integer> usedKeys = new HashMap<String, Integer>();

	private FoldingRegionFinder(final String text) {
		this.text = text;
		this.lineStarts = computeLineStarts(text);
	}

	/**
	 * Computes the foldable regions of the given text.
	 */
	public static List<Region> findRegions(final String text) {
		final FoldingRegionFinder finder = new FoldingRegionFinder(text);
		finder.scan();
		finder.computeStructure();
		finder.computeComments();
		return finder.regions;
	}

	/*
	 * Splits the text into words, ignoring comments and labels (@inv1).
	 */
	private void scan() {
		final int length = text.length();
		int i = 0;
		while (i < length) {
			final char c = text.charAt(i);

			if (c == '/' && i + 1 < length && text.charAt(i + 1) == '/') {
				while (i < length && text.charAt(i) != '\n'
						&& text.charAt(i) != '\r') {
					i++;
				}
			} else if (c == '/' && i + 1 < length && text.charAt(i + 1) == '*') {
				final int end = text.indexOf("*/", i + 2);
				final int commentEnd = end < 0 ? length : end + 2;
				comments.add(new int[] { i, commentEnd });
				i = commentEnd;
			} else if (isIdentifierPart(c)) {
				final int start = i;
				while (i < length && isIdentifierPart(text.charAt(i))) {
					i++;
				}
				final boolean isLabel = start > 0
						&& text.charAt(start - 1) == '@';
				if (!isLabel) {
					words.add(new Word(text.substring(start, i), start));
				}
			} else {
				i++;
			}
		}
	}

	private void computeStructure() {
		Word clause = null;
		Word event = null;
		String eventName = null;

		for (int index = 0; index < words.size(); index++) {
			final Word word = words.get(index);
			final String w = word.text;

			if (event != null) {
				if (Constants.END.equals(w)) {
					addRegion(Kind.EVENT, "event:" + eventName, event.offset,
							word.end(), nextOffset(index));
					event = null;
					continue;
				} else if (Constants.EVENT.equals(w)) {
					// "end" is missing, the event ends with the next one
					addRegion(Kind.EVENT, "event:" + eventName, event.offset,
							contentEnd(eventStart(index)), eventStart(index));
					event = null;
				} else {
					continue;
				}
			}

			if (Constants.EVENT.equals(w)) {
				event = new Word(w, eventStart(index));
				eventName = index + 1 < words.size() ? words.get(index + 1).text
						: "";
			} else if (CLAUSES.contains(w) || Constants.END.equals(w)) {
				if (clause != null) {
					addRegion(Kind.CLAUSE, "clause:" + clause.text,
							clause.offset, contentEnd(word.offset), word.offset);
				}
				clause = Constants.END.equals(w) ? null : word;
			}
		}

		if (event != null) {
			addRegion(Kind.EVENT, "event:" + eventName, event.offset,
					contentEnd(text.length()), -1);
		}
		if (clause != null) {
			addRegion(Kind.CLAUSE, "clause:" + clause.text, clause.offset,
					contentEnd(text.length()), -1);
		}
	}

	/*
	 * An event may start with modifiers, e.g. "convergent event inc".
	 */
	private int eventStart(final int eventIndex) {
		int start = words.get(eventIndex).offset;
		for (int i = eventIndex - 1; i >= 0; i--) {
			final String w = words.get(i).text;
			if (Constants.CONVERGENT.equals(w)
					|| Constants.ANTICIPATED.equals(w)
					|| Constants.ORDINARY.equals(w)) {
				start = words.get(i).offset;
			} else {
				break;
			}
		}
		return start;
	}

	private void computeComments() {
		int count = 0;
		for (final int[] comment : comments) {
			final int next = comment[1] < text.length() ? firstNonWhitespaceOnLine(comment[1])
					: -1;
			addRegion(Kind.COMMENT, "comment:" + count++, comment[0],
					comment[1], next);
		}
	}

	private int firstNonWhitespaceOnLine(final int offset) {
		for (int i = offset; i < text.length(); i++) {
			final char c = text.charAt(i);
			if (c == '\n' || c == '\r') {
				return -1;
			}
			if (!Character.isWhitespace(c)) {
				return i;
			}
		}
		return -1;
	}

	private int nextOffset(final int wordIndex) {
		int next = wordIndex + 1 < words.size() ? words.get(wordIndex + 1).offset
				: -1;
		// a comment directly behind "end" belongs to the event
		for (final int[] comment : comments) {
			if (comment[0] > words.get(wordIndex).offset
					&& (next < 0 || comment[0] < next)) {
				next = comment[0];
				break;
			}
		}
		return next;
	}

	/*
	 * End of the last non whitespace character in front of the offset.
	 */
	private int contentEnd(final int offset) {
		int end = Math.min(offset, text.length());
		while (end > 0 && Character.isWhitespace(text.charAt(end - 1))) {
			end--;
		}
		return end;
	}

	/**
	 * Adds a region from the line of <code>start</code> to the line of
	 * <code>end</code>. If <code>next</code> (the start of the next element,
	 * or -1) is on the last line, that line is excluded so the next element
	 * stays visible when the region is collapsed.
	 */
	private void addRegion(final Kind kind, final String key, final int start,
			final int end, final int next) {
		if (end <= start) {
			return;
		}

		final int startLine = lineOf(start);
		int endLine = lineOf(end - 1);
		if (next >= 0 && lineOf(next) == endLine) {
			endLine--;
		}

		if (endLine <= startLine) {
			// nothing to fold
			return;
		}

		final int regionStart = lineStarts[startLine];
		final int regionEnd = endLine + 1 < lineStarts.length ? lineStarts[endLine + 1]
				: text.length();

		regions.add(new Region(kind, uniqueKey(key), regionStart, regionEnd
				- regionStart));
	}

	private String uniqueKey(final String key) {
		final Integer count = usedKeys.get(key);
		usedKeys.put(key, count == null ? 1 : count + 1);
		return count == null ? key : key + "#" + count;
	}

	private int lineOf(final int offset) {
		int index = Arrays.binarySearch(lineStarts, offset);
		if (index < 0) {
			index = -index - 2;
		}
		return Math.max(0, index);
	}

	private static int[] computeLineStarts(final String text) {
		final List<Integer> starts = new ArrayList<Integer>();
		starts.add(0);
		for (int i = 0; i < text.length(); i++) {
			final char c = text.charAt(i);
			if (c == '\r') {
				if (i + 1 < text.length() && text.charAt(i + 1) == '\n') {
					i++;
				}
				starts.add(i + 1);
			} else if (c == '\n') {
				starts.add(i + 1);
			}
		}

		final int[] result = new int[starts.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = starts.get(i);
		}
		return result;
	}

	private static boolean isIdentifierPart(final char c) {
		return Character.isJavaIdentifierPart(c) || c == '\'';
	}
}
