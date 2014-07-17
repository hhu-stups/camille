/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texttools.prettyprint;

import org.eclipse.emf.common.util.EList;
import org.eventb.emf.core.context.Axiom;
import org.eventb.emf.core.context.Context;
import org.eventb.emf.core.context.util.ContextSwitch;
import org.eventb.texttools.Constants;

/**
 * {@link ContextSwitch} which supports {@link PrettyPrinter}s. This class is
 * not intended to be used by clients other than the {@link PrettyPrinter}.
 */
public class ContextPrintSwitch extends ContextSwitch<Boolean> implements
		PrettyPrintConstants, Constants {

	private final PrettyPrinter printer;

	protected ContextPrintSwitch(final PrettyPrinter prettyPrinter) {
		printer = prettyPrinter;
	}

	@Override
	public Boolean caseContext(final Context object) {
		/*
		 * Header for this context
		 */
		printer.appendWithSpace(CONTEXT);
		printer.append(object.getName());
		printer.appendComment(object);

		final EList<String> extendsNames = object.getExtendsNames();
		if (extendsNames.size() > 0) {
			printer.append(SPACE);
			printer.appendWithSpace(EXTENDS);
			printer.appendStringList(extendsNames);
		}

		printer.appendLineBreak();

		/*
		 * Now all context clauses
		 */
		final boolean newLine = (Boolean) printer.getPreference(
				PROP_NEWLINE_BETWEEN_CLAUSES, true);

		// sets
		printer.appendNameList(object.getSets(), SETS, newLine);

		// constants
		printer.appendNameList(object.getConstants(), CONSTANTS, newLine);

		// axioms
		printAxioms(object.getAxioms(), newLine);

		/*
		 * context footer
		 */
		printer.adjustIndent();
		printer.appendWithLineBreak(END);

		return true;
	}

	@Override
	public Boolean caseAxiom(final Axiom object) {
		printer.appendLabeledPredicate(object, true);
		return true;
	}

	private void printAxioms(final EList<Axiom> axioms, final boolean newLine) {
		if (axioms.size() > 0) {
			if (newLine) {
				printer.appendLineBreak();
			}

			printer.appendWithLineBreak(AXIOMS);
			printer.increaseIndentLevel();

			for (final Axiom axiom : axioms) {
				doSwitch(axiom);
			}

			printer.decreaseIndentLevel();
		}
	}
}
