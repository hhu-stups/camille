/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.reconciler;

import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eventb.texteditor.ui.TextDecoration.ESyntaxElement;

public class CommentTokenScanner extends RuleBasedScanner {

	public CommentTokenScanner() {
		super();

		final IRule[] rules = new IRule[2];
		final Token token = ESyntaxElement.Comment.getToken();
		rules[0] = new EndOfLineRule("//", token);
		rules[1] = new MultiLineRule("/*", "*/", token, (char) 0, true);
		setRules(rules);
	}
}
