/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.build.dom;

import org.eclipse.ui.IEditorInput;
import org.eventb.texteditor.ui.build.dom.DomManager.ParseResult;

public interface IParseResultListener {
	/**
	 * Method will be called when a new {@link ParseResult} is stored in the
	 * {@link DomManager} for a certain {@link IEditorInput}. Implementing
	 * listeners are asked to update according to the new result.
	 * 
	 * @param parseResult
	 *            The new {@link ParseResult} or <code>null</code>.
	 */
	public void parseResultChanged(ParseResult parseResult);
}
