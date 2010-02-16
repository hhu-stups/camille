/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.editor;

import java.util.ResourceBundle;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;

public class TrimLinesAction extends TextEditorAction {

	public TrimLinesAction(final ResourceBundle bundle, final String prefix,
			final ITextEditor editor) {
		super(bundle, prefix, editor);
		update();
	}

	@Override
	public void run() {
		final ITextEditor editor = getTextEditor();
		if (editor == null) {
			return;
		}

		if (!validateEditorInputState()) {
			return;
		}

		final IDocument document = getDocument(editor);
		if (document == null) {
			return;
		}

		int startLine = 0;
		int endLine = document.getNumberOfLines() - 1;

		final ITextSelection selection = getSelection(editor);
		if (selection != null) {
			startLine = selection.getStartLine();
			endLine = selection.getEndLine();
		}

		try {
			trimLines(document, startLine, endLine, SubMonitor.convert(null,
					"Trimming lines", 1));

			if (selection != null) {
				final int startOffset = document.getLineOffset(selection
						.getStartLine());
				editor.selectAndReveal(startOffset, 0);
			}
		} catch (final BadLocationException e) {
			// should not happen
		}
	}

	/**
	 * Returns the editor's document.
	 * 
	 * @param editor
	 *            the editor
	 * @return the editor's document
	 */
	private static IDocument getDocument(final ITextEditor editor) {
		final IDocumentProvider documentProvider = editor.getDocumentProvider();
		if (documentProvider == null) {
			return null;
		}

		final IDocument document = documentProvider.getDocument(editor
				.getEditorInput());
		if (document == null) {
			return null;
		}

		return document;
	}

	/**
	 * Returns the editor's selection.
	 * 
	 * @param editor
	 *            the editor
	 * @return the editor's selection
	 */
	private static ITextSelection getSelection(final ITextEditor editor) {
		final ISelectionProvider selectionProvider = editor
				.getSelectionProvider();
		if (selectionProvider == null) {
			return null;
		}

		final ISelection selection = selectionProvider.getSelection();
		if (!(selection instanceof ITextSelection)) {
			return null;
		}

		return (ITextSelection) selection;
	}

	@Override
	public void update() {
		super.update();
		if (!isEnabled()) {
			return;
		}

		if (!canModifyEditor()) {
			setEnabled(false);
			return;
		}

		final ITextEditor editor = getTextEditor();
		setEnabled(editor.isEditable());
	}

	public static int trimLines(final IDocument document, final int startLine,
			final int endLine, final IProgressMonitor monitor)
			throws BadLocationException {
		final SubMonitor subMonitor = SubMonitor.convert(monitor);

		if (startLine >= document.getNumberOfLines() || startLine > endLine) {
			return 0;
		}

		int removeChars = 0;
		final StringBuffer buffer = new StringBuffer();
		subMonitor.setWorkRemaining(endLine - startLine + 1);

		for (int line = startLine; line <= endLine; line++) {
			final String trimmedLine = trim(document, line);
			buffer.append(trimmedLine);
			removeChars += document.getLineLength(line) - trimmedLine.length();

			subMonitor.worked(1);
		}

		final int startLineOffset = document.getLineOffset(startLine);
		final int endLineOffset = document.getLineOffset(endLine)
				+ document.getLineLength(endLine);
		final String replaceString = buffer.toString();

		document.replace(startLineOffset, endLineOffset - startLineOffset,
				replaceString);
		subMonitor.worked(1);

		return removeChars;
	}

	private static String trim(final IDocument document, final int line)
			throws BadLocationException {
		final int lineOffset = document.getLineOffset(line);
		final int lineDelimiterLength = getLineDelimiterLength(document, line);
		int lineLength = document.getLineLength(line) - lineDelimiterLength;

		// correct new line length until no trailing whitespace left
		while (lineLength > 0
				&& Character.isWhitespace(document.getChar(lineOffset
						+ lineLength - 1))) {
			lineLength--;
		}

		final String lineDelimiter = lineDelimiterLength > 0 ? document
				.getLineDelimiter(line) : "";
		return document.get(lineOffset, lineLength) + lineDelimiter;
	}

	private static int getLineDelimiterLength(final IDocument document,
			final int line) throws BadLocationException {
		final String lineDelimiter = document.getLineDelimiter(line);
		return lineDelimiter != null ? lineDelimiter.length() : 0;
	}
}
