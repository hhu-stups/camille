/**
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen,
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html)
 * */

package org.eventb.texteditor.ui.editor.refactoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eventb.core.ast.FormulaFactory;
import org.eventb.texteditor.ui.editor.refactoring.EventBTokens.EventBlock;
import org.eventb.texteditor.ui.editor.refactoring.EventBTokens.Kind;
import org.eventb.texteditor.ui.editor.refactoring.EventBTokens.Token;
import org.eventb.texteditor.ui.editor.refactoring.ProjectStructure.Context;
import org.eventb.texteditor.ui.editor.refactoring.ProjectStructure.Event;
import org.eventb.texteditor.ui.editor.refactoring.ProjectStructure.Machine;
import org.eventb.texttools.Constants;

/**
 * Renaming of variables, constants, carrier sets, event parameters and events.
 * <p>
 * The element is renamed where it is declared and in all components that use
 * it: refining machines (including gluing invariants and witnesses),
 * extending contexts and machines seeing them, refining events.
 * </p>
 * <p>
 * Usage: {@link #resolve} the name at the cursor, {@link #validate} the new
 * name and compute the {@link #computeEdits edits} for each
 * {@link #getAffectedFiles affected file}.
 * </p>
 */
public final class RenameRefactoring {

	public enum Target {
		VARIABLE("variable"), CONSTANT("constant or carrier set"), PARAMETER(
				"parameter"), EVENT("event");

		public final String description;

		Target(final String description) {
			this.description = description;
		}
	}

	/** Thrown if the name at the cursor cannot be renamed */
	public static final class RenameException extends Exception {
		private static final long serialVersionUID = 1L;

		public RenameException(final String message) {
			super(message);
		}
	}

	/** What has to be renamed in one file */
	private interface Rule {
		void collect(EventBTokens tokens, String name, Set<Integer> offsets);
	}

	private static final Pattern LABEL_PATTERN = Pattern
			.compile("[\\p{L}_][\\p{L}\\p{N}_]*");

	private static final Set<String> KEYWORDS = new HashSet<String>();
	static {
		KEYWORDS.addAll(Arrays.asList(Constants.structural_keywords));
		KEYWORDS.addAll(Arrays.asList(Constants.machine_keywords));
		KEYWORDS.addAll(Arrays.asList(Constants.event_keywords));
		KEYWORDS.addAll(Arrays.asList(Constants.context_keywords));
		KEYWORDS.addAll(Arrays.asList(Constants.formula_keywords));
	}

	private final ProjectStructure structure;
	private final Target target;
	private final String name;
	private final String declaringComponent;
	private final Map<IFile, List<Rule>> rules = new LinkedHashMap<IFile, List<Rule>>();
	private final Map<IFile, String> componentNames = new LinkedHashMap<IFile, String>();
	/** names that must not be used as new name, with the component */
	private final Map<String, String> namesInUse = new LinkedHashMap<String, String>();

	private RenameRefactoring(final ProjectStructure structure,
			final Target target, final String name,
			final String declaringComponent) {
		this.structure = structure;
		this.target = target;
		this.name = name;
		this.declaringComponent = declaringComponent;
	}

	public Target getTarget() {
		return target;
	}

	public String getName() {
		return name;
	}

	public String getDeclaringComponent() {
		return declaringComponent;
	}

	public Set<IFile> getAffectedFiles() {
		return rules.keySet();
	}

	public List<String> getAffectedComponents() {
		return new ArrayList<String>(componentNames.values());
	}

	/**
	 * Determines what is renamed for the name at the given offset.
	 *
	 * @param structure
	 *            the saved state of the project
	 * @param componentName
	 *            the machine/context of the text
	 * @param text
	 *            the (saved) text of the component
	 * @param offset
	 *            the cursor position
	 */
	public static RenameRefactoring resolve(final ProjectStructure structure,
			final String componentName, final String text, final int offset)
			throws RenameException {
		final EventBTokens tokens = new EventBTokens(text);
		final int index = tokens.indexAt(offset);
		if (index < 0) {
			throw new RenameException("Place the cursor on a name to rename it.");
		}

		final Token token = tokens.get(index);
		final EventBlock eventBlock = tokens.eventOf(index);
		final Machine machine = structure.getMachine(componentName);
		final Context context = structure.getContext(componentName);

		if (machine == null && context == null) {
			throw new RenameException("'" + componentName
					+ "' is not known to the Rodin database. Save it first.");
		}

		if (token.kind == Kind.LABEL) {
			if (eventBlock != null && eventBlock.witnessLabels.contains(index)) {
				throw new RenameException(
						"Witness labels are renamed together with the abstract variable or parameter.");
			}
			throw new RenameException("Labels cannot be renamed yet.");
		}

		final String word = EventBTokens.unprimed(token.text);
		if (KEYWORDS.contains(word)) {
			throw new RenameException("'" + word
					+ "' is a keyword and cannot be renamed.");
		}

		// events: the label after "event" or a name in "refines"/"extends"
		if (machine != null && eventBlock != null) {
			if (index == eventBlock.labelIndex) {
				return renameEvent(structure, machine, word);
			}
			if (eventBlock.references.contains(index)) {
				final Machine abstractMachine = structure
						.getAbstractMachine(machine);
				if (abstractMachine == null
						|| !abstractMachine.events.containsKey(word)) {
					throw new RenameException("The abstract event '" + word
							+ "' does not exist.");
				}
				return renameEvent(structure, abstractMachine, word);
			}
		}

		if (structure.isComponent(word)) {
			throw new RenameException(
					"Machines and contexts can be renamed in the Event-B Explorer (context menu 'Rename...').");
		}

		if (machine != null) {
			if (eventBlock != null && eventBlock.label != null) {
				final Event event = machine.events.get(eventBlock.label);
				if (event != null && hasParameter(structure, machine, event, word)) {
					return renameParameter(structure, machine, event, word);
				}

				// a disappearing parameter of the abstract event (witness)
				final Machine abstractMachine = structure
						.getAbstractMachine(machine);
				if (event != null && abstractMachine != null) {
					for (final String label : event.refines) {
						final Event abstractEvent = abstractMachine.events
								.get(label);
						if (abstractEvent != null
								&& hasParameter(structure, abstractMachine,
										abstractEvent, word)) {
							return renameParameter(structure, abstractMachine,
									abstractEvent, word);
						}
					}
				}
			}

			final Machine declaring = topmostWithVariable(structure, machine,
					word);
			if (declaring != null) {
				return renameVariable(structure, declaring, word);
			}

			final Context declaringContext = findDeclaringContext(structure,
					structure.visibleContexts(machine), word);
			if (declaringContext != null) {
				return renameConstant(structure, declaringContext, word);
			}
		} else {
			final Context declaringContext = findDeclaringContext(structure,
					structure.contextClosure(Arrays.asList(context.name)), word);
			if (declaringContext != null) {
				return renameConstant(structure, declaringContext, word);
			}
		}

		throw new RenameException("'" + word
				+ "' is not a variable, constant, carrier set, parameter or event of a saved component.");
	}

	/**
	 * Checks the new name.
	 *
	 * @return an error message or <code>null</code> if the name is valid
	 */
	public String validate(final String newName) {
		if (newName == null || newName.length() == 0) {
			return "Enter a name.";
		}
		if (newName.equals(name)) {
			return "Enter a different name.";
		}
		if (KEYWORDS.contains(newName)) {
			return "'" + newName + "' is a keyword.";
		}
		if (target == Target.EVENT) {
			if (!LABEL_PATTERN.matcher(newName).matches()) {
				return "'" + newName + "' is not a valid event name.";
			}
		} else if (!FormulaFactory.getDefault().isValidIdentifierName(newName)
				|| newName.indexOf('\'') >= 0) {
			return "'" + newName + "' is not a valid identifier.";
		}

		final String usedIn = namesInUse.get(newName);
		if (usedIn != null) {
			return "'" + newName + "' is already used in " + usedIn + ".";
		}
		return null;
	}

	/**
	 * Computes the positions to rename in a text.
	 *
	 * @return the start offsets of the occurrences of the old name, in
	 *         ascending order
	 */
	public List<Integer> computeEdits(final IFile file, final String text) {
		final List<Rule> fileRules = rules.get(file);
		final Set<Integer> offsets = new TreeSet<Integer>();
		if (fileRules != null) {
			final EventBTokens tokens = new EventBTokens(text);
			for (final Rule rule : fileRules) {
				rule.collect(tokens, name, offsets);
			}
		}
		return new ArrayList<Integer>(offsets);
	}

	/*
	 * Resolution
	 */

	private static boolean hasParameter(final ProjectStructure structure,
			final Machine machine, final Event event, final String parameter) {
		if (event.parameters.contains(parameter)) {
			return true;
		}
		// an extended event also has the parameters of the abstract event
		if (event.extended) {
			final Machine abstractMachine = structure.getAbstractMachine(machine);
			if (abstractMachine != null) {
				for (final String label : event.refines) {
					final Event abstractEvent = abstractMachine.events.get(label);
					if (abstractEvent != null
							&& hasParameter(structure, abstractMachine,
									abstractEvent, parameter)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private static Machine topmostWithVariable(
			final ProjectStructure structure, final Machine machine,
			final String variable) {
		Machine current = machine;
		// a variable that disappeared in this machine (gluing invariant)
		final Set<String> visited = new HashSet<String>();
		while (current != null && !current.variables.contains(variable)
				&& visited.add(current.name)) {
			current = structure.getAbstractMachine(current);
		}
		if (current == null || !current.variables.contains(variable)) {
			return null;
		}
		// the variable may have been declared further up
		Machine abstractMachine = structure.getAbstractMachine(current);
		while (abstractMachine != null
				&& abstractMachine.variables.contains(variable)
				&& visited.add(abstractMachine.name)) {
			current = abstractMachine;
			abstractMachine = structure.getAbstractMachine(current);
		}
		return current;
	}

	private static Context findDeclaringContext(
			final ProjectStructure structure, final Set<String> contexts,
			final String constant) {
		for (final String name : contexts) {
			final Context context = structure.getContext(name);
			if (context != null && context.constants.contains(constant)) {
				return context;
			}
		}
		return null;
	}

	/*
	 * Planning
	 */

	private void addRule(final IFile file, final String component,
			final Rule rule) {
		List<Rule> fileRules = rules.get(file);
		if (fileRules == null) {
			fileRules = new ArrayList<Rule>();
			rules.put(file, fileRules);
			componentNames.put(file, component);
		}
		fileRules.add(rule);
	}

	private void addNamesInUse(final Iterable<String> names,
			final String component) {
		for (final String used : names) {
			if (!used.equals(name) && !namesInUse.containsKey(used)) {
				namesInUse.put(used, component);
			}
		}
	}

	private void addMachineNamesInUse(final Machine machine) {
		addNamesInUse(machine.variables, machine.name);
		addNamesInUse(structure.constantsOf(structure.visibleContexts(machine)),
				machine.name);
		for (final Event event : machine.events.values()) {
			addNamesInUse(event.parameters, machine.name + "." + event.label);
		}
	}

	private static RenameRefactoring renameVariable(
			final ProjectStructure structure, final Machine declaring,
			final String variable) {
		final RenameRefactoring refactoring = new RenameRefactoring(structure,
				Target.VARIABLE, variable, declaring.name);
		refactoring.planVariable(declaring, new HashSet<String>());
		return refactoring;
	}

	private void planVariable(final Machine machine, final Set<String> visited) {
		if (!visited.add(machine.name)) {
			return;
		}
		addRule(machine.file, machine.name, ALL_IDENTIFIERS);
		addRule(machine.file, machine.name, WITNESSES_OF_VARIABLE);
		addMachineNamesInUse(machine);

		for (final Machine refinement : structure.getRefinements(machine)) {
			if (refinement.variables.contains(name)) {
				planVariable(refinement, visited);
			} else if (visited.add(refinement.name)) {
				// the variable disappears: gluing invariants and witnesses
				addRule(refinement.file, refinement.name, ALL_IDENTIFIERS);
				addRule(refinement.file, refinement.name, WITNESSES_OF_VARIABLE);
				addMachineNamesInUse(refinement);
			}
		}
	}

	private static RenameRefactoring renameConstant(
			final ProjectStructure structure, final Context declaring,
			final String constant) {
		final RenameRefactoring refactoring = new RenameRefactoring(structure,
				Target.CONSTANT, constant, declaring.name);

		for (final Context context : structure.getContexts()) {
			final Set<String> closure = structure.contextClosure(Arrays
					.asList(context.name));
			if (closure.contains(declaring.name)) {
				refactoring.addRule(context.file, context.name, ALL_IDENTIFIERS);
				refactoring.addNamesInUse(structure.constantsOf(closure),
						context.name);
			}
		}
		for (final Machine machine : structure.getMachines()) {
			if (structure.visibleContexts(machine).contains(declaring.name)) {
				refactoring.addRule(machine.file, machine.name, ALL_IDENTIFIERS);
				refactoring.addMachineNamesInUse(machine);
			}
		}
		return refactoring;
	}

	private static RenameRefactoring renameParameter(
			final ProjectStructure structure, final Machine machine,
			final Event event, final String parameter) {
		// start with the event that introduced the parameter
		Machine currentMachine = machine;
		Event currentEvent = event;
		final Set<String> visited = new HashSet<String>();
		while (visited.add(currentMachine.name)) {
			final Machine abstractMachine = structure
					.getAbstractMachine(currentMachine);
			Event abstractEvent = null;
			if (abstractMachine != null) {
				for (final String label : currentEvent.refines) {
					final Event candidate = abstractMachine.events.get(label);
					if (candidate != null
							&& hasParameter(structure, abstractMachine,
									candidate, parameter)) {
						abstractEvent = candidate;
						break;
					}
				}
			}
			if (abstractEvent == null) {
				break;
			}
			currentMachine = abstractMachine;
			currentEvent = abstractEvent;
		}

		final RenameRefactoring refactoring = new RenameRefactoring(structure,
				Target.PARAMETER, parameter, currentMachine.name + "."
						+ currentEvent.label);
		refactoring.planParameter(currentMachine, currentEvent,
				new HashSet<String>());
		return refactoring;
	}

	private void planParameter(final Machine machine, final Event event,
			final Set<String> visited) {
		if (!visited.add(machine.name + "." + event.label)) {
			return;
		}
		addRule(machine.file, machine.name, new IdentifiersInEvent(event.label));
		addNamesInUse(machine.variables, machine.name);
		addNamesInUse(structure.constantsOf(structure.visibleContexts(machine)),
				machine.name);
		addNamesInUse(event.parameters, machine.name + "." + event.label);

		for (final Machine refinement : structure.getRefinements(machine)) {
			for (final Event refiningEvent : refinement.events.values()) {
				if (!refiningEvent.refines.contains(event.label)) {
					continue;
				}
				if (refiningEvent.extended
						|| refiningEvent.parameters.contains(name)) {
					planParameter(refinement, refiningEvent, visited);
				} else if (visited.add(refinement.name + "."
						+ refiningEvent.label)) {
					// the parameter disappears: witness
					addRule(refinement.file, refinement.name,
							new IdentifiersInEvent(refiningEvent.label));
					addRule(refinement.file, refinement.name,
							new WitnessesInEvent(refiningEvent.label));
					addNamesInUse(refinement.variables, refinement.name);
					addNamesInUse(refiningEvent.parameters, refinement.name
							+ "." + refiningEvent.label);
				}
			}
		}
	}

	private static RenameRefactoring renameEvent(
			final ProjectStructure structure, final Machine machine,
			final String label) throws RenameException {
		if ("INITIALISATION".equals(label)) {
			throw new RenameException(
					"The INITIALISATION event cannot be renamed.");
		}
		final RenameRefactoring refactoring = new RenameRefactoring(structure,
				Target.EVENT, label, machine.name);
		refactoring.planEvent(machine, new HashSet<String>());
		return refactoring;
	}

	private void planEvent(final Machine machine, final Set<String> visited) {
		if (!visited.add(machine.name)) {
			return;
		}
		addRule(machine.file, machine.name, EVENT_LABEL);
		addNamesInUse(machine.events.keySet(), machine.name);

		for (final Machine refinement : structure.getRefinements(machine)) {
			for (final Event refiningEvent : refinement.events.values()) {
				if (!refiningEvent.refines.contains(name)) {
					continue;
				}
				addRule(refinement.file, refinement.name,
						new EventReference(refiningEvent.label));
				if (refiningEvent.label.equals(name)) {
					// "event inc refines inc": rename the refinement too
					planEvent(refinement, visited);
				}
			}
		}
	}

	/*
	 * Rules
	 */

	private static boolean matchesIdentifier(final Token token,
			final String name) {
		return token.kind == Kind.IDENTIFIER
				&& name.equals(EventBTokens.unprimed(token.text));
	}

	/** all identifiers x and x' */
	private static final Rule ALL_IDENTIFIERS = new Rule() {
		@Override
		public void collect(final EventBTokens tokens, final String name,
				final Set<Integer> offsets) {
			for (int i = 0; i < tokens.getTokens().size(); i++) {
				final Token token = tokens.get(i);
				if (!tokens.isStructuralName(i)
						&& matchesIdentifier(token, name)) {
					offsets.add(token.offset);
				}
			}
		}
	};

	/** witness labels of a disappearing variable (@x') */
	private static final Rule WITNESSES_OF_VARIABLE = new Rule() {
		@Override
		public void collect(final EventBTokens tokens, final String name,
				final Set<Integer> offsets) {
			for (final EventBlock event : tokens.getEvents()) {
				for (final int index : event.witnessLabels) {
					final Token token = tokens.get(index);
					if (token.text.equals(name + "'")) {
						offsets.add(token.offset);
					}
				}
			}
		}
	};

	/** the label of the event in "event x" */
	private static final Rule EVENT_LABEL = new Rule() {
		@Override
		public void collect(final EventBTokens tokens, final String name,
				final Set<Integer> offsets) {
			for (final EventBlock event : tokens.getEvents()) {
				if (event.labelIndex >= 0 && name.equals(event.label)) {
					offsets.add(tokens.get(event.labelIndex).offset);
				}
			}
		}
	};

	/** identifiers in one event */
	private static final class IdentifiersInEvent implements Rule {
		private final String event;

		IdentifiersInEvent(final String event) {
			this.event = event;
		}

		@Override
		public void collect(final EventBTokens tokens, final String name,
				final Set<Integer> offsets) {
			final EventBlock block = tokens.findEvent(event);
			if (block == null) {
				return;
			}
			for (int i = block.first; i <= block.last; i++) {
				if (!tokens.isStructuralName(i)
						&& matchesIdentifier(tokens.get(i), name)) {
					offsets.add(tokens.get(i).offset);
				}
			}
		}
	}

	/** witness labels of a disappearing parameter (@p) in one event */
	private static final class WitnessesInEvent implements Rule {
		private final String event;

		WitnessesInEvent(final String event) {
			this.event = event;
		}

		@Override
		public void collect(final EventBTokens tokens, final String name,
				final Set<Integer> offsets) {
			final EventBlock block = tokens.findEvent(event);
			if (block == null) {
				return;
			}
			for (final int index : block.witnessLabels) {
				if (tokens.get(index).text.equals(name)) {
					offsets.add(tokens.get(index).offset);
				}
			}
		}
	}

	/** the name of the abstract event in "refines"/"extends" of one event */
	private static final class EventReference implements Rule {
		private final String event;

		EventReference(final String event) {
			this.event = event;
		}

		@Override
		public void collect(final EventBTokens tokens, final String name,
				final Set<Integer> offsets) {
			final EventBlock block = tokens.findEvent(event);
			if (block == null) {
				return;
			}
			for (final int index : block.references) {
				if (tokens.get(index).text.equals(name)) {
					offsets.add(tokens.get(index).offset);
				}
			}
		}
	}

	/**
	 * @return the components that are changed, for messages
	 */
	public String describeAffectedComponents() {
		final Set<String> names = new LinkedHashSet<String>(
				componentNames.values());
		final StringBuilder result = new StringBuilder();
		for (final String component : names) {
			if (result.length() > 0) {
				result.append(", ");
			}
			result.append(component);
		}
		return result.toString();
	}
}
