/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.reconciler;

import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.WordRule;
import org.eventb.texteditor.ui.TextDecoration.ESyntaxElement;
import org.eventb.texteditor.ui.reconciler.partitioning.LabelDetector;

public class LabelTokenScanner extends RuleBasedScanner {
	public LabelTokenScanner() {
		super();

		setRules(new IRule[] { new WordRule(new LabelDetector(),
				ESyntaxElement.Label.getToken(), true) });
	}
}
