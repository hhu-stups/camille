/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.reconciler.partitioning;

import org.eclipse.jface.text.rules.IWordDetector;

public class KeywordDetector implements IWordDetector {
	public boolean isWordStart(final char c) {
		return Character.isLetter(c) || c == '\u2115' || c == '\u2119'
				|| c == '\u2124';
	}

	public boolean isWordPart(final char c) {
		return Character.isLetterOrDigit(c) || c == '\u0031';
	}
}
