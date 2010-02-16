/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texttools.prettyprint;

import java.util.Iterator;

import org.eclipse.emf.common.util.EList;
import org.eventb.emf.core.machine.Action;
import org.eventb.emf.core.machine.Convergence;
import org.eventb.emf.core.machine.Event;
import org.eventb.emf.core.machine.Guard;
import org.eventb.emf.core.machine.Invariant;
import org.eventb.emf.core.machine.Machine;
import org.eventb.emf.core.machine.Variant;
import org.eventb.emf.core.machine.Witness;
import org.eventb.emf.core.machine.util.MachineSwitch;
import org.eventb.texttools.Constants;

/**
 * {@link MachineSwitch} which supports {@link PrettyPrinter}s. This class is
 * not intended to be used by clients other than the {@link PrettyPrinter}.
 */
public class MachinePrintSwitch extends MachineSwitch<Boolean> implements
		PrettyPrintConstants, Constants {

	private final PrettyPrinter printer;

	protected MachinePrintSwitch(final PrettyPrinter prettyPrinter) {
		printer = prettyPrinter;
	}

	@Override
	public Boolean caseMachine(final Machine object) {
		/*
		 * Header for this machine
		 */
		printer.appendWithSpace(MACHINE);
		printer.append(object.getName());
		printer.appendComment(object);

		final EList<String> refinesNames = object.getRefinesNames();
		if (refinesNames.size() > 0) {
			printer.append(SPACE);
			printer.appendWithSpace(REFINES);
			printer.appendStringList(refinesNames);
		}

		final EList<String> seesNames = object.getSeesNames();
		if (seesNames.size() > 0) {
			printer.append(SPACE);
			printer.appendWithSpace(SEES);
			printer.appendStringList(seesNames);
		}

		printer.appendLineBreak();

		/*
		 * Now all machine clauses
		 */
		final boolean newLine = (Boolean) printer.getPreference(
				PROP_NEWLINE_BETWEEN_CLAUSES, true);

		// variables
		printer.appendNameList(object.getVariables(), VARIABLES, newLine);

		// invariants
		printInvariants(object.getInvariants(), newLine);

		// variant
		final Variant variant = object.getVariant();
		if (variant != null) {
			if (newLine) {
				printer.appendLineBreak();
			}
			doSwitch(variant);
		}

		// events
		printEvents(object.getEvents(), newLine);

		/*
		 * machine footer
		 */
		printer.adjustIndent();
		printer.appendWithLineBreak(END);

		return true;
	}

	@Override
	public Boolean caseInvariant(final Invariant object) {
		printer.appendLabeledPredicate(object, true);
		return true;
	}

	@Override
	public Boolean caseGuard(final Guard object) {
		printer.appendLabeledPredicate(object, false);
		return true;
	}

	@Override
	public Boolean caseWitness(final Witness object) {
		printer.appendLabeledPredicate(object, false);
		return true;
	}

	@Override
	public Boolean caseAction(final Action object) {
		printer.adjustIndent();
		printer.appendLabel(object);
		printer.appendFormula(object.getAction(), printer.hasComment(object));
		printer.appendComment(object);

		return true;
	}

	@Override
	public Boolean caseVariant(final Variant object) {
		printer.appendWithSpace(VARIANT);
		printer.appendFormula(object.getExpression(), printer
				.hasComment(object));
		printer.appendComment(object);

		return true;
	}

	@Override
	public Boolean caseEvent(final Event object) {
		/*
		 * event header
		 */
		printer.adjustIndent();

		// convergence
		final Convergence convergence = object.getConvergence();
		if (Convergence.CONVERGENT.equals(convergence)
				&& (Boolean) printer.getPreference(PROP_PRINT_ORDINARY_KEYWORD,
						false)) {
			printer.appendWithSpace(ORDINARY);
		} else if (Convergence.ANTICIPATED.equals(convergence)) {
			printer.appendWithSpace(ANTICIPATED);
		} else if (Convergence.CONVERGENT.equals(convergence)) {
			printer.appendWithSpace(CONVERGENT);
		}

		// 'event' + name
		printer.appendWithSpace(EVENT);
		printer.append(object.getName());
		printer.appendComment(object);

		// refines / extends
		final EList<String> refinesNames = object.getRefinesNames();
		if (refinesNames.size() > 0) {
			if (printer.hasComment(object)) {
				printer.adjustIndent();
			} else {
				printer.append(SPACE);
			}

			if (object.isExtended()) {
				printer.appendWithSpace(EXTENDS);
			} else {
				printer.appendWithSpace(REFINES);
			}

			printer.appendStringList(refinesNames);
			printer.appendLineBreak();
		}

		if (!printer.hasComment(object) && refinesNames.size() == 0) {
			printer.appendLineBreak();
		}

		/*
		 * event body
		 */
		printer.increaseIndentLevel();

		// parameters
		printer.appendNameList(object.getParameters(), ANY, false);

		// guards
		final EList<Guard> guards = object.getGuards();
		if (guards.size() > 0) {
			printer.adjustIndent();
			printer.appendWithLineBreak(WHERE);

			printer.increaseIndentLevel();
			for (final Guard guard : guards) {
				doSwitch(guard);
			}
			printer.decreaseIndentLevel();
		}

		// witnesses
		final EList<Witness> witnesses = object.getWitnesses();
		if (witnesses.size() > 0) {
			printer.adjustIndent();
			printer.appendWithLineBreak(WITH);

			printer.increaseIndentLevel();
			for (final Witness witness : witnesses) {
				doSwitch(witness);
			}
			printer.decreaseIndentLevel();
		}

		// actions
		final EList<Action> actions = object.getActions();
		if (actions.size() > 0) {
			printer.adjustIndent();
			printer.appendWithLineBreak(THEN);

			printer.increaseIndentLevel();
			for (final Action action : actions) {
				doSwitch(action);
			}
			printer.decreaseIndentLevel();
		}

		/*
		 * event footer
		 */
		printer.decreaseIndentLevel();
		printer.adjustIndent();
		printer.appendWithLineBreak(END);

		return true;
	}

	private void printInvariants(final EList<Invariant> invariants,
			final boolean newLine) {
		if (invariants.size() > 0) {
			if (newLine) {
				printer.appendLineBreak();
			}

			printer.appendWithLineBreak(INVARIANTS);
			printer.increaseIndentLevel();

			for (final Invariant invariant : invariants) {
				doSwitch(invariant);
			}

			printer.decreaseIndentLevel();
		}
	}

	private void printEvents(final EList<Event> events, final boolean newLine) {
		if (events.size() > 0) {
			if (newLine) {
				printer.appendLineBreak();
			}

			printer.appendWithLineBreak(EVENTS);

			final Boolean indentEvents = (Boolean) printer.getPreference(
					PROP_INDENT_EVENTS, true);
			final Boolean linebreakBetweenEvents = (Boolean) printer
					.getPreference(PROP_NEWLINE_BETWEEN_EVENTS, true);

			if (indentEvents) {
				printer.increaseIndentLevel();
			}

			for (final Iterator<Event> iterator = events.iterator(); iterator
					.hasNext();) {
				final Event event = iterator.next();
				doSwitch(event);

				if (linebreakBetweenEvents && iterator.hasNext()) {
					printer.appendLineBreak();
				}
			}

			if (indentEvents) {
				printer.decreaseIndentLevel();
			}
		}
	}
}
