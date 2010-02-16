/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.editor.codecompletion;

import org.eclipse.jface.text.contentassist.ICompletionProposal;

public interface IRelevantProposal extends ICompletionProposal {
	public int getRelevance();
}
