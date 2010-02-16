/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.reconciler;

import org.eclipse.jface.text.rules.IWordDetector;

public class IdentifierDetector implements IWordDetector {
	public boolean isWordStart(final char c) {
		return Character.isJavaIdentifierStart(c);
	}

	public boolean isWordPart(final char c) {
		return Character.isJavaIdentifierPart(c) || c == '\'';
	}
}
