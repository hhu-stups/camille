/**
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen,
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html)
 * */

package org.eventb.texteditor.ui.editor.refactoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eventb.texttools.Constants;

/**
 * Splits an Event-B text into identifiers and labels (comments are skipped)
 * and finds the events in it. This is purely text based, so that the
 * positions refer exactly to the text in the editor.
 */
public final class EventBTokens {

	public enum Kind {
		/** identifier or keyword, including primes (x') */
		IDENTIFIER,
		/** label without the leading '@' (inv1, x') */
		LABEL
	}

	public static final class Token {
		public final Kind kind;
		public final String text;
		public final int offset;

		Token(final Kind kind, final String text, final int offset) {
			this.kind = kind;
			this.text = text;
			this.offset = offset;
		}

		public int end() {
			return offset + text.length();
		}

		boolean is(final String keyword) {
			return kind == Kind.IDENTIFIER && keyword.equals(text);
		}
	}

	/**
	 * An event in the text: <code>event label refines a ... end</code>.
	 * Token indices are inclusive.
	 */
	public static final class EventBlock {
		public final String label;
		/** index of the label token, -1 if the label is missing */
		public final int labelIndex;
		public final int first;
		public int last;
		/** indices of the names in the refines/extends clause */
		public final List<Integer> references = new ArrayList<Integer>();
		/** indices of the witness labels in the with section */
		public final List<Integer> witnessLabels = new ArrayList<Integer>();

		EventBlock(final String label, final int labelIndex, final int first) {
			this.label = label;
			this.labelIndex = labelIndex;
			this.first = first;
			this.last = first;
		}

		public boolean contains(final int tokenIndex) {
			return tokenIndex >= first && tokenIndex <= last;
		}
	}

	private static final Set<String> EVENT_SECTIONS = new HashSet<String>(
			Arrays.asList(Constants.ANY, Constants.WHERE, Constants.WHEN,
					Constants.WITH, Constants.THEN, Constants.BEGIN));

	/** keywords that end a list of component names */
	private static final Set<String> KEYWORDS = new HashSet<String>(
			Arrays.asList(Constants.structural_keywords));

	private final List<Token> tokens = new ArrayList<Token>();
	private final List<EventBlock> events = new ArrayList<EventBlock>();
	/** indices of event labels and names of components and events */
	private final Set<Integer> structuralNames = new HashSet<Integer>();

	public EventBTokens(final String text) {
		scan(text);
		findEvents();
	}

	public List<Token> getTokens() {
		return tokens;
	}

	public List<EventBlock> getEvents() {
		return events;
	}

	/**
	 * @return <code>true</code> if the token is the label of an event or the
	 *         name of a machine, context or (abstract) event, e.g.
	 *         <code>m0</code> in <code>refines m0</code>
	 */
	public boolean isStructuralName(final int index) {
		return structuralNames.contains(index);
	}

	public Token get(final int index) {
		return tokens.get(index);
	}

	/**
	 * @return the index of the token at the offset (the cursor may also be
	 *         directly behind the token), or -1
	 */
	public int indexAt(final int offset) {
		for (int i = 0; i < tokens.size(); i++) {
			final Token token = tokens.get(i);
			if (offset >= token.offset && offset <= token.end()) {
				return i;
			}
			if (token.offset > offset) {
				break;
			}
		}
		return -1;
	}

	/**
	 * @return the event containing the token, or <code>null</code>
	 */
	public EventBlock eventOf(final int tokenIndex) {
		for (final EventBlock event : events) {
			if (event.contains(tokenIndex)) {
				return event;
			}
		}
		return null;
	}

	/**
	 * @return the first event with the given label, or <code>null</code>
	 */
	public EventBlock findEvent(final String label) {
		for (final EventBlock event : events) {
			if (label.equals(event.label)) {
				return event;
			}
		}
		return null;
	}

	private void scan(final String text) {
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
				i = end < 0 ? length : end + 2;
			} else if (isIdentifierPart(c)) {
				final int start = i;
				while (i < length && isIdentifierPart(text.charAt(i))) {
					i++;
				}
				final boolean isLabel = start > 0
						&& text.charAt(start - 1) == '@';
				tokens.add(new Token(isLabel ? Kind.LABEL : Kind.IDENTIFIER,
						text.substring(start, i), start));
			} else {
				i++;
			}
		}
	}

	private void findEvents() {
		EventBlock current = null;
		boolean inReferences = false;
		boolean inWitnesses = false;
		boolean inComponentReferences = false;

		for (int i = 0; i < tokens.size(); i++) {
			final Token token = tokens.get(i);

			if (current == null && !token.is(Constants.EVENT)) {
				// machine/context level
				if (token.is(Constants.MACHINE) || token.is(Constants.CONTEXT)) {
					structuralNames.add(i + 1);
					inComponentReferences = false;
				} else if (token.is(Constants.REFINES)
						|| token.is(Constants.SEES)
						|| token.is(Constants.EXTENDS)) {
					inComponentReferences = true;
				} else if (token.kind == Kind.IDENTIFIER
						&& KEYWORDS.contains(token.text)) {
					inComponentReferences = false;
				} else if (inComponentReferences
						&& token.kind == Kind.IDENTIFIER) {
					structuralNames.add(i);
				}
				continue;
			}
			inComponentReferences = false;

			if (token.is(Constants.EVENT)) {
				if (current != null) {
					// "end" is missing
					current.last = i - 1;
				}
				final boolean hasLabel = i + 1 < tokens.size()
						&& tokens.get(i + 1).kind == Kind.IDENTIFIER;
				current = new EventBlock(hasLabel ? tokens.get(i + 1).text
						: null, hasLabel ? i + 1 : -1, i);
				events.add(current);
				if (hasLabel) {
					structuralNames.add(i + 1);
				}
				inReferences = false;
				inWitnesses = false;
				if (hasLabel) {
					i++;
				}
				continue;
			}

			if (current == null) {
				continue;
			}
			current.last = i;

			if (token.is(Constants.END)) {
				current = null;
			} else if (token.is(Constants.REFINES)
					|| token.is(Constants.EXTENDS)) {
				inReferences = true;
				inWitnesses = false;
			} else if (token.kind == Kind.IDENTIFIER
					&& EVENT_SECTIONS.contains(token.text)) {
				inReferences = false;
				inWitnesses = token.is(Constants.WITH);
			} else if (inReferences && token.kind == Kind.IDENTIFIER) {
				current.references.add(i);
				structuralNames.add(i);
			} else if (inWitnesses && token.kind == Kind.LABEL) {
				current.witnessLabels.add(i);
			}
		}

		if (current != null) {
			current.last = tokens.size() - 1;
		}
	}

	static boolean isIdentifierPart(final char c) {
		return Character.isJavaIdentifierPart(c) || c == '\'';
	}

	/**
	 * @return the identifier without primes (x' becomes x)
	 */
	public static String unprimed(final String identifier) {
		int end = identifier.length();
		while (end > 0 && identifier.charAt(end - 1) == '\'') {
			end--;
		}
		return identifier.substring(0, end);
	}
}
