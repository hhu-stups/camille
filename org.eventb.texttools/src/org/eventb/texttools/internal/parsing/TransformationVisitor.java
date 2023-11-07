/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texttools.internal.parsing;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import de.be4.eventb.core.parser.analysis.DepthFirstAdapter;
import de.be4.eventb.core.parser.node.AAction;
import de.be4.eventb.core.parser.node.AAnticipatedConvergence;
import de.be4.eventb.core.parser.node.AAxiom;
import de.be4.eventb.core.parser.node.ACarrierSet;
import de.be4.eventb.core.parser.node.AConstant;
import de.be4.eventb.core.parser.node.AContextParseUnit;
import de.be4.eventb.core.parser.node.AConvergentConvergence;
import de.be4.eventb.core.parser.node.ADerivedAxiom;
import de.be4.eventb.core.parser.node.ADerivedGuard;
import de.be4.eventb.core.parser.node.ADerivedInvariant;
import de.be4.eventb.core.parser.node.AEvent;
import de.be4.eventb.core.parser.node.AExtendedEventRefinement;
import de.be4.eventb.core.parser.node.AGuard;
import de.be4.eventb.core.parser.node.AInvariant;
import de.be4.eventb.core.parser.node.AMachineParseUnit;
import de.be4.eventb.core.parser.node.AOrdinaryConvergence;
import de.be4.eventb.core.parser.node.AParameter;
import de.be4.eventb.core.parser.node.ARefinesEventRefinement;
import de.be4.eventb.core.parser.node.AVariable;
import de.be4.eventb.core.parser.node.AVariant;
import de.be4.eventb.core.parser.node.AWitness;
import de.be4.eventb.core.parser.node.Node;
import de.be4.eventb.core.parser.node.PEventRefinement;
import de.be4.eventb.core.parser.node.TComment;
import de.be4.eventb.core.parser.node.TFormula;
import de.be4.eventb.core.parser.node.TIdentifierLiteral;
import de.be4.eventb.core.parser.node.TLabel;
import de.be4.eventb.core.parser.node.Token;
import de.hhu.stups.sablecc.patch.IToken;
import de.hhu.stups.sablecc.patch.PositionedNode;
import de.hhu.stups.sablecc.patch.SourcePosition;

import org.eclipse.emf.common.util.EList;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eventb.core.IVariant;
import org.eventb.emf.core.EventBCommented;
import org.eventb.emf.core.EventBElement;
import org.eventb.emf.core.EventBNamed;
import org.eventb.emf.core.EventBNamedCommentedPredicateElement;
import org.eventb.emf.core.EventBObject;
import org.eventb.emf.core.context.Axiom;
import org.eventb.emf.core.context.Context;
import org.eventb.emf.core.context.ContextFactory;
import org.eventb.emf.core.machine.Action;
import org.eventb.emf.core.machine.Convergence;
import org.eventb.emf.core.machine.Event;
import org.eventb.emf.core.machine.Guard;
import org.eventb.emf.core.machine.Invariant;
import org.eventb.emf.core.machine.Machine;
import org.eventb.emf.core.machine.MachineFactory;
import org.eventb.emf.core.machine.Variant;
import org.eventb.texttools.TextPositionUtil;
import org.eventb.texttools.model.texttools.TextRange;
import org.eventb.texttools.model.texttools.TexttoolsFactory;

public class TransformationVisitor extends DepthFirstAdapter {

	private IDocument document;

	private final Stack<EventBObject> stack = new Stack<EventBObject>();
	private final Stack<Convergence> convergenceStack = new Stack<Convergence>();

	@SuppressWarnings("unchecked")
	public <T extends EventBObject> T transform(final Node rootNode,
			final IDocument document) {
		this.document = document;
		stack.clear();
		convergenceStack.clear();

		rootNode.apply(this);

		this.document = null;

		assert stack.size() == 1;
		return (T) stack.pop();
	}

	@Override
	public void outAMachineParseUnit(final AMachineParseUnit node) {
		final Machine newNode = MachineFactory.eINSTANCE.createMachine();
		TextPositionUtil.annotatePosition(newNode, createTextRange(node));

		handleNamedAndCommented(newNode, node, node.getName(), node
				.getComments(), false);
		handleList(newNode, newNode.getRefinesNames(), node.getRefinesNames());
		handleList(newNode, newNode.getSeesNames(), node.getSeenNames());

		/*
		 * Child elements have been visited before and are lying on the stack in
		 * reverse order.
		 */
		handleList(newNode.getEvents(), node.getEvents().size());

		// variant
		if (node.getVariant() != null) {
			newNode.getVariants().add((Variant) stack.pop());
		}
		

		// TODO theorems
		//final EList<Theorem> theorems = newNode.getTheorems();
		// handleList(theorems, node.getTheorems().size());

		handleList(newNode.getInvariants(), node.getInvariants().size());
		handleList(newNode.getVariables(), node.getVariables().size());

		stack.push(newNode);
	}

	@Override
	public void outAContextParseUnit(final AContextParseUnit node) {
		final Context newNode = ContextFactory.eINSTANCE.createContext();
		TextPositionUtil.annotatePosition(newNode, createTextRange(node));

		handleNamedAndCommented(newNode, node, node.getName(), node
				.getComments(), false);
		handleList(newNode, newNode.getExtendsNames(), node.getExtendsNames());

		/*
		 * Child elements have been visited before and are lying on the stack in
		 * reverse order.
		 */
		handleList(newNode.getAxioms(), node.getAxioms().size());

		// TODO theorems
		// final EList<Theorem> theorems = newNode.getTheorems();
		// handleList(theorems, node.getTheorems().size());
		handleList(newNode.getConstants(), node.getConstants().size());
		handleList(newNode.getSets(), node.getSets().size());

		stack.push(newNode);

	}

	@Override
	public void outAEvent(final AEvent node) {
		final Event newNode = MachineFactory.eINSTANCE.createEvent();
		TextPositionUtil.annotatePosition(newNode, createTextRange(node));

		handleNamedAndCommented(newNode, node, node.getName(), node
				.getComments(), false);

		if (node.getConvergence() != null) {
			newNode.setConvergence(convergenceStack.pop());
		}

		// event refinement is a bit more complicated
		final PEventRefinement refinement = node.getRefinement();
		if (refinement != null) {
			if (refinement instanceof ARefinesEventRefinement) {
				handleList(newNode, newNode.getRefinesNames(),
						((ARefinesEventRefinement) refinement).getNames());
				newNode.setExtended(false);
			} else {
				final AExtendedEventRefinement extended = (AExtendedEventRefinement) refinement;
				final TIdentifierLiteral extendsToken = extended.getName();
				final String extendsName = extendsToken.getText();
				newNode.getRefinesNames().add(extendsName);
				storePosition(extendsName, newNode, extendsToken);
				newNode.setExtended(true);
			}
		} else {
			newNode.setExtended(false);
		}

		/*
		 * Child elements have been visited before and are lying on the stack in
		 * reverse order.
		 */
		handleList(newNode.getActions(), node.getActions().size());
		handleList(newNode.getWitnesses(), node.getWitnesses().size());
		handleList(newNode.getGuards(), node.getGuards().size());
		handleList(newNode.getParameters(), node.getParameters().size());

		stack.push(newNode);

	}

	@Override
	public void outAVariable(final AVariable node) {
		handleNamedAndCommented(MachineFactory.eINSTANCE.createVariable(),
				node, node.getName(), node.getComments(), true);
	}

	@Override
	public void outAInvariant(final AInvariant node) {
		handleLabeledPredicate(MachineFactory.eINSTANCE.createInvariant(),
				node, node.getPredicate(), node.getName(), node.getComments(),
				true);
	}

	@Override
	public void outADerivedInvariant(final ADerivedInvariant node) {
		final Invariant invariant = MachineFactory.eINSTANCE.createInvariant();
		invariant.setTheorem(true);
		handleLabeledPredicate(invariant, node, node.getPredicate(), node
				.getName(), node.getComments(), true);
	}

	@Override
	public void outAVariant(final AVariant node) {
		final Variant newNode = MachineFactory.eINSTANCE.createVariant();
		TextPositionUtil.annotatePosition(newNode, createTextRange(node));

		handleComment(newNode, node.getComments());
		// Work around an Event-B EMF bug that sets an explicit empty label and suppresses the default label.
		// TODO Allow a label here in the Camille grammar?
		newNode.setName(IVariant.DEFAULT_LABEL);

		final TFormula exprToken = node.getExpression();
		final String exprString = exprToken.getText();
		newNode.setExpression(exprString);
		storePosition(exprString, newNode, exprToken);

		stack.push(newNode);
	}

	@Override
	public void outAOrdinaryConvergence(final AOrdinaryConvergence node) {
		convergenceStack.push(Convergence.ORDINARY);
	}

	@Override
	public void outAAnticipatedConvergence(final AAnticipatedConvergence node) {
		convergenceStack.push(Convergence.ANTICIPATED);
	}

	@Override
	public void outAConvergentConvergence(final AConvergentConvergence node) {
		convergenceStack.push(Convergence.CONVERGENT);
	}

	@Override
	public void outAAction(final AAction node) {
		final Action newNode = MachineFactory.eINSTANCE.createAction();
		handleNamedAndCommented(newNode, node, node.getName(), node
				.getComments(), true);

		final TFormula actionToken = node.getAction();
		final String actionString = actionToken.getText();
		newNode.setAction(actionString);
		storePosition(actionString, newNode, actionToken);
	}

	@Override
	public void outAWitness(final AWitness node) {
		handleLabeledPredicate(MachineFactory.eINSTANCE.createWitness(), node,
				node.getPredicate(), node.getName(), node.getComments(), true);
	}

	@Override
	public void outAGuard(final AGuard node) {
		handleLabeledPredicate(MachineFactory.eINSTANCE.createGuard(), node,
				node.getPredicate(), node.getName(), node.getComments(), true);
	}
	
	@Override
	public void outADerivedGuard(final ADerivedGuard node) {
		Guard guard = MachineFactory.eINSTANCE.createGuard();
		guard.setTheorem(true);
		handleLabeledPredicate(guard, node, node.getPredicate(), node.getName(), node.getComments(), true);
	}		

	@Override
	public void outAParameter(final AParameter node) {
		handleNamedAndCommented(MachineFactory.eINSTANCE.createParameter(),
				node, node.getName(), node.getComments(), true);
	}

	@Override
	public void outAAxiom(final AAxiom node) {
		handleLabeledPredicate(ContextFactory.eINSTANCE.createAxiom(), node,
				node.getPredicate(), node.getName(), node.getComments(), true);
	}

	@Override
	public void outADerivedAxiom(final ADerivedAxiom node) {
		final Axiom axiom = ContextFactory.eINSTANCE.createAxiom();
		axiom.setTheorem(true);
		handleLabeledPredicate(axiom, node, node.getPredicate(),
				node.getName(), node.getComments(), true);
	}

	@Override
	public void outACarrierSet(final ACarrierSet node) {
		handleNamedAndCommented(ContextFactory.eINSTANCE.createCarrierSet(),
				node, node.getName(), node.getComments(), true);
	}

	@Override
	public void outAConstant(final AConstant node) {
		handleNamedAndCommented(ContextFactory.eINSTANCE.createConstant(),
				node, node.getName(), node.getComments(), true);
	}

	private void handleNamedAndCommented(final EventBNamed newNode,
			final PositionedNode node, final Token name,
			final LinkedList<TComment> comments, final boolean store) {
		if (store) {
			/*
			 * Need to annotate position before continuing because substrings
			 * will need the annotation.
			 */
			TextPositionUtil.annotatePosition((EventBElement) newNode,
					createTextRange(node));
		}

		handleComment((EventBCommented) newNode, comments);
		handleName(newNode, name);

		if (store) {
			stack.push((EventBElement) newNode);
		}
	}

	private void handleLabeledPredicate(
			final EventBNamedCommentedPredicateElement newNode,
			final Node node, final TFormula predicate, final TLabel name,
			final LinkedList<TComment> comments, final boolean store) {
		handleNamedAndCommented(newNode, node, name, comments, store);

		final String predText = predicate.getText();
		newNode.setPredicate(predText);
		storePosition(predText, newNode, predicate);
	}

	@SuppressWarnings("unchecked")
	private <T extends EventBObject> void handleList(final EList<T> targetList,
			final int childrenNumber) {
		for (int i = 0; i < childrenNumber; i++) {
			final T childNode = (T) stack.pop();
			targetList.add(0, childNode);
		}
	}

	private <T> void handleList(final EventBObject emfParent,
			final EList<String> targetList,
			final List<TIdentifierLiteral> children) {
		for (final TIdentifierLiteral token : children) {
			final String text = token.getText();
			targetList.add(text);
			storePosition(text, emfParent, token);
		}
	}

	private void handleName(final EventBNamed emfElement, final Token nameToken) {
		if (nameToken != null) {
			final String name = nameToken.getText();
			emfElement.setName(name);
			storePosition(name, (EventBElement) emfElement, nameToken);
		}
	}

	private void handleComment(final EventBCommented emfElement,
			final LinkedList<TComment> comments) {
		if (comments != null && comments.size() > 0) {
			final StringBuffer buffer = new StringBuffer();

			final Iterator<TComment> iterator = comments.iterator();
			while (iterator.hasNext()) {
				final TComment comment = iterator.next();
				buffer.append(comment.getText());

				if (iterator.hasNext()) {
					buffer.append('\n');
				}
			}

			final String completeComment = buffer.toString();

			emfElement.setComment(completeComment);

			final TComment firstToken = comments.get(0);
			final TextRange range = createTextRange(calculateOffset(firstToken
					.getLine(), firstToken.getPos()), buffer.length());
			storePosition(completeComment, (EventBElement) emfElement, range);
		}
	}

	private void storePosition(final String keyString,
			final EventBObject emfParent, final TextRange range) {
		if (keyString == null) {
			return;
		}

		if (range != null) {
			TextPositionUtil.addInternalPosition(emfParent, keyString, range);
		}
	}

	private void storePosition(final String keyString,
			final EventBObject emfParent, final IToken token) {
		storePosition(keyString, emfParent, createTextRange(token));
	}

	private TextRange createTextRange(final IToken token) {
		final int offset = calculateOffset(token.getLine(), token.getPos());
		final int length = token.getText().length();

		return createTextRange(offset, length);
	}

	private TextRange createTextRange(final PositionedNode node) {
		final int offset = calculateOffset(node.getStartPos());
		final int length = calculateOffset(node.getEndPos()) - offset;

		return createTextRange(offset, length);
	}

	private TextRange createTextRange(final int offset, final int length) {
		final TextRange range = TexttoolsFactory.eINSTANCE.createTextRange();
		range.setOffset(offset);
		range.setLength(length);

		return range;
	}

	private int calculateOffset(final int line, final int pos) {
		try {
			return document.getLineOffset(line - 1) + pos - 1;
		} catch (final BadLocationException e) {
			// IGNORE and return fallback value
			return 0;
		}
	}

	private int calculateOffset(final SourcePosition position) {
		if (position != null) {
			return calculateOffset(position.getLine(), position.getPos());
		}

		return 0;
	}
}
