/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texttools.formulas;

import java.util.List;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eventb.core.ast.ASTProblem;
import org.eventb.core.ast.FormulaFactory;
import org.eventb.core.ast.IParseResult;
import org.eventb.emf.core.EventBCommentedExpressionElement;
import org.eventb.emf.core.EventBElement;
import org.eventb.emf.core.EventBNamedCommentedComponentElement;
import org.eventb.emf.core.EventBNamedCommentedPredicateElement;
import org.eventb.emf.core.context.Context;
import org.eventb.emf.core.machine.Action;
import org.eventb.emf.core.machine.Machine;
import org.eventb.emf.formulas.BFormula;
import org.eventb.texttools.TextPositionUtil;
import org.eventb.texttools.model.texttools.TextRange;

public class FormulaResolver {

	private enum FormulaType {
		Expression, Predicate, Assignment
	};

	private static FormulaFactory formulaFactory = FormulaFactory.getDefault();

	public static List<FormulaParseException> resolveAllFormulas(
			final EventBNamedCommentedComponentElement astRoot) {
		// traverse tree using an iterator
		final TreeIterator<EObject> iterator = EcoreUtil.getAllContents(
				astRoot, false);
		List<FormulaParseException> exceptions = null;

		if (astRoot instanceof Machine) {
			final MachineResolveSwitch switcher = new MachineResolveSwitch();
			while (iterator.hasNext()) {
				// visit node
				final EObject next = iterator.next();
				final Boolean visitChildren = switcher.doSwitch(next);

				if (visitChildren != null ? !visitChildren : true) {
					iterator.prune();
				}
			}
			exceptions = switcher.getExceptions();
		} else if (astRoot instanceof Context) {
			final ContextResolveSwitch switcher = new ContextResolveSwitch();
			while (iterator.hasNext()) {
				// visit node
				final EObject next = iterator.next();
				final Boolean visitChildren = switcher.doSwitch(next);

				if (visitChildren != null ? !visitChildren : true) {
					iterator.prune();
				}
			}
			exceptions = switcher.getExceptions();
		}

		return exceptions;
	}

	public static void resolve(final EventBCommentedExpressionElement emfExpr)
			throws FormulaParseException {
		final String expression = emfExpr.getExpression();
		resolve(emfExpr, expression,
				formulaFactory.parseExpression(expression, emfExpr),
				FormulaType.Expression);
	}

	public static void resolve(
			final EventBNamedCommentedPredicateElement emfPredicate)
			throws FormulaParseException {
		final String predicate = emfPredicate.getPredicate();
		resolve(emfPredicate, predicate,
				formulaFactory.parsePredicate(predicate, emfPredicate),
				FormulaType.Predicate);
	}

	public static void resolve(final Action emfAction)
			throws FormulaParseException {
		final String action = emfAction.getAction();
		resolve(emfAction, action,
				formulaFactory.parseAssignment(action, emfAction),
				FormulaType.Assignment);
	}

	private static void resolve(final EventBElement emfExpr,
			final String content, final IParseResult parseResult,
			final FormulaType type) throws FormulaParseException {
		// parsing comes first
		final List<ASTProblem> problems = parseResult.getProblems();

		// no need to continue if any problems occured
		if (problems.size() > 0) {
			throw new FormulaParseException(emfExpr, content, problems);
		}

		final ResolveVisitor visitor = new ResolveVisitor();
		BFormula formula = null;
		final int offset = getOffset(emfExpr, content);

		switch (type) {
		case Expression:
			formula = visitor
					.convert(parseResult.getParsedExpression(), offset);
			break;
		case Predicate:
			formula = visitor.convert(parseResult.getParsedPredicate(), offset);
			break;
		case Assignment:
			formula = visitor
					.convert(parseResult.getParsedAssignment(), offset);
			break;

		default:
			break;
		}

		emfExpr.getExtensions().add(formula);
	}

	private static int getOffset(final EventBElement emfObject,
			final String content) {
		final TextRange range = TextPositionUtil.getInternalPosition(emfObject,
				content);
		return range != null ? range.getOffset() : 0;
	}
}
