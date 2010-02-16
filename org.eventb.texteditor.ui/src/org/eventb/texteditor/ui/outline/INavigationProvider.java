/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.outline;

import org.eventb.emf.core.EventBObject;
import org.eventb.texttools.model.texttools.TextRange;

public interface INavigationProvider {
	public TextRange getHighlightRange(final EventBObject element);
}
