package org.eventb.texttools.internal.parsing;

import junit.framework.TestCase;

import org.eclipse.emf.common.util.EList;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eventb.emf.core.machine.Action;
import org.eventb.emf.core.machine.Convergence;
import org.eventb.emf.core.machine.Event;
import org.eventb.emf.core.machine.Guard;
import org.eventb.emf.core.machine.Invariant;
import org.eventb.emf.core.machine.Machine;
import org.eventb.emf.core.machine.Parameter;
import org.eventb.emf.core.machine.Variable;
import org.eventb.texttools.TextPositionUtil;
import org.eventb.texttools.model.texttools.TextRange;

import de.be4.eventb.core.parser.BException;
import de.be4.eventb.core.parser.EventBParser;
import de.be4.eventb.core.parser.node.Node;
import de.be4.eventb.core.parser.node.Start;

public class TransformationVisitorTest extends TestCase {

	public void testMachineWithVariablesAndComments() throws BException {
		final String input = "machine Test variables\n" + "varA, varB,\n"
				+ "/*varC\n" + "comment*/" + "varC\n" + "end";
		final Start rootNode = parseInput(input, false);

		final TransformationVisitor visitor = new TransformationVisitor();
		final IDocument document = new Document(input);
		final Machine machine = visitor.transform(rootNode, document);

		Node n;

		assertEquals("Test", machine.getName());
		assertEquals(0, machine.getRefinesNames().size());
		assertEquals(0, machine.getSees().size());

		final EList<Variable> variables = machine.getVariables();
		assertEquals(3, variables.size());

		Variable variable = variables.get(0);
		assertNull(variable.getComment());
		assertEquals("varA", variable.getName());

		variable = variables.get(1);
		assertNull(variable.getComment());
		assertEquals("varB", variable.getName());

		variable = variables.get(2);
		assertEquals("varC\n" + "comment", variable.getComment());
		assertEquals("varC", variable.getName());
	}

	public void testMachineWithInvariantsAndComments() throws BException {
		final String input = "machine Test invariants\n" + "@inv1 1=1\n"
				+ "/*inv2\n" + "comment*/" + "@inv2 2=2\n" + "end";
		final Start rootNode = parseInput(input, false);

		final TransformationVisitor visitor = new TransformationVisitor();
		final IDocument document = new Document(input);
		final Machine machine = visitor.transform(rootNode, document);

		assertEquals("Test", machine.getName());
		assertEquals(0, machine.getRefinesNames().size());
		assertEquals(0, machine.getSees().size());
		assertEquals(0, machine.getVariables().size());

		final EList<Invariant> invariants = machine.getInvariants();
		assertEquals(2, invariants.size());

		Invariant invariant = invariants.get(0);
		assertNull(invariant.getComment());
		assertEquals("inv1", invariant.getName());
		assertEquals("1=1", invariant.getPredicate());

		invariant = invariants.get(1);
		assertEquals("inv2\n" + "comment", invariant.getComment());
		assertEquals("inv2", invariant.getName());
		assertEquals("2=2", invariant.getPredicate());
	}

	public void testTheorems() throws BException {
		final String input = "machine Test invariants\n" + "@inv1 1=1\n"
				+ "theorem @inv2 2=2\n" + "@inv3 3=3\n" + "end";
		final Start rootNode = parseInput(input, false);

		final TransformationVisitor visitor = new TransformationVisitor();
		final IDocument document = new Document(input);
		final Machine machine = visitor.transform(rootNode, document);

		final EList<Invariant> invariants = machine.getInvariants();
		assertEquals(3, invariants.size());

		Invariant invariant = invariants.get(0);
		assertNull(invariant.getComment());
		assertEquals("inv1", invariant.getName());
		assertEquals("1=1", invariant.getPredicate());

		invariant = invariants.get(1);
		assertNull(invariant.getComment());
		assertEquals("inv2", invariant.getName());
		assertEquals("2=2", invariant.getPredicate());
		assertTrue(invariant.isTheorem());

		invariant = invariants.get(2);
		assertNull(invariant.getComment());
		assertEquals("inv3", invariant.getName());
		assertEquals("3=3", invariant.getPredicate());
	}

	public void testMachineWithEvents() throws BException {
		final String input = "machine Test\n" + "refines MacA, MacB\n"
				+ "events\n" + "ordinary event evt1\n" + "any a\n"
				+ "where @grd1 a=1\n" + "then\n" + "@act1 a≔1\n" + "end\n"
				+ "\n" + "anticipated event evt2\n" + "refines evtAbstract\n"
				+ "any b\n" + "with @x b=x\n" + "then\n" + "@act1 b≔1\n"
				+ "end\n" + "end";
		final Start rootNode = parseInput(input, false);

		final TransformationVisitor visitor = new TransformationVisitor();
		final IDocument document = new Document(input);
		final Machine machine = visitor.transform(rootNode, document);

		assertEquals("Test", machine.getName());
		assertEquals(2, machine.getRefinesNames().size());
		assertEquals("MacA", machine.getRefinesNames().get(0));
		assertEquals("MacB", machine.getRefinesNames().get(1));
		assertEquals(0, machine.getSees().size());
		assertEquals(0, machine.getVariables().size());

		final EList<Event> events = machine.getEvents();
		assertEquals(2, events.size());

		Event event = events.get(0);
		assertNull(event.getComment());
		assertEquals("evt1", event.getName());
		assertEquals(Convergence.ORDINARY, event.getConvergence());
		final EList<Parameter> parameters = event.getParameters();
		assertEquals(1, parameters.size());
		assertNull(parameters.get(0).getComment());
		assertEquals("a", parameters.get(0).getName());
		final EList<Guard> guards = event.getGuards();
		assertEquals(1, guards.size());
		assertNull(guards.get(0).getComment());
		assertEquals("grd1", guards.get(0).getName());
		assertEquals("a=1", guards.get(0).getPredicate());
		final EList<Action> actions = event.getActions();
		assertEquals(1, actions.size());
		assertNull(actions.get(0).getComment());
		assertEquals("act1", actions.get(0).getName());
		assertEquals("a≔1", actions.get(0).getAction());

		event = events.get(1);
		assertEquals("evt2", event.getName());
	}

	public void testEventRefinement() throws BException {
		final String input = "machine Test\n" + "events\n" + "event evt1\n"
				+ "end\n" + "\n" + "event evt2\n" + "refines evtX, evtY\n"
				+ "end\n" + "event evt3\n" + "extends evtZ\n" + "end\n" + "end";
		final Start rootNode = parseInput(input, false);

		final TransformationVisitor visitor = new TransformationVisitor();
		final IDocument document = new Document(input);
		final Machine machine = visitor.transform(rootNode, document);

		final EList<Event> events = machine.getEvents();
		assertEquals(3, events.size());

		Event event = events.get(0);
		assertEquals("evt1", event.getName());
		assertEquals(Convergence.ORDINARY, event.getConvergence());
		assertEquals(0, event.getRefinesNames().size());
		assertFalse(event.isExtended());

		event = events.get(1);
		assertEquals("evt2", event.getName());
		assertEquals(Convergence.ORDINARY, event.getConvergence());
		assertEquals(2, event.getRefinesNames().size());
		assertFalse(event.isExtended());

		event = events.get(2);
		assertEquals("evt3", event.getName());
		assertEquals(Convergence.ORDINARY, event.getConvergence());
		assertEquals(1, event.getRefinesNames().size());
		assertTrue(event.isExtended());
	}

	public void testPositions() throws BException {
		final String input = "machine Test\n" + "events\n" + "event X\n"
				+ "any Y\n" + "end\n" + "\n" + "event Y\n" + "refines X\n"
				+ "end\n" + "end";
		final Start rootNode = parseInput(input, false);

		final TransformationVisitor visitor = new TransformationVisitor();
		final IDocument document = new Document(input);
		final Machine machine = visitor.transform(rootNode, document);

		final EList<Event> events = machine.getEvents();
		assertEquals(2, events.size());

		Event event = events.get(0);
		final TextRange eventRange1 = TextPositionUtil.getTextRange(event);
		assertNotNull(eventRange1);

		final EList<Parameter> parameters = event.getParameters();
		assertEquals(1, parameters.size());
		final Parameter parameter = event.getParameters().get(0);
		final TextRange paramRange = TextPositionUtil.getTextRange(parameter);
		final TextRange stringRange1 = TextPositionUtil.getInternalPosition(
				parameter, parameter.getName());

		assertNotNull(paramRange);
		assertNotNull(stringRange1);
		assertNotSame(eventRange1, paramRange);
		assertNotSame(paramRange, stringRange1);

		assertSame(stringRange1, TextPositionUtil.getInternalPosition(
				parameter, parameter.getName()));
		assertEquals(paramRange.getOffset(), stringRange1.getOffset());
		assertEquals(paramRange.getLength(), stringRange1.getLength());

		event = events.get(1);
		final TextRange eventRange2 = TextPositionUtil.getTextRange(event);
		assertNotNull(eventRange2);

		final EList<String> refinesNames = event.getRefinesNames();
		assertEquals(1, refinesNames.size());

		final TextRange refinesRange = TextPositionUtil.getInternalPosition(
				event, refinesNames.get(0));
		assertNotNull(refinesRange);

		assertNotSame(eventRange2, refinesRange);
		assertSame(refinesRange, TextPositionUtil.getInternalPosition(event,
				refinesNames.get(0)));
		assertTrue(refinesRange.getOffset() > eventRange2.getOffset());
	}

	private Start parseInput(final String input, final boolean debugOutput)
			throws BException {
		final EventBParser parser = new EventBParser();
		final Start rootNode = parser.parse(input, debugOutput);
		return rootNode;
	}
}
