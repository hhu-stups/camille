/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texttools.prettyprint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.Assert;
import org.eclipse.emf.common.util.EList;
import org.eventb.emf.core.EventBCommented;
import org.eventb.emf.core.EventBDerived;
import org.eventb.emf.core.EventBNamed;
import org.eventb.emf.core.EventBNamedCommentedPredicateElement;
import org.eventb.emf.core.EventBObject;
import org.eventb.emf.core.context.ContextPackage;
import org.eventb.emf.core.machine.MachinePackage;
import org.eventb.texttools.Constants;

public class PrettyPrinter implements PrettyPrintConstants {
	protected final String linebreak;

	private final Map<String, Object> preferences;
	private final MachinePrintSwitch machineSwitch;
	private final ContextPrintSwitch contextSwitch;

	private final StringBuilder buffer;
	private int indentationLevel = 0;

	private int indentWidth;
	private int tabWidth;
	private Boolean useTabsForIndentation;

	/**
	 * Creates a new pretty printer with the given configuration.
	 *
	 * @param buffer
	 *            {@link StringBuilder} to which this pretty printer append its
	 *            results.
	 * @param linebreak
	 *            The linebreak character to be used.
	 * @param preferences
	 *            A {@link Map<String, Object>} of preferences which shall be
	 *            used while prettyprinting. Possible keys are available as
	 *            constants in the {@link PrettyPrintConstants}.
	 */
	public PrettyPrinter(final StringBuilder buffer, final String linebreak,
			final Map<String, Object> preferences) {
		this.buffer = buffer;
		this.linebreak = linebreak;

		if (preferences != null) {
			this.preferences = preferences;
		} else {
			this.preferences = new HashMap<String, Object>();
		}

		machineSwitch = new MachinePrintSwitch(this);
		contextSwitch = new ContextPrintSwitch(this);

		initPreferences();
	}

	private void initPreferences() {
		indentWidth = (Integer) getPreference(PROP_INDENT_DEPTH, 2);
		useTabsForIndentation = (Boolean) getPreference(
				PROP_USE_TABS_FOR_INDENTATION, false);
		tabWidth = (Integer) getPreference(PROP_TAB_WIDTH, 4);
	}

	/**
	 * Creates a pretty print for the given {@link EventBObject} and appends it
	 * to the {@link StringBuilder} which was handed to this
	 * {@link PrettyPrinter} instance via the constructor.
	 *
	 * @param object
	 */
	public void prettyPrint(final EventBObject emfObject) {
		final String packageNsURI = emfObject.eClass().getEPackage().getNsURI();

		if (packageNsURI.equals(MachinePackage.eNS_URI)) {
			machineSwitch.doSwitch(emfObject);
		} else if (packageNsURI.equals(ContextPackage.eNS_URI)) {
			contextSwitch.doSwitch(emfObject);
		}
	}

	protected Object getPreference(final String key, final Object defaultValue) {
		if (preferences.containsKey(key)) {
			return preferences.get(key);
		} else {
			return defaultValue;
		}
	}

	protected void changeIndent(final int byValue) {
		Assert.isTrue(indentationLevel + byValue >= 0);
		indentationLevel += byValue;
	}

	protected void increaseIndentLevel() {
		changeIndent(indentWidth);
	}

	protected void decreaseIndentLevel() {
		changeIndent(-indentWidth);
	}

	protected void append(final char c) {
		buffer.append(c);
	}

	protected void append(final String string) {
		buffer.append(string);
	}

	protected void appendWithSpace(final String string) {
		buffer.append(string);
		buffer.append(SPACE);
	}

	protected void appendWithSpace(final char c) {
		buffer.append(c);
		buffer.append(SPACE);
	}

	protected void appendWithLineBreak(final String string) {
		buffer.append(string);
		appendLineBreak();
	}

	protected void appendLineBreak() {
		buffer.append(linebreak);
	}

	/**
	 * Appends the comment of the given {@link EventBCommented} if it contains a
	 * comment.
	 *
	 * @param commentedElement
	 * @return <code>true</code> if it contained a comment and this comment
	 *         consisted of multiple lines, <code>false</code> otherwise
	 */
	protected void appendComment(final EventBCommented commentedElement) {
		final String comment = commentedElement.getComment();

		if (!hasComment(commentedElement)) {
			return;
		}

		// insert blank if previous char is not a whitespace
		if (buffer.length() > 0
				&& !Character.isWhitespace(buffer.charAt(buffer.length() - 1))) {
			append(SPACE);
		}

		final StringTokenizer tokenizer = new StringTokenizer(comment, "\n\r");

		if (tokenizer.countTokens() <= 1) {
			append(COMMENT_SINGLELINE_BEGIN);
			append(comment.trim());
			appendLineBreak();
		}
		// multi line comment
		else {
			appendLineBreak();
			adjustIndent();
			append(COMMENT_MULTILINE_BEGIN);

			final int subIdentation = getCurrentIndentation()
					- indentationLevel - 1;

			append(tokenizer.nextToken().trim());
			appendLineBreak();

			changeIndent(subIdentation);

			while (tokenizer.hasMoreTokens()) {
				adjustIndent();
				append(tokenizer.nextToken().trim());

				if (tokenizer.hasMoreTokens()) {
					appendLineBreak();
				}
			}

			append(COMMENT_MULTILINE_END);
			appendLineBreak();

			changeIndent(-subIdentation);
		}
	}

	protected boolean hasComment(final EventBCommented commentedElement) {
		final String comment = commentedElement.getComment();
		return comment != null && comment.length() > 0;
	}

	protected void ensureNewLine() {
		final int lastLinebreak = buffer.lastIndexOf(linebreak);

		/*
		 * Check if: 1) last line has characters and 2) these characters are not
		 * all whitespaces
		 */
		if (lastLinebreak < buffer.length() - 1
				&& buffer.substring(lastLinebreak, buffer.length() - 1).trim()
						.length() > 0) {
			// begin new line
			appendLineBreak();
		}
	}

	protected void adjustIndent() {
		int indentRemaining = indentationLevel - getCurrentIndentation() + 1;

		if (useTabsForIndentation) {
			while (indentRemaining >= tabWidth) {
				append(TAB);
				indentRemaining -= tabWidth;
			}
		}

		// then use spaces for rest
		while (indentRemaining > 0) {
			append(SPACE);
			indentRemaining--;
		}
	}

	protected void appendLabeledPredicate(final EventBNamedCommentedPredicateElement object,
			final boolean derivedPossible) {
		adjustIndent();

		// deal with derived predicates (theorems)
		if (object instanceof EventBDerived
				&& ((EventBDerived) object).isTheorem()) {
			appendWithSpace(Constants.THEOREM);
		}

		appendLabel(object);
		appendFormula(object.getPredicate(), hasComment(object));
		appendComment(object);
	}

	/**
	 * Returns the indentation level of the current position, i.e., how many
	 * characters the buffer contains behind the last line break.
	 *
	 * @return
	 */
	private int getCurrentIndentation() {
		final int lastLinebreak = buffer.lastIndexOf(linebreak);
		return buffer.length() - lastLinebreak;
	}

	protected void appendFormula(final String formula,
			final boolean followedByComment) {
		final StringTokenizer tokenizer = new StringTokenizer(formula, "\n\r");

		if (tokenizer.countTokens() <= 1) {
			if (!followedByComment) {
				appendWithLineBreak(formula.trim());
			} else {
				append(formula.trim());
			}
		} else {
			final int subIndent = getCurrentIndentation() - indentationLevel
					- 1;

			appendWithLineBreak(tokenizer.nextToken().trim());
			changeIndent(subIndent);

			while (tokenizer.hasMoreTokens()) {
				adjustIndent();
				final String line = tokenizer.nextToken().trim();

				if (tokenizer.hasMoreTokens() || !followedByComment) {
					appendWithLineBreak(line);
				} else {
					append(line);
				}
			}

			changeIndent(-subIndent);
		}

	}

	protected void appendLabel(final EventBNamed namedElement) {
		final String name = namedElement.getName();
		append(AT);
		appendWithSpace(name);
	}

	protected void appendStringList(final List<String> strings) {
		for (int i = 0; i < strings.size(); i++) {
			appendWithSpace(strings.get(i));
		}
	}

	protected void appendNameList(final EList<? extends EventBNamed> list,
			final String label, final boolean newLineBefore) {
		if (list.size() > 0) {
			if (newLineBefore) {
				appendLineBreak();
			}

			adjustIndent();
			appendWithSpace(label);
			appendNamedElementList(list);
		}
	}

	protected void appendNamedElementList(
			final EList<? extends EventBNamed> elements) {
		final int subIndent = getCurrentIndentation() - indentationLevel - 1;
		changeIndent(subIndent);

		boolean lastBeganNewLine = true;

		for (int i = 0; i < elements.size(); i++) {
			final EventBNamed element = elements.get(i);
			EventBCommented commented = null;

			if (element instanceof EventBCommented) {
				commented = (EventBCommented) element;
				commented = hasComment(commented) ? commented : null;

				if (commented != null && !lastBeganNewLine) {
					appendLineBreak();
				}
			}

			adjustIndent();
			append(element.getName());

			// begin a new line if we had a comment for this element
			if (commented != null) {
				appendComment(commented);
				lastBeganNewLine = true;

				if (i < elements.size() - 1) {
					adjustIndent();
				}
			} else {
				append(SPACE);
				lastBeganNewLine = false;
			}
		}

		appendLineBreak();
		changeIndent(-subIndent);
	}
}
