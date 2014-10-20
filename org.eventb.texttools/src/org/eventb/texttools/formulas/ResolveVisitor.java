/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texttools.formulas;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eventb.core.ast.Assignment;
import org.eventb.core.ast.AssociativeExpression;
import org.eventb.core.ast.AssociativePredicate;
import org.eventb.core.ast.AtomicExpression;
import org.eventb.core.ast.BecomesEqualTo;
import org.eventb.core.ast.BecomesMemberOf;
import org.eventb.core.ast.BecomesSuchThat;
import org.eventb.core.ast.BinaryExpression;
import org.eventb.core.ast.BinaryPredicate;
import org.eventb.core.ast.BoolExpression;
import org.eventb.core.ast.BoundIdentDecl;
import org.eventb.core.ast.BoundIdentifier;
import org.eventb.core.ast.Expression;
import org.eventb.core.ast.ExtendedExpression;
import org.eventb.core.ast.ExtendedPredicate;
import org.eventb.core.ast.Formula;
import org.eventb.core.ast.FreeIdentifier;
import org.eventb.core.ast.ISimpleVisitor;
import org.eventb.core.ast.IntegerLiteral;
import org.eventb.core.ast.LiteralPredicate;
import org.eventb.core.ast.MultiplePredicate;
import org.eventb.core.ast.Predicate;
import org.eventb.core.ast.QuantifiedExpression;
import org.eventb.core.ast.QuantifiedPredicate;
import org.eventb.core.ast.RelationalPredicate;
import org.eventb.core.ast.SetExtension;
import org.eventb.core.ast.SimplePredicate;
import org.eventb.core.ast.SourceLocation;
import org.eventb.core.ast.UnaryExpression;
import org.eventb.core.ast.UnaryPredicate;
import org.eventb.emf.formulas.BAssignmentResolved;
import org.eventb.emf.formulas.BExpressionResolved;
import org.eventb.emf.formulas.BFormula;
import org.eventb.emf.formulas.BPredicateResolved;
import org.eventb.emf.formulas.BecomesEqualToAssignment;
import org.eventb.emf.formulas.BecomesMemberOfAssignment;
import org.eventb.emf.formulas.BecomesSuchThatAssignment;
import org.eventb.emf.formulas.BinaryOperator;
import org.eventb.emf.formulas.BoundIdentifierExpression;
import org.eventb.emf.formulas.Constant;
import org.eventb.emf.formulas.ExistPredicate;
import org.eventb.emf.formulas.ForallPredicate;
import org.eventb.emf.formulas.FormulasFactory;
import org.eventb.emf.formulas.FormulasPackage;
import org.eventb.emf.formulas.IdentifierExpression;
import org.eventb.emf.formulas.IntegerLiteralExpression;
import org.eventb.emf.formulas.MultiOperand;
import org.eventb.emf.formulas.QuantifiedIntersectionExpression1;
import org.eventb.emf.formulas.QuantifiedIntersectionExpression2;
import org.eventb.emf.formulas.QuantifiedUnionExpression1;
import org.eventb.emf.formulas.QuantifiedUnionExpression2;
import org.eventb.emf.formulas.SetComprehensionExpression1;
import org.eventb.emf.formulas.SetComprehensionExpression2;
import org.eventb.emf.formulas.UnaryOperator;
import org.eventb.texttools.TextPositionUtil;
import org.eventb.texttools.TextToolsPlugin;

@SuppressWarnings("deprecation")
public class ResolveVisitor implements ISimpleVisitor {
	private static final Map<Integer, EClass> idToEClass = new HashMap<Integer, EClass>();

	private Map<BFormula, Formula> emfToRodinElements;

	private final Stack<BFormula> stack = new Stack<BFormula>();
	private final Stack<List<String>> boundIdDeclStack = new Stack<List<String>>();

	private int textOffset;

	static {
		// Constants:
		// INTEGER, NATURAL, NATURAL1, BOOL, TRUE, BTRUE, FALSE, BFALSE,
		// EMPTYSET
		idToEClass.put(Formula.INTEGER, FormulasPackage.eINSTANCE.getINT());
		idToEClass.put(Formula.NATURAL, FormulasPackage.eINSTANCE.getNAT());
		idToEClass.put(Formula.NATURAL1, FormulasPackage.eINSTANCE.getNAT1());
		idToEClass.put(Formula.BOOL, FormulasPackage.eINSTANCE.getBOOL());
		idToEClass.put(Formula.TRUE, FormulasPackage.eINSTANCE.getTRUE());
		idToEClass.put(Formula.BTRUE, FormulasPackage.eINSTANCE.getTRUTH());
		idToEClass.put(Formula.FALSE, FormulasPackage.eINSTANCE.getFALSE());
		idToEClass.put(Formula.BFALSE, FormulasPackage.eINSTANCE.getFALSITY());
		idToEClass.put(Formula.EMPTYSET,
				FormulasPackage.eINSTANCE.getEMPTYSET());
		idToEClass.put(Formula.KPRED,
				FormulasPackage.eINSTANCE.getPredExpression());
		idToEClass.put(Formula.KSUCC,
				FormulasPackage.eINSTANCE.getSuccExpression());

		// Unary operators:
		// KCARD, POW, POW1, KUNION, KINTER, KDOM, KRAN, KPRJ1, KPRJ2, KID,
		// KMIN, KMAX, CONVERSE, UNMINUS
		// NOT, KFINITE, KBOOL
		idToEClass.put(Formula.KCARD,
				FormulasPackage.eINSTANCE.getCardExpression());
		idToEClass.put(Formula.POW,
				FormulasPackage.eINSTANCE.getPowExpression());
		idToEClass.put(Formula.POW1,
				FormulasPackage.eINSTANCE.getPow1Expression());
		idToEClass.put(Formula.KUNION,
				FormulasPackage.eINSTANCE.getKUnionExpression());
		idToEClass.put(Formula.KINTER,
				FormulasPackage.eINSTANCE.getKIntersectionExpression());
		idToEClass.put(Formula.KDOM,
				FormulasPackage.eINSTANCE.getDomainExpression());
		idToEClass.put(Formula.KRAN,
				FormulasPackage.eINSTANCE.getRangeExpression());
		idToEClass.put(Formula.KPRJ1,
				FormulasPackage.eINSTANCE.getPrj1Expression());
		idToEClass.put(Formula.KPRJ1_GEN,
				FormulasPackage.eINSTANCE.getPrj1GenExpression());
		idToEClass.put(Formula.KPRJ2,
				FormulasPackage.eINSTANCE.getPrj2Expression());
		idToEClass.put(Formula.KPRJ2_GEN,
				FormulasPackage.eINSTANCE.getPrj2GenExpression());
		idToEClass.put(Formula.KID,
				FormulasPackage.eINSTANCE.getIdentityExpression());
		idToEClass.put(Formula.KID_GEN,
				FormulasPackage.eINSTANCE.getIdentityGenExpression());
		idToEClass.put(Formula.KMIN,
				FormulasPackage.eINSTANCE.getMinExpression());
		idToEClass.put(Formula.KMAX,
				FormulasPackage.eINSTANCE.getMaxExpression());
		idToEClass.put(Formula.CONVERSE,
				FormulasPackage.eINSTANCE.getInverseExpression());
		idToEClass.put(Formula.UNMINUS,
				FormulasPackage.eINSTANCE.getUnaryMinusExpression());
		idToEClass
				.put(Formula.NOT, FormulasPackage.eINSTANCE.getNotPredicate());
		idToEClass.put(Formula.KFINITE,
				FormulasPackage.eINSTANCE.getFinitePredicate());
		idToEClass.put(Formula.KBOOL,
				FormulasPackage.eINSTANCE.getBoolExpression());

		// Binary operators: EQUAL, NOTEQUAL, LT, LE, GT, GE, IN, NOTIN, SUBSET,
		// NOTSUBSET, SUBSETEQ, NOTSUBSETEQ, MAPSTO, REL, TREL, SREL, STREL,
		// PFUN, TFUN, PINJ, TINJ, PSUR, TSUR, TBIJ, SETMINUS, CPROD, DPROD,
		// PPROD, DOMRES, DOMSUB, RANRES, RANSUB, UPTO, MINUS, DIV, MOD, EXPN,
		// FUNIMAGE, RELIMAGE, LIMP, LEQV
		idToEClass.put(Formula.EQUAL,
				FormulasPackage.eINSTANCE.getEqualPredicate());
		idToEClass.put(Formula.NOTEQUAL,
				FormulasPackage.eINSTANCE.getNotEqualPredicate());
		idToEClass
				.put(Formula.LT, FormulasPackage.eINSTANCE.getLessPredicate());
		idToEClass.put(Formula.LE,
				FormulasPackage.eINSTANCE.getLessEqualPredicate());
		idToEClass.put(Formula.GT,
				FormulasPackage.eINSTANCE.getGreaterPredicate());
		idToEClass.put(Formula.GE,
				FormulasPackage.eINSTANCE.getGreaterEqualPredicate());
		idToEClass.put(Formula.IN,
				FormulasPackage.eINSTANCE.getBelongPredicate());
		idToEClass.put(Formula.NOTIN,
				FormulasPackage.eINSTANCE.getNotBelongPredicate());
		idToEClass.put(Formula.SUBSET,
				FormulasPackage.eINSTANCE.getSubsetStrictPredicate());
		idToEClass.put(Formula.NOTSUBSET,
				FormulasPackage.eINSTANCE.getNotSubsetStrictPredicate());
		idToEClass.put(Formula.SUBSETEQ,
				FormulasPackage.eINSTANCE.getSubsetPredicate());
		idToEClass.put(Formula.NOTSUBSETEQ,
				FormulasPackage.eINSTANCE.getNotSubsetPredicate());
		idToEClass.put(Formula.MAPSTO,
				FormulasPackage.eINSTANCE.getMapletExpression());
		idToEClass.put(Formula.REL,
				FormulasPackage.eINSTANCE.getRelationExpression());
		idToEClass.put(Formula.TREL,
				FormulasPackage.eINSTANCE.getTotalRelationExpression());
		idToEClass.put(Formula.SREL,
				FormulasPackage.eINSTANCE.getSurjectiveRelationExpression());
		idToEClass.put(Formula.STREL, FormulasPackage.eINSTANCE
				.getTotalSurjectiveRelationExpression());
		idToEClass.put(Formula.PFUN,
				FormulasPackage.eINSTANCE.getPartialFunctionExpression());
		idToEClass.put(Formula.TFUN,
				FormulasPackage.eINSTANCE.getTotalFunctionExpression());
		idToEClass.put(Formula.PINJ,
				FormulasPackage.eINSTANCE.getPartialInjectionExpression());
		idToEClass.put(Formula.TINJ,
				FormulasPackage.eINSTANCE.getTotalInjectionExpression());
		idToEClass.put(Formula.PSUR,
				FormulasPackage.eINSTANCE.getPartialSurjectionExpression());
		idToEClass.put(Formula.TSUR,
				FormulasPackage.eINSTANCE.getTotalSurjectionExpression());
		idToEClass.put(Formula.TBIJ,
				FormulasPackage.eINSTANCE.getTotalBijectionExpression());
		idToEClass.put(Formula.SETMINUS,
				FormulasPackage.eINSTANCE.getSetSubtractionExpression());
		idToEClass.put(Formula.CPROD,
				FormulasPackage.eINSTANCE.getCartesianProductExpression());
		idToEClass.put(Formula.DPROD,
				FormulasPackage.eINSTANCE.getDirectProductExpression());
		idToEClass.put(Formula.PPROD,
				FormulasPackage.eINSTANCE.getParallelProductExpression());
		idToEClass.put(Formula.DOMRES,
				FormulasPackage.eINSTANCE.getDomainRestrictionExpression());
		idToEClass.put(Formula.DOMSUB,
				FormulasPackage.eINSTANCE.getDomainSubtractionExpression());
		idToEClass.put(Formula.RANRES,
				FormulasPackage.eINSTANCE.getRangeRestrictionExpression());
		idToEClass.put(Formula.RANSUB,
				FormulasPackage.eINSTANCE.getRangeSubtractionExpression());
		idToEClass.put(Formula.UPTO,
				FormulasPackage.eINSTANCE.getUptoExpression());
		idToEClass.put(Formula.MINUS,
				FormulasPackage.eINSTANCE.getSubtractExpression());
		idToEClass.put(Formula.DIV,
				FormulasPackage.eINSTANCE.getDivisionExpression());
		idToEClass.put(Formula.MOD,
				FormulasPackage.eINSTANCE.getModuloExpression());
		idToEClass.put(Formula.EXPN,
				FormulasPackage.eINSTANCE.getExponentiationExpression());
		idToEClass.put(Formula.FUNIMAGE,
				FormulasPackage.eINSTANCE.getFunctionExpression());
		idToEClass.put(Formula.RELIMAGE,
				FormulasPackage.eINSTANCE.getImageExpression());
		idToEClass.put(Formula.LIMP,
				FormulasPackage.eINSTANCE.getImplicationPredicate());
		idToEClass.put(Formula.LEQV,
				FormulasPackage.eINSTANCE.getEquivalencePredicate());

		// Multi operand predicates/expressions
		idToEClass.put(Formula.BUNION,
				FormulasPackage.eINSTANCE.getUnionExpression());
		idToEClass.put(Formula.BINTER,
				FormulasPackage.eINSTANCE.getIntersectionExpression());
		idToEClass.put(Formula.BCOMP,
				FormulasPackage.eINSTANCE.getBackwardCompositionExpression());
		idToEClass.put(Formula.FCOMP,
				FormulasPackage.eINSTANCE.getForwardCompositionExpression());
		idToEClass.put(Formula.OVR,
				FormulasPackage.eINSTANCE.getRelationalOverridingExpression());
		idToEClass.put(Formula.PLUS,
				FormulasPackage.eINSTANCE.getAddExpression());
		idToEClass.put(Formula.MUL,
				FormulasPackage.eINSTANCE.getMulExpression());
		idToEClass.put(Formula.LAND,
				FormulasPackage.eINSTANCE.getAndPredicate());
		idToEClass.put(Formula.LOR, FormulasPackage.eINSTANCE.getOrPredicate());
		idToEClass.put(Formula.SETEXT,
				FormulasPackage.eINSTANCE.getSetExpression());
		idToEClass.put(Formula.KPARTITION,
				FormulasPackage.eINSTANCE.getPartitionPredicate());

		// Quantifiers:
		// FORALL, EXISTS
		idToEClass.put(Formula.FORALL,
				FormulasPackage.eINSTANCE.getForallPredicate());
		idToEClass.put(Formula.EXISTS,
				FormulasPackage.eINSTANCE.getExistPredicate());
		// cannot decide QUNION and QINTER here
	}

	public BExpressionResolved convert(final Expression rodinExpr,
			final int offset) {
		traverseFormula(rodinExpr, offset);
		return (BExpressionResolved) stack.pop();
	}

	public BPredicateResolved convert(final Predicate predicate,
			final int offset) {
		traverseFormula(predicate, offset);
		return (BPredicateResolved) stack.pop();
	}

	public BAssignmentResolved convert(final Assignment assignment,
			final int offset) {
		traverseFormula(assignment, offset);
		return (BAssignmentResolved) stack.pop();
	}

	private void traverseFormula(final Formula rodinFormula, final int offset) {
		emfToRodinElements = new HashMap<BFormula, Formula>();

		textOffset = offset;

		rodinFormula.accept(this);
		assert stack.size() == 1;
	}

	/**
	 * Handles a newly created emf node, i.e., push to stack, store mapping to
	 * Rodin element
	 * 
	 * @param node
	 * @param rodinFormula
	 */
	private void storeNode(final BFormula node, final Formula rodinFormula) {
		annotatePosition(node, rodinFormula);
		emfToRodinElements.put(node, rodinFormula);
		stack.push(node);
	}

	public Map<BFormula, Formula> getEmfToRodinMapping() {
		return emfToRodinElements;
	}

	@Override
	public void visitAssociativeExpression(
			final AssociativeExpression expression) {
		handleMultiChildren(expression, expression.getChildren());
	}

	@Override
	public void visitAssociativePredicate(final AssociativePredicate predicate) {
		handleMultiChildren(predicate, predicate.getChildren());
	}

	@Override
	public void visitAtomicExpression(final AtomicExpression expression) {
		final EClass eClass = getMatchingEClass(expression);
		final EObject eObject = FormulasFactory.eINSTANCE.create(eClass);
		storeNode((BFormula) eObject, expression);
	}

	@Override
	public void visitBecomesEqualTo(final BecomesEqualTo assignment) {
		final FreeIdentifier[] identifiers = assignment
				.getAssignedIdentifiers();
		visitChildren(identifiers);

		final Expression[] expressions = assignment.getExpressions();
		visitChildren(expressions);

		final BecomesEqualToAssignment newNode = FormulasFactory.eINSTANCE
				.createBecomesEqualToAssignment();

		// attach expression children
		final EList<BExpressionResolved> exprChildren = newNode
				.getExpressions();
		for (int i = 0; i < expressions.length; i++) {
			exprChildren.add(0, (BExpressionResolved) stack.pop());
		}

		// attach identifier children
		final EList<IdentifierExpression> identChildren = newNode
				.getIdentifiers();
		for (int i = 0; i < identifiers.length; i++) {
			identChildren.add(0, (IdentifierExpression) stack.pop());
		}

		storeNode(newNode, assignment);
	}

	@Override
	public void visitBecomesMemberOf(final BecomesMemberOf assignment) {
		final FreeIdentifier[] identifiers = assignment
				.getAssignedIdentifiers();
		visitChildren(identifiers);

		final Expression expression = assignment.getSet();
		visitChildren(expression);

		final BecomesMemberOfAssignment newNode = FormulasFactory.eINSTANCE
				.createBecomesMemberOfAssignment();

		// attach expression child
		newNode.setExpression((BExpressionResolved) stack.pop());

		// attach identifier children
		final EList<IdentifierExpression> identChildren = newNode
				.getIdentifiers();
		for (int i = 0; i < identifiers.length; i++) {
			identChildren.add(0, (IdentifierExpression) stack.pop());
		}

		storeNode(newNode, assignment);
	}

	@Override
	public void visitBecomesSuchThat(final BecomesSuchThat assignment) {
		final FreeIdentifier[] identifiers = assignment
				.getAssignedIdentifiers();
		visitChildren(identifiers);

		// add bound identifier declarations
		final List<String> primedIdents = new LinkedList<String>();
		for (int i = 0; i < identifiers.length; i++) {
			primedIdents.add(identifiers[i].getName() + "'");
		}
		boundIdDeclStack.push(primedIdents);

		final Predicate predicate = assignment.getCondition();
		visitChildren(predicate);

		boundIdDeclStack.pop();

		final BecomesSuchThatAssignment newNode = FormulasFactory.eINSTANCE
				.createBecomesSuchThatAssignment();

		// attach expression child
		newNode.setPredicate((BPredicateResolved) stack.pop());

		// attach identifier children
		final EList<IdentifierExpression> identChildren = newNode
				.getIdentifiers();
		for (int i = 0; i < identifiers.length; i++) {
			identChildren.add(0, (IdentifierExpression) stack.pop());
		}

		storeNode(newNode, assignment);
	}

	@Override
	public void visitBinaryExpression(final BinaryExpression expression) {
		handleTwoChildren(expression, expression.getLeft(),
				expression.getRight());
	}

	@Override
	public void visitBinaryPredicate(final BinaryPredicate predicate) {
		handleTwoChildren(predicate, predicate.getLeft(), predicate.getRight());
	}

	@Override
	public void visitBoolExpression(final BoolExpression expression) {
		handleSingleChild(expression, expression.getPredicate());
	}

	@Override
	public void visitBoundIdentDecl(final BoundIdentDecl boundIdentDecl) {
		final BoundIdentifierExpression node = FormulasFactory.eINSTANCE
				.createBoundIdentifierExpression();
		node.setName(boundIdentDecl.getName());
		storeNode(node, boundIdentDecl);
	}

	@Override
	public void visitBoundIdentifier(final BoundIdentifier identifierExpression) {
		final BoundIdentifierExpression node = FormulasFactory.eINSTANCE
				.createBoundIdentifierExpression();

		final List<String> identDecls = boundIdDeclStack.peek();
		final int index = identifierExpression.getBoundIndex();
		final String name = identDecls.get(index);
		node.setName(name);

		storeNode(node, identifierExpression);
	}

	@Override
	public void visitFreeIdentifier(final FreeIdentifier identifierExpression) {
		final IdentifierExpression node = FormulasFactory.eINSTANCE
				.createIdentifierExpression();
		node.setName(identifierExpression.getName());
		storeNode(node, identifierExpression);
	}

	@Override
	public void visitIntegerLiteral(final IntegerLiteral expression) {
		final IntegerLiteralExpression node = FormulasFactory.eINSTANCE
				.createIntegerLiteralExpression();
		node.setNumber(expression.getValue());

		storeNode(node, expression);
	}

	@Override
	public void visitLiteralPredicate(final LiteralPredicate predicate) {
		final Constant node = (Constant) FormulasFactory.eINSTANCE
				.create(getMatchingEClass(predicate));
		storeNode(node, predicate);
	}

	@Override
	public void visitMultiplePredicate(final MultiplePredicate predicate) {
		handleMultiChildren(predicate, predicate.getChildren());
	}

	@Override
	public void visitQuantifiedExpression(final QuantifiedExpression expression) {
		final BoundIdentDecl[] localIdentifiers = expression
				.getBoundIdentDecls();
		boundIdDeclStack.push(convertIdentDecl(localIdentifiers));
		for (final BoundIdentDecl boundIdentDecl : localIdentifiers) {
			boundIdentDecl.accept(this);
		}

		expression.getPredicate().accept(this);
		expression.getExpression().accept(this);
		boundIdDeclStack.pop();

		final boolean isFullType = localIdentifiers.length > 0;
		BExpressionResolved node = null;
		EList<IdentifierExpression> identifiers = null;

		switch (expression.getTag()) {
		case Formula.QUNION:
			if (isFullType) {
				final QuantifiedUnionExpression1 newNode = FormulasFactory.eINSTANCE
						.createQuantifiedUnionExpression1();
				newNode.setExpression((BExpressionResolved) stack.pop());
				newNode.setPredicate((BPredicateResolved) stack.pop());
				identifiers = newNode.getIdentifiers();
				node = newNode;
			} else {
				final QuantifiedUnionExpression2 newNode = FormulasFactory.eINSTANCE
						.createQuantifiedUnionExpression2();
				newNode.setExpression((BExpressionResolved) stack.pop());
				newNode.setPredicate((BPredicateResolved) stack.pop());
				node = newNode;
			}
			break;
		case Formula.QINTER:
			if (isFullType) {
				final QuantifiedIntersectionExpression1 newNode = FormulasFactory.eINSTANCE
						.createQuantifiedIntersectionExpression1();
				newNode.setExpression((BExpressionResolved) stack.pop());
				newNode.setPredicate((BPredicateResolved) stack.pop());
				identifiers = newNode.getIdentifiers();
				node = newNode;
			} else {
				final QuantifiedIntersectionExpression2 newNode = FormulasFactory.eINSTANCE
						.createQuantifiedIntersectionExpression2();
				newNode.setExpression((BExpressionResolved) stack.pop());
				newNode.setPredicate((BPredicateResolved) stack.pop());
				node = newNode;
			}
			break;
		case Formula.CSET:
			if (isFullType) {
				final SetComprehensionExpression1 newNode = FormulasFactory.eINSTANCE
						.createSetComprehensionExpression1();
				newNode.setExpression((BExpressionResolved) stack.pop());
				newNode.setPredicate((BPredicateResolved) stack.pop());
				identifiers = newNode.getIdentifiers();
				node = newNode;
			} else {
				final SetComprehensionExpression2 newNode = FormulasFactory.eINSTANCE
						.createSetComprehensionExpression2();
				newNode.setExpression((BExpressionResolved) stack.pop());
				newNode.setPredicate((BPredicateResolved) stack.pop());
				node = newNode;
			}
			break;

		default:
			// TODO handle unexpected tags
			break;
		}

		// attach declared local variables (coming in reverse order from stack)
		for (int i = 0; i < localIdentifiers.length; i++) {
			identifiers.add(0, (IdentifierExpression) stack.pop());
		}

		storeNode(node, expression);
	}

	@Override
	public void visitQuantifiedPredicate(final QuantifiedPredicate predicate) {
		// visit children
		final BoundIdentDecl[] localIdentifiers = predicate
				.getBoundIdentDecls();
		boundIdDeclStack.push(convertIdentDecl(localIdentifiers));
		for (final BoundIdentDecl boundIdentDecl : localIdentifiers) {
			boundIdentDecl.accept(this);
		}

		predicate.getPredicate().accept(this);
		boundIdDeclStack.pop();

		final BFormula node = (BFormula) FormulasFactory.eINSTANCE
				.create(getMatchingEClass(predicate));
		EList<BoundIdentifierExpression> identifiers = null;

		switch (predicate.getTag()) {
		// FORALL, EXISTS
		case Formula.FORALL:
			final ForallPredicate forallPred = (ForallPredicate) node;
			identifiers = forallPred.getIdentifiers();
			forallPred.setPredicate((BPredicateResolved) stack.pop());
			break;
		case Formula.EXISTS:
			final ExistPredicate existPred = (ExistPredicate) node;
			identifiers = existPred.getIdentifiers();
			existPred.setPredicate((BPredicateResolved) stack.pop());
			break;

		default:
			// TODO handle unexpected tags
			break;
		}

		// attach declared local variables (coming in reverse order from stack)
		for (int i = 0; i < localIdentifiers.length; i++) {
			identifiers.add(0, (BoundIdentifierExpression) stack.pop());
		}

		storeNode(node, predicate);
	}

	@Override
	public void visitRelationalPredicate(final RelationalPredicate predicate) {
		handleTwoChildren(predicate, predicate.getLeft(), predicate.getRight());
	}

	@Override
	public void visitSetExtension(final SetExtension expression) {
		handleMultiChildren(expression, expression.getMembers());
	}

	@Override
	public void visitSimplePredicate(final SimplePredicate predicate) {
		handleSingleChild(predicate, predicate.getExpression());
	}

	@Override
	public void visitUnaryExpression(final UnaryExpression expression) {
		handleSingleChild(expression, expression.getChild());
	}

	@Override
	public void visitUnaryPredicate(final UnaryPredicate predicate) {
		handleSingleChild(predicate, predicate.getChild());
	}

	private void handleSingleChild(final Formula rodinNode, final Formula child) {
		// visit child
		child.accept(this);

		// create node itself
		final UnaryOperator node = (UnaryOperator) FormulasFactory.eINSTANCE
				.create(getMatchingEClass(rodinNode));

		// add the children nodes
		node.setChild(stack.pop());

		storeNode(node, rodinNode);
	}

	private void handleTwoChildren(final Formula rodinNode,
			final Formula leftChild, final Formula rightChild) {
		// visit children
		visitChildren(leftChild);
		visitChildren(rightChild);

		// create node itself
		final BinaryOperator node = (BinaryOperator) FormulasFactory.eINSTANCE
				.create(getMatchingEClass(rodinNode));

		// add the children nodes
		node.setRight(stack.pop());
		node.setLeft(stack.pop());

		storeNode(node, rodinNode);
	}

	private void handleMultiChildren(final Formula rodinNode,
			final Formula[] rodinChildren) {
		visitChildren(rodinChildren);

		// create node itself
		final MultiOperand node = (MultiOperand) FormulasFactory.eINSTANCE
				.create(getMatchingEClass(rodinNode));

		// add the children nodes
		final EList<BFormula> children = node.getChildren();
		for (int i = 0; i < rodinChildren.length; i++) {
			children.add(0, stack.pop());
		}

		storeNode(node, rodinNode);
	}

	private void visitChildren(final Formula[] rodinChildren) {
		for (final Formula child : rodinChildren) {
			child.accept(this);
		}
	}

	private void visitChildren(final Formula child) {
		child.accept(this);
	}

	private List<String> convertIdentDecl(final BoundIdentDecl[] identDecl) {
		LinkedList<String> result = null;
		if (!boundIdDeclStack.isEmpty()) {
			result = new LinkedList<String>(boundIdDeclStack.peek());
		} else {
			result = new LinkedList<String>();
		}

		for (int i = 0; i < identDecl.length; i++) {
			result.addFirst(identDecl[i].getName());
		}

		return result;
	}

	private void annotatePosition(final BFormula node,
			final Formula rodinFormula) {

		final SourceLocation location = rodinFormula.getSourceLocation();
		if (location != null) {
			final int startPos = location.getStart();
			final int endPos = location.getEnd();

			TextPositionUtil.annotatePosition(node, textOffset + startPos,
					endPos - startPos + 1);
		}

		/*
		 * FIXME find out why SourceLocation is missing sometimes
		 */
	}

	private EClass getMatchingEClass(final Formula formula) {
		final EClass eClass = idToEClass.get(formula.getTag());

		if (eClass == null) {
			final String message = "Unknown Rodin formula type: ["
					+ formula.getTag() + "] " + formula.toString();

			TextToolsPlugin
					.getDefault()
					.getLog()
					.log(new Status(IStatus.ERROR, TextToolsPlugin.PLUGIN_ID,
							message));

			throw new UnsupportedOperationException(message);
		}

		return eClass;
	}

	@Override
	public void visitExtendedExpression(ExtendedExpression expression) {
		System.out
				.println("##################################### mean visitor: visitExtendedExpression");
		// TODO Auto-generated method stub

	}

	@Override
	public void visitExtendedPredicate(ExtendedPredicate predicate) {
		System.out
				.println("##################################### mean visitor: visitExtendedPredicate");
		// TODO Auto-generated method stub

	}
}
