/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.build.ast;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eventb.texttools.ParseException;

public class ParseExceptionWrapperDiagnostic implements IParseProblemWrapper {

	private final ParseException exception;
	private final IDocument document;

	public ParseExceptionWrapperDiagnostic(final IDocument document,
			final ParseException e) {
		this.document = document;
		exception = e;
	}

	/* (non-Javadoc)
	 * @see org.eventb.emf.texteditor2.build.ast.IParseProblemWrapper#getColumn()
	 */
	public int getColumn() {
		return exception.getPosition();
	}

	/* (non-Javadoc)
	 * @see org.eventb.emf.texteditor2.build.ast.IParseProblemWrapper#getLine()
	 */
	public int getLine() {
		return exception.getLine();
	}

	/* (non-Javadoc)
	 * @see org.eventb.emf.texteditor2.build.ast.IParseProblemWrapper#getOffset()
	 */
	public int getOffset() {
		try {
			return document.getLineOffset(getLine()) + getColumn();
		} catch (final BadLocationException e) {
			// IGNORE and return fallback value
			return 0;
		}
	}

	/* (non-Javadoc)
	 * @see org.eventb.emf.texteditor2.build.ast.IParseProblemWrapper#getTokenLength()
	 */
	public int getTokenLength() {
		return exception.getTokenLength();
	}

	/* (non-Javadoc)
	 * @see org.eventb.emf.texteditor2.build.ast.IParseProblemWrapper#getToken()
	 */
	public String getToken() {
		try {
			return document.get(getOffset(), getTokenLength());
		} catch (final BadLocationException e) {
			// IGNORE and return fallback value
			return "";
		}
	}

	/* (non-Javadoc)
	 * @see org.eventb.emf.texteditor2.build.ast.IParseProblemWrapper#getMessage()
	 */
	public String getMessage() {
		return exception.getLocalizedMessage();
	}
}
