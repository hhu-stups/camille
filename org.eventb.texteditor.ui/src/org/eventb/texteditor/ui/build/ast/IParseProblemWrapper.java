/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.build.ast;

public interface IParseProblemWrapper {

	public abstract int getColumn();

	public abstract int getLine();

	public abstract int getOffset();

	public abstract int getTokenLength();

	public abstract String getToken();

	public abstract String getMessage();

}