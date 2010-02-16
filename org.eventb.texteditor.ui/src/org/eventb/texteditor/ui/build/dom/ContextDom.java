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
import org.eventb.emf.core.context.Context;

public class ContextDom extends AbstractComponentDom {
	private Context context;
	private final Map<String, Constant> constants = new HashMap<String, Constant>();
	private final Map<String, CarrierSet> sets = new HashMap<String, CarrierSet>();

	private final List<ContextDom> extendedContexts = new LinkedList<ContextDom>();

	ContextDom(final Resource resource, final Context context) {
		super(Type.Context, null, resource);
		this.context = context;
	}

	@Override
	protected synchronized void doReset() {
		constants.clear();
		sets.clear();
		extendedContexts.clear();
	}

	@Override
	protected synchronized Set<String> doGetIdentifiers() {
		final Set<String> result = new HashSet<String>();

		result.addAll(getConstants(true).keySet());
		result.addAll(getSets(true).keySet());

		return result;
	}

	@Override
	protected synchronized IdentifierType doGetIdentifierType(
			final String identifier) {
		if (getConstants(true).containsKey(identifier)) {
			return IdentifierType.Constant;
		}

		if (getSets(true).containsKey(identifier)) {
			return IdentifierType.Set;
		}

		return null;
	}

	public Set<IComponentDom> getReferencedDoms(final boolean transitive) {
		final List<ContextDom> contexts = getExtendedContexts();
		final HashSet<IComponentDom> result = new HashSet<IComponentDom>(
				contexts);

		if (transitive) {
			for (final IComponentDom referencedDom : contexts) {
				result.addAll(referencedDom.getReferencedDoms(true));
			}
		}

		return result;
	}

	public synchronized List<ContextDom> getExtendedContexts() {
		checkInitialization();
		return extendedContexts;
	}

	public synchronized Map<String, Constant> getConstants(
			final boolean includeInherited) {
		checkInitialization();

		final Map<String, Constant> result = new HashMap<String, Constant>();

		if (includeInherited) {
			// add referenced context's constants
			for (final ContextDom dom : getExtendedContexts()) {
				result.putAll(dom.getConstants(includeInherited));
			}
		}
		// finally the local constants
		result.putAll(constants);

		return result;
	}

	public synchronized Map<String, CarrierSet> getSets(
			final boolean includeInherited) {
		checkInitialization();

		final Map<String, CarrierSet> result = new HashMap<String, CarrierSet>();

		if (includeInherited) {
			// add referenced context's constants
			for (final ContextDom dom : getExtendedContexts()) {
				result.putAll(dom.getSets(includeInherited));
			}
		}

		// finally the local sets
		result.putAll(sets);

		return result;
	}

	synchronized void addExtendedContext(final ContextDom dom) {
		extendedContexts.add(dom);
	}

	synchronized void addConstant(final Constant constant) {
		constants.put(constant.getName(), constant);
	}

	synchronized void addSet(final CarrierSet set) {
		sets.put(set.getName(), set);
	}

	public synchronized EventBObject getEventBElement() {
		return context;
	}

	public void setContext(final Context context) {
		this.context = context;
	}
}
