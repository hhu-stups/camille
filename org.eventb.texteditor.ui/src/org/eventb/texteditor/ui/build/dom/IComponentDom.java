/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.build.dom;

import java.util.Set;

import org.eclipse.emf.ecore.resource.Resource;

public interface IComponentDom extends IDom {

	public void reset();

	public void resetAndinit();

	public Resource getResource();

	public Set<IComponentDom> getReferencedDoms(final boolean transitive);
}
