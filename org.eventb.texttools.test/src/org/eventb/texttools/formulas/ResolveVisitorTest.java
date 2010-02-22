package org.eventb.texttools.formulas;

import junit.framework.TestCase;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eventb.core.ast.Formula;
import org.eventb.emf.core.AbstractExtension;
import org.eventb.emf.core.machine.Action;
import org.eventb.emf.core.machine.Invariant;
import org.eventb.emf.core.machine.MachineFactory;
import org.eventb.emf.core.machine.Variant;
import org.eventb.emf.formulas.AddExpression;
import org.eventb.emf.formulas.AndPredicate;
import org.eventb.emf.formulas.BExpressionResolved;
import org.eventb.emf.formulas.BFormula;
import org.eventb.emf.formulas.BecomesEqualToAssignment;
import org.eventb.emf.formulas.BecomesMemberOfAssignment;
import org.eventb.emf.formulas.BecomesSuchThatAssignment;
import org.eventb.emf.formulas.BoundIdentifierExpression;
import org.eventb.emf.formulas.EqualPredicate;
import org.eventb.emf.formulas.FALSITY;
import org.eventb.emf.formulas.ForallPredicate;
import org.eventb.emf.formulas.IdentifierExpression;
import org.eventb.emf.formulas.IdentityGenExpression;
import org.eventb.emf.formulas.IntegerLiteralExpression;
import org.eventb.emf.formulas.LessPredicate;
import org.eventb.emf.formulas.PartitionPredicate;
import org.eventb.emf.formulas.PredExpression;
import org.eventb.emf.formulas.Prj1GenExpression;
import org.eventb.emf.formulas.QuantifiedUnionExpression1;
import org.eventb.emf.formulas.SetExpression;
import org.eventb.emf.formulas.TRUE;
import org.eventb.emf.formulas.TRUTH;
import org.eventb.texttools.TextPositionUtil;
import org.eventb.texttools.model.texttools.TextRange;
import org.eventb.texttools.model.texttools.TexttoolsFactory;

public class ResolveVisitorTest extends TestCase {

	/**
	 * Tests free_identifier, integer_literal, add_expression
	 * 
	 * @throws FormulaParseException
	 */
	public void testAdd() throws FormulaParseException {
		final String input = "x+5+y";
		final Variant emfExpr = MachineFactory.eINSTANCE.createVariant();
		emfExpr.setExpression(input);
		FormulaResolver.resolve(emfExpr);
		assertEquals(1, emfExpr.getExtensions().size());
		final AbstractExtension extension = emfExpr.getExtensions().get(0);

		assertTrue(extension instanceof AddExpression);
		final AddExpression addExpr = (AddExpression) extension;
		assertEquals(3, addExpr.getChildren().size());
		assertTrue(addExpr.getChildren().get(0) instanceof IdentifierExpression);
		assertTrue(addExpr.getChildren().get(1) instanceof IntegerLiteralExpression);
	}

	/**
	 * Tests free_identifier, EQUAL, TRUE
	 * 
	 * @throws FormulaParseException
	 */
	public void testEqual() throws FormulaParseException {
		final String input = "x=TRUE";
		final Invariant emfExpr = MachineFactory.eINSTANCE.createInvariant();
		emfExpr.setPredicate(input);
		FormulaResolver.resolve(emfExpr);
		assertEquals(1, emfExpr.getExtensions().size());
		final AbstractExtension extension = emfExpr.getExtensions().get(0);

		assertTrue(extension instanceof EqualPredicate);
		final EqualPredicate addExpr = (EqualPredicate) extension;
		assertTrue(addExpr.getLeft() instanceof IdentifierExpression);
		assertEquals("x", ((IdentifierExpression) addExpr.getLeft()).getName());
		assertTrue(addExpr.getRight() instanceof TRUE);
	}

	/**
	 * Tests TRUTH, FALSITY and AND
	 * 
	 * @throws FormulaParseException
	 */
	public void testAnd() throws FormulaParseException {
		final String input = "\u22a4 \u2227 \u22a4 \u2227 \u22a5";
		final Invariant emfExpr = MachineFactory.eINSTANCE.createInvariant();
		emfExpr.setPredicate(input);
		FormulaResolver.resolve(emfExpr);
		assertEquals(1, emfExpr.getExtensions().size());
		final AbstractExtension extension = emfExpr.getExtensions().get(0);

		assertTrue(extension instanceof AndPredicate);
		final AndPredicate andPred = (AndPredicate) extension;
		assertEquals(3, andPred.getChildren().size());
		assertTrue(andPred.getChildren().get(0) instanceof TRUTH);
		assertTrue(andPred.getChildren().get(1) instanceof TRUTH);
		assertTrue(andPred.getChildren().get(2) instanceof FALSITY);
	}

	/**
	 * Tests KPRED
	 * 
	 * @throws FormulaParseException
	 */
	public void testPred() throws FormulaParseException {
		final String input = "pred";
		final Variant emfExpr = MachineFactory.eINSTANCE.createVariant();
		emfExpr.setExpression(input);
		FormulaResolver.resolve(emfExpr);
		assertEquals(1, emfExpr.getExtensions().size());
		final AbstractExtension extension = emfExpr.getExtensions().get(0);

		assertTrue(extension instanceof PredExpression);
	}

	/**
	 * Tests {@link Formula#KPRJ1_GEN}
	 * 
	 * @throws FormulaParseException
	 */
	public void testPrj1Gen() throws FormulaParseException {
		final String input = "prj1";
		final Variant emfExpr = MachineFactory.eINSTANCE.createVariant();
		emfExpr.setExpression(input);
		FormulaResolver.resolve(emfExpr);
		assertEquals(1, emfExpr.getExtensions().size());
		final AbstractExtension extension = emfExpr.getExtensions().get(0);

		assertTrue(extension instanceof Prj1GenExpression);
	}

	/**
	 * Tests Tests {@link Formula#KID_GEN}
	 * 
	 * @throws FormulaParseException
	 */
	public void testIdentityGen() throws FormulaParseException {
		final String input = "id";
		final Variant emfExpr = MachineFactory.eINSTANCE.createVariant();
		emfExpr.setExpression(input);
		FormulaResolver.resolve(emfExpr);
		assertEquals(1, emfExpr.getExtensions().size());
		final AbstractExtension extension = emfExpr.getExtensions().get(0);

		assertTrue(extension instanceof IdentityGenExpression);
	}

	/**
	 * Tests forall predicate with 2 local variables and an equal as predicate
	 * 
	 * @throws FormulaParseException
	 */
	public void testForall() throws FormulaParseException {
		final String input = "\u2200 x,y \u00b7 x=y";
		final Invariant emfExpr = MachineFactory.eINSTANCE.createInvariant();
		emfExpr.setPredicate(input);
		FormulaResolver.resolve(emfExpr);
		assertEquals(1, emfExpr.getExtensions().size());
		final AbstractExtension extension = emfExpr.getExtensions().get(0);

		assertTrue(extension instanceof ForallPredicate);
		final ForallPredicate predicate = (ForallPredicate) extension;
		final EList<BoundIdentifierExpression> identifiers = predicate
				.getIdentifiers();
		assertEquals("x", identifiers.get(0).getName());
		assertEquals("y", identifiers.get(1).getName());

		assertTrue(predicate.getPredicate() instanceof EqualPredicate);
		final EqualPredicate equalPred = (EqualPredicate) predicate
				.getPredicate();
		assertTrue(equalPred.getLeft() instanceof BoundIdentifierExpression);
		assertTrue(equalPred.getRight() instanceof BoundIdentifierExpression);
	}

	public void testQuantifiedUnion() throws FormulaParseException {
		final String input = "\u22c3 x,y \u00b7 \u22a5 \u2223 x";
		final Variant emfExpr = MachineFactory.eINSTANCE.createVariant();
		emfExpr.setExpression(input);
		FormulaResolver.resolve(emfExpr);
		assertEquals(1, emfExpr.getExtensions().size());
		final AbstractExtension extension = emfExpr.getExtensions().get(0);

		assertTrue(extension instanceof QuantifiedUnionExpression1);
		final QuantifiedUnionExpression1 expression = (QuantifiedUnionExpression1) extension;

		assertEquals(2, expression.getIdentifiers().size());
		assertTrue(expression.getPredicate() instanceof FALSITY);
		assertTrue(expression.getExpression() instanceof IdentifierExpression);
	}

	public void testPartitionExpression() throws FormulaParseException {
		final String input = "partition(S, {a}, {b})";
		final Invariant emfExpr = MachineFactory.eINSTANCE.createInvariant();
		emfExpr.setPredicate(input);
		FormulaResolver.resolve(emfExpr);
		assertEquals(1, emfExpr.getExtensions().size());
		final AbstractExtension extension = emfExpr.getExtensions().get(0);

		assertTrue(extension instanceof PartitionPredicate);
		final PartitionPredicate predicate = (PartitionPredicate) extension;

		final EList<BFormula> children = predicate.getChildren();
		assertEquals(3, children.size());
		assertTrue(children.get(0) instanceof IdentifierExpression);
		assertTrue(children.get(1) instanceof SetExpression);
		assertTrue(children.get(2) instanceof SetExpression);
	}

	public void testBecomesEqual() throws FormulaParseException {
		final String input = "x ≔ x + 1";
		final Action emfExpr = MachineFactory.eINSTANCE.createAction();
		emfExpr.setAction(input);
		FormulaResolver.resolve(emfExpr);
		assertEquals(1, emfExpr.getExtensions().size());
		final AbstractExtension extension = emfExpr.getExtensions().get(0);

		assertTrue(extension instanceof BecomesEqualToAssignment);
		final BecomesEqualToAssignment assignment = (BecomesEqualToAssignment) extension;

		assertEquals(1, assignment.getIdentifiers().size());
		final EList<IdentifierExpression> identifiers = assignment
				.getIdentifiers();
		assertEquals("x", identifiers.get(0).getName());

		assertEquals(1, assignment.getExpressions().size());
		final EList<BExpressionResolved> expressions = assignment
				.getExpressions();
		assertTrue(expressions.get(0) instanceof AddExpression);
	}

	public void testSuchThat() throws FormulaParseException {
		final String input = "x :\u2223 x < x'";
		final Action emfExpr = MachineFactory.eINSTANCE.createAction();
		emfExpr.setAction(input);
		FormulaResolver.resolve(emfExpr);
		assertEquals(1, emfExpr.getExtensions().size());
		final AbstractExtension extension = emfExpr.getExtensions().get(0);

		assertTrue(extension instanceof BecomesSuchThatAssignment);
		final BecomesSuchThatAssignment assignment = (BecomesSuchThatAssignment) extension;

		assertEquals(1, assignment.getIdentifiers().size());
		final EList<IdentifierExpression> identifiers = assignment
				.getIdentifiers();
		assertEquals("x", identifiers.get(0).getName());

		assertTrue(assignment.getPredicate() instanceof LessPredicate);
		final LessPredicate lessPred = (LessPredicate) assignment
				.getPredicate();

		assertEquals("x", ((IdentifierExpression) lessPred.getLeft()).getName());
		assertEquals("x'", ((BoundIdentifierExpression) lessPred.getRight())
				.getName());
	}

	public void testElementOf() throws FormulaParseException {
		final String input = "x :\u2208 S";
		final Action emfExpr = MachineFactory.eINSTANCE.createAction();
		emfExpr.setAction(input);
		FormulaResolver.resolve(emfExpr);
		assertEquals(1, emfExpr.getExtensions().size());
		final AbstractExtension extension = emfExpr.getExtensions().get(0);

		assertTrue(extension instanceof BecomesMemberOfAssignment);
		final BecomesMemberOfAssignment assignment = (BecomesMemberOfAssignment) extension;

		assertEquals(1, assignment.getIdentifiers().size());
		final EList<IdentifierExpression> identifiers = assignment
				.getIdentifiers();
		assertEquals("x", identifiers.get(0).getName());

		assertTrue(assignment.getExpression() instanceof IdentifierExpression);
		final IdentifierExpression identExpr = (IdentifierExpression) assignment
				.getExpression();
		assertEquals("S", identExpr.getName());
	}

	public void testSourceAnnotations() throws FormulaParseException {
		final String input = "\u2200 x,y \u00b7 x=z+5+y";
		final Invariant emfExpr = MachineFactory.eINSTANCE.createInvariant();
		emfExpr.setPredicate(input);
		FormulaResolver.resolve(emfExpr);
		assertEquals(1, emfExpr.getExtensions().size());
		final AbstractExtension extension = emfExpr.getExtensions().get(0);

		final TreeIterator<Object> iterator = EcoreUtil.getAllContents(
				extension, false);
		while (iterator.hasNext()) {
			final Object next = iterator.next();
			if (next instanceof BFormula) {
				final BFormula formula = (BFormula) next;
				final EAnnotation annotation = formula
						.getEAnnotation(TextPositionUtil.ANNOTATION_TEXTRANGE);
				assertNotNull(annotation);
			} else if (next instanceof EAnnotation) {
				final EAnnotation annotation = (EAnnotation) next;
				assertNotNull(annotation.getEModelElement());
				assertNotNull(TextPositionUtil.getTextRange(annotation
						.getEModelElement()));
			} else if (next instanceof TextRange) {
				final TextRange range = (TextRange) next;
				assertTrue(range.getOffset() + " >= " + 0,
						range.getOffset() >= 0);
				assertTrue(range.getOffset() + " < " + input.length(), range
						.getOffset() < input.length());
				assertTrue(range.getLength() + " <= " + input.length(), range
						.getLength() <= input.length());
				assertTrue(range.getLength() + " > " + input.length(), range
						.getLength() > 0);
			} else {
				System.out.println("Found untested types: "
						+ next.getClass().getSimpleName());
			}
		}
	}

	public void testSourceOffset() throws FormulaParseException {
		final String input = "\u2200 x,y \u00b7 x=z+5+y";
		final int offset = 50;

		final Invariant emfExpr = MachineFactory.eINSTANCE.createInvariant();
		emfExpr.setPredicate(input);
		TextPositionUtil.annotatePosition(emfExpr, offset - 10,
				input.length() + 10);

		final TextRange subRange = TexttoolsFactory.eINSTANCE.createTextRange();
		subRange.setOffset(offset);
		subRange.setLength(input.length());
		TextPositionUtil.addInternalPosition(emfExpr, input, subRange);

		FormulaResolver.resolve(emfExpr);
		assertEquals(1, emfExpr.getExtensions().size());
		final AbstractExtension extension = emfExpr.getExtensions().get(0);

		final TreeIterator<EObject> iterator = EcoreUtil.getAllContents(
				extension, false);

		while (iterator.hasNext()) {
			final EObject next = iterator.next();

			if (next instanceof BFormula) {
				// test: every element has a position annotation
				final BFormula formula = (BFormula) next;
				final EAnnotation annotation = formula
						.getEAnnotation(TextPositionUtil.ANNOTATION_TEXTRANGE);
				assertNotNull(annotation);
			} else if (next instanceof TextRange) {
				// test: the position information is within the expected range
				final TextRange range = (TextRange) next;
				assertTrue(range.getOffset() + " >= " + offset, range
						.getOffset() >= offset);
				assertTrue(range.getOffset() + " < " + offset + input.length(),
						range.getOffset() < offset + input.length());
				assertTrue(range.getLength() + " > " + 0, range.getLength() > 0);
				assertTrue(range.getLength() + " <= " + input.length(), range
						.getLength() <= input.length());
			} else if (!(next instanceof EAnnotation)) {
				System.out.println("Found untested types: "
						+ next.getClass().getSimpleName());
			}
		}
	}
}
