/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.editor.codecompletion;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension3;
import org.eclipse.jface.text.link.ILinkedModeListener;
import org.eclipse.jface.text.link.InclusivePositionUpdater;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.link.ProposalPosition;
import org.eclipse.jface.text.templates.GlobalTemplateVariables;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.jface.text.templates.TemplateProposal;
import org.eclipse.jface.text.templates.TemplateVariable;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;
import org.eventb.texteditor.ui.TextEditorPlugin;
import org.eventb.texteditor.ui.editor.EventBTextEditor;

public class EventBTemplateProposal extends TemplateProposal implements
		IRelevantProposal, ICompletionProposal, ICompletionProposalExtension,
		ICompletionProposalExtension2, ICompletionProposalExtension3 {

	private final IRegion fRegion;

	private IRegion fSelectedRegion; // initialized by apply()
	private InclusivePositionUpdater fUpdater;

	public EventBTemplateProposal(final Template template,
			final TemplateContext context, final IRegion region,
			final Image image, final int relevance) {
		super(template, context, region, image, relevance);

		fRegion = region;
	}

	/**
	 * Inserts the template offered by this proposal into the viewer's document
	 * and sets up a <code>LinkedModeUI</code> on the viewer to edit any of the
	 * template's unresolved variables.
	 * 
	 * @param viewer
	 *            {@inheritDoc}
	 * @param trigger
	 *            {@inheritDoc}
	 * @param stateMask
	 *            {@inheritDoc}
	 * @param offset
	 *            {@inheritDoc}
	 */
	@Override
	public void apply(final ITextViewer viewer, final char trigger,
			final int stateMask, final int offset) {

		final IDocument document = viewer.getDocument();
		try {
			final TemplateContext fContext = getContext();
			fContext.setReadOnly(false);
			int start;
			TemplateBuffer templateBuffer;
			{
				final int oldReplaceOffset = getReplaceOffset();
				try {
					// this may already modify the document (e.g. add imports)
					templateBuffer = fContext.evaluate(getTemplate());
				} catch (final TemplateException e1) {
					fSelectedRegion = fRegion;
					return;
				}

				start = getReplaceOffset();
				final int shift = start - oldReplaceOffset;
				final int end = Math.max(getReplaceEndOffset(), offset + shift);

				// insert template string
				final String templateString = templateBuffer.getString();
				document.replace(start, end - start, templateString);
			}

			// translate positions
			final LinkedModeModel model = new LinkedModeModel();
			final TemplateVariable[] variables = templateBuffer.getVariables();
			boolean hasPositions = false;
			for (int i = 0; i != variables.length; i++) {
				final TemplateVariable variable = variables[i];

				if (variable.isUnambiguous()) {
					continue;
				}

				final LinkedPositionGroup group = new LinkedPositionGroup();

				final int[] offsets = variable.getOffsets();
				final int length = variable.getLength();

				LinkedPosition first;
				{
					final String[] values = variable.getValues();
					final ICompletionProposal[] proposals = new ICompletionProposal[values.length];
					for (int j = 0; j < values.length; j++) {
						ensurePositionCategoryInstalled(document, model);
						final Position pos = new Position(offsets[0] + start,
								length);
						document.addPosition(getCategory(), pos);
						proposals[j] = new PositionBasedCompletionProposal(
								values[j], pos, length);
					}

					if (proposals.length > 1) {
						first = new ProposalPosition(document, offsets[0]
								+ start, length, proposals);
					} else {
						first = new LinkedPosition(document,
								offsets[0] + start, length);
					}
				}

				for (int j = 0; j != offsets.length; j++) {
					if (j == 0) {
						group.addPosition(first);
					} else {
						group.addPosition(new LinkedPosition(document,
								offsets[j] + start, length));
					}
				}

				model.addGroup(group);
				hasPositions = true;
			}

			if (hasPositions) {
				model.forceInstall();

				final EventBTextEditor editor = getEventBEditor();
				if (editor != null) {
					model.addLinkingListener(new EditorSynchronizer(editor));
				}

				final LinkedModeUI ui = new EditorLinkedModeUI(model, viewer);
				ui.setExitPosition(viewer, getCaretOffset(templateBuffer)
						+ start, 0, Integer.MAX_VALUE);
				ui.enter();

				fSelectedRegion = ui.getSelectedRegion();
			} else {
				ensurePositionCategoryRemoved(document);
				fSelectedRegion = new Region(getCaretOffset(templateBuffer)
						+ start, 0);
			}
		} catch (final BadLocationException e) {
			openErrorDialog(viewer.getTextWidget().getShell(), e);
			ensurePositionCategoryRemoved(document);
			fSelectedRegion = fRegion;
		} catch (final BadPositionCategoryException e) {
			openErrorDialog(viewer.getTextWidget().getShell(), e);
			fSelectedRegion = fRegion;
		}

	}

	private EventBTextEditor getEventBEditor() {
		final IEditorPart part = TextEditorPlugin.getPlugin().getActivePage()
				.getActiveEditor();
		if (part instanceof EventBTextEditor) {
			return (EventBTextEditor) part;
		} else {
			return null;
		}
	}

	private void ensurePositionCategoryInstalled(final IDocument document,
			final LinkedModeModel model) {
		if (!document.containsPositionCategory(getCategory())) {
			document.addPositionCategory(getCategory());
			fUpdater = new InclusivePositionUpdater(getCategory());
			document.addPositionUpdater(fUpdater);

			model.addLinkingListener(new ILinkedModeListener() {

				/*
				 * @see
				 * org.eclipse.jface.text.link.ILinkedModeListener#left(org.
				 * eclipse.jface.text.link.LinkedModeModel, int)
				 */
				public void left(final LinkedModeModel environment,
						final int flags) {
					ensurePositionCategoryRemoved(document);
				}

				public void suspend(final LinkedModeModel environment) {
				}

				public void resume(final LinkedModeModel environment,
						final int flags) {
				}
			});
		}
	}

	private void ensurePositionCategoryRemoved(final IDocument document) {
		if (document.containsPositionCategory(getCategory())) {
			try {
				document.removePositionCategory(getCategory());
			} catch (final BadPositionCategoryException e) {
				// ignore
			}
			document.removePositionUpdater(fUpdater);
		}
	}

	private String getCategory() {
		return "TemplateProposalCategory_" + toString(); //$NON-NLS-1$
	}

	private int getCaretOffset(final TemplateBuffer buffer) {

		final TemplateVariable[] variables = buffer.getVariables();
		for (int i = 0; i != variables.length; i++) {
			final TemplateVariable variable = variables[i];
			if (variable.getType().equals(GlobalTemplateVariables.Cursor.NAME)) {
				return variable.getOffsets()[0];
			}
		}

		return buffer.getString().length();
	}

	private void openErrorDialog(final Shell shell, final Exception e) {
		MessageDialog.openError(shell, "Template Evaluation Error", e
				.getMessage());
	}

	/*
	 * @see ICompletionProposal#getSelection(IDocument)
	 */
	@Override
	public Point getSelection(final IDocument document) {
		return new Point(fSelectedRegion.getOffset(), fSelectedRegion
				.getLength());
	}
}
