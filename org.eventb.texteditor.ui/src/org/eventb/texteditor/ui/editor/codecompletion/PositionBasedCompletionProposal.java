/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.editor.codecompletion;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

/**
 * A position based completion proposal.
 * 
 * @since 3.0
 */
final class PositionBasedCompletionProposal implements ICompletionProposal,
		ICompletionProposalExtension2 {

	/** The string to be displayed in the completion proposal popup */
	private final String fDisplayString;
	/** The replacement string */
	private final String fReplacementString;
	/** The replacement position. */
	private final Position fReplacementPosition;
	/** The cursor position after this proposal has been applied */
	private final int fCursorPosition;
	/** The image to be displayed in the completion proposal popup */
	private final Image fImage;
	/** The context information of this proposal */
	private final IContextInformation fContextInformation;
	/** The additional info of this proposal */
	private final String fAdditionalProposalInfo;

	/**
	 * Creates a new completion proposal based on the provided information. The
	 * replacement string is considered being the display string too. All
	 * remaining fields are set to <code>null</code>.
	 * 
	 * @param replacementString
	 *            the actual string to be inserted into the document
	 * @param replacementPosition
	 *            the position of the text to be replaced
	 * @param cursorPosition
	 *            the position of the cursor following the insert relative to
	 *            replacementOffset
	 */
	public PositionBasedCompletionProposal(final String replacementString,
			final Position replacementPosition, final int cursorPosition) {
		this(replacementString, replacementPosition, cursorPosition, null,
				null, null, null);
	}

	/**
	 * Creates a new completion proposal. All fields are initialized based on
	 * the provided information.
	 * 
	 * @param replacementString
	 *            the actual string to be inserted into the document
	 * @param replacementPosition
	 *            the position of the text to be replaced
	 * @param cursorPosition
	 *            the position of the cursor following the insert relative to
	 *            replacementOffset
	 * @param image
	 *            the image to display for this proposal
	 * @param displayString
	 *            the string to be displayed for the proposal
	 * @param contextInformation
	 *            the context information associated with this proposal
	 * @param additionalProposalInfo
	 *            the additional information associated with this proposal
	 */
	public PositionBasedCompletionProposal(final String replacementString,
			final Position replacementPosition, final int cursorPosition,
			final Image image, final String displayString,
			final IContextInformation contextInformation,
			final String additionalProposalInfo) {
		Assert.isNotNull(replacementString);
		Assert.isTrue(replacementPosition != null);

		fReplacementString = replacementString;
		fReplacementPosition = replacementPosition;
		fCursorPosition = cursorPosition;
		fImage = image;
		fDisplayString = displayString;
		fContextInformation = contextInformation;
		fAdditionalProposalInfo = additionalProposalInfo;
	}

	/*
	 * @see ICompletionProposal#apply(IDocument)
	 */
	public void apply(final IDocument document) {
		try {
			document.replace(fReplacementPosition.getOffset(),
					fReplacementPosition.getLength(), fReplacementString);
		} catch (final BadLocationException x) {
			// ignore
		}
	}

	/*
	 * @see ICompletionProposal#getSelection(IDocument)
	 */
	public Point getSelection(final IDocument document) {
		return new Point(fReplacementPosition.getOffset() + fCursorPosition, 0);
	}

	/*
	 * @see ICompletionProposal#getContextInformation()
	 */
	public IContextInformation getContextInformation() {
		return fContextInformation;
	}

	/*
	 * @see ICompletionProposal#getImage()
	 */
	public Image getImage() {
		return fImage;
	}

	/*
	 * @see
	 * org.eclipse.jface.text.contentassist.ICompletionProposal#getDisplayString
	 * ()
	 */
	public String getDisplayString() {
		if (fDisplayString != null) {
			return fDisplayString;
		}
		return fReplacementString;
	}

	/*
	 * @see ICompletionProposal#getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {
		return fAdditionalProposalInfo;
	}

	/*
	 * @see
	 * org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#apply
	 * (org.eclipse.jface.text.ITextViewer, char, int, int)
	 */
	public void apply(final ITextViewer viewer, final char trigger,
			final int stateMask, final int offset) {
		apply(viewer.getDocument());
	}

	/*
	 * @see
	 * org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#selected
	 * (org.eclipse.jface.text.ITextViewer, boolean)
	 */
	public void selected(final ITextViewer viewer, final boolean smartToggle) {
	}

	/*
	 * @see
	 * org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#unselected
	 * (org.eclipse.jface.text.ITextViewer)
	 */
	public void unselected(final ITextViewer viewer) {
	}

	/*
	 * @see
	 * org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#validate
	 * (org.eclipse.jface.text.IDocument, int,
	 * org.eclipse.jface.text.DocumentEvent)
	 */
	public boolean validate(final IDocument document, final int offset,
			final DocumentEvent event) {
		try {
			final String content = document.get(fReplacementPosition
					.getOffset(), offset - fReplacementPosition.getOffset());
			if (fReplacementString.startsWith(content)) {
				return true;
			}
		} catch (final BadLocationException e) {
			// ignore concurrently modified document
		}
		return false;
	}

}
