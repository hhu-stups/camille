/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.editor;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.eventb.texteditor.ui.build.ReconcilingStrategy;
import org.eventb.texteditor.ui.editor.codecompletion.DefaultContentAssist;
import org.eventb.texteditor.ui.editor.hover.EventBTextHover;
import org.eventb.texteditor.ui.editor.navigation.DeclarationHyperlinkDetector;
import org.eventb.texteditor.ui.reconciler.EventBPresentationReconciler;
import org.eventb.texteditor.ui.reconciler.partitioning.PartitionScanner;

public class SourceViewerConfiguration extends TextSourceViewerConfiguration {

	private final EventBTextEditor editor;
	private EventBPresentationReconciler presentationReconciler;
	private EventBTextHover textHover;
	private DeclarationHyperlinkDetector declarationDetector;

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
		// labels ("@inv1") form their own partition, needed for label proposals
		assistant.setContentAssistProcessor(processor,
				PartitionScanner.CONTENT_TYPE_LABEL);

		assistant
				.setInformationControlCreator(getInformationControlCreator(sourceViewer));
		assistant.enableAutoActivation(true);
		assistant.setAutoActivationDelay(500);

		return assistant;
	}

	@Override
	public ITextHover getTextHover(final ISourceViewer sourceViewer,
			final String contentType) {
		// one hover for all content types, it holds the hover index
		if (textHover == null) {
			/*
			 * Replaces the standard text hover. Annotations are filtered with
			 * the same preferences as there.
			 */
			textHover = new EventBTextHover(editor, sourceViewer,
					new EventBTextHover.AnnotationFilter() {
						@Override
						public boolean isShown(final Annotation annotation) {
							return isShownInText(annotation);
						}
					});
			if (sourceViewer.getTextWidget() != null) {
				sourceViewer.getTextWidget().addDisposeListener(
						new DisposeListener() {
							@Override
							public void widgetDisposed(final DisposeEvent e) {
								textHover.dispose();
							}
						});
			}
		}
		return textHover;
	}

	@Override
	public IHyperlinkDetector[] getHyperlinkDetectors(
			final ISourceViewer sourceViewer) {
		final IHyperlinkDetector[] others = super
				.getHyperlinkDetectors(sourceViewer);
		final IHyperlinkDetector[] result = new IHyperlinkDetector[1 + (others != null ? others.length
				: 0)];
		result[0] = getDeclarationDetector(sourceViewer);
		if (others != null) {
			System.arraycopy(others, 0, result, 1, others.length);
		}
		return result;
	}

	/**
	 * @return the detector for "jump to declaration" (Ctrl/Cmd+click, F3)
	 */
	public DeclarationHyperlinkDetector getDeclarationDetector(
			final ISourceViewer sourceViewer) {
		if (declarationDetector == null) {
			// shares the information read from the Rodin database
			declarationDetector = new DeclarationHyperlinkDetector(editor,
					(EventBTextHover) getTextHover(sourceViewer,
							IDocument.DEFAULT_CONTENT_TYPE));
		}
		return declarationDetector;
	}

	@Override
	public ITextHover getTextHover(final ISourceViewer sourceViewer,
			final String contentType, final int stateMask) {
		// independent of modifier keys
		return getTextHover(sourceViewer, contentType);
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
