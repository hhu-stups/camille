/**
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen,
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html)
 * */

package org.eventb.texteditor.ui.editor.proofstatus;

import java.util.List;

import org.eventb.texteditor.ui.editor.proofstatus.ProofStatusIndex.ProofObligation;

/**
 * Hover content for a label: some text (e.g. problems) and the proof
 * obligations of the labeled element.
 */
public final class ProofObligationHoverInput {
	/** shown above the proof obligations, first line bold, may be empty */
	public final String text;
	public final String component;
	public final List<ProofObligation> obligations;

	public ProofObligationHoverInput(final String text, final String component,
			final List<ProofObligation> obligations) {
		this.text = text;
		this.component = component;
		this.obligations = obligations;
	}

	@Override
	public String toString() {
		final StringBuilder result = new StringBuilder(text);
		for (final ProofObligation obligation : obligations) {
			result.append('\n').append(obligation.name).append(": ")
					.append(obligation.describeStatus());
		}
		return result.toString();
	}
}
