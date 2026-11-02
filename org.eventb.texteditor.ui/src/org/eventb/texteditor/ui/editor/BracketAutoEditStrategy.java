/**
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen,
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html)
 * */

package org.eventb.texteditor.ui.editor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

/**
 * Closing brackets like in other editors:
 * <ul>
 * <li>typing an opening bracket also inserts the closing one, unless the
 * line already contains an unmatched closing bracket behind the cursor</li>
 * <li>typing a closing bracket in front of the same one just moves over
 * it, if no bracket in the line is left open</li>
 * <li>deleting the opening bracket of an empty <code>()</code> or
 * <code>[]</code> deletes both</li>
 * </ul>
 */
public class BracketAutoEditStrategy implements IAutoEditStrategy {

	private static final String OPENING = "([{";
	private static final String CLOSING = ")]}";

	@Override
	public void customizeDocumentCommand(final IDocument document,
			final DocumentCommand command) {
		try {
			if (command.length == 0 && command.text != null
					&& command.text.length() == 1) {
				typed(document, command, command.text.charAt(0));
			} else if (command.length == 1 && command.text != null
					&& command.text.isEmpty()) {
				deleted(document, command);
			}
		} catch (final BadLocationException e) {
			// IGNORE, the command is executed unchanged
		}
	}

	private void typed(final IDocument document, final DocumentCommand command,
			final char c) throws BadLocationException {
		final int open = OPENING.indexOf(c);
		if (open >= 0) {
			final char close = CLOSING.charAt(open);
			if (!hasUnmatchedClosing(document, command.offset, c, close)) {
				command.text = "" + c + close;
				command.caretOffset = command.offset + 1;
				command.shiftsCaret = false;
			}
		} else if (CLOSING.indexOf(c) >= 0
				&& command.offset < document.getLength()
				&& document.getChar(command.offset) == c
				&& isBalanced(document, command.offset,
						OPENING.charAt(CLOSING.indexOf(c)), c)) {
			// the closing bracket is already there (e.g. inserted above)
			command.text = "";
			command.caretOffset = command.offset + 1;
			command.shiftsCaret = false;
		}
	}

	private void deleted(final IDocument document, final DocumentCommand command)
			throws BadLocationException {
		if (command.offset + 1 >= document.getLength()) {
			return;
		}
		final char c = document.getChar(command.offset);
		final char next = document.getChar(command.offset + 1);
		if ((c == '(' && next == ')') || (c == '[' && next == ']') || (c == '{' && next == '}')) {
			command.length = 2;
		}
	}

	/**
	 * @return <code>true</code> if the line has no unclosed opening bracket
	 *         of this kind, so another closing bracket is not needed
	 */
	private static boolean isBalanced(final IDocument document,
			final int offset, final char open, final char close)
			throws BadLocationException {
		final IRegion line = document.getLineInformationOfOffset(offset);
		final String text = document.get(line.getOffset(), line.getLength());
		int depth = 0;
		for (int i = 0; i < text.length(); i++) {
			if (text.charAt(i) == open) {
				depth++;
			} else if (text.charAt(i) == close) {
				depth--;
			}
		}
		return depth <= 0;
	}

	/**
	 * @return <code>true</code> if the rest of the line contains more closing
	 *         than opening brackets of this kind, i.e. a closing bracket
	 *         that is waiting for the bracket being typed
	 */
	private static boolean hasUnmatchedClosing(final IDocument document,
			final int offset, final char open, final char close)
			throws BadLocationException {
		final IRegion line = document.getLineInformationOfOffset(offset);
		final String rest = document.get(offset, line.getOffset()
				+ line.getLength() - offset);
		int depth = 0;
		for (int i = 0; i < rest.length(); i++) {
			final char c = rest.charAt(i);
			if (c == open) {
				depth++;
			} else if (c == close && --depth < 0) {
				return true;
			}
		}
		return false;
	}
}
