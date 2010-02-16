/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.build.dom;

import java.util.List;
import java.util.Set;

import org.eventb.emf.core.EventBObject;

public interface IDom {
	public enum Type {
		Machine, Context, Event, Formula, Unknown
	};

	public enum IdentifierType {
		GlobalVariable, Parameter, Constant, Set, LocalVariable
	};

	public Type getType();

	public EventBObject getEventBElement();

	public IDom getParent();

	public void addChild(final IDom child);

	public List<IDom> getChildren();

	public IDom getScopingDom(final int offset);

	public Set<String> getIdentifiers();

	public IdentifierType getIdentifierType(String identifier);
}
