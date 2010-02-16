/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.build.dom;

public interface IDomChangeListener {
	/**
	 * Method will be called when a new {@link IDom} is stored in the
	 * {@link DomManager} for a certain AST. Implementing listeners are asked to
	 * update according to the new result.
	 * 
	 * @param dom
	 *            The new {@link IDom} or <code>null</code>.
	 */
	public void domChanged(final IComponentDom changedDom);
}
