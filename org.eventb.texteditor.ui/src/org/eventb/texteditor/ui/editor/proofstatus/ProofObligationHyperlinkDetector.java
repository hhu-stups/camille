/**
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen,
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html)
 * */

package org.eventb.texteditor.ui.editor.proofstatus;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eventb.texteditor.ui.editor.EventBTextEditor;
import org.eventb.texteditor.ui.editor.proofstatus.ProofStatusIndex.ProofObligation;
import org.eventb.texteditor.ui.editor.proofstatus.ProofStatusIndex.Status;
import org.eventb.texteditor.ui.editor.proofstatus.ProofStatusSupport.LabelAt;

/**
 * Ctrl+Click (Cmd+Click) on a label offers to open its proof obligations in
 * the prover. Proof obligations that are not proven are listed first.
 */
public class ProofObligationHyperlinkDetector extends AbstractHyperlinkDetector {

	private final EventBTextEditor editor;

	public ProofObligationHyperlinkDetector(final EventBTextEditor editor) {
		this.editor = editor;
	}

	@Override
	public IHyperlink[] detectHyperlinks(final ITextViewer textViewer,
			final IRegion region, final boolean canShowMultipleHyperlinks) {
		final ProofStatusSupport support = editor.getProofStatusSupport();
		final IDocument document = textViewer.getDocument();
		if (support == null || document == null || region == null) {
			return null;
		}

		final LabelAt label = ProofStatusSupport.findLabel(document.get(),
				region.getOffset());
		final List<ProofObligation> obligations = support
				.getObligations(label);
		if (obligations.isEmpty()) {
			return null;
		}

		final IRegion linkRegion = new Region(label.offset, label.length);
		final List<IHyperlink> links = new ArrayList<IHyperlink>();
		for (final boolean proven : new boolean[] { false, true }) {
			for (final ProofObligation obligation : obligations) {
				final boolean isProven = obligation.status == Status.DISCHARGED
						&& !obligation.broken;
				if (isProven == proven) {
					links.add(new ProofObligationHyperlink(linkRegion,
							obligation));
				}
			}
		}
		if (!canShowMultipleHyperlinks) {
			return new IHyperlink[] { links.get(0) };
		}
		return links.toArray(new IHyperlink[links.size()]);
	}

	private static final class ProofObligationHyperlink implements IHyperlink {
		private final IRegion region;
		private final ProofObligation obligation;

		ProofObligationHyperlink(final IRegion region,
				final ProofObligation obligation) {
			this.region = region;
			this.obligation = obligation;
		}

		@Override
		public IRegion getHyperlinkRegion() {
			return region;
		}

		@Override
		public String getTypeLabel() {
			return null;
		}

		@Override
		public String getHyperlinkText() {
			return "Prove " + obligation.name + " ("
					+ obligation.describeStatus() + ")";
		}

		@Override
		public void open() {
			ProofObligationOpener.open(obligation.handle);
		}
	}
}
