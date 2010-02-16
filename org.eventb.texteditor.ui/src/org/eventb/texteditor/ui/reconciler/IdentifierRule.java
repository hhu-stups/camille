/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.reconciler;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.Token;
import org.eventb.texteditor.ui.TextDecoration.ESyntaxElement;
import org.eventb.texteditor.ui.build.dom.IDom;

public class IdentifierRule implements IRule {

	/** The word detector used by this rule. */
	private final IWordDetector fDetector = new IdentifierDetector();

	/**
	 * The default token to be returned on success and if nothing else has been
	 * specified.
	 */
	private final IToken fDefaultToken = new Token(null);

	/** Buffer used for pattern detection. */
	private final StringBuffer fBuffer = new StringBuffer();

	private int offset;

	private IDom dom;

	public IdentifierRule() {
	}

	/*
	 * @see IRule#evaluate(ICharacterScanner)
	 */
	public IToken evaluate(final ICharacterScanner scanner) {
		if (dom != null) {
			// read next word
			readIdentifier(scanner);

			if (fBuffer.length() > 0) {
				final String buffer = fBuffer.toString();

				// determine type for identifier
				IDom.IdentifierType type = null;
				final IDom scope = dom.getScopingDom(offset);
				if (scope != null) {
					type = scope.getIdentifierType(buffer);
				}

				// resolve text attribute
				final IToken token = getToken(type);
				if (token != null) {
					return token;
				}

				if (fDefaultToken.isUndefined()) {
					unreadBuffer(scanner);
				}

				return fDefaultToken;
			}
		}

		return Token.UNDEFINED;
	}

	private void readIdentifier(final ICharacterScanner scanner) {
		fBuffer.setLength(0);

		if (newIdentifierBegins(scanner)) {
			int c = scanner.read();

			if (c != ICharacterScanner.EOF && fDetector.isWordStart((char) c)) {
				do {
					fBuffer.append((char) c);
					c = scanner.read();
				} while (c != ICharacterScanner.EOF
						&& fDetector.isWordPart((char) c));
				scanner.unread();
			} else {
				scanner.unread();
			}
		}
	}

	private boolean newIdentifierBegins(final ICharacterScanner scanner) {

		// read char before offset
		scanner.unread();
		final int c = scanner.read();

		if (fDetector.isWordStart((char) c) || fDetector.isWordPart((char) c)) {
			return false;
		}

		return true;
	}

	private IToken getToken(final IDom.IdentifierType type) {
		if (type != null) {
			switch (type) {
			case GlobalVariable:
				return ESyntaxElement.GlobalVariable.getToken();
			case LocalVariable:
				return ESyntaxElement.BoundedVariable.getToken();
			case Parameter:
				return ESyntaxElement.Parameter.getToken();
			case Constant:
				return ESyntaxElement.Constant.getToken();
			case Set:
				return ESyntaxElement.Set.getToken();
			}
		}

		return null;
	}

	/**
	 * Returns the characters in the buffer to the scanner.
	 * 
	 * @param scanner
	 *            the scanner to be used
	 */
	protected void unreadBuffer(final ICharacterScanner scanner) {
		for (int i = fBuffer.length() - 1; i >= 0; i--) {
			scanner.unread();
		}
	}

	public void setOffset(final int offset) {
		this.offset = offset;
	}

	public void setDom(final IDom dom) {
		this.dom = dom;
	}
}
