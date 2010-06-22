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
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eventb.texteditor.ui.TextEditorPlugin;
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
		if (!hasSyntaxErrors(editor)) {
			final int cursorOffset = editor.getCursorOffset();

			final IEditorInput editorInput = editor.getEditorInput();
			final ParseResult parseResult = TextEditorPlugin.getDomManager()
					.getLastParseResult(editorInput);

			if (parseResult != null) {
				final IDocument document = editor.getDocumentProvider()
						.getDocument(editor.getEditorInput());

				if (document != null) {
					final String remberedContent = document.get();

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

					if (!newContent.equals(remberedContent)) {
						document.set(newContent);
						editor.setCurserOffset(cursorOffset);
					}
				}
			}
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
