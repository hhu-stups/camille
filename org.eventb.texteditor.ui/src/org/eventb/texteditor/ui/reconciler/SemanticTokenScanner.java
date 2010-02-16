/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.reconciler;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eventb.texteditor.ui.TextEditorPlugin;
import org.eventb.texteditor.ui.TextDecoration.ESyntaxElement;
import org.eventb.texteditor.ui.build.dom.DomManager;
import org.eventb.texteditor.ui.build.dom.IComponentDom;
import org.eventb.texteditor.ui.reconciler.partitioning.KeywordDetector;
import org.eventb.texteditor.ui.reconciler.partitioning.WordPredicateRule;
import org.eventb.texttools.Constants;

public class SemanticTokenScanner extends RuleBasedScanner {
	private final DomManager domManager = TextEditorPlugin.getDomManager();
	private final IdentifierRule identifierRule;
	private IComponentDom currentDom;
	private Resource resource;

	public SemanticTokenScanner() {
		super();
		setRules(getRules());
		identifierRule = new IdentifierRule();
	}

	private IRule[] getRules() {
		final List<IRule> rules = new ArrayList<IRule>();

		// rule for structural keywords
		final WordPredicateRule structKWRule = new WordPredicateRule(
				new KeywordDetector());
		for (final String keyword : Constants.structural_keywords) {
			structKWRule.addWord(keyword, ESyntaxElement.Keyword.getToken());
		}
		rules.add(structKWRule);

		// rule for formula keywords
		final WordPredicateRule formulaKwRule = new WordPredicateRule(
				new KeywordDetector());
		for (final String keyword : Constants.formula_keywords) {
			formulaKwRule.addWord(keyword, ESyntaxElement.MathKeyword
					.getToken());
		}
		rules.add(formulaKwRule);

		return rules.toArray(new IRule[rules.size()]);
	}

	@Override
	public IToken nextToken() {
		final int startOffset = fOffset;

		final IToken nextToken = super.nextToken();
		if (acceptToken(nextToken)) {
			return nextToken;
		}

		fTokenOffset = startOffset;
		fOffset = startOffset;
		fColumn = UNDEFINED;

		checkInit();
		identifierRule.setOffset(fOffset);
		final IToken token = identifierRule.evaluate(this);

		if (!token.isUndefined()) {
			return token;
		}

		if (read() == EOF) {
			return Token.EOF;
		}

		return fDefaultReturnToken;
	}

	private boolean acceptToken(final IToken nextToken) {
		/*
		 * If the next token is part of an identifier too (e.g. a event name)
		 * don't accept this token.
		 */
		final int c = read();
		if (Character.isJavaIdentifierPart(c) || c == '\'') {
			unread();
			return false;
		}

		return nextToken == ESyntaxElement.Keyword.getToken()
				|| nextToken == ESyntaxElement.MathKeyword.getToken()
				|| nextToken.isEOF();
	}

	public void setInputResource(final Resource resource) {
		this.resource = resource;
		currentDom = null;
	}

	private void checkInit() {
		if (currentDom == null && resource != null) {
			currentDom = domManager.getDom(resource);
			identifierRule.setDom(currentDom);
		}
	}
}
