/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.build.ast;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eventb.core.ast.ASTProblem;
import org.eventb.core.ast.SourceLocation;

public class FormulaExceptionWrapperDiagnostic implements IParseProblemWrapper {

	final ASTProblem problem;
	private final IDocument document;
	private final int startOffset;

	public FormulaExceptionWrapperDiagnostic(final ASTProblem problem,
			final int formulaOffset, final IDocument document) {
		this.problem = problem;
		startOffset = formulaOffset;
		this.document = document;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eventb.emf.texteditor2.build.ast.IParseProblemWrapper#getColumn()
	 */
	public int getColumn() {
		try {
			final int offset = getOffset();
			return offset
					- document.getLineOffset(document.getLineOfOffset(offset));
		} catch (final BadLocationException e) {
			// IGNORE
			return 0;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eventb.emf.texteditor2.build.ast.IParseProblemWrapper#getLine()
	 */
	public int getLine() {
		try {
			return document.getLineOfOffset(getOffset());
		} catch (final BadLocationException e) {
			// IGNORE
			return 0;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eventb.emf.texteditor2.build.ast.IParseProblemWrapper#getOffset()
	 */
	public int getOffset() {
		return startOffset + problem.getSourceLocation().getStart();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eventb.emf.texteditor2.build.ast.IParseProblemWrapper#getTokenLength
	 * ()
	 */
	public int getTokenLength() {
		final SourceLocation location = problem.getSourceLocation();
		return location.getEnd() - location.getStart();
	}

	/*
	 * (non-Javadoc)
	 * 
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eventb.emf.texteditor2.build.ast.IParseProblemWrapper#getMessage()
	 */
	public String getMessage() {
		return String
				.format(problem.getMessage().toString(), problem.getArgs());
	}
}
