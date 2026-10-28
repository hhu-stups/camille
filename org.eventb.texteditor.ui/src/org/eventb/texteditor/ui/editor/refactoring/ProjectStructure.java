/**
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen,
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html)
 * */

package org.eventb.texteditor.ui.editor.refactoring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eventb.core.ICarrierSet;
import org.eventb.core.IConstant;
import org.eventb.core.IContextRoot;
import org.eventb.core.IEvent;
import org.eventb.core.IExtendsContext;
import org.eventb.core.IMachineRoot;
import org.eventb.core.IParameter;
import org.eventb.core.IRefinesEvent;
import org.eventb.core.IRefinesMachine;
import org.eventb.core.ISeesContext;
import org.eventb.core.IVariable;
import org.rodinp.core.IRodinProject;
import org.rodinp.core.RodinCore;
import org.rodinp.core.RodinDBException;

/**
 * The declarations and relations (refines, sees, extends) of all machines
 * and contexts of a project, read from the Rodin database.
 */
public final class ProjectStructure {

	public static final class Event {
		public final String label;
		public final boolean extended;
		public final List<String> refines = new ArrayList<String>();
		public final Set<String> parameters = new LinkedHashSet<String>();

		Event(final String label, final boolean extended) {
			this.label = label;
			this.extended = extended;
		}
	}

	public static final class Machine {
		public final String name;
		public final IFile file;
		public final List<String> refines = new ArrayList<String>();
		public final List<String> sees = new ArrayList<String>();
		public final Set<String> variables = new LinkedHashSet<String>();
		public final Map<String, Event> events = new LinkedHashMap<String, Event>();

		Machine(final String name, final IFile file) {
			this.name = name;
			this.file = file;
		}
	}

	public static final class Context {
		public final String name;
		public final IFile file;
		public final List<String> extendsContexts = new ArrayList<String>();
		/** carrier sets and constants */
		public final Set<String> constants = new LinkedHashSet<String>();

		Context(final String name, final IFile file) {
			this.name = name;
			this.file = file;
		}
	}

	private final Map<String, Machine> machines = new LinkedHashMap<String, Machine>();
	private final Map<String, Context> contexts = new LinkedHashMap<String, Context>();

	private ProjectStructure() {
		// use read()
	}

	public static ProjectStructure read(final IProject project)
			throws RodinDBException {
		final ProjectStructure result = new ProjectStructure();
		final IRodinProject rodinProject = RodinCore.valueOf(project);
		if (rodinProject == null || !rodinProject.exists()) {
			return result;
		}

		for (final IMachineRoot root : rodinProject
				.getRootElementsOfType(IMachineRoot.ELEMENT_TYPE)) {
			final Machine machine = new Machine(root.getComponentName(), root
					.getRodinFile().getResource());
			for (final IRefinesMachine clause : root.getRefinesClauses()) {
				if (clause.hasAbstractMachineName()) {
					machine.refines.add(clause.getAbstractMachineName());
				}
			}
			for (final ISeesContext clause : root.getSeesClauses()) {
				if (clause.hasSeenContextName()) {
					machine.sees.add(clause.getSeenContextName());
				}
			}
			for (final IVariable variable : root.getVariables()) {
				if (variable.hasIdentifierString()) {
					machine.variables.add(variable.getIdentifierString());
				}
			}
			for (final IEvent rodinEvent : root.getEvents()) {
				if (!rodinEvent.hasLabel()) {
					continue;
				}
				final Event event = new Event(rodinEvent.getLabel(),
						rodinEvent.hasExtended() && rodinEvent.isExtended());
				for (final IRefinesEvent clause : rodinEvent
						.getRefinesClauses()) {
					if (clause.hasAbstractEventLabel()) {
						event.refines.add(clause.getAbstractEventLabel());
					}
				}
				for (final IParameter parameter : rodinEvent.getParameters()) {
					if (parameter.hasIdentifierString()) {
						event.parameters.add(parameter.getIdentifierString());
					}
				}
				machine.events.put(event.label, event);
			}
			result.machines.put(machine.name, machine);
		}

		for (final IContextRoot root : rodinProject
				.getRootElementsOfType(IContextRoot.ELEMENT_TYPE)) {
			final Context context = new Context(root.getComponentName(), root
					.getRodinFile().getResource());
			for (final IExtendsContext clause : root.getExtendsClauses()) {
				if (clause.hasAbstractContextName()) {
					context.extendsContexts.add(clause.getAbstractContextName());
				}
			}
			for (final ICarrierSet set : root.getCarrierSets()) {
				if (set.hasIdentifierString()) {
					context.constants.add(set.getIdentifierString());
				}
			}
			for (final IConstant constant : root.getConstants()) {
				if (constant.hasIdentifierString()) {
					context.constants.add(constant.getIdentifierString());
				}
			}
			result.contexts.put(context.name, context);
		}

		return result;
	}

	public Machine getMachine(final String name) {
		return machines.get(name);
	}

	public Context getContext(final String name) {
		return contexts.get(name);
	}

	public boolean isComponent(final String name) {
		return machines.containsKey(name) || contexts.containsKey(name);
	}

	/**
	 * @return the machine refined by the given one, or <code>null</code>
	 */
	public Machine getAbstractMachine(final Machine machine) {
		return machine.refines.isEmpty() ? null : machines.get(machine.refines
				.get(0));
	}

	/**
	 * @return the machines refining the given one directly
	 */
	public List<Machine> getRefinements(final Machine machine) {
		final List<Machine> result = new ArrayList<Machine>();
		for (final Machine other : machines.values()) {
			if (other.refines.contains(machine.name)) {
				result.add(other);
			}
		}
		return result;
	}

	/**
	 * @return the given contexts and all contexts they extend (directly or
	 *         indirectly)
	 */
	public Set<String> contextClosure(final Collection<String> start) {
		final Set<String> result = new LinkedHashSet<String>();
		final List<String> todo = new ArrayList<String>(start);
		while (!todo.isEmpty()) {
			final String name = todo.remove(0);
			if (result.add(name)) {
				final Context context = contexts.get(name);
				if (context != null) {
					todo.addAll(context.extendsContexts);
				}
			}
		}
		return result;
	}

	/**
	 * @return all carrier sets and constants of the given contexts
	 */
	public Set<String> constantsOf(final Collection<String> contextNames) {
		final Set<String> result = new LinkedHashSet<String>();
		for (final String name : contextNames) {
			final Context context = contexts.get(name);
			if (context != null) {
				result.addAll(context.constants);
			}
		}
		return result;
	}

	/**
	 * @return the contexts that are visible in a machine (seen contexts and
	 *         the contexts they extend)
	 */
	public Set<String> visibleContexts(final Machine machine) {
		return contextClosure(machine.sees);
	}

	public Collection<Machine> getMachines() {
		return machines.values();
	}

	public Collection<Context> getContexts() {
		return contexts.values();
	}
}
