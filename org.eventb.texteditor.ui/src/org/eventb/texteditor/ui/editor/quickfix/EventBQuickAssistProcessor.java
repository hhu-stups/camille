/**
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen,
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html)
 * */

package org.eventb.texteditor.ui.editor.quickfix;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.ui.IFileEditorInput;
import org.eventb.texteditor.ui.editor.EventBTextEditor;
import org.eventb.texteditor.ui.editor.refactoring.ProjectStructure;
import org.rodinp.core.RodinDBException;

/**
 * Quick fixes (Ctrl+1 / Cmd+1) for Camille, see
 * {@link DeclarationQuickFixes}.
 */
public class EventBQuickAssistProcessor implements IQuickAssistProcessor {

	private final EventBTextEditor editor;

	public EventBQuickAssistProcessor(final EventBTextEditor editor) {
		this.editor = editor;
	}

	@Override
	public ICompletionProposal[] computeQuickAssistProposals(
			final IQuickAssistInvocationContext context) {
		final IDocument document = context.getSourceViewer().getDocument();
		if (document == null) {
			return null;
		}

		final List<DeclarationQuickFixes.Fix> fixes = DeclarationQuickFixes
				.compute(document.get(), context.getOffset(), readStructure());
		if (fixes.isEmpty()) {
			return null;
		}

		final ICompletionProposal[] proposals = new ICompletionProposal[fixes
				.size()];
		for (int i = 0; i < proposals.length; i++) {
			proposals[i] = new QuickFixProposal(fixes.get(i), editor);
		}
		return proposals;
	}

	private ProjectStructure readStructure() {
		if (editor == null
				|| !(editor.getEditorInput() instanceof IFileEditorInput)) {
			return null;
		}
		final IFile file = ((IFileEditorInput) editor.getEditorInput())
				.getFile();
		try {
			return ProjectStructure.read(file.getProject());
		} catch (final RodinDBException e) {
			return null;
		}
	}

	@Override
	public String getErrorMessage() {
		return null;
	}

	@Override
	public boolean canFix(final Annotation annotation) {
		return false;
	}

	@Override
	public boolean canAssist(final IQuickAssistInvocationContext context) {
		return true;
	}
}
