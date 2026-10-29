/**
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen,
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html)
 * */

package org.eventb.texteditor.ui.editor.quickfix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eventb.texteditor.ui.Images;
import org.eventb.texteditor.ui.editor.codecompletion.StructureAnalyzer;
import org.eventb.texteditor.ui.editor.refactoring.EventBTokens;
import org.eventb.texteditor.ui.editor.refactoring.EventBTokens.EventBlock;
import org.eventb.texteditor.ui.editor.refactoring.EventBTokens.Kind;
import org.eventb.texteditor.ui.editor.refactoring.EventBTokens.Token;
import org.eventb.texteditor.ui.editor.refactoring.ProjectStructure;
import org.eventb.texteditor.ui.editor.refactoring.ProjectStructure.Context;
import org.eventb.texteditor.ui.editor.refactoring.ProjectStructure.Event;
import org.eventb.texteditor.ui.editor.refactoring.ProjectStructure.Machine;
import org.eventb.texttools.Constants;

/**
 * Quick fixes (Ctrl+1) for a name that is used but not declared: declare it
 * as variable (with typing invariant and initialisation), as parameter of the
 * event (with typing guard), as constant (with typing axiom) or carrier set,
 * or add the context that declares it to the <code>sees</code> or
 * <code>extends</code> clause. If a known name differs only in upper/lower
 * case, the name can be replaced by it.
 * <p>
 * The analysis is text based (unsaved changes count) and uses the Rodin
 * database only for the elements of other components.
 * </p>
 */
public final class DeclarationQuickFixes {

	/** One proposal */
	public static final class Fix {
		public final String label;
		public final String image;
		public final TextChange change;

		Fix(final String label, final String image, final TextChange change) {
			this.label = label;
			this.image = image;
			this.change = change;
		}
	}

	private static final String DEFAULT_TYPE = "ℕ"; // NAT
	private static final String DEFAULT_VALUE = "0";
	private static final String ELEMENT_OF = " ∈ ";
	private static final String BECOMES_EQUAL = " ≔ ";

	private static final Set<String> KEYWORDS = new HashSet<String>();
	/** keywords that end a clause of a machine or context */
	private static final Set<String> CLAUSE_ENDS = new HashSet<String>();
	static {
		KEYWORDS.addAll(Arrays.asList(Constants.structural_keywords));
		KEYWORDS.addAll(Arrays.asList(Constants.formula_keywords));
		CLAUSE_ENDS.addAll(Arrays.asList(Constants.structural_keywords));
		CLAUSE_ENDS.remove(Constants.THEOREM);
	}

	private static final Pattern BINDER = Pattern
			.compile("[∀∃λ⋃⋂{]([^·{}|]*)·");

	private final String text;
	private final EventBTokens tokens;
	private final ProjectStructure structure;
	private final String name;
	private final int tokenIndex;
	private final EventBlock event;

	private DeclarationQuickFixes(final String text, final EventBTokens tokens,
			final ProjectStructure structure, final String name,
			final int tokenIndex) {
		this.text = text;
		this.tokens = tokens;
		this.structure = structure;
		this.name = name;
		this.tokenIndex = tokenIndex;
		this.event = tokens.eventOf(tokenIndex);
	}

	/**
	 * @param structure
	 *            the saved state of the project, may be <code>null</code>
	 * @return the quick fixes for the name at the offset (empty if it is
	 *         declared or no name)
	 */
	public static List<Fix> compute(final String text, final int offset,
			final ProjectStructure structure) {
		final EventBTokens tokens = new EventBTokens(text);
		final int index = tokens.indexAt(offset);
		if (index < 0) {
			return Collections.emptyList();
		}
		final Token token = tokens.get(index);
		final String name = EventBTokens.unprimed(token.text);
		if (token.kind != Kind.IDENTIFIER || tokens.isStructuralName(index)
				|| name.length() == 0 || KEYWORDS.contains(name)
				|| !Character.isJavaIdentifierStart(name.charAt(0))) {
			return Collections.emptyList();
		}

		final DeclarationQuickFixes fixes = new DeclarationQuickFixes(text,
				tokens, structure, name, index);
		return fixes.compute();
	}

	private List<Fix> compute() {
		final List<Fix> result = new ArrayList<Fix>();
		if (isInClause(tokenIndex, Constants.VARIABLES, Constants.CONSTANTS,
				Constants.SETS)
				|| (event != null && event.parameters.contains(tokenIndex))
				|| isBound()) {
			return result;
		}

		final Set<String> visible = visibleNames();
		if (visible.contains(name)) {
			return result;
		}

		final Set<String> candidates = new LinkedHashSet<String>(visible);
		for (final String keyword : Constants.formula_keywords) {
			if (Character.isLetter(keyword.charAt(0))
					&& keyword.charAt(0) < 128) {
				candidates.add(keyword);
			}
		}
		final Token token = tokens.get(tokenIndex);
		for (final String candidate : candidates) {
			if (candidate.equalsIgnoreCase(name)) {
				result.add(new Fix("Change to '" + candidate + "'",
						Images.IMG_VARIABLE, new TextChange().replace(
								token.offset, name.length(), candidate)));
			}
		}

		final boolean isMachine = tokens.getTokens().size() > 0
				&& isKeyword(0, Constants.MACHINE);
		if (isMachine) {
			result.add(declareVariable());
			if (event != null && event.label != null
					&& !"INITIALISATION".equals(event.label)) {
				result.add(declareParameter());
			}
			for (final String context : contextsDeclaringName(Constants.SEES)) {
				result.add(addReference(Constants.SEES, context));
			}
		} else {
			result.add(declareConstant());
			result.add(declareCarrierSet());
			for (final String context : contextsDeclaringName(Constants.EXTENDS)) {
				result.add(addReference(Constants.EXTENDS, context));
			}
		}
		return result;
	}

	/*
	 * Is the name declared?
	 */

	/**
	 * @return the variables, constants, sets and parameters that can be used
	 *         at the position of the name
	 */
	private Set<String> visibleNames() {
		final Set<String> declared = new LinkedHashSet<String>();

		// in this text
		for (int i = 0; i < tokens.getTokens().size(); i++) {
			if (tokens.get(i).kind == Kind.IDENTIFIER
					&& isInClause(i, Constants.VARIABLES, Constants.CONSTANTS,
							Constants.SETS)) {
				declared.add(tokens.get(i).text);
			}
		}
		if (event != null) {
			for (final int index : event.parameters) {
				declared.add(tokens.get(index).text);
			}
		}
		if (structure == null) {
			return declared;
		}

		// seen/extended contexts
		final Set<String> contexts = new LinkedHashSet<String>();
		contexts.addAll(namesInClause(Constants.SEES));
		contexts.addAll(namesInClause(Constants.EXTENDS));
		declared.addAll(structure.constantsOf(structure
				.contextClosure(contexts)));

		// abstract machines (disappearing variables, abstract parameters)
		final List<String> refines = namesInClause(Constants.REFINES);
		Machine machine = refines.isEmpty() ? null : structure
				.getMachine(refines.get(0));
		final Set<String> visited = new HashSet<String>();
		boolean first = true;
		while (machine != null && visited.add(machine.name)) {
			declared.addAll(machine.variables);
			if (first && event != null) {
				for (final int index : event.references) {
					final Event abstractEvent = machine.events.get(tokens
							.get(index).text);
					if (abstractEvent != null) {
						declared.addAll(abstractEvent.parameters);
					}
				}
			}
			first = false;
			machine = structure.getAbstractMachine(machine);
		}
		return declared;
	}

	/**
	 * Bound by a quantifier, lambda or set comprehension, e.g. x in
	 * <code>∀x·x ∈ S</code>.
	 */
	private boolean isBound() {
		final Pattern word = Pattern.compile("(?<![\\w'])"
				+ Pattern.quote(name) + "(?![\\w])");
		final Matcher binder = BINDER.matcher(text);
		while (binder.find()) {
			if (word.matcher(binder.group(1)).find()) {
				return true;
			}
		}
		return false;
	}

	private List<String> contextsDeclaringName(final String keyword) {
		final List<String> result = new ArrayList<String>();
		if (structure == null) {
			return result;
		}
		final Set<String> visible = structure.contextClosure(namesInClause(keyword));
		for (final Context context : structure.getContexts()) {
			if (context.constants.contains(name)
					&& !visible.contains(context.name)
					&& !isOwnComponent(context.name)) {
				result.add(context.name);
			}
		}
		return result;
	}

	private boolean isOwnComponent(final String component) {
		return tokens.getTokens().size() > 1
				&& tokens.get(1).text.equals(component);
	}

	/*
	 * Proposals
	 */

	private Fix declareVariable() {
		final TextChange change = new TextChange();
		addToList(change, Constants.VARIABLES, name, Constants.INVARIANTS,
				Constants.VARIANT, Constants.EVENTS, Constants.END);
		addLabeled(change, Constants.INVARIANTS, "inv", name + ELEMENT_OF,
				DEFAULT_TYPE, Constants.VARIANT, Constants.EVENTS,
				Constants.END);

		String label = "Declare variable '" + name + "' with typing invariant";
		final EventBlock init = tokens.findEvent("INITIALISATION");
		if (init != null && addInitialisation(change, init)) {
			label += " and initialisation";
		}
		return new Fix(label, Images.IMG_VARIABLE, change);
	}

	private Fix declareConstant() {
		final TextChange change = new TextChange();
		addToList(change, Constants.CONSTANTS, name, Constants.AXIOMS,
				Constants.END);
		addLabeled(change, Constants.AXIOMS, "axm", name + ELEMENT_OF,
				DEFAULT_TYPE, Constants.END);
		return new Fix("Declare constant '" + name + "' with typing axiom",
				Images.IMG_CONSTANT, change);
	}

	private Fix declareCarrierSet() {
		final TextChange change = new TextChange();
		addToList(change, Constants.SETS, name, Constants.CONSTANTS,
				Constants.AXIOMS, Constants.END);
		return new Fix("Declare carrier set '" + name + "'",
				Images.IMG_CARRIER_SET, change);
	}

	private Fix addReference(final String keyword, final String context) {
		final TextChange change = new TextChange();
		final int clause = findTopLevel(keyword);
		if (clause >= 0) {
			change.insert(tokens.get(lastTokenOfClause(clause)).end(), " "
					+ context);
		} else {
			// behind the header (and the refines clause of a machine)
			final int refines = findTopLevel(Constants.REFINES);
			final int anchor = refines >= 0 ? lastTokenOfClause(refines)
					: Math.min(1, tokens.getTokens().size() - 1);
			change.insert(lineEnd(tokens.get(anchor).end()), "\n"
					+ indentOf(tokens.get(0).offset) + keyword + " "
					+ context);
		}
		return new Fix("Add '" + keyword + " " + context + "' (declares '"
				+ name + "')", Images.IMG_CONTEXT, change);
	}

	private Fix declareParameter() {
		final TextChange change = new TextChange();
		final int any = findInEvent(Constants.ANY);
		final int where = findInEvent(Constants.WHERE);
		final int when = findInEvent(Constants.WHEN);
		final int with = findInEvent(Constants.WITH);
		final int then = findInEvent(Constants.THEN);
		final int begin = findInEvent(Constants.BEGIN);
		final int end = findInEvent(Constants.END);

		final String eventIndent = indentOf(tokens.get(event.first).offset);
		final String sectionIndent = eventIndent + "  ";
		final int guardNumber = StructureAnalyzer.nextLabelNumber(text,
				tokens.get(event.first).offset + 1, "grd");
		final String guardStart = "@grd" + guardNumber + " " + name
				+ ELEMENT_OF;

		final int guards = where >= 0 ? where : when;
		if (any >= 0) {
			change.insert(tokens.get(lastTokenOfSection(any)).end(), " " + name);
		}

		if (guards >= 0) {
			if (any < 0) {
				// "when" becomes "any p where"
				change.replace(tokens.get(guards).offset, tokens.get(guards)
						.text.length(), Constants.ANY + " " + name + "\n"
						+ indentOf(tokens.get(guards).offset)
						+ Constants.WHERE);
			}
			final int last = lastTokenOfSection(guards);
			final String indent = last > guards ? indentOf(tokens.get(last).offset)
					: indentOf(tokens.get(guards).offset) + "  ";
			change.insertWithPlaceholder(lineEnd(tokens.get(last).end()),
					"\n" + indent + guardStart, DEFAULT_TYPE, "");
		} else {
			// no guards yet
			final int next = firstExisting(with, then, begin, end);
			if (next < 0) {
				return new Fix("Declare parameter '" + name + "'",
						Images.IMG_VARIABLE, change);
			}
			final String indent = next == end ? sectionIndent
					: indentOf(tokens.get(next).offset);
			final StringBuilder before = new StringBuilder();
			if (any < 0) {
				before.append(indent).append(Constants.ANY).append(' ')
						.append(name).append('\n');
			}
			before.append(indent).append(Constants.WHERE).append('\n')
					.append(indent).append("  ").append(guardStart);
			if (next == begin) {
				// "begin" (no parameters, no guards) becomes "then"
				final String start = before.substring(indent.length());
				change.replace(tokens.get(begin).offset,
						Constants.BEGIN.length(), start + DEFAULT_TYPE + "\n"
								+ indent + Constants.THEN, start.length(),
						DEFAULT_TYPE.length());
			} else {
				change.insertWithPlaceholder(lineStart(tokens.get(next).offset),
						before.toString(), DEFAULT_TYPE, "\n");
			}
		}

		return new Fix("Declare parameter '" + name + "' of event '"
				+ event.label + "' with typing guard", Images.IMG_VARIABLE,
				change);
	}

	private boolean addInitialisation(final TextChange change,
			final EventBlock init) {
		int actions = -1;
		for (int i = init.first; i <= init.last; i++) {
			if (isKeyword(i, Constants.THEN) || isKeyword(i, Constants.BEGIN)) {
				actions = i;
			}
		}
		if (actions < 0) {
			return false;
		}

		int last = actions;
		for (int i = actions + 1; i <= init.last; i++) {
			if (isKeyword(i, Constants.END)) {
				break;
			}
			last = i;
		}

		final String indent = last > actions ? indentOf(tokens.get(last).offset)
				: indentOf(tokens.get(actions).offset) + "  ";
		final int number = StructureAnalyzer.nextLabelNumber(text,
				tokens.get(init.first).offset + 1, "act");
		change.insertWithPlaceholder(lineEnd(tokens.get(last).end()), "\n"
				+ indent + "@act" + number + " " + name + BECOMES_EQUAL,
				DEFAULT_VALUE, "");
		return true;
	}

	/**
	 * Adds a name to a list clause (<code>variables</code>, ...), creating
	 * the clause in front of the first of the following keywords if needed.
	 */
	private void addToList(final TextChange change, final String keyword,
			final String item, final String... followingKeywords) {
		final int clause = findTopLevel(keyword);
		if (clause >= 0) {
			change.insert(tokens.get(lastTokenOfClause(clause)).end(), " "
					+ item);
		} else {
			insertClause(change, keyword + " " + item, null, null,
					followingKeywords);
		}
	}

	/**
	 * Adds a labeled formula to a clause (<code>invariants</code>, ...).
	 */
	private void addLabeled(final TextChange change, final String keyword,
			final String labelPrefix, final String formulaStart,
			final String placeholder, final String... followingKeywords) {
		final int number = StructureAnalyzer.nextLabelNumber(text, 0,
				labelPrefix);
		final String formula = "@" + labelPrefix + number + " " + formulaStart;

		final int clause = findTopLevel(keyword);
		if (clause >= 0) {
			final int last = lastTokenOfClause(clause);
			final String indent = last > clause ? indentOf(tokens.get(last).offset)
					: indentOf(tokens.get(clause).offset) + "  ";
			change.insertWithPlaceholder(lineEnd(tokens.get(last).end()), "\n"
					+ indent + formula, placeholder, "");
		} else {
			insertClause(change, keyword, formula, placeholder,
					followingKeywords);
		}
	}

	private void insertClause(final TextChange change, final String header,
			final String formula, final String placeholder,
			final String... followingKeywords) {
		int next = -1;
		for (final String keyword : followingKeywords) {
			next = findTopLevel(keyword);
			if (next >= 0) {
				break;
			}
		}

		final String indent;
		final int offset;
		if (next >= 0) {
			offset = lineStart(tokens.get(next).offset);
			indent = indentOf(tokens.get(next).offset);
		} else {
			offset = text.length();
			indent = "";
		}

		final String before = (offset > 0 && text.charAt(offset - 1) != '\n' ? "\n"
				: "")
				+ indent + header;
		if (formula == null) {
			change.insert(offset, before + "\n");
		} else {
			change.insertWithPlaceholder(offset, before + "\n" + indent + "  "
					+ formula, placeholder, "\n");
		}
	}

	/*
	 * Token helpers
	 */

	private boolean isKeyword(final int index, final String keyword) {
		final Token token = tokens.get(index);
		return token.kind == Kind.IDENTIFIER && keyword.equals(token.text);
	}

	private boolean isClauseEnd(final int index) {
		final Token token = tokens.get(index);
		return token.kind == Kind.IDENTIFIER
				&& CLAUSE_ENDS.contains(token.text)
				&& tokens.eventOf(index) == null;
	}

	/** first occurrence of a keyword outside of events */
	private int findTopLevel(final String keyword) {
		for (int i = 0; i < tokens.getTokens().size(); i++) {
			if (isKeyword(i, keyword) && tokens.eventOf(i) == null) {
				return i;
			}
		}
		return -1;
	}

	private int lastTokenOfClause(final int clause) {
		int last = clause;
		for (int i = clause + 1; i < tokens.getTokens().size(); i++) {
			if (isClauseEnd(i) || tokens.eventOf(i) != null) {
				break;
			}
			last = i;
		}
		return last;
	}

	private boolean isInClause(final int index, final String... keywords) {
		if (tokens.eventOf(index) != null) {
			return false;
		}
		for (int i = index - 1; i >= 0; i--) {
			if (isClauseEnd(i)) {
				for (final String keyword : keywords) {
					if (isKeyword(i, keyword)) {
						return true;
					}
				}
				return false;
			}
		}
		return false;
	}

	private List<String> namesInClause(final String keyword) {
		final List<String> result = new ArrayList<String>();
		final int clause = findTopLevel(keyword);
		if (clause >= 0) {
			for (int i = clause + 1; i <= lastTokenOfClause(clause); i++) {
				if (tokens.get(i).kind == Kind.IDENTIFIER) {
					result.add(tokens.get(i).text);
				}
			}
		}
		return result;
	}

	private static final Set<String> EVENT_SECTIONS = new HashSet<String>(
			Arrays.asList(Constants.ANY, Constants.WHERE, Constants.WHEN,
					Constants.WITH, Constants.THEN, Constants.BEGIN,
					Constants.END));

	private int findInEvent(final String keyword) {
		for (int i = event.first + 1; i <= event.last; i++) {
			if (isKeyword(i, keyword)) {
				return i;
			}
		}
		return -1;
	}

	private int lastTokenOfSection(final int section) {
		int last = section;
		for (int i = section + 1; i <= event.last; i++) {
			final Token token = tokens.get(i);
			if (token.kind == Kind.IDENTIFIER
					&& EVENT_SECTIONS.contains(token.text)) {
				break;
			}
			last = i;
		}
		return last;
	}

	private static int firstExisting(final int... indices) {
		for (final int index : indices) {
			if (index >= 0) {
				return index;
			}
		}
		return -1;
	}

	/*
	 * Text helpers
	 */

	private int lineStart(final int offset) {
		int i = offset;
		while (i > 0 && text.charAt(i - 1) != '\n' && text.charAt(i - 1) != '\r') {
			i--;
		}
		return i;
	}

	/** offset of the line delimiter (or the end of the text) */
	private int lineEnd(final int offset) {
		int i = offset;
		while (i < text.length() && text.charAt(i) != '\n'
				&& text.charAt(i) != '\r') {
			i++;
		}
		return i;
	}

	private String indentOf(final int offset) {
		final int start = lineStart(offset);
		int i = start;
		while (i < text.length() && (text.charAt(i) == ' ' || text.charAt(i) == '\t')) {
			i++;
		}
		return text.substring(start, i);
	}
}
