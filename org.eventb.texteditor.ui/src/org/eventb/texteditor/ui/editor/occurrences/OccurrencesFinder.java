/**
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen,
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html)
 * */

package org.eventb.texteditor.ui.editor.occurrences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.text.Position;
import org.eventb.texteditor.ui.editor.refactoring.EventBTokens;
import org.eventb.texteditor.ui.editor.refactoring.EventBTokens.EventBlock;
import org.eventb.texteditor.ui.editor.refactoring.EventBTokens.Kind;
import org.eventb.texteditor.ui.editor.refactoring.EventBTokens.Token;
import org.eventb.texttools.Constants;

/**
 * Finds all occurrences of the variable, constant, set or parameter at the
 * cursor in the text of a component. A parameter is only searched in its
 * event; a variable or constant is not found in events that declare a
 * parameter with the same name.
 * <p>
 * The search is text based, so it also works for unsaved text with syntax
 * errors.
 * </p>
 */
public final class OccurrencesFinder {

	private static final Set<String> KEYWORDS = new HashSet<String>();
	static {
		KEYWORDS.addAll(Arrays.asList(Constants.structural_keywords));
		KEYWORDS.addAll(Arrays.asList(Constants.formula_keywords));
	}

	private OccurrencesFinder() {
		// utility class
	}

	/**
	 * @return the occurrences (without primes), empty if there is no
	 *         identifier at the offset
	 */
	public static List<Position> find(final EventBTokens tokens,
			final int offset) {
		final List<Position> result = new ArrayList<Position>();

		final int index = tokens.indexAt(offset);
		if (index < 0) {
			return result;
		}

		final Token token = tokens.get(index);
		final String name = EventBTokens.unprimed(token.text);
		if (token.kind != Kind.IDENTIFIER || tokens.isStructuralName(index)
				|| name.length() == 0 || KEYWORDS.contains(name)
				|| !Character.isJavaIdentifierStart(name.charAt(0))) {
			return result;
		}

		// a parameter: only in its event
		final EventBlock event = tokens.eventOf(index);
		if (event != null && declaresParameter(tokens, event, name)) {
			collect(tokens, name, event.first, event.last, result);
			return result;
		}

		int start = 0;
		for (final EventBlock other : tokens.getEvents()) {
			if (declaresParameter(tokens, other, name)) {
				// skip events with a parameter of the same name
				collect(tokens, name, start, other.first - 1, result);
				start = other.last + 1;
			}
		}
		collect(tokens, name, start, tokens.getTokens().size() - 1, result);
		return result;
	}

	private static boolean declaresParameter(final EventBTokens tokens,
			final EventBlock event, final String name) {
		for (final int index : event.parameters) {
			if (tokens.get(index).text.equals(name)) {
				return true;
			}
		}
		return false;
	}

	private static void collect(final EventBTokens tokens, final String name,
			final int first, final int last, final List<Position> result) {
		for (int i = Math.max(0, first); i <= last; i++) {
			final Token token = tokens.get(i);
			if (token.kind == Kind.IDENTIFIER && !tokens.isStructuralName(i)
					&& name.equals(EventBTokens.unprimed(token.text))) {
				result.add(new Position(token.offset, name.length()));
			}
		}
	}
}
