/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texttools;

public interface Constants {
	public static final String MACHINE = "machine";
	public static final String CONTEXT = "context";
	public static final String REFINES = "refines";
	public static final String SEES = "sees";
	public static final String EXTENDS = "extends";
	public static final String VARIABLES = "variables";
	public static final String INVARIANTS = "invariants";
	public static final String THEOREM = "theorem";
	public static final String VARIANT = "variant";
	public static final String EVENTS = "events";
	public static final String EVENT = "event";
	public static final String ORDINARY = "ordinary";
	public static final String ANTICIPATED = "anticipated";
	public static final String CONVERGENT = "convergent";
	public static final String ANY = "any";
	public static final String WHERE = "where";
	public static final String WHEN = "when";
	public static final String WITH = "with";
	public static final String THEN = "then";
	public static final String BEGIN = "begin";
	public static final String AXIOMS = "axioms";
	public static final String CONSTANTS = "constants";
	public static final String SETS = "sets";
	public static final String END = "end";

	public static final String[] structural_keywords = { MACHINE, CONTEXT,
			REFINES, SEES, EXTENDS, VARIABLES, INVARIANTS, THEOREM, VARIANT,
			EVENTS, EVENT, ORDINARY, ANTICIPATED, CONVERGENT, ANY, WHERE,
			WHEN, WITH, THEN, BEGIN, AXIOMS, CONSTANTS, SETS, END };

	public static final String[] machine_keywords = { MACHINE, REFINES, SEES,
			EXTENDS, VARIABLES, INVARIANTS, THEOREM, VARIANT, EVENTS, END };

	public static final String[] event_keywords = { REFINES, EXTENDS, EVENT,
			ORDINARY, ANTICIPATED, CONVERGENT, ANY, WHERE, WHEN, WITH, THEN,
			BEGIN, END };

	public static final String[] context_keywords = { CONTEXT, EXTENDS, AXIOMS,
			CONSTANTS, SETS, END };

	public static final String[] formula_keywords = { "BOOL", "FALSE", "TRUE",
			"bool", "card", "dom", "finite", "id", "inter", "max", "min",
			"mod", "pred", "prj1", "prj2", "ran", "succ", "union", "\u2115",
			"\u2115\u0031", "\u2119", "\u2119\u0031", "\u2124" };
}
