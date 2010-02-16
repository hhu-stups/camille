/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.reconciler.partitioning;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordRule;

public class WordPredicateRule extends WordRule implements IPredicateRule {

	public WordPredicateRule(final IWordDetector _detector) {
		super(_detector);
	}

	public IToken getSuccessToken() {
		return fDefaultToken;
	}

	@Override
	public IToken evaluate(final ICharacterScanner scanner) {
		// read char before offset
		scanner.unread();
		final int c = scanner.read();

		if (fDetector.isWordStart((char) c) || fDetector.isWordPart((char) c)) {
			return Token.UNDEFINED;
		}

		return super.evaluate(scanner);
	}

	public IToken evaluate(final ICharacterScanner _scanner,
			final boolean _resume) {
		return evaluate(_scanner);
	}

	public void clearWords() {
		fWords.clear();
	}
}
