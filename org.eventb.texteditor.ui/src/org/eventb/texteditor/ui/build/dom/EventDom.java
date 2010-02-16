/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.build.dom;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eventb.emf.core.EventBObject;
import org.eventb.emf.core.machine.Event;
import org.eventb.emf.core.machine.Parameter;

public class EventDom extends AbstractDom {
	private final Event event;

	private final Map<String, Event> refinedEvents = new HashMap<String, Event>();
	private final Map<String, Parameter> parameters = new HashMap<String, Parameter>();

	public EventDom(final Event event, final MachineDom parent) {
		super(Type.Event, parent);
		this.event = event;
	}

	@Override
	protected synchronized Set<String> doGetIdentifiers() {
		final Set<String> result = new HashSet<String>();

		result.addAll(getParameters().keySet());

		return result;
	}

	@Override
	protected IdentifierType doGetIdentifierType(final String identifier) {
		if (parameters.containsKey(identifier)) {
			return IdentifierType.Parameter;
		}

		return null;
	}

	public EventBObject getEventBElement() {
		return event;
	}

	public Map<String, Parameter> getParameters() {
		return parameters;
	}

	public void addAllParameters(final List<Parameter> params) {
		for (final Parameter parameter : params) {
			parameters.put(parameter.getName(), parameter);
		}
	}

	public Map<String, Event> getRefinedEvents() {
		return refinedEvents;
	}

	public void addAllRefinedEvent(final List<Event> events) {
		for (final Event evt : events) {
			refinedEvents.put(evt.getName(), evt);
		}
	}
}
