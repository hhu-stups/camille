/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texttools.formulas;

import java.util.List;

import org.eventb.core.ast.ASTProblem;
import org.eventb.core.ast.SourceLocation;
import org.eventb.emf.core.EventBElement;
import org.eventb.emf.core.EventBExpression;

@SuppressWarnings("serial")
public class FormulaParseException extends Exception {

	private final EventBElement emfExpression;

	private final List<ASTProblem> astProblems;

	private final String formula;

	public FormulaParseException(final EventBElement emfExpr,
			final String formula, final List<ASTProblem> problems) {
		emfExpression = emfExpr;
		this.formula = formula;
		astProblems = problems;
	}

	/**
	 * The {@link EventBExpression} which caused the problem. The Rodin formula
	 * which was parsed can be found in {@link EventBExpression#getExpression()}
	 * .
	 * 
	 * @return
	 */
	public EventBElement getEmfObject() {
		return emfExpression;
	}

	/**
	 * The {@link ASTProblem}s that occured during parsing the formula (see
	 * {@link #getEmfObject()} -> {@link EventBExpression#getExpression()}.
	 * 
	 * @return
	 */
	public List<ASTProblem> getAstProblems() {
		return astProblems;
	}

	public String getFormula() {
		return formula;
	}

	@Override
	public String getLocalizedMessage() {
		final StringBuilder buffer = new StringBuilder();
		buffer.append("Parse problems in formula '");
		buffer.append(getFormula());
		buffer.append("': [");
		
		List<ASTProblem> problems = getAstProblems();
		for (int i = 0; i < problems.size(); i++) {
			ASTProblem problem = problems.get(i);
			
			buffer.append(" (");
			SourceLocation location = problem.getSourceLocation();
			buffer.append(location.getStart() + "-" +location.getEnd());
			buffer.append(") ");
			buffer.append(String
					.format(problem.getMessage().toString(), problem.getArgs()));
		}

		buffer.append(" ]");
		
		return buffer.toString();
	}
}
