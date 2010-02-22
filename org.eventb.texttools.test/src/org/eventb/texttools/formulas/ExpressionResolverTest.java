package org.eventb.texttools.formulas;

import junit.framework.TestCase;

import org.eventb.emf.core.machine.MachineFactory;
import org.eventb.emf.core.machine.Variant;

public class ExpressionResolverTest extends TestCase {

	public void testParseError() {
		final String input = "x y";
		final Variant emfExpr = MachineFactory.eINSTANCE.createVariant();
		emfExpr.setExpression(input);
		try {
			FormulaResolver.resolve(emfExpr);
			fail("Expected Exception was not thrown");
		} catch (final FormulaParseException e) {
			assertEquals(emfExpr, e.getEmfObject());
			assertEquals(1, e.getAstProblems().size());
		}
	}
}
