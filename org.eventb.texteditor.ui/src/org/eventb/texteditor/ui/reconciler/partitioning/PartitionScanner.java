/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.reconciler.partitioning;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordPatternRule;

public class PartitionScanner extends RuleBasedPartitionScanner {

	private static final String PREFIX = "content_type_";
	public static final String CONTENT_TYPE_STRUCTURAL_KEYWORD = PREFIX
			+ "structural_keyword";
	public static final String CONTENT_TYPE_FORMULA_KEYWORD = PREFIX
			+ "formula_keyword";
	public static final String CONTENT_TYPE_COMMENT = PREFIX + "comment";
	public static final String CONTENT_TYPE_LABEL = PREFIX + "label";

	public static final String[] CONTENT_TYPES = {
			IDocument.DEFAULT_CONTENT_TYPE, CONTENT_TYPE_STRUCTURAL_KEYWORD,
			CONTENT_TYPE_FORMULA_KEYWORD, CONTENT_TYPE_COMMENT,
			CONTENT_TYPE_LABEL };

	public static final Token TOKEN_STRUCTURAL_KEYWORD = new Token(
			CONTENT_TYPE_STRUCTURAL_KEYWORD);
	public static final Token TOKEN_FORMULA_KEYWORD = new Token(
			CONTENT_TYPE_FORMULA_KEYWORD);
	public static final Token TOKEN_COMMENT = new Token(CONTENT_TYPE_COMMENT);
	public static final Token TOKEN_LABEL = new Token(CONTENT_TYPE_LABEL);

	public PartitionScanner() {
		super();

		final List<IPredicateRule> rules = getRules();
		final IPredicateRule[] result = new IPredicateRule[rules.size()];
		rules.toArray(result);
		setPredicateRules(result);
	}

	private List<IPredicateRule> getRules() {
		final List<IPredicateRule> rules = new ArrayList<IPredicateRule>();
		// comment rules
		rules.add(new EndOfLineRule("//", TOKEN_COMMENT));
		rules.add(new MultiLineRule("/*", "*/", TOKEN_COMMENT, (char) 0, true));

		// label rule
		rules.add(new WordPatternRule(new LabelDetector(), "@", null,
				PartitionScanner.TOKEN_LABEL));

		return rules;
	}
}
