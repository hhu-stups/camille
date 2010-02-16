/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texttools;

@SuppressWarnings("serial")
public class ParseException extends Exception {

	private final int line;
	private final int position;
	private final int tokenLength;

	public ParseException(final String message, final int line,
			final int position, final int tokenLength) {
		super(message);
		this.line = line;
		this.position = position;
		this.tokenLength = tokenLength;
	}

	public int getLine() {
		return line;
	}

	public int getPosition() {
		return position;
	}

	public int getTokenLength() {
		return tokenLength;
	}
}
