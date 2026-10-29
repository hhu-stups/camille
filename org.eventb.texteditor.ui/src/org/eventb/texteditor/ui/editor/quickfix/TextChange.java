/**
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen,
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html)
 * */

package org.eventb.texteditor.ui.editor.quickfix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;

/**
 * Several replacements in a text, computed on the original text. Parts of the
 * inserted text can be marked as placeholders (e.g. the type in
 * <code>y ∈ ℕ</code>) which the user can then edit in linked mode.
 */
public final class TextChange {

	private static final class Edit {
		final int offset;
		final int length;
		final String text;
		final int order;
		final List<int[]> placeholders = new ArrayList<int[]>();

		Edit(final int offset, final int length, final String text,
				final int order) {
			this.offset = offset;
			this.length = length;
			this.text = text;
			this.order = order;
		}
	}

	private final List<Edit> edits = new ArrayList<Edit>();

	/**
	 * Replaces a part of the text. Placeholders are given as pairs of offset
	 * (relative to the start of <code>text</code>) and length.
	 */
	public TextChange replace(final int offset, final int length,
			final String text, final int... placeholders) {
		final Edit edit = new Edit(offset, length, text, edits.size());
		for (int i = 0; i + 1 < placeholders.length; i += 2) {
			edit.placeholders.add(new int[] { placeholders[i],
					placeholders[i + 1] });
		}
		edits.add(edit);
		return this;
	}

	public TextChange insert(final int offset, final String text,
			final int... placeholders) {
		return replace(offset, 0, text, placeholders);
	}

	/**
	 * Convenience: inserts <code>before + placeholder + after</code> with the
	 * middle part as placeholder.
	 */
	public TextChange insertWithPlaceholder(final int offset,
			final String before, final String placeholder, final String after) {
		return insert(offset, before + placeholder + after, before.length(),
				placeholder.length());
	}

	/**
	 * Applies all edits.
	 *
	 * @return the positions of the placeholders in the changed document, in
	 *         the order of the text
	 */
	public List<Position> apply(final IDocument document)
			throws BadLocationException {
		final List<Edit> sorted = sorted();

		// final positions: shift by all edits in front of them
		final List<Position> result = new ArrayList<Position>();
		int delta = 0;
		for (final Edit edit : sorted) {
			for (final int[] placeholder : edit.placeholders) {
				result.add(new Position(edit.offset + delta + placeholder[0],
						placeholder[1]));
			}
			delta += edit.text.length() - edit.length;
		}

		// apply from back to front, the offsets stay valid
		for (int i = sorted.size() - 1; i >= 0; i--) {
			final Edit edit = sorted.get(i);
			document.replace(edit.offset, edit.length, edit.text);
		}
		return result;
	}

	/**
	 * Applies the edits to a string (for tests and previews).
	 */
	public String apply(final String text) {
		final StringBuilder result = new StringBuilder(text);
		final List<Edit> sorted = sorted();
		for (int i = sorted.size() - 1; i >= 0; i--) {
			final Edit edit = sorted.get(i);
			result.replace(edit.offset, edit.offset + edit.length, edit.text);
		}
		return result.toString();
	}

	private List<Edit> sorted() {
		final List<Edit> sorted = new ArrayList<Edit>(edits);
		// stable for insertions at the same offset: order of creation
		Collections.sort(sorted, new Comparator<Edit>() {
			@Override
			public int compare(final Edit a, final Edit b) {
				if (a.offset != b.offset) {
					return a.offset < b.offset ? -1 : 1;
				}
				return a.order - b.order;
			}
		});
		return sorted;
	}
}
