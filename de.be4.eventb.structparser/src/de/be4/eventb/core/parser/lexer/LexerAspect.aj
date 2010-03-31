package de.be4.eventb.core.parser.lexer;

import de.be4.eventb.core.parser.EventBLexerException;
import de.be4.eventb.core.parser.node.Token;
import de.be4.eventb.core.parser.node.TComment;

public privileged aspect LexerAspect pertarget(peek(Lexer)) {

	pointcut peek(Lexer lexer) : execution(Token peek()) && target(lexer);
	
	after(Lexer lexer) throwing(LexerException e) throws LexerException : peek(lexer) {
		// if exception is already converted just throw it 
		if (e instanceof EventBLexerException){
			throw e;
		}
		
		// otherwise convert exception to contain last position and text
		final int line = lexer.line;
		final int pos = lexer.pos;
		final StringBuffer text = lexer.text;
		
		throw new EventBLexerException(lexer.token, e.getMessage(), text.toString(), line, pos);
	}
}
