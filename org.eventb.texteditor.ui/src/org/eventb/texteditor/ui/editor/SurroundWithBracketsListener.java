/**
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen,
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html)
 * */

package org.eventb.texteditor.ui.editor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Point;

/**
 * Typing an opening bracket while text is selected puts the selection into
 * brackets instead of replacing it: select <code>x + y</code>, type
 * <code>(</code> and get <code>(x + y)</code>. The inner text stays selected
 * so the brackets can be nested by typing again.
 * <p>
 * This is a single document modification (one undo step), so it is as cheap
 * as typing one character.
 * </p>
 */
public class SurroundWithBracketsListener implements VerifyKeyListener {

	private static final String OPENING = "([{";
	private static final String CLOSING = ")]}";

	private final EventBTextEditor editor;
	private final ITextViewer viewer;

	public SurroundWithBracketsListener(final EventBTextEditor editor,
			final ITextViewer viewer) {
		this.editor = editor;
		this.viewer = viewer;
	}

	@Override
	public void verifyKey(final VerifyEvent event) {
		if (!event.doit) {
			return;
		}

		final int index = OPENING.indexOf(event.character);
		if (index < 0) {
			return;
		}

		/*
		 * Ignore shortcuts like Ctrl+(. AltGr (reported as Ctrl+Alt on
		 * Windows) must still work, it is needed for [ and { on many
		 * keyboard layouts (e.g. German).
		 */
		final boolean ctrlOnly = (event.stateMask & SWT.CTRL) != 0
				&& (event.stateMask & SWT.ALT) == 0;
		if (ctrlOnly || (event.stateMask & SWT.COMMAND) != 0) {
			return;
		}

		if (!editor.isEditable() || editor.isInLinkedMode()) {
			return;
		}

		final Point selection = viewer.getSelectedRange();
		if (selection == null || selection.y <= 0) {
			return;
		}

		final IDocument document = viewer.getDocument();
		if (document == null) {
			return;
		}

		final int offset = selection.x;
		final int length = selection.y;

		final IRewriteTarget rewriteTarget = editor.getAdapter(IRewriteTarget.class);
		if (rewriteTarget != null) {
			rewriteTarget.beginCompoundChange();
		}

		try {
			final String selected = document.get(offset, length);
			document.replace(offset, length, OPENING.charAt(index) + selected
					+ CLOSING.charAt(index));
			viewer.setSelectedRange(offset + 1, length);
			event.doit = false;
		} catch (final BadLocationException e) {
			// IGNORE, let the key be handled normally
		} finally {
			if (rewriteTarget != null) {
				rewriteTarget.endCompoundChange();
			}
		}
	}
}
