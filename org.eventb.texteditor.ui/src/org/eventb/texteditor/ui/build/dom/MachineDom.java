/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.build.dom;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.ecore.resource.Resource;
import org.eventb.emf.core.EventBObject;
import org.eventb.emf.core.context.CarrierSet;
import org.eventb.emf.core.context.Constant;
import org.eventb.emf.core.machine.Machine;
import org.eventb.emf.core.machine.Variable;

public class MachineDom extends AbstractComponentDom {
	private Machine machine;
	private final Map<String, Variable> variables = new HashMap<String, Variable>();

	private final List<MachineDom> refinedMachines = new LinkedList<MachineDom>();
	private final List<ContextDom> seenContexts = new LinkedList<ContextDom>();

	MachineDom(final Resource resource, final Machine machine) {
		super(Type.Machine, null, resource);
		this.machine = machine;
	}

	@Override
	protected synchronized void doReset() {
		refinedMachines.clear();
		seenContexts.clear();
		variables.clear();
	}

	@Override
	protected synchronized Set<String> doGetIdentifiers() {
		final Set<String> result = new HashSet<String>();

		result.addAll(getVariables(true).keySet());
		result.addAll(getConstants().keySet());
		result.addAll(getSets().keySet());

		return result;
	}

	@Override
	protected synchronized IdentifierType doGetIdentifierType(
			final String identifier) {
		if (variables.containsKey(identifier)) {
			return IdentifierType.GlobalVariable;
		}

		if (getConstants().containsKey(identifier)) {
			return IdentifierType.Constant;
		}

		if (getSets().containsKey(identifier)) {
			return IdentifierType.Set;
		}

		return null;
	}

	public Set<IComponentDom> getReferencedDoms(final boolean transitive) {
		final List<ContextDom> contexts = getSeenContexts();
		final List<MachineDom> machines = getRefinedMachines();
		final HashSet<IComponentDom> result = new HashSet<IComponentDom>(
				contexts);
		result.addAll(machines);

		if (transitive) {
			for (final IComponentDom referencedDom : contexts) {
				result.addAll(referencedDom.getReferencedDoms(transitive));
			}

			for (final IComponentDom referencedDom : machines) {
				result.addAll(referencedDom.getReferencedDoms(false));
			}
		}

		return result;
	}

	public synchronized Map<String, Variable> getVariables(
			final boolean includeInherited) {
		checkInitialization();

		final Map<String, Variable> result = new HashMap<String, Variable>();

		if (includeInherited) {
			// add referenced machine's variables
			for (final MachineDom dom : getRefinedMachines()) {
				/*
				 * Only add directly referenced machine's variables, but not
				 * transitively. Variables need to be redeclared then.
				 */
				result.putAll(dom.getVariables(false));
			}
		}

		// finally the local variables
		result.putAll(variables);

		return result;
	}

	public synchronized Map<String, Constant> getConstants() {
		checkInitialization();

		final Map<String, Constant> result = new HashMap<String, Constant>();

		// add referenced context's constants
		for (final ContextDom dom : getSeenContexts()) {
			result.putAll(dom.getConstants(true));
		}

		return result;
	}

	public synchronized Map<String, CarrierSet> getSets() {
		checkInitialization();

		final Map<String, CarrierSet> result = new HashMap<String, CarrierSet>();

		// add referenced context's constants
		for (final ContextDom dom : getSeenContexts()) {
			result.putAll(dom.getSets(true));
		}

		return result;
	}

	public synchronized List<MachineDom> getRefinedMachines() {
		checkInitialization();
		return refinedMachines;
	}

	public synchronized List<ContextDom> getSeenContexts() {
		checkInitialization();
		return seenContexts;
	}

	synchronized void addVariable(final Variable var) {
		variables.put(var.getName(), var);
	}

	synchronized void addRefinedMachine(final MachineDom dom) {
		refinedMachines.add(dom);
	}

	synchronized void addSeenContext(final ContextDom dom) {
		seenContexts.add(dom);
	}

	public synchronized EventBObject getEventBElement() {
		return machine;
	}

	public void setMachine(final Machine machine) {
		this.machine = machine;
	}
}
