/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texttools.prettyprint;

public interface PrettyPrintConstants {
	public static final String PROP_INDENT_DEPTH = "indentation.depth";
	public static final String PROP_USE_TABS_FOR_INDENTATION = "use.tabs.for.indentation";
	public static final String PROP_TAB_WIDTH = "tab.width";
	public static final String PROP_INDENT_EVENTS = "indent.events";
	public static final String PROP_NEWLINE_BETWEEN_CLAUSES = "newline.between.clauses";
	public static final String PROP_NEWLINE_BETWEEN_EVENTS = "newline.between.events";
	public static final String PROP_PRINT_ORDINARY_KEYWORD = "print.ordinary.keyword";

	public static final char SPACE = ' ';
	public static final char TAB = '\t';
	public static final char AT = '@';
	public static final String COMMENT_SINGLELINE_BEGIN = "// ";
	public static final String COMMENT_MULTILINE_BEGIN = "/* ";
	public static final String COMMENT_MULTILINE_END = " */";
}
