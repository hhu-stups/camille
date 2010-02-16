/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.editor;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.eventb.texteditor.ui.build.ReconcilingStrategy;
import org.eventb.texteditor.ui.editor.codecompletion.DefaultContentAssist;
import org.eventb.texteditor.ui.reconciler.EventBPresentationReconciler;
import org.eventb.texteditor.ui.reconciler.partitioning.PartitionScanner;

public class SourceViewerConfiguration extends TextSourceViewerConfiguration {

	private final EventBTextEditor editor;
	private EventBPresentationReconciler presentationReconciler;

	public SourceViewerConfiguration(final EventBTextEditor editor,
			final IPreferenceStore preferenceStore) {
		super(preferenceStore);
		this.editor = editor;
	}

	@Override
	public IContentAssistant getContentAssistant(
			final ISourceViewer sourceViewer) {
		final ContentAssistant assistant = new ContentAssistant();
		final IContentAssistProcessor processor = new DefaultContentAssist(
				editor);

		assistant.setContentAssistProcessor(processor,
				IDocument.DEFAULT_CONTENT_TYPE);
		assistant.setContentAssistProcessor(processor,
				PartitionScanner.CONTENT_TYPE_STRUCTURAL_KEYWORD);
		assistant.setContentAssistProcessor(processor,
				PartitionScanner.CONTENT_TYPE_FORMULA_KEYWORD);

		assistant
				.setInformationControlCreator(getInformationControlCreator(sourceViewer));
		assistant.enableAutoActivation(true);
		assistant.setAutoActivationDelay(500);

		return assistant;
	}

	@Override
	public IPresentationReconciler getPresentationReconciler(
			final ISourceViewer sourceViewer) {
		if (presentationReconciler == null) {
			presentationReconciler = new EventBPresentationReconciler();
			presentationReconciler
					.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
		}

		return presentationReconciler;
	}

	@Override
	public final IReconciler getReconciler(final ISourceViewer sourceViewer) {
		final ReconcilingStrategy reconcileStrategy = new ReconcilingStrategy(
				editor);

		final MonoReconciler reconciler = new MonoReconciler(reconcileStrategy,
				false);
		reconciler.setIsAllowedToModifyDocument(true);
		reconciler.setProgressMonitor(new NullProgressMonitor());
		reconciler.setDelay(500);

		return reconciler;
	}

	@Override
	public String[] getConfiguredContentTypes(final ISourceViewer sourceViewer) {
		return PartitionScanner.CONTENT_TYPES;
	}
}
