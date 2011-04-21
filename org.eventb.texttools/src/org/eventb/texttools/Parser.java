/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texttools;

import org.eclipse.jface.text.IDocument;
import org.eventb.emf.core.EventBObject;
import org.eventb.texttools.internal.parsing.TransformationVisitor;

import de.be4.eventb.core.parser.BException;
import de.be4.eventb.core.parser.EventBLexerException;
import de.be4.eventb.core.parser.EventBParseException;
import de.be4.eventb.core.parser.EventBParser;
import de.be4.eventb.core.parser.node.Start;
import de.be4.eventb.core.parser.node.Token;
import de.be4.eventb.core.parser.parser.ParserException;
import de.hhu.stups.sablecc.patch.SourcePositions;
import de.hhu.stups.sablecc.patch.SourcecodeRange;

public class Parser {

	private final Boolean allowSynatxExtensions = false;

	private final EventBParser parser = new EventBParser();
	private final TransformationVisitor transformer = new TransformationVisitor();

	/**
	 * Parses the content of the given {@link IDocument}.
	 * 
	 * @param <T>
	 * @param document
	 * @return
	 * @throws ParseException
	 *             If the contents cannot be parsed
	 * @throws IllegalArgumentException
	 *             If called with <code>null</code> as document parameter
	 */
	public <T extends EventBObject> T parse(final IDocument document)
			throws ParseException {

		if (document == null) {
			throw new IllegalArgumentException(
					"Parser may not be called without input document");
		}

		String input;

		input = document.get();

		try {
			final Start rootNode = parser.parse(input, false);
			T transform = transformer.<T> transform(rootNode, document);

			return transform;
		} catch (final BException e) {
			final Exception cause = e.getCause();

			if (cause instanceof ParserException) {
				final ParserException ex = (ParserException) cause;
				final Token token = ex.getToken();

				throw new ParseException(
						adjustMessage(ex.getLocalizedMessage()),
						token.getLine() - 1, token.getPos() - 1, token
								.getText().length());
			}
			if (cause instanceof EventBLexerException) {
				final EventBLexerException ex = (EventBLexerException) cause;
				final String lastText = ex.getLastText();

				throw new ParseException(
						adjustMessage(ex.getLocalizedMessage()),
						ex.getLastLine() - 1, ex.getLastPos() - 1,
						lastText.length());
			}

			if (cause instanceof EventBParseException) {
				final EventBParseException ex = (EventBParseException) cause;
				final SourcecodeRange range = ex.getRange();
				final SourcePositions positions = parser.getSourcePositions();

				if (range != null && positions != null) {
					throw new ParseException(
							adjustMessage(ex.getLocalizedMessage()),
							positions.getBeginLine(range) - 1,
							positions.getBeginColumn(range) - 1, positions
									.getRangeString(range).length());
				} else {
					final Token token = ex.getToken();
					if (token != null) {
						throw new ParseException(
								adjustMessage(ex.getLocalizedMessage()),
								token.getLine() - 1, token.getPos() - 1, token
										.getText().length());
					}
				}
			}

			throw new ParseException(e.getLocalizedMessage(), 0, 0, 1);
		}
	}

	private String adjustMessage(final String localizedMessage) {
		final StringBuilder result = new StringBuilder(localizedMessage);

		// remove position information if found
		final int posEnd = result.indexOf("] ");
		if (result.charAt(0) == '[' && posEnd > 0) {
			result.delete(0, posEnd + 2);
		}

		// make sure first character is upercase
		result.setCharAt(0, Character.toUpperCase(result.charAt(0)));

		return result.toString().trim();
	}
}
