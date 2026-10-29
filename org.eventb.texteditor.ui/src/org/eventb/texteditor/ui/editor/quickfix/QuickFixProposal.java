/**
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen,
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html)
 * */

package org.eventb.texteditor.ui.editor.quickfix;

import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;
import org.eventb.texteditor.ui.Images;
import org.eventb.texteditor.ui.TextEditorPlugin;
import org.eventb.texteditor.ui.editor.EventBTextEditor;
import org.eventb.texteditor.ui.editor.codecompletion.EditorSynchronizer;

/**
 * Applies a {@link DeclarationQuickFixes.Fix} as one undoable change. The
 * inserted placeholders (e.g. the type <code>ℕ</code>) can then be edited
 * one after another with Tab.
 */
public class QuickFixProposal implements ICompletionProposal,
		ICompletionProposalExtension2 {

	private final DeclarationQuickFixes.Fix fix;
	private final EventBTextEditor editor;
	private Point selection;

	public QuickFixProposal(final DeclarationQuickFixes.Fix fix,
			final EventBTextEditor editor) {
		this.fix = fix;
		this.editor = editor;
	}

	@Override
	public void apply(final ITextViewer viewer, final char trigger,
			final int stateMask, final int offset) {
		final IDocument document = viewer.getDocument();
		final IRewriteTarget rewriteTarget = editor != null ? editor
				.getAdapter(IRewriteTarget.class) : null;
		final List<Position> placeholders;

		if (rewriteTarget != null) {
			rewriteTarget.beginCompoundChange();
		}
		try {
			placeholders = fix.change.apply(document);
		} catch (final BadLocationException e) {
			TextEditorPlugin.getPlugin().getLog()
					.warn("Quick fix could not be applied", e);
			return;
		} finally {
			if (rewriteTarget != null) {
				rewriteTarget.endCompoundChange();
			}
		}

		if (placeholders.isEmpty()) {
			selection = null;
			return;
		}

		final Position first = placeholders.get(0);
		selection = new Point(first.getOffset(), first.getLength());
		try {
			final LinkedModeModel model = new LinkedModeModel();
			int sequence = 0;
			for (final Position placeholder : placeholders) {
				final LinkedPositionGroup group = new LinkedPositionGroup();
				group.addPosition(new LinkedPosition(document, placeholder
						.getOffset(), placeholder.getLength(), sequence++));
				model.addGroup(group);
			}
			model.forceInstall();
			if (editor != null) {
				model.addLinkingListener(new EditorSynchronizer(editor));
			}

			final Position last = placeholders.get(placeholders.size() - 1);
			final LinkedModeUI ui = new EditorLinkedModeUI(model, viewer);
			ui.setExitPosition(viewer, last.getOffset() + last.getLength(), 0,
					Integer.MAX_VALUE);
			ui.setCyclingMode(LinkedModeUI.CYCLE_WHEN_NO_PARENT);
			ui.enter();
			final org.eclipse.jface.text.IRegion region = ui
					.getSelectedRegion();
			selection = new Point(region.getOffset(), region.getLength());
		} catch (final BadLocationException e) {
			// IGNORE, the change is applied without linked mode
		}
	}

	@Override
	public void apply(final IDocument document) {
		try {
			fix.change.apply(document);
		} catch (final BadLocationException e) {
			// IGNORE
		}
	}

	@Override
	public Point getSelection(final IDocument document) {
		return selection;
	}

	@Override
	public String getAdditionalProposalInfo() {
		return null;
	}

	@Override
	public String getDisplayString() {
		return fix.label;
	}

	@Override
	public Image getImage() {
		return Images.getImage(fix.image);
	}

	@Override
	public IContextInformation getContextInformation() {
		return null;
	}

	@Override
	public void selected(final ITextViewer viewer, final boolean smartToggle) {
		// IGNORE
	}

	@Override
	public void unselected(final ITextViewer viewer) {
		// IGNORE
	}

	@Override
	public boolean validate(final IDocument document, final int offset,
			final org.eclipse.jface.text.DocumentEvent event) {
		return false;
	}
}
