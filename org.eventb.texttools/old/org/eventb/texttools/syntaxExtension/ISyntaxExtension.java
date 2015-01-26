package org.eventb.texttools.syntaxExtension;

import org.eventb.emf.core.EventBObject;

public interface ISyntaxExtension {
	
	void setConcreteKeyword(String keyword, String extensionId);
	
	/**
	 * 
	 * @param extensionId The extension-id this block is bound to
	 * @param text The text that has to be parsed
	 * @param root The Event-B root element i.e. machine or context
	 * @param element The surrounding Event-B element e.g. an event
	 */
	void parse(String extensionId, String text, EventBObject root, EventBObject element);
	
}
