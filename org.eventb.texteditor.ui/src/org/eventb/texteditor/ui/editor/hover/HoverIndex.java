/**
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen,
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html)
 * */

package org.eventb.texteditor.ui.editor.hover;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eventb.core.ICarrierSet;
import org.eventb.core.ICommentedElement;
import org.eventb.core.IConstant;
import org.eventb.core.IContextRoot;
import org.eventb.core.IEvent;
import org.eventb.core.IEventBRoot;
import org.eventb.core.IExtendsContext;
import org.eventb.core.IMachineRoot;
import org.eventb.core.IParameter;
import org.eventb.core.IRefinesEvent;
import org.eventb.core.IRefinesMachine;
import org.eventb.core.ISCContextRoot;
import org.eventb.core.ISCEvent;
import org.eventb.core.ISCMachineRoot;
import org.eventb.core.ISeesContext;
import org.eventb.core.IVariable;
import org.eventb.core.ast.ITypeEnvironment;
import org.eventb.core.ast.Type;
import org.rodinp.core.IInternalElement;
import org.rodinp.core.IRodinFile;
import org.rodinp.core.RodinCore;

/**
 * Immutable lookup table with the hover texts of one machine or context.
 * <p>
 * It is computed from the Rodin database (the saved state of the component
 * and of the components it refines, sees or extends) and the static checker
 * results (types). It does not depend on the editor's parse result, so it is
 * also available while the text contains syntax errors. Because it is
 * immutable, the hover (which runs in a background thread) can read it
 * without any locking.
 * </p>
 */
public final class HoverIndex {

	public static final HoverIndex EMPTY = new HoverIndex(
			Collections.<String, String> emptyMap(),
			Collections.<String, Map<String, String>> emptyMap(), null);

	/** identifier or name -> hover text */
	private final Map<String, String> entries;

	/** event label -> (parameter -> hover text) */
	private final Map<String, Map<String, String>> parameters;

	/** First problem while reading the database, or <code>null</code>. */
	private final Throwable problem;

	private HoverIndex(final Map<String, String> entries,
			final Map<String, Map<String, String>> parameters,
			final Throwable problem) {
		this.entries = entries;
		this.parameters = parameters;
		this.problem = problem;
	}

	/**
	 * @return the first problem that occurred while reading the Rodin
	 *         database (the index may be incomplete then), or
	 *         <code>null</code>
	 */
	public Throwable getProblem() {
		return problem;
	}

	static HoverIndex failed(final Throwable problem) {
		return new HoverIndex(Collections.<String, String> emptyMap(),
				Collections.<String, Map<String, String>> emptyMap(), problem);
	}

	/**
	 * @param word
	 *            the identifier
	 * @param event
	 *            label of the event the word is in, or <code>null</code>
	 * @return the hover text or <code>null</code>
	 */
	public String lookup(final String word, final String event) {
		if (event != null) {
			final Map<String, String> eventParameters = parameters.get(event);
			if (eventParameters != null && eventParameters.containsKey(word)) {
				return eventParameters.get(word);
			}
		}
		return entries.get(word);
	}

	/**
	 * Reads the information from the Rodin database. Must not be called in
	 * the UI thread.
	 */
	public static HoverIndex build(final IFile file) throws CoreException {
		final IRodinFile rodinFile = RodinCore.valueOf(file);
		if (rodinFile == null || !rodinFile.exists()) {
			return EMPTY;
		}

		final IInternalElement root = rodinFile.getRoot();
		final Builder builder = new Builder();
		if (root instanceof IMachineRoot) {
			builder.addMachine((IMachineRoot) root);
		} else if (root instanceof IContextRoot) {
			builder.addContext((IContextRoot) root);
		}
		return new HoverIndex(builder.entries, builder.parameters,
				builder.problem);
	}

	private static final class Builder {
		/*
		 * Entries are added with decreasing priority: the first text for a
		 * name wins (e.g. a variable before an event with the same name).
		 */
		final Map<String, String> entries = new HashMap<String, String>();
		final Map<String, Map<String, String>> parameters = new HashMap<String, Map<String, String>>();

		private ITypeEnvironment types;

		/** first problem, the remaining sections are read anyway */
		Throwable problem;

		private interface Section {
			void read() throws Exception;
		}

		/**
		 * Reads one part of the information. A problem in one part (e.g. a
		 * missing or inconsistent static checker file) must not prevent the
		 * other parts from being shown.
		 */
		private void section(final Section section) {
			try {
				section.read();
			} catch (final Exception e) {
				if (problem == null) {
					problem = e;
				}
			} catch (final LinkageError e) {
				if (problem == null) {
					problem = e;
				}
			}
		}

		/** Types are optional, they need a successful static check. */
		private void readTypes(final IEventBRoot root) {
			section(new Section() {
				@Override
				public void read() throws Exception {
					if (root instanceof IMachineRoot) {
						final ISCMachineRoot sc = root.getSCMachineRoot();
						if (sc.exists()) {
							types = sc.getTypeEnvironment();
						}
					} else {
						final ISCContextRoot sc = root.getSCContextRoot();
						if (sc.exists()) {
							types = sc.getTypeEnvironment();
						}
					}
				}
			});
		}

		void addMachine(final IMachineRoot machine) {
			readTypes(machine);

			final List<IMachineRoot> abstractMachines = new ArrayList<IMachineRoot>();
			section(new Section() {
				@Override
				public void read() throws Exception {
					abstractMachines.addAll(abstractMachines(machine));
				}
			});

			section(new Section() {
				@Override
				public void read() throws Exception {
					addVariables(machine, abstractMachines);
				}
			});

			final List<IContextRoot> seen = new ArrayList<IContextRoot>();
			section(new Section() {
				@Override
				public void read() throws Exception {
					for (final ISeesContext sees : machine.getSeesClauses()) {
						final IContextRoot context = sees.getSeenContextRoot();
						if (context != null && context.exists()) {
							seen.add(context);
						}
					}
					addContextContents(closure(seen));
				}
			});

			section(new Section() {
				@Override
				public void read() throws Exception {
					final ISCMachineRoot scMachine = machine.getSCMachineRoot();
					for (final IEvent event : machine.getEvents()) {
						addEvent(machine, scMachine, event, abstractMachines);
					}
				}
			});

			section(new Section() {
				@Override
				public void read() throws Exception {
					if (abstractMachines.isEmpty()) {
						return;
					}
					final IMachineRoot abstractMachine = abstractMachines.get(0);
					for (final IEvent event : abstractMachine.getEvents()) {
						if (event.hasLabel()) {
							add(event.getLabel(), withComment("event "
									+ event.getLabel()
									+ "\nof abstract machine "
									+ abstractMachine.getComponentName(), event));
						}
					}
				}
			});

			section(new Section() {
				@Override
				public void read() throws Exception {
					for (final IRefinesMachine refines : machine
							.getRefinesClauses()) {
						if (refines.hasAbstractMachineName()) {
							final IMachineRoot root = refines
									.getAbstractMachineRoot();
							add(refines.getAbstractMachineName(), withComment(
									"machine "
											+ refines.getAbstractMachineName()
											+ "\nrefined by this machine",
									root));
						}
					}
					for (final IContextRoot context : seen) {
						add(context.getComponentName(), withComment("context "
								+ context.getComponentName()
								+ "\nseen by this machine", context));
					}
				}
			});
		}

		private void addVariables(final IMachineRoot machine,
				final List<IMachineRoot> abstractMachines) throws CoreException {
			final String machineName = machine.getComponentName();
			final Set<String> ownVariables = new LinkedHashSet<String>();
			for (final IVariable variable : machine.getVariables()) {
				if (!variable.hasIdentifierString()) {
					continue;
				}
				final String name = variable.getIdentifierString();
				ownVariables.add(name);
				final StringBuilder text = new StringBuilder("variable ")
						.append(name).append("\ndeclared in machine ")
						.append(machineName);
				ICommentedElement commented = variable;
				final IVariable abstractVariable = findVariable(
						abstractMachines, name);
				if (abstractVariable != null) {
					text.append(" (also in abstract machine ")
							.append(componentName(abstractVariable))
							.append(')');
					if (!hasComment(variable)) {
						commented = abstractVariable;
					}
				}
				appendType(text, name);
				appendComment(text, commented);
				add(name, text.toString());
			}
			for (final IMachineRoot abstractMachine : abstractMachines) {
				for (final IVariable variable : abstractMachine.getVariables()) {
					if (!variable.hasIdentifierString()) {
						continue;
					}
					final String name = variable.getIdentifierString();
					if (ownVariables.contains(name)) {
						continue;
					}
					final StringBuilder text = new StringBuilder("variable ")
							.append(name)
							.append("\ndeclared in abstract machine ")
							.append(abstractMachine.getComponentName())
							.append(" (not redeclared in ")
							.append(machineName).append(')');
					appendType(text, name);
					appendComment(text, variable);
					add(name, text.toString());
				}
			}
		}

		void addContext(final IContextRoot context) {
			readTypes(context);

			section(new Section() {
				@Override
				public void read() throws Exception {
					final List<IContextRoot> contexts = new ArrayList<IContextRoot>();
					contexts.add(context);
					final List<IContextRoot> all = closure(contexts);
					addContextContents(all);

					for (final IContextRoot extended : all) {
						if (extended != context) {
							add(extended.getComponentName(), withComment(
									"context " + extended.getComponentName()
											+ "\nextended by this context",
									extended));
						}
					}
				}
			});
		}

		private void addEvent(final IMachineRoot machine,
				final ISCMachineRoot scMachine, final IEvent event,
				final List<IMachineRoot> abstractMachines)
				throws CoreException {
			if (!event.hasLabel()) {
				return;
			}
			final String label = event.getLabel();

			// parameter types from the static checker
			ITypeEnvironment eventTypes = null;
			try {
				if (types != null && scMachine.exists()) {
					for (final ISCEvent scEvent : scMachine.getSCEvents()) {
						if (label.equals(scEvent.getLabel())) {
							eventTypes = scEvent.getTypeEnvironment(types);
							break;
						}
					}
				}
			} catch (final CoreException e) {
				// types are optional
				if (problem == null) {
					problem = e;
				}
			}

			final Map<String, String> eventParameters = new HashMap<String, String>();
			for (final IParameter parameter : event.getParameters()) {
				if (!parameter.hasIdentifierString()) {
					continue;
				}
				final String name = parameter.getIdentifierString();
				final StringBuilder text = new StringBuilder("parameter ")
						.append(name).append("\nof event ").append(label);
				if (eventTypes != null) {
					final Type type = eventTypes.getType(name);
					if (type != null) {
						text.append("\ntype: ").append(type);
					}
				}
				appendComment(text, parameter);
				eventParameters.put(name, text.toString());
			}
			parameters.put(label, eventParameters);

			// the event itself
			final StringBuilder text = new StringBuilder("event ")
					.append(label).append("\nof machine ")
					.append(machine.getComponentName());
			ICommentedElement commented = event;
			for (final IRefinesEvent refines : event.getRefinesClauses()) {
				if (refines.hasAbstractEventLabel()) {
					final String abstractLabel = refines
							.getAbstractEventLabel();
					text.append("\nrefines ").append(abstractLabel);
					if (!abstractMachines.isEmpty()) {
						text.append(" of machine ").append(
								abstractMachines.get(0).getComponentName());
						final IEvent abstractEvent = findEvent(
								abstractMachines.get(0), abstractLabel);
						if (!hasComment(event) && abstractEvent != null) {
							commented = abstractEvent;
						}
					}
				}
			}
			appendComment(text, commented);
			add(label, text.toString());
		}

		private void addContextContents(final List<IContextRoot> contexts)
				throws CoreException {
			for (final IContextRoot context : contexts) {
				for (final IConstant constant : context.getConstants()) {
					if (constant.hasIdentifierString()) {
						addDeclaration("constant",
								constant.getIdentifierString(), context,
								constant);
					}
				}
				for (final ICarrierSet set : context.getCarrierSets()) {
					if (set.hasIdentifierString()) {
						addDeclaration("carrier set",
								set.getIdentifierString(), context, set);
					}
				}
			}
		}

		private void addDeclaration(final String kind, final String name,
				final IContextRoot context, final ICommentedElement element)
				throws CoreException {
			final StringBuilder text = new StringBuilder(kind).append(' ')
					.append(name).append("\ndeclared in context ")
					.append(context.getComponentName());
			appendType(text, name);
			appendComment(text, element);
			add(name, text.toString());
		}

		private void add(final String name, final String text) {
			if (name != null && !entries.containsKey(name)) {
				entries.put(name, text);
			}
		}

		private void appendType(final StringBuilder text, final String name) {
			if (types != null) {
				final Type type = types.getType(name);
				if (type != null) {
					text.append("\ntype: ").append(type);
				}
			}
		}

		/** All machines refined directly or indirectly, nearest first. */
		private static List<IMachineRoot> abstractMachines(
				final IMachineRoot machine) throws CoreException {
			final List<IMachineRoot> result = new ArrayList<IMachineRoot>();
			final Set<String> visited = new LinkedHashSet<String>();
			visited.add(machine.getComponentName());
			IMachineRoot current = machine;
			while (current != null) {
				IMachineRoot next = null;
				for (final IRefinesMachine refines : current
						.getRefinesClauses()) {
					final IMachineRoot root = refines.getAbstractMachineRoot();
					if (root != null && root.exists()
							&& visited.add(root.getComponentName())) {
						result.add(root);
						next = root;
					}
				}
				current = next;
			}
			return result;
		}

		/** The given contexts and all contexts they extend. */
		private static List<IContextRoot> closure(
				final List<IContextRoot> start) throws CoreException {
			final List<IContextRoot> result = new ArrayList<IContextRoot>();
			final Set<String> visited = new LinkedHashSet<String>();
			final List<IContextRoot> todo = new ArrayList<IContextRoot>(start);
			while (!todo.isEmpty()) {
				final IContextRoot context = todo.remove(0);
				if (!visited.add(context.getComponentName())) {
					continue;
				}
				result.add(context);
				for (final IExtendsContext extendsClause : context
						.getExtendsClauses()) {
					final IContextRoot root = extendsClause
							.getAbstractContextRoot();
					if (root != null && root.exists()) {
						todo.add(root);
					}
				}
			}
			return result;
		}

		private static IVariable findVariable(
				final List<IMachineRoot> machines, final String name)
				throws CoreException {
			for (final IMachineRoot machine : machines) {
				for (final IVariable variable : machine.getVariables()) {
					if (variable.hasIdentifierString()
							&& name.equals(variable.getIdentifierString())) {
						return variable;
					}
				}
			}
			return null;
		}

		private static IEvent findEvent(final IMachineRoot machine,
				final String label) throws CoreException {
			for (final IEvent event : machine.getEvents()) {
				if (event.hasLabel() && label.equals(event.getLabel())) {
					return event;
				}
			}
			return null;
		}

		private static String componentName(final IInternalElement element) {
			return ((IEventBRoot) element.getRoot())
					.getComponentName();
		}

		private static boolean hasComment(final ICommentedElement element)
				throws CoreException {
			return element != null && element.hasComment()
					&& element.getComment().trim().length() > 0;
		}

		private static String withComment(final String text,
				final ICommentedElement element) throws CoreException {
			final StringBuilder result = new StringBuilder(text);
			appendComment(result, element);
			return result.toString();
		}

		private static void appendComment(final StringBuilder text,
				final ICommentedElement element) throws CoreException {
			if (element != null && element.exists() && hasComment(element)) {
				text.append("\n\n").append(element.getComment().trim());
			}
		}
	}
}
