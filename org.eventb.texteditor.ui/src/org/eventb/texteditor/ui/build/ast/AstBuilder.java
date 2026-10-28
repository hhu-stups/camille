/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.build.ast;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IFileEditorInput;
import org.eventb.emf.core.EventBNamedCommentedComponentElement;
import org.eventb.emf.core.machine.Machine;
import org.eventb.texteditor.ui.MarkerHelper;
import org.eventb.texteditor.ui.TextEditorPlugin;
import org.eventb.texteditor.ui.build.IBuildPhase;
import org.eventb.texteditor.ui.editor.EventBTextEditor;
import org.eventb.texttools.ComponentNameUtil;
import org.eventb.texttools.ComponentNameUtil.Kind;
import org.eventb.texttools.ParseException;
import org.eventb.texttools.Parser;
import org.eventb.texttools.PersistenceHelper;
import org.eventb.texttools.TextPositionUtil;
import org.eventb.texttools.model.texttools.TextRange;

public class AstBuilder implements IBuildPhase {

	private final FormulaTranslator translator = new FormulaTranslator();
	private final Parser textParser = new Parser();

	private boolean successful;

	public boolean isUIPhase() {
		return false;
	}

	public boolean canFail() {
		return true;
	}

	public boolean wasSuccessful() {
		return successful;
	}

	public void run(final EventBTextEditor editor, final Resource resource,
			final IDocument document, final IProgressMonitor monitor) {
		successful = false;
		
		final EventBNamedCommentedComponentElement astRoot = build(resource, document, true, editor,
				monitor);

		if (astRoot != null) {
			final String docContent = document.get();
			TextEditorPlugin.getDomManager().storeParseResult(
					editor.getEditorInput(), docContent, astRoot);
			successful = true;
		} else {
			successful = false;
		}
	}

	private EventBNamedCommentedComponentElement build(final Resource resource,
			final IDocument document, final boolean markFormulaErrors,
			final EventBTextEditor editor, IProgressMonitor monitor) {
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}

		final EventBNamedCommentedComponentElement astRoot = createAST(resource, document);
		monitor.worked(1);

		if (astRoot != null
				&& !checkComponentHeader(editor, resource, document, astRoot)) {
			return null;
		}

		if (monitor.isCanceled()) {
			return astRoot;
		}

		if (astRoot != null) {
			/*
			 * Translate formula input from ASCII to mathmatical language
			 */
			monitor.subTask("Translating formulas");
			translator.translateFormulas(astRoot, document, editor);
			monitor.worked(1);

			if (monitor.isCanceled()) {
				return null;
			}

			/*
			 * Resolver can work even if some formulas could not be translated.
			 * Other formulas might be ok. Anyway we want to highlight errors.
			 */
			monitor.subTask("Resolving formulas");
			FormulasResolver.resolveFormulas(resource, astRoot, document,
					markFormulaErrors);
			monitor.worked(1);
			/*
			 * Add text representation as annotation to resource's model
			 */
			PersistenceHelper.addTextAnnotation(astRoot, document.get(), System
					.currentTimeMillis());
		}

		return astRoot;
	}

	private EventBNamedCommentedComponentElement createAST(final Resource resource,
			final IDocument document) {
		/*
		 * Clean up all markers which belong to our tool (including subtypes)
		 * 'cause we don't know whether following tools will be able to run,
		 * i.e., whether parsing will be successful.
		 */
		final MarkerHelper helper = new MarkerHelper(TextEditorPlugin.SYNTAXERROR_MARKER_ID);
		helper.deleteMarkers(resource, true, IResource.DEPTH_INFINITE);

		EventBNamedCommentedComponentElement component = null;

		try {
			component = textParser.parse(document);
		} catch (final ParseException e) {
			reportProblem(helper, resource, document, e, Diagnostic.ERROR);
		} catch (final RuntimeException e) {
			/*
			 * Unexpected failure inside the parser or the AST transformation.
			 * Report it as a syntax problem instead of letting it kill the
			 * reconciler thread.
			 */
			TextEditorPlugin.getPlugin().getLog().log(
					new Status(IStatus.WARNING, TextEditorPlugin.PLUGIN_ID,
							"Unexpected error while parsing", e));
			final String message = e.getLocalizedMessage() != null ? e
					.getLocalizedMessage() : e.getClass().getSimpleName();
			reportProblem(helper, resource, document, new ParseException(
					"Internal parser error: " + message, 0, 0, 1),
					Diagnostic.ERROR);
		}

		return component;
	}

	/**
	 * Checks that the text fits to the file: a machine file must contain a
	 * machine, a context file a context, and the name should be the file
	 * name.
	 *
	 * @return <code>false</code> if the text must not be merged into the
	 *         Rodin database
	 */
	private boolean checkComponentHeader(final EventBTextEditor editor,
			final Resource resource, final IDocument document,
			final EventBNamedCommentedComponentElement astRoot) {
		if (editor == null
				|| !(editor.getEditorInput() instanceof IFileEditorInput)) {
			return true;
		}

		final IFile file = ((IFileEditorInput) editor.getEditorInput())
				.getFile();
		final Kind expectedKind = ComponentNameUtil.kindForFileExtension(file
				.getFileExtension());
		if (expectedKind == null) {
			return true;
		}

		final Kind actualKind = astRoot instanceof Machine ? Kind.MACHINE
				: Kind.CONTEXT;
		final String expectedName = file.getFullPath().removeFileExtension()
				.lastSegment();
		final MarkerHelper helper = new MarkerHelper(
				TextEditorPlugin.SYNTAXERROR_MARKER_ID);

		final int[] namePosition = getNamePosition(document, astRoot);

		if (actualKind != expectedKind) {
			final String expected = expectedKind == Kind.MACHINE ? "machine"
					: "context";
			final String actual = actualKind == Kind.MACHINE ? "machine"
					: "context";
			reportProblem(helper, resource, document, createProblem(document,
					namePosition, "This file contains a " + expected
							+ ", but the text describes a " + actual
							+ ". It cannot be stored in this file."),
					Diagnostic.ERROR);
			return false;
		}

		if (astRoot.getName() != null && !astRoot.getName().equals(expectedName)) {
			reportProblem(helper, resource, document, createProblem(document,
					namePosition, "The name '" + astRoot.getName()
							+ "' differs from the file name. It will be"
							+ " renamed to '" + expectedName
							+ "' when saving."), Diagnostic.WARNING);
		}

		return true;
	}

	/**
	 * @return offset and length of the component name in the document
	 */
	private static int[] getNamePosition(final IDocument document,
			final EventBNamedCommentedComponentElement astRoot) {
		final String name = astRoot.getName();
		if (name != null) {
			final TextRange range = TextPositionUtil.getInternalPosition(
					astRoot, name);
			try {
				if (range != null
						&& name.equals(document.get(range.getOffset(),
								range.getLength()))) {
					return new int[] { range.getOffset(), range.getLength() };
				}
			} catch (final BadLocationException e) {
				// fall through
			}

			final ComponentNameUtil.NameInfo info = ComponentNameUtil
					.findComponentName(document.get());
			if (info != null) {
				return new int[] { info.offset, info.getLength() };
			}
		}

		return new int[] { 0, 1 };
	}

	private static ParseException createProblem(final IDocument document,
			final int[] position, final String message) {
		int line = 0;
		int column = 0;
		try {
			line = document.getLineOfOffset(position[0]);
			column = position[0] - document.getLineOffset(line);
		} catch (final BadLocationException e) {
			// use start of document
		}
		return new ParseException(message, line, column, Math.max(1,
				position[1]));
	}

	private void reportProblem(final MarkerHelper helper,
			final Resource resource, final IDocument document,
			final ParseException e, final int severity) {
		final IParseProblemWrapper wrappedException = new ParseExceptionWrapperDiagnostic(
				document, e);

		final BasicDiagnostic diagnostic = new BasicDiagnostic(
				severity, TextEditorPlugin.PLUGIN_ID, 0, e
						.getLocalizedMessage(), new Object[] { resource,
						wrappedException });

		try {
			helper.createMarkers(diagnostic);
		} catch (final CoreException ex) {
			TextEditorPlugin.getPlugin().getLog().log(
					new Status(IStatus.ERROR, TextEditorPlugin.PLUGIN_ID,
							"Could not create markers", ex));
		}
	}
}
