/**
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen,
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html)
 * */

package org.eventb.texttools;

import de.be4.eventb.core.parser.BException;
import de.be4.eventb.core.parser.EventBParser;
import de.be4.eventb.core.parser.node.AContextParseUnit;
import de.be4.eventb.core.parser.node.AMachineParseUnit;
import de.be4.eventb.core.parser.node.PParseUnit;
import de.be4.eventb.core.parser.node.Start;
import de.be4.eventb.core.parser.node.Token;

/**
 * Helper for the name in the header of a machine/context text
 * (<code>machine m0</code> resp. <code>context c0</code>).
 * <p>
 * In Rodin the name of a component is always defined by its file name. If the
 * name in the text differs (e.g. because the text was copied from another
 * file, or the file was renamed in Rodin), the text has to be adapted.
 * Otherwise the text and the Rodin database cannot be matched and changes
 * get lost.
 * </p>
 */
public final class ComponentNameUtil {

	public enum Kind {
		MACHINE, CONTEXT
	}

	/**
	 * Position of the component name in a text.
	 */
	public static final class NameInfo {
		public final Kind kind;
		public final String name;
		public final int offset;

		NameInfo(final Kind kind, final String name, final int offset) {
			this.kind = kind;
			this.name = name;
			this.offset = offset;
		}

		public int getLength() {
			return name.length();
		}
	}

	private ComponentNameUtil() {
		// utility class
	}

	/**
	 * Parses the given text and returns kind, name and position of the
	 * component header.
	 *
	 * @param text
	 * @return the found information or <code>null</code> if the text cannot
	 *         be parsed
	 */
	public static NameInfo findComponentName(final String text) {
		if (text == null) {
			return null;
		}

		final Start start;
		try {
			start = new EventBParser().parse(text, false);
		} catch (final BException e) {
			return null;
		} catch (final RuntimeException e) {
			return null;
		}

		final PParseUnit unit = start.getPParseUnit();
		Token nameToken = null;
		Kind kind = null;
		if (unit instanceof AMachineParseUnit) {
			nameToken = ((AMachineParseUnit) unit).getName();
			kind = Kind.MACHINE;
		} else if (unit instanceof AContextParseUnit) {
			nameToken = ((AContextParseUnit) unit).getName();
			kind = Kind.CONTEXT;
		}

		if (nameToken == null) {
			return null;
		}

		final int offset = toOffset(text, nameToken.getLine(),
				nameToken.getPos());
		final String name = nameToken.getText();
		if (offset < 0 || !text.startsWith(name, offset)) {
			return null;
		}

		return new NameInfo(kind, name, offset);
	}

	/**
	 * Replaces the component name in the given text.
	 *
	 * @return the new text, or the unchanged text if no name could be found
	 */
	public static String replaceComponentName(final String text,
			final NameInfo info, final String newName) {
		if (info == null || newName == null || newName.equals(info.name)) {
			return text;
		}

		return text.substring(0, info.offset) + newName
				+ text.substring(info.offset + info.getLength());
	}

	/**
	 * Returns the component kind that matches a Rodin file extension
	 * (<code>bum</code> or <code>buc</code>), or <code>null</code>.
	 */
	public static Kind kindForFileExtension(final String extension) {
		if ("bum".equals(extension)) {
			return Kind.MACHINE;
		}
		if ("buc".equals(extension)) {
			return Kind.CONTEXT;
		}
		return null;
	}

	/**
	 * Converts a 1-based line/column position (as used by the parser) into an
	 * offset. "\r\n", "\r" and "\n" are each treated as one line break.
	 */
	static int toOffset(final String text, final int line, final int column) {
		if (line < 1 || column < 1) {
			return -1;
		}

		int currentLine = 1;
		int i = 0;
		while (currentLine < line && i < text.length()) {
			final char c = text.charAt(i);
			if (c == '\r') {
				if (i + 1 < text.length() && text.charAt(i + 1) == '\n') {
					i++;
				}
				currentLine++;
			} else if (c == '\n') {
				currentLine++;
			}
			i++;
		}

		if (currentLine != line) {
			return -1;
		}

		final int offset = i + column - 1;
		return offset <= text.length() ? offset : -1;
	}
}
