/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.build.ast;

import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jface.text.IDocument;
import org.eventb.core.ast.ASTProblem;
import org.eventb.emf.core.EventBNamedCommentedComponentElement;
import org.eventb.texteditor.ui.MarkerHelper;
import org.eventb.texteditor.ui.TextEditorPlugin;
import org.eventb.texttools.TextPositionUtil;
import org.eventb.texttools.formulas.FormulaParseException;
import org.eventb.texttools.formulas.FormulaResolver;
import org.eventb.texttools.model.texttools.TextRange;
import org.rodinp.core.RodinMarkerUtil;

public class FormulasResolver {

	private static final MarkerHelper markerHelper = new MarkerHelper(
			TextEditorPlugin.SYNTAXERROR_MARKER_ID);

	public static void resolveFormulas(final Resource resource,
			final EventBNamedCommentedComponentElement astRoot,
			final IDocument document, final boolean markErrors) {

		String projectName = resource.getURI().segment(1);
		final List<FormulaParseException> exceptions = FormulaResolver
				.resolveAllFormulas(projectName, astRoot);
		if (markErrors && exceptions.size() > 0) {
			markerHelper.deleteMarkers(resource,
					RodinMarkerUtil.RODIN_PROBLEM_MARKER, true,
					IResource.DEPTH_ZERO);
			markErrors(exceptions, resource, document);
		}
	}

	private static void markErrors(
			final List<FormulaParseException> exceptions,
			final Resource resource, final IDocument document) {
		// TODO Do we really want to add a marker/diagnostic to the resource?
		// When the model is saved the RodinDB will create another marker for
		// it. Maybe having a marker in the texteditor is enough.

		final BasicDiagnostic diagnostic = new BasicDiagnostic(
				Diagnostic.ERROR, TextEditorPlugin.PLUGIN_ID, 0,
				"Error occured when parsing the formulas",
				new Object[] { resource });

		for (final FormulaParseException ex : exceptions) {
			final String inputFormula = ex.getFormula();
			final TextRange range = TextPositionUtil.getInternalPosition(
					ex.getEmfObject(), inputFormula);
			final int offset = range.getOffset();

			final List<ASTProblem> problems = ex.getAstProblems();
			for (final ASTProblem problem : problems) {
				diagnostic.add(createChildDiagnostic(problem, offset,
						inputFormula, document, resource));
			}
		}

		try {
			markerHelper.createMarkers(diagnostic);
		} catch (final CoreException e) {
			TextEditorPlugin.INSTANCE.log(e);
		}

	}

	private static Diagnostic createChildDiagnostic(final ASTProblem problem,
			final int offset, final String inputFormula,
			final IDocument document, final Resource resource) {

		final IParseProblemWrapper wrappedException = new FormulaExceptionWrapperDiagnostic(
				problem, offset, document);

		final BasicDiagnostic diagnostic = new BasicDiagnostic(
				Diagnostic.ERROR, TextEditorPlugin.PLUGIN_ID, 0,
				wrappedException.getMessage(), new Object[] { resource,
						wrappedException });

		return diagnostic;
	}
}
