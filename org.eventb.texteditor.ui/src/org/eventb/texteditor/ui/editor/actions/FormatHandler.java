/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.editor.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eventb.texteditor.ui.TextEditorPlugin;
import org.eventb.texteditor.ui.build.Builder;
import org.eventb.texteditor.ui.build.dom.DomManager.ParseResult;
import org.eventb.texteditor.ui.editor.EventBTextEditor;
import org.eventb.texttools.prettyprint.PrettyPrinter;

public class FormatHandler extends AbstractHandler {
	public FormatHandler() {
		super();
		setBaseEnabled(true);
	}

	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);

		if (activeEditor != null && activeEditor instanceof EventBTextEditor) {
			try {
				formatContent((EventBTextEditor) activeEditor);
			} catch (final CoreException e) {
				throw new ExecutionException("Content formatting failed", e);
			}
		}

		return null;
	}

	private void formatContent(final EventBTextEditor editor)
			throws CoreException {
		if (!editor.isEditable()) {
			return;
		}

		final IEditorInput editorInput = editor.getEditorInput();
		final IDocument document = editor.getDocumentProvider().getDocument(
				editorInput);
		if (document == null) {
			return;
		}

		final String rememberedContent = document.get();
		ParseResult parseResult = TextEditorPlugin.getDomManager()
				.getLastParseResult(editorInput);

		/*
		 * The last parse result is produced by the reconciler with a delay.
		 * If the user formats right after typing it may be outdated, and
		 * formatting it would silently throw away the latest edits. So make
		 * sure we format exactly the current text.
		 */
		if (parseResult == null
				|| !rememberedContent.equals(parseResult.textInput)) {
			final Builder builder = new Builder();
			builder.run(editor, document, null);
			if (!builder.wasSuccessful()) {
				// current text has syntax errors, don't touch it
				return;
			}
			parseResult = TextEditorPlugin.getDomManager().getLastParseResult(
					editorInput);
		} else if (hasSyntaxErrors(editor)) {
			return;
		}

		if (parseResult == null
				|| !rememberedContent.equals(parseResult.textInput)) {
			return;
		}

		final int cursorOffset = editor.getCursorOffset();

		/* prefer \n as the line delimiter */
		String lineDelimiter = document.getLegalLineDelimiters()[0];
		for (int i = 1; i < document.getLegalLineDelimiters().length; i++) {
			if (document.getLegalLineDelimiters()[i].equals("\n")) {
				lineDelimiter = "\n";
				break;
			}
		}

		final StringBuilder buffer = new StringBuilder();
		new PrettyPrinter(buffer, lineDelimiter, null)
				.prettyPrint(parseResult.astRoot);

		final String newContent = buffer.toString();

		if (!newContent.equals(rememberedContent)) {
			try {
				/*
				 * replace() instead of set(): keeps it a normal, undoable edit
				 */
				document.replace(0, document.getLength(), newContent);
			} catch (final BadLocationException e) {
				return;
			}
			editor.setCurserOffset(Math.min(cursorOffset, document.getLength()));
		}
	}

	private boolean hasSyntaxErrors(final EventBTextEditor editor)
			throws CoreException {
		final IEditorInput editorInput = editor.getEditorInput();

		if (editorInput instanceof IFileEditorInput) {
			final IFileEditorInput fileInput = (IFileEditorInput) editorInput;
			final IMarker[] markers = fileInput.getFile().findMarkers(
					TextEditorPlugin.SYNTAXERROR_MARKER_ID, true,
					IResource.DEPTH_INFINITE);

			return markers.length > 0;
		}

		return true;
	}
}
