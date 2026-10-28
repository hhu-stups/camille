/**
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen,
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html)
 * */

package org.eventb.texteditor.ui.build;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;

/**
 * Immutable copy of an editor document that is used for one build run.
 * <p>
 * The reconciler runs in a background thread while the user keeps typing. If
 * the build worked directly on the live editor document, text positions
 * computed by the parser could refer to an older or newer state than the
 * one that was parsed. All phases of a build therefore work on such a
 * snapshot. Changes that must be written back into the editor (e.g. the
 * ASCII to Unicode translation of formulas) can check with
 * {@link #isUpToDate()} whether the live document is still unchanged.
 * </p>
 */
public class DocumentSnapshot extends Document {

	private final IDocument original;
	private final long originalStamp;

	public DocumentSnapshot(final IDocument original) {
		super(original.get());
		this.original = original;
		this.originalStamp = getStamp(original);
	}

	/**
	 * @return the live document this snapshot was taken from
	 */
	public IDocument getOriginal() {
		return original;
	}

	/**
	 * @return <code>true</code> if the original document has not been
	 *         modified since this snapshot was taken
	 */
	public boolean isUpToDate() {
		if (originalStamp != IDocumentExtension4.UNKNOWN_MODIFICATION_STAMP) {
			return originalStamp == getStamp(original);
		}
		// fall back to (expensive) content comparison
		return get().equals(original.get());
	}

	private static long getStamp(final IDocument document) {
		if (document instanceof IDocumentExtension4) {
			return ((IDocumentExtension4) document).getModificationStamp();
		}
		return IDocumentExtension4.UNKNOWN_MODIFICATION_STAMP;
	}
}
