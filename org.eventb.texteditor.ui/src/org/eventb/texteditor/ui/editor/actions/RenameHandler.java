/**
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen,
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html)
 * */

package org.eventb.texteditor.ui.editor.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eventb.texteditor.ui.TextEditorPlugin;
import org.eventb.texteditor.ui.editor.EventBTextEditor;
import org.eventb.texteditor.ui.editor.refactoring.ProjectStructure;
import org.eventb.texteditor.ui.editor.refactoring.RenameRefactoring;
import org.eventb.texteditor.ui.editor.refactoring.RenameRefactoring.RenameException;
import org.rodinp.core.RodinDBException;

/**
 * Renames the variable, constant, carrier set, parameter or event at the
 * cursor in all affected machines and contexts.
 */
public class RenameHandler extends AbstractHandler {

	private static final String TITLE = "Rename";

	@Override
	public Object execute(final ExecutionEvent event) {
		final IEditorPart part = HandlerUtil.getActiveEditor(event);
		if (!(part instanceof EventBTextEditor)
				|| !(part.getEditorInput() instanceof IFileEditorInput)) {
			return null;
		}

		final EventBTextEditor editor = (EventBTextEditor) part;
		final Shell shell = editor.getSite().getShell();
		final IWorkbenchPage page = editor.getSite().getPage();
		final IFile file = ((IFileEditorInput) editor.getEditorInput())
				.getFile();

		/*
		 * The refactoring works on the saved state (Rodin database), so the
		 * editor content must be saved.
		 */
		if (editor.isDirty()) {
			if (!MessageDialog.openConfirm(shell, TITLE,
					"The editor has unsaved changes. Save them and continue?")) {
				return null;
			}
			editor.doSave(null);
			if (editor.isDirty()) {
				return null;
			}
		}

		final RenameRefactoring refactoring;
		try {
			refactoring = resolve(editor, file);
		} catch (final RenameException e) {
			MessageDialog.openInformation(shell, TITLE, e.getMessage());
			return null;
		} catch (final RodinDBException e) {
			MessageDialog.openError(shell, TITLE,
					"Cannot read the Rodin database: " + e.getMessage());
			return null;
		}

		final String problem = checkOtherEditors(page, refactoring);
		if (problem != null) {
			MessageDialog.openInformation(shell, TITLE, problem);
			return null;
		}

		final String description = refactoring.getTarget().description;
		final InputDialog dialog = new InputDialog(shell, TITLE + " "
				+ capitalize(description), "New name for " + description
				+ " '" + refactoring.getName() + "' of "
				+ refactoring.getDeclaringComponent()
				+ ".\nChanges are made in: "
				+ refactoring.describeAffectedComponents(),
				refactoring.getName(), new IInputValidator() {
					@Override
					public String isValid(final String newText) {
						return refactoring.validate(newText.trim());
					}
				});
		if (dialog.open() != Window.OK) {
			return null;
		}

		final String newName = dialog.getValue().trim();
		BusyIndicator.showWhile(shell.getDisplay(), new Runnable() {
			@Override
			public void run() {
				apply(editor, page, refactoring, newName);
			}
		});
		return null;
	}

	private static RenameRefactoring resolve(final EventBTextEditor editor,
			final IFile file) throws RenameException, RodinDBException {
		final ProjectStructure[] structure = new ProjectStructure[1];
		final RodinDBException[] exception = new RodinDBException[1];
		BusyIndicator.showWhile(editor.getSite().getShell().getDisplay(),
				new Runnable() {
					@Override
					public void run() {
						try {
							structure[0] = ProjectStructure.read(file
									.getProject());
						} catch (final RodinDBException e) {
							exception[0] = e;
						}
					}
				});
		if (exception[0] != null) {
			throw exception[0];
		}

		final IDocument document = editor.getDocumentProvider().getDocument(
				editor.getEditorInput());
		final String componentName = file.getFullPath().removeFileExtension()
				.lastSegment();
		return RenameRefactoring.resolve(structure[0], componentName,
				document.get(), editor.getCursorOffset());
	}

	/**
	 * Files that are open in another editor (e.g. the Rodin editor) with
	 * unsaved changes cannot be changed.
	 */
	private static String checkOtherEditors(final IWorkbenchPage page,
			final RenameRefactoring refactoring) {
		for (final IFile file : refactoring.getAffectedFiles()) {
			for (final IEditorReference reference : page.findEditors(
					new FileEditorInput(file), null,
					IWorkbenchPage.MATCH_INPUT)) {
				if (!TextEditorPlugin.TEXTEDITOR_ID.equals(reference.getId())
						&& reference.isDirty()) {
					return file.getName()
							+ " has unsaved changes in another editor. Save or close it first.";
				}
			}
		}
		return null;
	}

	private static void apply(final EventBTextEditor activeEditor,
			final IWorkbenchPage page, final RenameRefactoring refactoring,
			final String newName) {
		final List<String> notSaved = new ArrayList<String>();
		final List<String> failed = new ArrayList<String>();
		int occurrences = 0;

		for (final IFile file : refactoring.getAffectedFiles()) {
			EventBTextEditor editor = findEditor(page, file);
			boolean openedByUs = false;
			if (editor == null) {
				try {
					final IEditorPart part = IDE.openEditor(page, file,
							TextEditorPlugin.TEXTEDITOR_ID, false);
					if (part instanceof EventBTextEditor) {
						editor = (EventBTextEditor) part;
						openedByUs = true;
					}
				} catch (final PartInitException e) {
					TextEditorPlugin.getPlugin().getLog().log(e.getStatus());
				}
			}
			if (editor == null) {
				failed.add(file.getName());
				continue;
			}

			final boolean wasDirty = editor.isDirty();
			try {
				occurrences += rename(editor, file, refactoring, newName,
						editor == activeEditor);
			} catch (final BadLocationException e) {
				failed.add(file.getName());
			}

			if (!wasDirty) {
				editor.doSave(null);
			} else {
				notSaved.add(file.getName());
			}
			if (openedByUs && !editor.isDirty()) {
				page.closeEditor(editor, false);
			}
		}

		activeEditor.getEditorSite().getActionBars().getStatusLineManager()
				.setMessage(
						"Renamed '" + refactoring.getName() + "' to '"
								+ newName + "': " + occurrences
								+ " occurrence(s) in "
								+ refactoring.describeAffectedComponents());

		if (!failed.isEmpty() || !notSaved.isEmpty()) {
			final StringBuilder message = new StringBuilder();
			if (!failed.isEmpty()) {
				message.append("Could not change: ").append(failed)
						.append("\n");
			}
			if (!notSaved.isEmpty()) {
				message.append("Changed but not saved (they had unsaved changes before): ")
						.append(notSaved);
			}
			MessageDialog.openWarning(activeEditor.getSite().getShell(),
					TITLE, message.toString());
		}
	}

	/**
	 * Replaces all occurrences in one editor as one undoable change.
	 *
	 * @return the number of replaced occurrences
	 */
	private static int rename(final EventBTextEditor editor, final IFile file,
			final RenameRefactoring refactoring, final String newName,
			final boolean keepCursor) throws BadLocationException {
		final IDocument document = editor.getDocumentProvider().getDocument(
				editor.getEditorInput());
		final List<Integer> offsets = refactoring.computeEdits(file,
				document.get());
		if (offsets.isEmpty()) {
			return 0;
		}

		final int oldLength = refactoring.getName().length();
		final int delta = newName.length() - oldLength;
		int cursor = keepCursor ? editor.getCursorOffset() : -1;

		final IRewriteTarget rewriteTarget = editor
				.getAdapter(IRewriteTarget.class);
		if (rewriteTarget != null) {
			rewriteTarget.beginCompoundChange();
		}
		try {
			// from back to front, so that the offsets stay valid
			int newCursor = cursor;
			for (int i = offsets.size() - 1; i >= 0; i--) {
				final int offset = offsets.get(i);
				document.replace(offset, oldLength, newName);
				if (cursor >= offset + oldLength) {
					newCursor += delta;
				} else if (cursor > offset) {
					// inside of the renamed name
					newCursor = offset + Math.min(cursor - offset,
							newName.length());
				}
			}
			cursor = newCursor;
		} finally {
			if (rewriteTarget != null) {
				rewriteTarget.endCompoundChange();
			}
		}

		if (keepCursor && cursor >= 0) {
			editor.selectAndReveal(cursor, 0);
		}
		return offsets.size();
	}

	private static EventBTextEditor findEditor(final IWorkbenchPage page,
			final IFile file) {
		for (final IEditorReference reference : page.findEditors(
				new FileEditorInput(file), TextEditorPlugin.TEXTEDITOR_ID,
				IWorkbenchPage.MATCH_INPUT | IWorkbenchPage.MATCH_ID)) {
			final IEditorPart part = reference.getEditor(true);
			if (part instanceof EventBTextEditor) {
				return (EventBTextEditor) part;
			}
		}
		return null;
	}

	private static String capitalize(final String text) {
		return Character.toUpperCase(text.charAt(0)) + text.substring(1);
	}
}
