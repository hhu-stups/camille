/**
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen,
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html)
 * */

package org.eventb.texteditor.ui.editor.occurrences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eventb.texteditor.ui.editor.refactoring.EventBTokens;
import org.eventb.texteditor.ui.editor.refactoring.EventBTokens.Kind;
import org.eventb.texteditor.ui.editor.refactoring.EventBTokens.Token;
import org.eventb.texttools.Constants;

/**
 * Finds the identifiers that are bound by quantifiers (<code>∀x·</code>,
 * <code>∃x·</code>), lambda abstractions (<code>λx·</code>), quantified
 * unions/intersections (<code>⋃x·</code>, <code>⋂x·</code>) and set
 * comprehensions (<code>{x·P ∣ E}</code>), together with their scope. The
 * ASCII notation (<code>!</code>, <code>#</code>, <code>%</code>,
 * <code>.</code>) is recognized as well, because the text may not have been
 * translated yet.
 * <p>
 * The scope of a binder extends to the end of the enclosing brackets, or to
 * the end of the formula (next label or keyword). The expression of a
 * lambda or quantified union/intersection (<code>λx·P ∣ E</code>) ends in
 * front of the next predicate operator, e.g. in <code>(λx·P ∣ E) = {x}</code>
 * the last x is free.
 * </p>
 */
public final class BoundIdentifiers {

	/** One binder with its bound identifiers */
	public static final class Binding {
		final Set<String> names;
		/** offset of the binder */
		final int start;
		/** end of the scope (exclusive) */
		final int end;

		Binding(final Set<String> names, final int start, final int end) {
			this.names = names;
			this.start = start;
			this.end = end;
		}

		boolean contains(final int offset) {
			return offset >= start && offset < end;
		}
	}

	private static final String BINDERS = "\u2200\u2203\u03bb\u22c3\u22c2!#%";
	private static final String DOTS = "\u00b7.";
	/** binders of expressions (λ, ⋃, ⋂) */
	private static final String EXPRESSION_BINDERS = "\u03bb\u22c3\u22c2%";
	private static final char MID = '\u2223';
	/**
	 * operators that cannot be part of an expression: relations between
	 * expressions and logical connectives
	 */
	private static final String PREDICATE_OPERATORS = "=\u2260\u2208\u2209"
			+ "\u2282\u2284\u2286\u2288<>\u2264\u2265"
			+ "\u2227\u2228\u21d2\u21d4\u00ac&:";

	private static final Set<String> KEYWORDS = new HashSet<String>(
			Arrays.asList(Constants.structural_keywords));

	private final char[] chars;
	private final TreeSet<Integer> formulaEnds = new TreeSet<Integer>();
	private final List<Binding> bindings = new ArrayList<Binding>();

	public BoundIdentifiers(final String text, final EventBTokens tokens) {
		this.chars = maskComments(text);

		// formulas end at labels and keywords
		for (final Token token : tokens.getTokens()) {
			if (token.kind == Kind.LABEL) {
				formulaEnds.add(token.offset - 1);
			} else if (KEYWORDS.contains(token.text)) {
				formulaEnds.add(token.offset);
			}
		}

		for (int i = 0; i < chars.length; i++) {
			final char c = chars[i];
			if (BINDERS.indexOf(c) >= 0) {
				addBinding(i, i + 1, false);
			} else if (c == '{') {
				addBinding(i, i + 1, true);
			}
		}
	}

	/**
	 * @return the innermost binding of the identifier at the offset, or
	 *         <code>null</code> if it is free there
	 */
	public Binding bindingOf(final String name, final int offset) {
		Binding result = null;
		for (final Binding binding : bindings) {
			if (binding.contains(offset) && binding.names.contains(name)
					&& (result == null || binding.start > result.start)) {
				result = binding;
			}
		}
		return result;
	}

	private void addBinding(final int binder, final int listStart,
			final boolean isComprehension) {
		// the list of bound identifiers up to the dot, e.g. "x, y" or "x\u21a6y"
		int dot = -1;
		for (int i = listStart; i < chars.length; i++) {
			final char c = chars[i];
			if (DOTS.indexOf(c) >= 0) {
				dot = i;
				break;
			}
			if (!(isIdentifierPart(c) || Character.isWhitespace(c) || c == ','
					|| c == '\u21a6' || c == '(' || c == ')' || c == '|'
					|| c == '-' || c == '>')) {
				return;
			}
		}
		if (dot < 0) {
			return;
		}

		final Set<String> names = new LinkedHashSet<String>();
		int i = listStart;
		while (i < dot) {
			if (isIdentifierPart(chars[i])) {
				final int start = i;
				while (i < dot && isIdentifierPart(chars[i])) {
					i++;
				}
				names.add(new String(chars, start, i - start));
			} else {
				i++;
			}
		}
		if (names.isEmpty()) {
			return;
		}

		final Integer next = formulaEnds.higher(binder);
		final int formulaEnd = next != null ? next : chars.length;
		int end = isComprehension ? closingBracket(binder, formulaEnd)
				: endOfEnclosingBrackets(binder, formulaEnd);
		if (EXPRESSION_BINDERS.indexOf(chars[binder]) >= 0) {
			end = endOfExpression(dot + 1, end);
		}
		bindings.add(new Binding(names, binder, end));
	}

	/**
	 * For λx·P ∣ E: the first predicate operator behind the "∣" (outside of
	 * brackets) ends the expression E.
	 */
	private int endOfExpression(final int start, final int end) {
		int depth = 0;
		boolean inExpression = false;
		for (int i = start; i < end; i++) {
			final char c = chars[i];
			if (isOpening(c)) {
				depth++;
			} else if (isClosing(c)) {
				depth--;
			} else if (depth == 0) {
				if (!inExpression) {
					// "\u2223", or ASCII "|" which is not part of "|->"
					inExpression = c == MID
							|| (c == '|' && !(i + 1 < end && chars[i + 1] == '-'));
				} else if (PREDICATE_OPERATORS.indexOf(c) >= 0
						&& !isMaplet(i)) {
					return i;
				}
			}
		}
		return end;
	}

	/** the ">" of an ASCII maplet "|->" */
	private boolean isMaplet(final int index) {
		return chars[index] == '>' && index >= 2 && chars[index - 1] == '-'
				&& chars[index - 2] == '|';
	}

	/** end of the innermost brackets around the offset */
	private int endOfEnclosingBrackets(final int offset, final int formulaEnd) {
		final Integer previous = formulaEnds.lower(offset);
		final int formulaStart = previous != null ? previous : 0;
		int depth = 0;
		for (int i = offset - 1; i >= formulaStart; i--) {
			final char c = chars[i];
			if (isClosing(c)) {
				depth++;
			} else if (isOpening(c)) {
				if (depth == 0) {
					return closingBracket(i, formulaEnd);
				}
				depth--;
			}
		}
		return formulaEnd;
	}

	/** position behind the bracket matching the one at the offset */
	private int closingBracket(final int open, final int formulaEnd) {
		int depth = 0;
		for (int i = open; i < formulaEnd; i++) {
			final char c = chars[i];
			if (isOpening(c)) {
				depth++;
			} else if (isClosing(c)) {
				depth--;
				if (depth == 0) {
					return i + 1;
				}
			}
		}
		return formulaEnd;
	}

	private static boolean isOpening(final char c) {
		return c == '(' || c == '[' || c == '{';
	}

	private static boolean isClosing(final char c) {
		return c == ')' || c == ']' || c == '}';
	}

	private static boolean isIdentifierPart(final char c) {
		return Character.isJavaIdentifierPart(c) || c == '\'';
	}

	private static char[] maskComments(final String text) {
		final char[] result = text.toCharArray();
		int i = 0;
		while (i < result.length) {
			if (result[i] == '/' && i + 1 < result.length
					&& result[i + 1] == '/') {
				while (i < result.length && result[i] != '\n'
						&& result[i] != '\r') {
					result[i++] = ' ';
				}
			} else if (result[i] == '/' && i + 1 < result.length
					&& result[i + 1] == '*') {
				final int end = text.indexOf("*/", i + 2);
				final int stop = end < 0 ? result.length : end + 2;
				while (i < stop) {
					if (result[i] != '\n' && result[i] != '\r') {
						result[i] = ' ';
					}
					i++;
				}
			} else {
				i++;
			}
		}
		return result;
	}
}
