/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.build.ast;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jface.text.IDocument;
import org.eventb.emf.core.EventBNamedCommentedComponentElement;
import org.eventb.texteditor.ui.MarkerHelper;
import org.eventb.texteditor.ui.TextEditorPlugin;
import org.eventb.texteditor.ui.build.IBuildPhase;
import org.eventb.texteditor.ui.editor.EventBTextEditor;
import org.eventb.texttools.ParseException;
import org.eventb.texttools.Parser;
import org.eventb.texttools.PersistenceHelper;

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
			final IParseProblemWrapper wrappedException = new ParseExceptionWrapperDiagnostic(
					document, e);

			final BasicDiagnostic diagnostic = new BasicDiagnostic(
					Diagnostic.ERROR, TextEditorPlugin.PLUGIN_ID, 0, e
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

		return component;
	}
}
