/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.reconciler;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eventb.texteditor.ui.reconciler.partitioning.KeywordDetector;
import org.eventb.texteditor.ui.reconciler.partitioning.WordPredicateRule;

public class KeywordTokenScanner extends RuleBasedScanner {
	public KeywordTokenScanner(final String[] keywords,
			final TextAttribute textAttribute) {
		super();

		final IToken token = new Token(textAttribute);
		final WordPredicateRule keywordRule = new WordPredicateRule(
				new KeywordDetector());

		for (final String keyword : keywords) {
			keywordRule.addWord(keyword, token);
		}

		setRules(new IRule[] { keywordRule });
	}
}
